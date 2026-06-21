package com.xinzhe.projectmentor.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.config.EmailVerificationProperties;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
public class EmailVerificationService {

    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final StringRedisTemplate stringRedisTemplate;

    private final JavaMailSender javaMailSender;

    private final EmailVerificationProperties properties;

    private final UserMapper userMapper;

    private final SecureRandom secureRandom;

    private final Clock clock;

    private final Map<String, LocalCode> localCodes = new ConcurrentHashMap<>();

    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    private final AtomicBoolean redisWarningLogged = new AtomicBoolean(false);

    @Autowired
    public EmailVerificationService(StringRedisTemplate stringRedisTemplate,
                                    JavaMailSender javaMailSender,
                                    EmailVerificationProperties properties,
                                    UserMapper userMapper) {
        this(stringRedisTemplate, javaMailSender, properties, userMapper, new SecureRandom(), Clock.systemDefaultZone());
    }

    EmailVerificationService(StringRedisTemplate stringRedisTemplate,
                             JavaMailSender javaMailSender,
                             EmailVerificationProperties properties,
                             UserMapper userMapper,
                             SecureRandom secureRandom,
                             Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.javaMailSender = javaMailSender;
        this.properties = properties;
        this.userMapper = userMapper;
        this.secureRandom = secureRandom;
        this.clock = clock;
    }

    public void sendRegisterCode(String email, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        ensureEmailNotRegistered(normalizedEmail);

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        Duration ttl = Duration.ofMinutes(properties.getCodeTtlMinutes());

        recordSendAttempt(normalizedEmail, normalizeIp(clientIp));
        storeCode(normalizedEmail, code, ttl);
        sendMail(normalizedEmail, code);
    }

    public void verifyRegisterCode(String email, String verificationCode) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        String submittedCode = verificationCode == null ? "" : verificationCode.trim();

        if (!submittedCode.matches("\\d{6}")) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱验证码不能为空或格式不正确");
        }

        String expectedCode = readCode(normalizedEmail);

        if (expectedCode == null || expectedCode.isBlank()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱验证码已过期，请重新获取");
        }

        if (!expectedCode.equals(submittedCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱验证码错误");
        }

        deleteCode(normalizedEmail);
    }

    private void ensureEmailNotRegistered(String email) {
        User emailUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, email)
                        .last("LIMIT 1")
        );

        if (emailUser != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱已被注册");
        }
    }

    private void recordSendAttempt(String email, String clientIp) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        Duration hourTtl = Duration.between(now, nextHour).plusMinutes(5);

        String hour = HOUR_FORMAT.format(now);
        String cooldownKey = "email-code:cooldown:" + email;
        String emailHourKey = "email-code:email:hour:" + email + ":" + hour;
        String ipHourKey = "email-code:ip:hour:" + clientIp + ":" + hour;

        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码发送过于频繁，请稍后再试");
            }

            incrementRedisLimit(emailHourKey, properties.getHourlyLimitPerEmail(), hourTtl, "该邮箱验证码发送次数过多，请稍后再试");
            incrementRedisLimit(ipHourKey, properties.getHourlyLimitPerIp(), hourTtl, "验证码请求过于频繁，请稍后再试");
            stringRedisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(properties.getSendCooldownSeconds()));
            redisWarningLogged.set(false);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logRedisFallback();
            long expiresAt = nextHour.toInstant().toEpochMilli();
            incrementLocalLimit("local:" + emailHourKey, properties.getHourlyLimitPerEmail(), expiresAt, "该邮箱验证码发送次数过多，请稍后再试");
            incrementLocalLimit("local:" + ipHourKey, properties.getHourlyLimitPerIp(), expiresAt, "验证码请求过于频繁，请稍后再试");
            incrementLocalLimit("local:" + cooldownKey, 1, clock.millis() + properties.getSendCooldownSeconds() * 1000L, "验证码发送过于频繁，请稍后再试");
        }

        if (localCounters.size() > 4096) {
            cleanupExpiredCounters();
        }
    }

    private void incrementRedisLimit(String key, int limit, Duration ttl, String message) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, ttl);
        }
        if (count != null && count > limit) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
    }

    private void incrementLocalLimit(String key, int limit, long expiresAt, String message) {
        LocalCounter counter = localCounters.get(key);
        if (counter == null || counter.expiresAt() <= clock.millis()) {
            counter = new LocalCounter(0, expiresAt);
        }
        int nextCount = counter.count() + 1;
        if (nextCount > limit) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, message);
        }
        localCounters.put(key, new LocalCounter(nextCount, expiresAt));
    }

    private void storeCode(String email, String code, Duration ttl) {
        long expiresAt = clock.millis() + ttl.toMillis();
        localCodes.put(email, new LocalCode(code, expiresAt));

        try {
            stringRedisTemplate.opsForValue().set(codeKey(email), code, ttl);
            redisWarningLogged.set(false);
        } catch (Exception e) {
            logRedisFallback();
        }
    }

    private String readCode(String email) {
        try {
            String code = stringRedisTemplate.opsForValue().get(codeKey(email));
            if (code != null && !code.isBlank()) {
                redisWarningLogged.set(false);
                return code;
            }
        } catch (Exception e) {
            logRedisFallback();
        }

        LocalCode localCode = localCodes.get(email);
        if (localCode == null || localCode.expiresAt() <= clock.millis()) {
            localCodes.remove(email);
            return null;
        }
        return localCode.code();
    }

    private void deleteCode(String email) {
        localCodes.remove(email);
        try {
            stringRedisTemplate.delete(codeKey(email));
            redisWarningLogged.set(false);
        } catch (Exception e) {
            logRedisFallback();
        }
    }

    private void sendMail(String email, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            if (properties.getFrom() != null && !properties.getFrom().isBlank()) {
                message.setFrom(properties.getFrom());
            }
            message.setTo(email);
            message.setSubject(properties.getSubject());
            message.setText("""
                    你的 ProjectMentor AI 注册验证码是：%s

                    验证码 %d 分钟内有效，请勿转发给他人。
                    如果这不是你本人操作，可以忽略这封邮件。
                    """.formatted(code, properties.getCodeTtlMinutes()));

            javaMailSender.send(message);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "验证码发送失败，请稍后再试");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeIp(String clientIp) {
        return clientIp == null || clientIp.isBlank() ? "unknown" : clientIp.trim();
    }

    private String codeKey(String email) {
        return "email-code:register:" + email;
    }

    private void cleanupExpiredCounters() {
        long now = clock.millis();
        localCounters.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    private void logRedisFallback() {
        if (redisWarningLogged.compareAndSet(false, true)) {
            log.warn("Email verification Redis unavailable; using in-memory fallback");
        }
    }

    private record LocalCode(String code, long expiresAt) {
    }

    private record LocalCounter(int count, long expiresAt) {
    }
}
