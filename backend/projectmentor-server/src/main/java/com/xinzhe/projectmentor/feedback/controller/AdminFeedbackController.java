package com.xinzhe.projectmentor.feedback.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.feedback.dto.AdminFeedbackStatusRequest;
import com.xinzhe.projectmentor.feedback.service.FeedbackService;
import com.xinzhe.projectmentor.feedback.vo.AdminFeedbackPageVO;
import com.xinzhe.projectmentor.feedback.vo.AdminFeedbackVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/feedback")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    @GetMapping
    public Result<AdminFeedbackPageVO> listFeedback(@RequestParam(required = false) String type,
                                                   @RequestParam(required = false) String status,
                                                   @RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        return Result.success(feedbackService.listAdminFeedback(type, status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public Result<AdminFeedbackVO> getFeedbackDetail(@PathVariable Long id) {
        return Result.success(feedbackService.getAdminFeedbackDetail(id));
    }

    @PutMapping("/{id}/status")
    public Result<AdminFeedbackVO> updateFeedbackStatus(@PathVariable Long id,
                                                       @Valid @RequestBody AdminFeedbackStatusRequest request) {
        return Result.success(feedbackService.updateAdminFeedbackStatus(id, request));
    }
}
