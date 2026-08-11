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
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

@Slf4j
@Service
public class EmailVerificationService {

    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private static final Pattern VERIFICATION_CODE_PATTERN = Pattern.compile("\\d{6}");

    private static final String VERIFY_FAIL_KEY_PREFIX = "email-code:register:verify-fail:";

    private static final DefaultRedisScript<Long> INCREMENT_VERIFY_FAILURE_SCRIPT = new DefaultRedisScript<>("""
            local count = redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
            if count == 1 then
                redis.call('PEXPIRE', KEYS[1], ARGV[2])
            end
            return count
            """, Long.class);

    private static final DefaultRedisScript<Long> DELETE_IF_VALUE_MATCHES_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('GET', KEYS[1]) == ARGV[1] then
                return redis.call('DEL', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate stringRedisTemplate;

    private final JavaMailSender javaMailSender;

    private final EmailVerificationProperties properties;

    private final UserMapper userMapper;

    private final SecureRandom secureRandom;

    private final Clock clock;

    private final Map<String, LocalCode> localCodes = new ConcurrentHashMap<>();

    private final Map<InvalidatedCodeKey, Long> localInvalidatedCodes = new ConcurrentHashMap<>();

    private final Map<String, LocalCounter> localCounters = new ConcurrentHashMap<>();

    private final Map<String, LocalCooldown> localCooldowns = new ConcurrentHashMap<>();

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
        String normalizedIp = normalizeIp(clientIp);
        ensureEmailNotRegistered(normalizedEmail);

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        String sendAttemptId = Long.toUnsignedString(secureRandom.nextLong(), 36);
        CodeValue codeValue = new CodeValue(
                code,
                sendAttemptId,
                "v1:" + sendAttemptId + ":" + code
        );
        Duration ttl = Duration.ofMinutes(properties.getCodeTtlMinutes());

        recordSendAttempt(normalizedEmail, normalizedIp, sendAttemptId);
        storeCode(normalizedEmail, codeValue, ttl);
        try {
            sendMail(normalizedEmail, code);
            clearAllVerifyFailures(normalizedEmail);
        } catch (BusinessException e) {
            cleanupFailedSend(normalizedEmail, codeValue, sendAttemptId);
            throw e;
        }
    }

    public void validateRegisterCode(String email, String verificationCode, String clientIp) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        String normalizedIp = normalizeIp(clientIp);
        String submittedCode = verificationCode == null ? "" : verificationCode.trim();
        CodeValue expectedCode = readCode(normalizedEmail);

        if (!VERIFICATION_CODE_PATTERN.matcher(submittedCode).matches()) {
            if (expectedCode != null) {
                ensureVerifyAttemptsAvailable(normalizedEmail, normalizedIp, expectedCode);
                rejectVerificationAttempt(
                        normalizedEmail,
                        normalizedIp,
                        expectedCode,
                        "邮箱验证码不能为空或格式不正确"
                );
            }
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱验证码不能为空或格式不正确");
        }

        if (expectedCode == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱验证码已过期，请重新获取");
        }

        ensureVerifyAttemptsAvailable(normalizedEmail, normalizedIp, expectedCode);

        if (!expectedCode.code().equals(submittedCode)) {
            rejectVerificationAttempt(normalizedEmail, normalizedIp, expectedCode, "邮箱验证码错误");
        }

