package com.xinzhe.projectmentor.auth.service;

import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class RegistrationRateLimitService {

    static final int HOURLY_LIMIT = 3;

    static final int DAILY_LIMIT = 10;

    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final StringRedisTemplate stringRedisTemplate;

    private final Clock clock;

    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    private final AtomicBoolean redisWarningLogged = new AtomicBoolean(false);

    @Autowired
    public RegistrationRateLimitService(StringRedisTemplate stringRedisTemplate) {
        this(stringRedisTemplate, Clock.systemDefaultZone());
    }

    RegistrationRateLimitService(StringRedisTemplate stringRedisTemplate, Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.clock = clock;
    }

    public void checkAllowed(String clientIp) {
        WindowKeys keys = buildWindowKeys(clientIp);

        if (localCount(keys.hourKey(), keys.hourExpiresAt()) >= HOURLY_LIMIT
                || localCount(keys.dayKey(), keys.dayExpiresAt()) >= DAILY_LIMIT) {
            reject();
        }

        try {
            if (redisCount(keys.hourKey()) >= HOURLY_LIMIT
                    || redisCount(keys.dayKey()) >= DAILY_LIMIT) {
                reject();
            }
            redisWarningLogged.set(false);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logRedisFallback();
        }
    }

    public void recordSuccessfulRegistration(String clientIp) {
        WindowKeys keys = buildWindowKeys(clientIp);
        incrementLocal(keys.hourKey(), keys.hourExpiresAt());
        incrementLocal(keys.dayKey(), keys.dayExpiresAt());

        try {
            incrementRedis(keys.hourKey(), keys.hourTtl());
            incrementRedis(keys.dayKey(), keys.dayTtl());
            redisWarningLogged.set(false);
        } catch (Exception e) {
            logRedisFallback();
        }

        if (localCounters.size() > 2048) {
            cleanupExpiredCounters();
        }
    }

    private WindowKeys buildWindowKeys(String clientIp) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        ZonedDateTime nextDay = now.plusDays(1).toLocalDate().atStartOfDay(now.getZone());
        String normalizedIp = clientIp == null || clientIp.isBlank() ? "unknown" : clientIp;

        return new WindowKeys(
                "register:ip:hour:" + normalizedIp + ":" + HOUR_FORMAT.format(now),
                "register:ip:day:" + normalizedIp + ":" + DAY_FORMAT.format(now),
                nextHour.toInstant().toEpochMilli(),
                nextDay.toInstant().toEpochMilli(),
                Duration.between(now, nextHour).plusMinutes(5),
                Duration.between(now, nextDay).plusMinutes(5)
        );
    }

    private long redisCount(String key) {
        String value = stringRedisTemplate.opsForValue().get(key);
        if (value == null || value.isBlank()) {
            return 0L;
        }
        return Long.parseLong(value);
    }

    private void incrementRedis(String key, Duration ttl) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, ttl);
        }
    }

    private int localCount(String key, long expiresAt) {
        LocalCounter counter = localCounters.get(key);
        if (counter == null) {
            return 0;
        }
        if (counter.expiresAt() <= clock.millis()) {
            localCounters.remove(key, counter);
            return 0;
        }
        return counter.count();
    }

    private void incrementLocal(String key, long expiresAt) {
        localCounters.compute(key, (ignored, counter) -> {
            if (counter == null || counter.expiresAt() <= clock.millis()) {
                return new LocalCounter(1, expiresAt);
            }
            return new LocalCounter(counter.count() + 1, expiresAt);
        });
    }

    private void cleanupExpiredCounters() {
        long now = clock.millis();
        localCounters.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    private void reject() {
        throw new BusinessException(ErrorCode.PARAM_ERROR, "注册过于频繁，请稍后再试");
    }

    private void logRedisFallback() {
        if (redisWarningLogged.compareAndSet(false, true)) {
            log.warn("Registration rate-limit Redis unavailable; using in-memory fallback");
        }
    }

    private record LocalCounter(int count, long expiresAt) {
    }

    private record WindowKeys(String hourKey,
                              String dayKey,
                              long hourExpiresAt,
                              long dayExpiresAt,
                              Duration hourTtl,
                              Duration dayTtl) {
    }
}
