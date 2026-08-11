package com.xinzhe.projectmentor.auth.service;

import com.xinzhe.projectmentor.auth.config.EmailVerificationProperties;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTests {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    private static final String CLIENT_IP = "203.0.113.10";

    @Test
    void disabledVerificationBypassesSendValidateAndConsume() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newService(false, mailSender, mock(UserMapper.class));

        assertThatNoException()
                .isThrownBy(() -> service.sendRegisterCode("user@example.com", CLIENT_IP));
        assertThatNoException()
                .isThrownBy(() -> service.validateRegisterCode("user@example.com", null, CLIENT_IP));
        assertThatNoException()
                .isThrownBy(() -> service.consumeRegisterCode("user@example.com"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void enabledVerificationRejectsMissingCode() {
        EmailVerificationService service = newService(true, mock(JavaMailSender.class), mock(UserMapper.class));

        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", null, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码不能为空或格式不正确");
    }

    @Test
    void sendRegisterCodeRejectsRegisteredEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.selectOne(any())).thenReturn(new User());
        EmailVerificationService service = newService(true, mailSender, userMapper);

        assertThatThrownBy(() -> service.sendRegisterCode("Used@Example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已被注册");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void successfulSendStoresCodeAndAppliesCooldown() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newEnabledService(mailSender, fixedClock());

        service.sendRegisterCode("User@Example.com", CLIENT_IP);
        String code = capturedCodes(mailSender, 1).get(0);

        assertThatNoException()
                .isThrownBy(() -> service.validateRegisterCode("user@example.com", code, CLIENT_IP));
        assertThatThrownBy(() -> service.sendRegisterCode("user@example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码发送过于频繁，请稍后再试");
    }

    @Test
    void smtpFailureRemovesGeneratedCodeAndCooldown() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp unavailable"))
                .doNothing()
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailVerificationService service = newEnabledService(mailSender, fixedClock());

        assertThatThrownBy(() -> service.sendRegisterCode("user@example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码发送失败，请稍后再试");
        String failedCode = capturedCodes(mailSender, 1).get(0);

        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", failedCode, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");
        assertThatNoException()
                .isThrownBy(() -> service.sendRegisterCode("user@example.com", CLIENT_IP));
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void smtpFailureTombstonesBlockEveryCodeLeftByRedisCleanupFailure() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenThrow(new IllegalStateException("redis cleanup unavailable"));

        String codeKey = "email-code:register:user@example.com";
        AtomicReference<String> storedCode = new AtomicReference<>();
        doAnswer(invocation -> {
            if (codeKey.equals(invocation.getArgument(0))) {
                storedCode.set(invocation.getArgument(1));
            }
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(valueOperations.get(codeKey)).thenAnswer(invocation -> storedCode.get());

        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailVerificationService service = newService(
                redisTemplate,
                mailSender,
                mockUnregisteredUserMapper(),
                enabledProperties(),
                fixedClock()
        );

        assertThatThrownBy(() -> service.sendRegisterCode("user@example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码发送失败，请稍后再试");
        String firstFailedCode = capturedCodes(mailSender, 1).get(0);
        String firstStoredCode = storedCode.get();
        assertThat(firstStoredCode).endsWith(":" + firstFailedCode);

        assertThatThrownBy(() -> service.sendRegisterCode("user@example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码发送失败，请稍后再试");
        String secondFailedCode = capturedCodes(mailSender, 2).get(1);
        String secondStoredCode = storedCode.get();
        assertThat(secondStoredCode).endsWith(":" + secondFailedCode);

        storedCode.set(firstStoredCode);
        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", firstFailedCode, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");

        storedCode.set(secondStoredCode);
        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", secondFailedCode, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");
    }

    @Test
    void smtpFailureKeepsHourlyEmailCounter() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailVerificationProperties properties = enabledProperties();
        properties.setHourlyLimitPerEmail(2);
        properties.setHourlyLimitPerIp(20);
        EmailVerificationService service = newService(
                null,
                mailSender,
                mockUnregisteredUserMapper(),
                properties,
                fixedClock()
        );

        for (int attempt = 0; attempt < 2; attempt++) {
            assertThatThrownBy(() -> service.sendRegisterCode("user@example.com", CLIENT_IP))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("验证码发送失败，请稍后再试");
        }

        assertThatThrownBy(() -> service.sendRegisterCode("user@example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("该邮箱验证码发送次数过多，请稍后再试");
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFailureKeepsHourlyIpCounter() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        doThrow(new MailSendException("smtp unavailable"))
                .when(mailSender).send(any(SimpleMailMessage.class));
        EmailVerificationProperties properties = enabledProperties();
        properties.setHourlyLimitPerEmail(20);
        properties.setHourlyLimitPerIp(2);
        EmailVerificationService service = newService(
                null,
                mailSender,
                mockUnregisteredUserMapper(),
                properties,
                fixedClock()
        );

        assertThatThrownBy(() -> service.sendRegisterCode("first@example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码发送失败，请稍后再试");
        assertThatThrownBy(() -> service.sendRegisterCode("second@example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码发送失败，请稍后再试");

        assertThatThrownBy(() -> service.sendRegisterCode("third@example.com", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码请求过于频繁，请稍后再试");
        verify(mailSender, times(2)).send(any(SimpleMailMessage.class));
    }

    @Test
    void correctCodeRemainsValidUntilConsumed() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newEnabledService(mailSender, fixedClock());

        service.sendRegisterCode("User@Example.com", CLIENT_IP);
        String code = capturedCodes(mailSender, 1).get(0);

        assertThatNoException()
                .isThrownBy(() -> service.validateRegisterCode("user@example.com", code, CLIENT_IP));
        assertThatNoException()
                .isThrownBy(() -> service.validateRegisterCode("user@example.com", code, CLIENT_IP));

        service.consumeRegisterCode("USER@example.com");

        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", code, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");
    }

    @Test
    void fifthWrongCodeInvalidatesCurrentCode() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newEnabledService(mailSender, fixedClock());

        service.sendRegisterCode("user@example.com", CLIENT_IP);
        String code = capturedCodes(mailSender, 1).get(0);
        String wrongCode = differentCode(code);

        for (int attempt = 1; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", wrongCode, CLIENT_IP))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("邮箱验证码错误");
        }

        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", wrongCode, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误次数过多，请重新获取验证码");
        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", code, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");
    }

    @Test
    void malformedCodeCountsTowardAttemptLimitWhenCodeExists() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newEnabledService(mailSender, fixedClock());
        service.sendRegisterCode("user@example.com", CLIENT_IP);

        for (int attempt = 1; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", "123", CLIENT_IP))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("邮箱验证码不能为空或格式不正确");
        }

        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", "123", CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误次数过多，请重新获取验证码");
    }

    @Test
    void verifyFailureCounterIsIsolatedByClientIp() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newEnabledService(mailSender, fixedClock());
        service.sendRegisterCode("user@example.com", CLIENT_IP);
        String code = capturedCodes(mailSender, 1).get(0);
        String wrongCode = differentCode(code);

        for (int attempt = 1; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", wrongCode, CLIENT_IP))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("邮箱验证码错误");
        }

        String otherIp = "198.51.100.25";
        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", wrongCode, otherIp))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码错误");
        assertThatNoException()
                .isThrownBy(() -> service.validateRegisterCode("user@example.com", code, otherIp));
    }

    @Test
    void successfulResendResetsVerifyFailureCounter() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MutableClock clock = mutableClock();
        EmailVerificationService service = newEnabledService(mailSender, clock);

        service.sendRegisterCode("user@example.com", CLIENT_IP);
        String firstCode = capturedCodes(mailSender, 1).get(0);
        String firstWrongCode = differentCode(firstCode);
        for (int attempt = 1; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", firstWrongCode, CLIENT_IP))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("邮箱验证码错误");
        }

        clock.advanceSeconds(61);
        service.sendRegisterCode("user@example.com", CLIENT_IP);
        String secondCode = capturedCodes(mailSender, 2).get(1);

        assertThatThrownBy(() -> service.validateRegisterCode(
                "user@example.com",
                differentCode(secondCode),
                CLIENT_IP
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码错误");
        assertThatNoException()
                .isThrownBy(() -> service.validateRegisterCode("user@example.com", secondCode, CLIENT_IP));
    }

    @Test
    void expiredCodeRequiresANewCode() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MutableClock clock = mutableClock();
        EmailVerificationService service = newEnabledService(mailSender, clock);

        service.sendRegisterCode("user@example.com", CLIENT_IP);
        String code = capturedCodes(mailSender, 1).get(0);
        clock.advanceSeconds(601);

        assertThatThrownBy(() -> service.validateRegisterCode("user@example.com", code, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisUnavailableUsesLocalFallbackForSendValidateConsumeAndAttemptLimit() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.hasKey(anyString())).thenThrow(new IllegalStateException("redis unavailable"));
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis unavailable"));
        when(redisTemplate.opsForHash()).thenThrow(new IllegalStateException("redis unavailable"));
        when(redisTemplate.delete(anyString())).thenThrow(new IllegalStateException("redis unavailable"));
        when(redisTemplate.execute(
                any(DefaultRedisScript.class),
                anyList(),
                any(Object[].class)
        )).thenThrow(new IllegalStateException("redis unavailable"));

        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newService(
                redisTemplate,
                mailSender,
                mockUnregisteredUserMapper(),
                enabledProperties(),
                fixedClock()
        );

        service.sendRegisterCode("first@example.com", CLIENT_IP);
        String firstCode = capturedCodes(mailSender, 1).get(0);
        assertThatNoException()
                .isThrownBy(() -> service.validateRegisterCode("first@example.com", firstCode, CLIENT_IP));
        assertThatNoException()
                .isThrownBy(() -> service.consumeRegisterCode("first@example.com"));
        assertThatThrownBy(() -> service.validateRegisterCode("first@example.com", firstCode, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");

        service.sendRegisterCode("second@example.com", CLIENT_IP);
        String secondCode = capturedCodes(mailSender, 2).get(1);
        String wrongCode = differentCode(secondCode);
        for (int attempt = 1; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.validateRegisterCode("second@example.com", wrongCode, CLIENT_IP))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("邮箱验证码错误");
        }
        assertThatThrownBy(() -> service.validateRegisterCode("second@example.com", wrongCode, CLIENT_IP))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误次数过多，请重新获取验证码");
    }

    @Test
    @SuppressWarnings("unchecked")
    void redisPathUsesCodeGenerationIpFieldAndFiniteVerifyTtl() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.increment(anyString())).thenReturn(1L);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(true);

        String codeKey = "email-code:register:user@example.com";
        AtomicReference<String> storedCode = new AtomicReference<>();
        doAnswer(invocation -> {
            if (codeKey.equals(invocation.getArgument(0))) {
                storedCode.set(invocation.getArgument(1));
            }
            return null;
        }).when(valueOperations).set(anyString(), anyString(), any(Duration.class));
        when(valueOperations.get(codeKey)).thenAnswer(invocation -> storedCode.get());

        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newService(
                redisTemplate,
                mailSender,
                mockUnregisteredUserMapper(),
                enabledProperties(),
                fixedClock()
        );

        service.sendRegisterCode("User@Example.com", CLIENT_IP);
        String code = capturedCodes(mailSender, 1).get(0);
        assertThat(storedCode.get()).startsWith("v1:").endsWith(":" + code);
        String generation = storedCode.get().substring(3, storedCode.get().lastIndexOf(':'));

        assertThatThrownBy(() -> service.validateRegisterCode(
                "user@example.com",
                differentCode(code),
                CLIENT_IP
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码错误");

        String failKey = "email-code:register:verify-fail:user@example.com";
        String failField = CLIENT_IP + "|" + generation;
        verify(hashOperations).get(failKey, failField);
        verify(redisTemplate).execute(
                argThat(script -> script.getScriptAsString().contains("HINCRBY")),
                eq(List.of(failKey)),
                eq(failField),
                eq("600000")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void legacyPlainRedisCodeKeepsValidationAndCompareDeleteCompatibility() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOperations = mock(HashOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        String codeKey = "email-code:register:legacy@example.com";
        String legacyCode = "123456";
        when(valueOperations.get(codeKey)).thenReturn(legacyCode);

        EmailVerificationService service = newService(
                redisTemplate,
                mock(JavaMailSender.class),
                mockUnregisteredUserMapper(),
                enabledProperties(),
                fixedClock()
        );
        String wrongCode = differentCode(legacyCode);

        for (int attempt = 1; attempt < 5; attempt++) {
            assertThatThrownBy(() -> service.validateRegisterCode(
                    "legacy@example.com",
                    wrongCode,
                    CLIENT_IP
            ))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("邮箱验证码错误");
        }
        assertThatThrownBy(() -> service.validateRegisterCode(
                "legacy@example.com",
                wrongCode,
                CLIENT_IP
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("验证码错误次数过多，请重新获取验证码");

        verify(redisTemplate).execute(
                argThat(script -> script.getScriptAsString().contains("redis.call('GET'")),
                eq(List.of(codeKey)),
                eq(legacyCode)
        );
        assertThatThrownBy(() -> service.validateRegisterCode(
                "legacy@example.com",
                legacyCode,
                CLIENT_IP
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");
    }

    private EmailVerificationService newService(boolean enabled,
                                                 JavaMailSender mailSender,
                                                 UserMapper userMapper) {
        EmailVerificationProperties properties = enabledProperties();
        properties.setEnabled(enabled);
        return newService(null, mailSender, userMapper, properties, fixedClock());
    }

    private EmailVerificationService newEnabledService(JavaMailSender mailSender, Clock clock) {
        return newService(
                null,
                mailSender,
                mockUnregisteredUserMapper(),
                enabledProperties(),
                clock
        );
    }

    private EmailVerificationService newService(StringRedisTemplate redisTemplate,
                                                 JavaMailSender mailSender,
                                                 UserMapper userMapper,
                                                 EmailVerificationProperties properties,
                                                 Clock clock) {
        return new EmailVerificationService(
                redisTemplate,
                mailSender,
                properties,
                userMapper,
                new SecureRandom(),
                clock
        );
    }

    private EmailVerificationProperties enabledProperties() {
        EmailVerificationProperties properties = new EmailVerificationProperties();
        properties.setEnabled(true);
        properties.setCodeTtlMinutes(10);
        properties.setSendCooldownSeconds(60);
        properties.setHourlyLimitPerEmail(5);
        properties.setHourlyLimitPerIp(20);
        properties.setMaxVerifyAttempts(5);
        properties.setSubject("ProjectMentor AI 邮箱验证码");
        return properties;
    }

    private UserMapper mockUnregisteredUserMapper() {
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.selectOne(any())).thenReturn(null);
        return userMapper;
    }

    private List<String> capturedCodes(JavaMailSender mailSender, int expectedMessages) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(expectedMessages)).send(captor.capture());
        return captor.getAllValues().stream()
                .map(this::extractCode)
                .toList();
    }

    private String extractCode(SimpleMailMessage message) {
        Matcher matcher = CODE_PATTERN.matcher(message.getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }

    private String differentCode(String code) {
        return code.equals("000000") ? "000001" : "000000";
    }

    private Clock fixedClock() {
        return Clock.fixed(
                Instant.parse("2026-06-20T00:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
    }

    private MutableClock mutableClock() {
        return new MutableClock(
                Instant.parse("2026-06-20T00:00:00Z"),
                ZoneId.of("Asia/Shanghai")
        );
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private final ZoneId zone;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private void advanceSeconds(long seconds) {
            instant = instant.plusSeconds(seconds);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
