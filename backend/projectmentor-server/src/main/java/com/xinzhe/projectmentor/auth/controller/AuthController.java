package com.xinzhe.projectmentor.auth.controller;

import com.xinzhe.projectmentor.auth.dto.LoginRequest;
import com.xinzhe.projectmentor.auth.dto.RegisterRequest;
import com.xinzhe.projectmentor.auth.service.AuthService;
import com.xinzhe.projectmentor.auth.service.RegistrationRateLimitService;
import com.xinzhe.projectmentor.auth.vo.LoginResponse;
import com.xinzhe.projectmentor.auth.vo.UserInfoVO;
import com.xinzhe.projectmentor.common.Result;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import com.xinzhe.projectmentor.util.IpUtils;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final RegistrationRateLimitService registrationRateLimitService;

    @PostMapping("/register")
    public Result<LoginResponse> register(@Valid @RequestBody RegisterRequest request,
                                          HttpServletRequest httpServletRequest) {
        String clientIp = IpUtils.resolveClientIp(httpServletRequest);
        registrationRateLimitService.checkAllowed(clientIp);
        LoginResponse response = authService.register(request);
        registrationRateLimitService.recordSuccessfulRegistration(clientIp);
        return Result.success(response);
    }

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    @GetMapping("/me")
    public Result<UserInfoVO> me() {
        return Result.success(authService.getCurrentUser());
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }
}
