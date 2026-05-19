package com.xinzhe.projectmentor.interview.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.interview.dto.StartInterviewRequest;
import com.xinzhe.projectmentor.interview.dto.SubmitAnswerRequest;
import com.xinzhe.projectmentor.interview.service.InterviewService;
import com.xinzhe.projectmentor.interview.vo.InterviewSessionVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interview")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping("/start")
    public Result<InterviewSessionVO> startInterview(@Valid @RequestBody StartInterviewRequest request) {
        return Result.success(interviewService.startInterview(request));
    }

    @PostMapping("/{sessionId}/answer")
    public Result<InterviewSessionVO> submitAnswer(@PathVariable Long sessionId,
                                                   @Valid @RequestBody SubmitAnswerRequest request) {
        return Result.success(interviewService.submitAnswer(sessionId, request));
    }

    @GetMapping("/{sessionId}")
    public Result<InterviewSessionVO> getSessionDetail(@PathVariable Long sessionId) {
        return Result.success(interviewService.getSessionDetail(sessionId));
    }

    @PostMapping("/{sessionId}/finish")
    public Result<InterviewSessionVO> finishInterview(@PathVariable Long sessionId) {
        return Result.success(interviewService.finishInterview(sessionId));
    }
}