        clearVerifyFailure(normalizedEmail, normalizedIp, expectedCode.generation());
    }

    public void consumeRegisterCode(String email) {
        if (!properties.isEnabled()) {
            return;
        }

        String normalizedEmail = normalizeEmail(email);
        boolean cleanupFailed = false;

        try {
            localCodes.remove(normalizedEmail);
            localInvalidatedCodes.keySet().removeIf(key -> key.email().equals(normalizedEmail));
            clearLocalVerifyFailures(normalizedEmail);
        } catch (Exception e) {
            cleanupFailed = true;
        }

        try {
            stringRedisTemplate.delete(codeKey(normalizedEmail));
            redisWarningLogged.set(false);
        } catch (Exception e) {
            cleanupFailed = true;
        }

        try {
            stringRedisTemplate.delete(verifyFailKey(normalizedEmail));
            redisWarningLogged.set(false);
        } catch (Exception e) {
            cleanupFailed = true;
        }

        if (cleanupFailed) {
            log.warn("Email verification cleanup failed after successful registration");
        }
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

    private void recordSendAttempt(String email, String clientIp, String sendAttemptId) {
        ZonedDateTime now = ZonedDateTime.now(clock);
        ZonedDateTime nextHour = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);
        Duration hourTtl = Duration.between(now, nextHour).plusMinutes(5);

        String hour = HOUR_FORMAT.format(now);
        String cooldownKey = cooldownKey(email);
        String emailHourKey = "email-code:email:hour:" + email + ":" + hour;
        String ipHourKey = "email-code:ip:hour:" + clientIp + ":" + hour;

        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(cooldownKey))) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码发送过于频繁，请稍后再试");
            }

            incrementRedisLimit(emailHourKey, properties.getHourlyLimitPerEmail(), hourTtl, "该邮箱验证码发送次数过多，请稍后再试");
            incrementRedisLimit(ipHourKey, properties.getHourlyLimitPerIp(), hourTtl, "验证码请求过于频繁，请稍后再试");
            Boolean cooldownAcquired = stringRedisTemplate.opsForValue().setIfAbsent(
                    cooldownKey,
                    sendAttemptId,
                    Duration.ofSeconds(properties.getSendCooldownSeconds())
            );
            if (!Boolean.TRUE.equals(cooldownAcquired)) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码发送过于频繁，请稍后再试");
            }
            redisWarningLogged.set(false);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logRedisFallback();
            long expiresAt = nextHour.toInstant().toEpochMilli();
            incrementLocalLimit("local:" + emailHourKey, properties.getHourlyLimitPerEmail(), expiresAt, "该邮箱验证码发送次数过多，请稍后再试");
            incrementLocalLimit("local:" + ipHourKey, properties.getHourlyLimitPerIp(), expiresAt, "验证码请求过于频繁，请稍后再试");
            recordLocalCooldown(cooldownKey, sendAttemptId);
        }

        if (localCounters.size() > 4096
                || localCodes.size() > 4096
                || localInvalidatedCodes.size() > 4096
                || localCooldowns.size() > 4096) {
            cleanupExpiredLocalState();
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

    private void recordLocalCooldown(String key, String sendAttemptId) {
        long expiresAt = clock.millis() + properties.getSendCooldownSeconds() * 1000L;
        localCooldowns.compute(key, (ignored, cooldown) -> {
            if (cooldown != null && cooldown.expiresAt() > clock.millis()) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "验证码发送过于频繁，请稍后再试");
            }
            return new LocalCooldown(sendAttemptId, expiresAt);
        });
    }

    private void ensureVerifyAttemptsAvailable(String email, String clientIp, CodeValue expectedCode) {
        if (readVerifyFailureCount(email, clientIp, expectedCode.generation()) >= maxVerifyAttempts()) {
            invalidateCode(email, expectedCode);
            throw tooManyVerifyAttempts();
        }
    }

    private void rejectVerificationAttempt(String email,
                                           String clientIp,
                                           CodeValue expectedCode,
                                           String message) {
        if (incrementVerifyFailure(email, clientIp, expectedCode.generation()) >= maxVerifyAttempts()) {
            invalidateCode(email, expectedCode);
            throw tooManyVerifyAttempts();
        }
        throw new BusinessException(ErrorCode.PARAM_ERROR, message);
    }

    private int incrementVerifyFailure(String email, String clientIp, String generation) {
        Duration ttl = Duration.ofMinutes(properties.getCodeTtlMinutes());
        String field = verifyFailField(clientIp, generation);
        String localKey = localVerifyFailKey(email, field);
        int localCount = incrementLocalCounter(localKey, clock.millis() + ttl.toMillis());
        int redisCount = 0;

        try {
            Long count = stringRedisTemplate.execute(
                    INCREMENT_VERIFY_FAILURE_SCRIPT,
                    List.of(verifyFailKey(email)),
                    field,
                    Long.toString(ttl.toMillis())
            );
            if (count != null) {
                redisCount = count > Integer.MAX_VALUE ? Integer.MAX_VALUE : count.intValue();
            }
            redisWarningLogged.set(false);
        } catch (Exception e) {
            logRedisFallback();
        }

        if (localCounters.size() > 4096
                || localCodes.size() > 4096
                || localInvalidatedCodes.size() > 4096
                || localCooldowns.size() > 4096) {
            cleanupExpiredLocalState();
        }
        return Math.max(localCount, redisCount);
    }

    private int readVerifyFailureCount(String email, String clientIp, String generation) {
        String field = verifyFailField(clientIp, generation);
        int localCount = localCounterValue(localVerifyFailKey(email, field));

        try {
            Object value = stringRedisTemplate.opsForHash().get(verifyFailKey(email), field);
            redisWarningLogged.set(false);
            if (value == null) {
                return localCount;
            }
            long parsed = Long.parseLong(value.toString());
            int redisCount = parsed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) parsed;
            return Math.max(localCount, redisCount);
        } catch (Exception e) {
            logRedisFallback();
            return localCount;
        }
    }

    private int incrementLocalCounter(String key, long expiresAt) {
        LocalCounter updated = localCounters.compute(key, (ignored, counter) -> {
            int nextCount = 1;
            long effectiveExpiresAt = expiresAt;
            if (counter != null && counter.expiresAt() > clock.millis()) {
                nextCount = counter.count() == Integer.MAX_VALUE
                        ? Integer.MAX_VALUE
                        : counter.count() + 1;
                effectiveExpiresAt = counter.expiresAt();
            }
            return new LocalCounter(nextCount, effectiveExpiresAt);
        });
        return updated.count();
    }

    private int localCounterValue(String key) {
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

    private void clearVerifyFailure(String email, String clientIp, String generation) {
        String field = verifyFailField(clientIp, generation);
        localCounters.remove(localVerifyFailKey(email, field));
        try {
            stringRedisTemplate.opsForHash().delete(verifyFailKey(email), field);
            redisWarningLogged.set(false);
        } catch (Exception e) {
            logRedisFallback();
        }
    }

    private void clearAllVerifyFailures(String email) {
        boolean cleanupFailed = false;
        try {
            clearLocalVerifyFailures(email);
        } catch (Exception e) {
            cleanupFailed = true;
        }
        try {
            stringRedisTemplate.delete(verifyFailKey(email));
            redisWarningLogged.set(false);
        } catch (Exception e) {
            cleanupFailed = true;
        }
        if (cleanupFailed) {
            log.warn("Email verification failure-counter cleanup failed");
        }
    }

    private void clearLocalVerifyFailures(String email) {
        String prefix = localVerifyFailPrefix(email);
        localCounters.keySet().removeIf(key -> key.startsWith(prefix));
    }

    private BusinessException tooManyVerifyAttempts() {
        return new BusinessException(
                ErrorCode.PARAM_ERROR,
                "验证码错误次数过多，请重新获取验证码"
        );
    }

    private int maxVerifyAttempts() {
        return Math.max(1, properties.getMaxVerifyAttempts());
    }

    private void storeCode(String email, CodeValue codeValue, Duration ttl) {
        long expiresAt = clock.millis() + ttl.toMillis();
        localCodes.put(email, new LocalCode(codeValue, expiresAt, false));

        try {
            stringRedisTemplate.opsForValue().set(codeKey(email), codeValue.storedValue(), ttl);
            localCodes.computeIfPresent(
                    email,
                    (ignored, localCode) -> codeValue.storedValue().equals(
                            localCode.codeValue().storedValue()
                    ) ? new LocalCode(localCode.codeValue(), localCode.expiresAt(), true) : localCode
            );
            redisWarningLogged.set(false);
        } catch (Exception e) {
            logRedisFallback();
        }
    }

    private CodeValue readCode(String email) {
        LocalCode localCode = activeLocalCode(email);
        if (localCode != null && !localCode.redisStored()) {
            return isLocallyInvalidated(email, localCode.codeValue()) ? null : localCode.codeValue();
        }

        try {
            String storedValue = stringRedisTemplate.opsForValue().get(codeKey(email));
            if (storedValue != null && !storedValue.isBlank()) {
                CodeValue codeValue = parseCodeValue(storedValue);
                redisWarningLogged.set(false);
                if (codeValue != null) {
                    return isLocallyInvalidated(email, codeValue) ? null : codeValue;
                }
            }
        } catch (Exception e) {
            logRedisFallback();
        }

        return localCode == null || isLocallyInvalidated(email, localCode.codeValue())
                ? null
                : localCode.codeValue();
    }

    private LocalCode activeLocalCode(String email) {
        LocalCode localCode = localCodes.get(email);
        if (localCode != null && localCode.expiresAt() <= clock.millis()) {
            localCodes.remove(email, localCode);
            return null;
        }
        return localCode;
    }

    private CodeValue parseCodeValue(String storedValue) {
        if (storedValue.startsWith("v1:")) {
            int codeSeparator = storedValue.lastIndexOf(':');
            if (codeSeparator > 3 && codeSeparator < storedValue.length() - 1) {
                String generation = storedValue.substring(3, codeSeparator);
                String code = storedValue.substring(codeSeparator + 1);
                if (VERIFICATION_CODE_PATTERN.matcher(code).matches()) {
                    return new CodeValue(code, generation, storedValue);
                }
            }
        }
        if (VERIFICATION_CODE_PATTERN.matcher(storedValue).matches()) {
            return new CodeValue(storedValue, "legacy", storedValue);
        }
        return null;
    }

    private void invalidateCode(String email, CodeValue expectedCode) {
        rememberInvalidatedCode(email, expectedCode);
        localCodes.computeIfPresent(
                email,
                (ignored, localCode) -> expectedCode.storedValue().equals(
                        localCode.codeValue().storedValue()
                ) ? null : localCode
        );
        try {
            stringRedisTemplate.execute(
                    DELETE_IF_VALUE_MATCHES_SCRIPT,
                    List.of(codeKey(email)),
                    expectedCode.storedValue()
            );
            redisWarningLogged.set(false);
        } catch (Exception e) {
            logRedisFallback();
        }
    }

    private boolean isLocallyInvalidated(String email, CodeValue codeValue) {
        InvalidatedCodeKey key = new InvalidatedCodeKey(email, codeValue.storedValue());
        Long expiresAt = localInvalidatedCodes.get(key);
        if (expiresAt == null) {
            return false;
        }
        if (expiresAt <= clock.millis()) {
            localInvalidatedCodes.remove(key, expiresAt);
            return false;
        }
        return true;
    }

    private void rememberInvalidatedCode(String email, CodeValue codeValue) {
        long expiresAt = clock.millis() + Duration.ofMinutes(properties.getCodeTtlMinutes()).toMillis();
        localInvalidatedCodes.put(
                new InvalidatedCodeKey(email, codeValue.storedValue()),
                expiresAt
        );
    }

    private void cleanupFailedSend(String email, CodeValue codeValue, String sendAttemptId) {
        rememberInvalidatedCode(email, codeValue);
        localCodes.computeIfPresent(
                email,
                (ignored, localCode) -> codeValue.storedValue().equals(
                        localCode.codeValue().storedValue()
                ) ? null : localCode
        );
        localCooldowns.computeIfPresent(
                cooldownKey(email),
                (ignored, cooldown) -> sendAttemptId.equals(cooldown.sendAttemptId()) ? null : cooldown
        );

        boolean cleanupFailed = false;
        try {
            stringRedisTemplate.execute(
                    DELETE_IF_VALUE_MATCHES_SCRIPT,
                    List.of(codeKey(email)),
                    codeValue.storedValue()
            );
            redisWarningLogged.set(false);
        } catch (Exception e) {
            cleanupFailed = true;
        }

        try {
            stringRedisTemplate.execute(
                    DELETE_IF_VALUE_MATCHES_SCRIPT,
                    List.of(cooldownKey(email)),
                    sendAttemptId
            );
            redisWarningLogged.set(false);
        } catch (Exception e) {
            cleanupFailed = true;
        }

        if (cleanupFailed) {
            log.warn("Email verification cleanup failed after mail send error");
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

    private String cooldownKey(String email) {
        return "email-code:cooldown:" + email;
    }

    private String verifyFailKey(String email) {
        return VERIFY_FAIL_KEY_PREFIX + email;
    }

    private String localVerifyFailPrefix(String email) {
        return "local:" + verifyFailKey(email) + ":";
    }

    private String verifyFailField(String clientIp, String generation) {
        return clientIp + "|" + generation;
    }

    private String localVerifyFailKey(String email, String field) {
        return localVerifyFailPrefix(email) + field;
    }

    private void cleanupExpiredLocalState() {
        long now = clock.millis();
        localCounters.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        localCodes.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        localInvalidatedCodes.entrySet().removeIf(entry -> entry.getValue() <= now);
        localCooldowns.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }

    private void logRedisFallback() {
        if (redisWarningLogged.compareAndSet(false, true)) {
            log.warn("Email verification Redis unavailable; using in-memory fallback");
        }
    }

    private record CodeValue(String code, String generation, String storedValue) {
    }

    private record LocalCode(CodeValue codeValue, long expiresAt, boolean redisStored) {
    }

    private record InvalidatedCodeKey(String email, String storedValue) {
    }

    private record LocalCounter(int count, long expiresAt) {
    }

    private record LocalCooldown(String sendAttemptId, long expiresAt) {
    }
}
