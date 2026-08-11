package com.xinzhe.projectmentor.auth.controller;

import com.xinzhe.projectmentor.auth.dto.RegisterRequest;
import com.xinzhe.projectmentor.auth.service.AuthService;
import com.xinzhe.projectmentor.auth.service.EmailVerificationService;
import com.xinzhe.projectmentor.auth.service.RegistrationRateLimitService;
import com.xinzhe.projectmentor.auth.vo.LoginResponse;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.common.Result;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTests {

    private static final String CLIENT_IP = "203.0.113.10";

    @Test
    void registrationFailureKeepsCodeAvailableForCorrectedSubmission() {
        AuthService authService = mock(AuthService.class);
        RegistrationRateLimitService rateLimitService = mock(RegistrationRateLimitService.class);
        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        AuthController controller = new AuthController(
                authService,
                rateLimitService,
                emailVerificationService
        );
        RegisterRequest request = registerRequest();
        BusinessException registrationFailure = new BusinessException(
                ErrorCode.PARAM_ERROR,
                "用户名已存在"
        );
        when(authService.register(request)).thenThrow(registrationFailure);

        assertThatThrownBy(() -> controller.register(request, httpRequest()))
                .isSameAs(registrationFailure);

        verify(rateLimitService).checkAllowed(CLIENT_IP);
        verify(emailVerificationService).validateRegisterCode(
                request.getEmail(),
                request.getVerificationCode(),
                CLIENT_IP
        );
        verify(authService).register(request);
        verify(emailVerificationService, never()).consumeRegisterCode(request.getEmail());
        verify(rateLimitService, never()).recordSuccessfulRegistration(CLIENT_IP);

        RegisterRequest correctedRequest = registerRequest();
        correctedRequest.setUsername("corrected-tester");
        LoginResponse loginResponse = mock(LoginResponse.class);
        when(authService.register(correctedRequest)).thenReturn(loginResponse);

        Result<LoginResponse> correctedResult = controller.register(correctedRequest, httpRequest());

        assertThat(correctedResult.getData()).isSameAs(loginResponse);
        verify(rateLimitService, times(2)).checkAllowed(CLIENT_IP);
        verify(emailVerificationService, times(2)).validateRegisterCode(
                correctedRequest.getEmail(),
                correctedRequest.getVerificationCode(),
                CLIENT_IP
        );
        verify(emailVerificationService).consumeRegisterCode(correctedRequest.getEmail());
        verify(rateLimitService).recordSuccessfulRegistration(CLIENT_IP);
    }

    @Test
    void successfulRegistrationConsumesCodeAndRecordsSuccessInOrder() {
        AuthService authService = mock(AuthService.class);
        RegistrationRateLimitService rateLimitService = mock(RegistrationRateLimitService.class);
        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        AuthController controller = new AuthController(
                authService,
                rateLimitService,
                emailVerificationService
        );
        RegisterRequest request = registerRequest();
        LoginResponse loginResponse = mock(LoginResponse.class);
        when(authService.register(request)).thenReturn(loginResponse);

        Result<LoginResponse> result = controller.register(request, httpRequest());

        assertThat(result.getData()).isSameAs(loginResponse);
        InOrder inOrder = inOrder(rateLimitService, emailVerificationService, authService);
        inOrder.verify(rateLimitService).checkAllowed(CLIENT_IP);
        inOrder.verify(emailVerificationService).validateRegisterCode(
                request.getEmail(),
                request.getVerificationCode(),
                CLIENT_IP
        );
        inOrder.verify(authService).register(request);
        inOrder.verify(emailVerificationService).consumeRegisterCode(request.getEmail());
        inOrder.verify(rateLimitService).recordSuccessfulRegistration(CLIENT_IP);
    }

    @Test
    void validationFailureStopsRegistrationAndSuccessAccounting() {
        AuthService authService = mock(AuthService.class);
        RegistrationRateLimitService rateLimitService = mock(RegistrationRateLimitService.class);
        EmailVerificationService emailVerificationService = mock(EmailVerificationService.class);
        AuthController controller = new AuthController(
                authService,
                rateLimitService,
                emailVerificationService
        );
        RegisterRequest request = registerRequest();
        BusinessException validationFailure = new BusinessException(
                ErrorCode.PARAM_ERROR,
                "邮箱验证码错误"
        );
        doThrow(validationFailure).when(emailVerificationService).validateRegisterCode(
                request.getEmail(),
                request.getVerificationCode(),
                CLIENT_IP
        );

        assertThatThrownBy(() -> controller.register(request, httpRequest()))
                .isSameAs(validationFailure);

        verify(rateLimitService).checkAllowed(CLIENT_IP);
        verify(emailVerificationService).validateRegisterCode(
                request.getEmail(),
                request.getVerificationCode(),
                CLIENT_IP
        );
        verify(authService, never()).register(request);
        verify(emailVerificationService, never()).consumeRegisterCode(request.getEmail());
        verify(rateLimitService, never()).recordSuccessfulRegistration(CLIENT_IP);
    }

    private RegisterRequest registerRequest() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("tester");
        request.setPassword("password123");
        request.setEmail("Tester@Example.com");
        request.setVerificationCode("123456");
        return request;
    }

    private MockHttpServletRequest httpRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(CLIENT_IP);
        return request;
    }
}
