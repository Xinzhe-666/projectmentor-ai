package com.xinzhe.projectmentor.feedback.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.feedback.dto.FeedbackSubmitRequest;
import com.xinzhe.projectmentor.feedback.service.FeedbackService;
import com.xinzhe.projectmentor.feedback.vo.FeedbackVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public Result<FeedbackVO> submitFeedback(@Valid @RequestBody FeedbackSubmitRequest request) {
        return Result.success(feedbackService.submitFeedback(request));
    }
}
