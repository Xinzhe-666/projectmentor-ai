package com.xinzhe.projectmentor.defense.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.defense.dto.CreateDefenseSessionRequest;
import com.xinzhe.projectmentor.defense.dto.DefenseAnswerResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseQuestionResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseSessionResponse;
import com.xinzhe.projectmentor.defense.dto.DefenseSessionReviewResponse;
import com.xinzhe.projectmentor.defense.dto.SubmitDefenseAnswerRequest;
import com.xinzhe.projectmentor.defense.service.DefenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DefenseController {

    private final DefenseService defenseService;

    @PostMapping("/api/projects/{id}/defense/sessions")
    public Result<DefenseSessionResponse> createSession(@PathVariable("id") Long projectId,
                                                        @Valid @RequestBody CreateDefenseSessionRequest request) {
        return Result.success(defenseService.createSession(projectId, request));
    }

    @GetMapping("/api/defense/sessions/{id}/questions")
    public Result<List<DefenseQuestionResponse>> getQuestions(@PathVariable("id") Long sessionId) {
        return Result.success(defenseService.getQuestions(sessionId));
    }

    @PostMapping("/api/defense/questions/{id}/answer")
    public Result<DefenseAnswerResponse> submitAnswer(@PathVariable("id") Long questionId,
                                                      @Valid @RequestBody SubmitDefenseAnswerRequest request) {
        return Result.success(defenseService.submitAnswer(questionId, request));
    }

    @GetMapping("/api/defense/sessions/{id}/review")
    public Result<DefenseSessionReviewResponse> getSessionReview(@PathVariable("id") Long sessionId) {
        return Result.success(defenseService.getSessionReview(sessionId));
    }
}
