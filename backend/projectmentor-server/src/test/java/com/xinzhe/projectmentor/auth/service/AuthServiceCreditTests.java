package com.xinzhe.projectmentor.auth.service;

import com.xinzhe.projectmentor.auth.dto.RegisterRequest;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.auth.vo.LoginResponse;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.entity.CreditLog;
import com.xinzhe.projectmentor.credit.entity.UserPlan;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import com.xinzhe.projectmentor.credit.mapper.UserPlanMapper;
import com.xinzhe.projectmentor.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceCreditTests {

    @Test
    void registrationGrantsTenCreditsAndWritesGiftLog() {
        UserMapper userMapper = mock(UserMapper.class);
        UserPlanMapper userPlanMapper = mock(UserPlanMapper.class);
        CreditLogMapper creditLogMapper = mock(CreditLogMapper.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(jwtUtil.generateToken(7L, "tester")).thenReturn("token");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        AuthService service = new AuthService(
                userMapper,
                userPlanMapper,
                creditLogMapper,
                passwordEncoder,
                jwtUtil
        );
        RegisterRequest request = new RegisterRequest();
        request.setUsername("tester");
        request.setPassword("password123");
        request.setEmail("tester@example.com");

        LoginResponse response = service.register(request);

        ArgumentCaptor<UserPlan> planCaptor = ArgumentCaptor.forClass(UserPlan.class);
        ArgumentCaptor<CreditLog> logCaptor = ArgumentCaptor.forClass(CreditLog.class);
        verify(userPlanMapper).insert(planCaptor.capture());
        verify(creditLogMapper).insert(logCaptor.capture());

        assertThat(response.getRemainingCredits()).isEqualTo(CreditCostConstants.REGISTER_GIFT);
        assertThat(planCaptor.getValue().getRemainingCredits()).isEqualTo(10);
        assertThat(logCaptor.getValue().getChangeAmount()).isEqualTo(10);
        assertThat(logCaptor.getValue().getOperationType()).isEqualTo(CreditCostConstants.OP_REGISTER_GIFT);
    }

    @Test
    void registrationStoresEmailInLowercase() {
        UserMapper userMapper = mock(UserMapper.class);
        UserPlanMapper userPlanMapper = mock(UserPlanMapper.class);
        CreditLogMapper creditLogMapper = mock(CreditLogMapper.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        when(jwtUtil.generateToken(8L, "tester2")).thenReturn("token");
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(8L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        AuthService service = new AuthService(
                userMapper,
                userPlanMapper,
                creditLogMapper,
                passwordEncoder,
                jwtUtil
        );
        RegisterRequest request = new RegisterRequest();
        request.setUsername("tester2");
        request.setPassword("password123");
        request.setEmail("Tester2@Example.COM");

        service.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("tester2@example.com");
    }

    @Test
    void duplicateConstraintReturnsStableBusinessErrorWithoutCreatingCredits() {
        UserMapper userMapper = mock(UserMapper.class);
        UserPlanMapper userPlanMapper = mock(UserPlanMapper.class);
        CreditLogMapper creditLogMapper = mock(CreditLogMapper.class);
        BCryptPasswordEncoder passwordEncoder = mock(BCryptPasswordEncoder.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);

        when(userMapper.selectOne(any())).thenReturn(null);
        when(passwordEncoder.encode("password123")).thenReturn("encoded");
        doThrow(new DuplicateKeyException("database constraint detail"))
                .when(userMapper).insert(any(User.class));

        AuthService service = new AuthService(
                userMapper,
                userPlanMapper,
                creditLogMapper,
                passwordEncoder,
                jwtUtil
        );
        RegisterRequest request = new RegisterRequest();
        request.setUsername("racing-user");
        request.setPassword("password123");
        request.setEmail("racing@example.com");

        assertThatThrownBy(() -> service.register(request))
                .isInstanceOf(com.xinzhe.projectmentor.common.BusinessException.class)
                .hasMessage("用户名或邮箱已被注册")
                .hasMessageNotContaining("database constraint detail");

        verify(userPlanMapper, never()).insert(any(UserPlan.class));
        verify(creditLogMapper, never()).insert(any(CreditLog.class));
        verify(jwtUtil, never()).generateToken(any(), any());
    }
}
