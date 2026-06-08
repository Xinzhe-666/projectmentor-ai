package com.xinzhe.projectmentor.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xinzhe.projectmentor.auth.dto.LoginRequest;
import com.xinzhe.projectmentor.auth.dto.RegisterRequest;
import com.xinzhe.projectmentor.auth.entity.User;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.auth.mapper.UserMapper;
import com.xinzhe.projectmentor.auth.vo.LoginResponse;
import com.xinzhe.projectmentor.auth.vo.UserInfoVO;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.credit.CreditCostConstants;
import com.xinzhe.projectmentor.credit.entity.CreditLog;
import com.xinzhe.projectmentor.credit.entity.UserPlan;
import com.xinzhe.projectmentor.credit.mapper.CreditLogMapper;
import com.xinzhe.projectmentor.credit.mapper.UserPlanMapper;
import com.xinzhe.projectmentor.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;

    private final UserPlanMapper userPlanMapper;

    private final CreditLogMapper creditLogMapper;

    private final BCryptPasswordEncoder passwordEncoder;

    private final JwtUtil jwtUtil;

    @Transactional(rollbackFor = Exception.class)
    public LoginResponse register(RegisterRequest request) {
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
                        .last("LIMIT 1")
        );

        if (existUser != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名已存在");
        }

        User emailUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getEmail, request.getEmail())
                        .last("LIMIT 1")
        );

        if (emailUser != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "邮箱已被注册");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setRole("USER");
        user.setStatus(1);

        userMapper.insert(user);

        UserPlan userPlan = new UserPlan();
        userPlan.setUserId(user.getId());
        userPlan.setPlanType("FREE");
        userPlan.setRemainingCredits(CreditCostConstants.REGISTER_GIFT);
        userPlanMapper.insert(userPlan);

        CreditLog creditLog = new CreditLog();
        creditLog.setUserId(user.getId());
        creditLog.setChangeAmount(CreditCostConstants.REGISTER_GIFT);
        creditLog.setBeforeAmount(0);
        creditLog.setAfterAmount(CreditCostConstants.REGISTER_GIFT);
        creditLog.setOperationType(CreditCostConstants.OP_REGISTER_GIFT);
        creditLog.setRemark("新用户注册赠送 10 次 AI 额度");
        creditLogMapper.insert(creditLog);

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return buildLoginResponse(user, token, CreditCostConstants.REGISTER_GIFT);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, request.getUsername())
                        .last("LIMIT 1")
        );

        if (user == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }

        if (user.getStatus() == null || user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
        }

        boolean passwordMatched = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!passwordMatched) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "用户名或密码错误");
        }

        UserPlan userPlan = userPlanMapper.selectOne(
                new LambdaQueryWrapper<UserPlan>()
                        .eq(UserPlan::getUserId, user.getId())
                        .last("LIMIT 1")
        );

        Integer remainingCredits = userPlan == null ? 0 : userPlan.getRemainingCredits();

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());

        return buildLoginResponse(user, token, remainingCredits);
    }

    public UserInfoVO getCurrentUser() {
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }

        return buildUserInfo(user);
    }

    public void logout() {
        // 第一版 JWT 是无状态登录，退出登录主要由前端删除 localStorage 中的 token。
        // 后续增强版可以把 token 加入 Redis 黑名单，实现服务端强制失效。
    }

    private LoginResponse buildLoginResponse(User user, String token, Integer remainingCredits) {
        return LoginResponse.builder()
                .token(token)
                .userInfo(buildUserInfo(user))
                .remainingCredits(remainingCredits)
                .build();
    }

    private UserInfoVO buildUserInfo(User user) {
        return UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
