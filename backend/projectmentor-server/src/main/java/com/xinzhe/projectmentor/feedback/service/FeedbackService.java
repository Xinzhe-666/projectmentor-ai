package com.xinzhe.projectmentor.feedback.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xinzhe.projectmentor.admin.service.AdminService;
import com.xinzhe.projectmentor.auth.interceptor.UserContext;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import com.xinzhe.projectmentor.feedback.dto.AdminFeedbackStatusRequest;
import com.xinzhe.projectmentor.feedback.dto.FeedbackSubmitRequest;
import com.xinzhe.projectmentor.feedback.entity.Feedback;
import com.xinzhe.projectmentor.feedback.mapper.FeedbackMapper;
import com.xinzhe.projectmentor.feedback.vo.AdminFeedbackPageVO;
import com.xinzhe.projectmentor.feedback.vo.AdminFeedbackVO;
import com.xinzhe.projectmentor.feedback.vo.FeedbackVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FeedbackService {

    private static final int DEFAULT_PAGE = 1;

    private static final int DEFAULT_SIZE = 10;

    private static final int MAX_SIZE = 50;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "BUG",
            "UX",
            "AUDIT_INACCURATE",
            "QA_INACCURATE",
            "INTERVIEW_QUESTION",
            "UPLOAD",
            "OTHER"
    );

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING",
            "PROCESSING",
            "RESOLVED",
            "WONTFIX"
    );

    private final FeedbackMapper feedbackMapper;

    private final AdminService adminService;

    @Transactional(rollbackFor = Exception.class)
    public FeedbackVO submitFeedback(FeedbackSubmitRequest request) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录后提交反馈");
        }

        String type = normalizeType(request.getType());
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈类型不合法");
        }

        String content = trimToNull(request.getContent());
        if (content == null || content.length() < 5 || content.length() > 2000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈内容长度必须在 5 到 2000 个字符之间");
        }

        Feedback feedback = new Feedback();
        LocalDateTime now = LocalDateTime.now();
        feedback.setUserId(userId);
        feedback.setType(type);
        feedback.setContent(content);
        feedback.setContact(trimToNull(request.getContact()));
        feedback.setPageUrl(trimToNull(request.getPageUrl()));
        feedback.setStatus("PENDING");
        feedback.setCreateTime(now);
        feedback.setUpdateTime(now);
        feedback.setDeleted(0);

        feedbackMapper.insert(feedback);

        return FeedbackVO.builder()
                .id(feedback.getId())
                .type(feedback.getType())
                .status(feedback.getStatus())
                .createTime(feedback.getCreateTime())
                .build();
    }

    public AdminFeedbackPageVO listAdminFeedback(String type,
                                                String status,
                                                String keyword,
                                                Integer page,
                                                Integer size) {
        adminService.requireAdmin();

        String normalizedType = normalizeOptionalType(type);
        String normalizedStatus = normalizeOptionalStatus(status);
        String normalizedKeyword = trimToNull(keyword);
        int safePage = sanitizePage(page);
        int safeSize = sanitizeSize(size);
        int offset = (safePage - 1) * safeSize;

        Long total = feedbackMapper.countAdminFeedback(normalizedType, normalizedStatus, normalizedKeyword);
        List<AdminFeedbackVO> records = total == null || total == 0
                ? List.of()
                : feedbackMapper.selectAdminFeedbackPage(
                        normalizedType,
                        normalizedStatus,
                        normalizedKeyword,
                        offset,
                        safeSize
                );

        return AdminFeedbackPageVO.builder()
                .records(records)
                .total(total == null ? 0L : total)
                .page(safePage)
                .size(safeSize)
                .build();
    }

    public AdminFeedbackVO getAdminFeedbackDetail(Long id) {
        adminService.requireAdmin();
        return requireAdminFeedback(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public AdminFeedbackVO updateAdminFeedbackStatus(Long id, AdminFeedbackStatusRequest request) {
        adminService.requireAdmin();
        requireAdminFeedback(id);

        String status = normalizeOptionalStatus(request.getStatus());
        if (status == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈状态不能为空");
        }

        String adminNote = trimToNull(request.getAdminNote());
        if (adminNote != null && adminNote.length() > 1000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "管理员备注不能超过 1000 个字符");
        }

        feedbackMapper.update(null, new LambdaUpdateWrapper<Feedback>()
                .set(Feedback::getStatus, status)
                .set(Feedback::getAdminNote, adminNote)
                .set(Feedback::getUpdateTime, LocalDateTime.now())
                .eq(Feedback::getId, id)
                .eq(Feedback::getDeleted, 0));

        return requireAdminFeedback(id);
    }

    private AdminFeedbackVO requireAdminFeedback(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈ID不能为空");
        }

        AdminFeedbackVO feedback = feedbackMapper.selectAdminFeedbackById(id);
        if (feedback == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "反馈不存在");
        }
        return feedback;
    }

    private String normalizeOptionalType(String type) {
        String normalizedType = trimToNull(type);
        if (normalizedType == null) {
            return null;
        }

        normalizedType = normalizeType(normalizedType);
        if (!ALLOWED_TYPES.contains(normalizedType)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈类型不合法");
        }
        return normalizedType;
    }

    private String normalizeOptionalStatus(String status) {
        String normalizedStatus = trimToNull(status);
        if (normalizedStatus == null) {
            return null;
        }

        normalizedStatus = normalizedStatus.toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈状态不合法");
        }
        return normalizedStatus;
    }

    private String normalizeType(String type) {
        return type == null ? null : type.trim().toUpperCase(Locale.ROOT);
    }

    private int sanitizePage(Integer page) {
        int safePage = page == null ? DEFAULT_PAGE : page;
        return Math.max(1, safePage);
    }

    private int sanitizeSize(Integer size) {
        int safeSize = size == null ? DEFAULT_SIZE : size;
        return Math.max(1, Math.min(safeSize, MAX_SIZE));
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
