package com.xinzhe.projectmentor.auth.service;

import com.xinzhe.projectmentor.auth.config.EmailVerificationProperties;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailVerificationServiceTests {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\d{6}");

    @Test
    void disabledVerificationAllowsMissingCodeAndSkipsMail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailVerificationService service = newService(false, mailSender, mock(UserMapper.class));

        assertThatNoException()
                .isThrownBy(() -> service.verifyRegisterCode("user@example.com", null));
        assertThatNoException()
                .isThrownBy(() -> service.sendRegisterCode("user@example.com", "203.0.113.10"));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void enabledVerificationRejectsMissingCode() {
        EmailVerificationService service = newService(true, mock(JavaMailSender.class), mock(UserMapper.class));

        assertThatThrownBy(() -> service.verifyRegisterCode("user@example.com", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码不能为空或格式不正确");
    }

    @Test
    void sendRegisterCodeRejectsRegisteredEmail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.selectOne(any())).thenReturn(new User());
        EmailVerificationService service = newService(true, mailSender, userMapper);

        assertThatThrownBy(() -> service.sendRegisterCode("Used@Example.com", "203.0.113.10"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱已被注册");
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void wrongCodeFailsRegistrationVerification() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.selectOne(any())).thenReturn(null);
        EmailVerificationService service = newService(true, mailSender, userMapper);

        service.sendRegisterCode("User@Example.com", "203.0.113.10");
        String code = capturedCode(mailSender);
        String wrongCode = code.equals("000000") ? "000001" : "000000";

        assertThatThrownBy(() -> service.verifyRegisterCode("user@example.com", wrongCode))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码错误");
    }

    @Test
    void correctCodeAllowsVerificationAndDeletesCode() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        UserMapper userMapper = mock(UserMapper.class);
        when(userMapper.selectOne(any())).thenReturn(null);
        EmailVerificationService service = newService(true, mailSender, userMapper);

        service.sendRegisterCode("User@Example.com", "203.0.113.10");
        String code = capturedCode(mailSender);

        assertThatNoException()
                .isThrownBy(() -> service.verifyRegisterCode("user@example.com", code));
        assertThatThrownBy(() -> service.verifyRegisterCode("user@example.com", code))
                .isInstanceOf(BusinessException.class)
                .hasMessage("邮箱验证码已过期，请重新获取");
    }

    private EmailVerificationService newService(boolean enabled,
                                                JavaMailSender mailSender,
                                                UserMapper userMapper) {
        EmailVerificationProperties properties = new EmailVerificationProperties();
        properties.setEnabled(enabled);
        properties.setCodeTtlMinutes(10);
        properties.setSendCooldownSeconds(60);
        properties.setHourlyLimitPerEmail(5);
        properties.setHourlyLimitPerIp(20);
        properties.setSubject("ProjectMentor AI 邮箱验证码");

        return new EmailVerificationService(
                null,
                mailSender,
                properties,
                userMapper,
                new SecureRandom(),
                Clock.fixed(Instant.parse("2026-06-20T00:00:00Z"), ZoneId.of("Asia/Shanghai"))
        );
    }

    private String capturedCode(JavaMailSender mailSender) {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        Matcher matcher = CODE_PATTERN.matcher(captor.getValue().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group();
    }
}
