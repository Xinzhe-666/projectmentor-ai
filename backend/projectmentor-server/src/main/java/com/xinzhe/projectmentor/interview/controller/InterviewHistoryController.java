package com.xinzhe.projectmentor.interview.controller;

import com.xinzhe.projectmentor.common.PageResult;
import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.interview.service.InterviewService;
import com.xinzhe.projectmentor.interview.vo.InterviewSessionListItemVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interviews")
@RequiredArgsConstructor
public class InterviewHistoryController {

    private final InterviewService interviewService;

    @GetMapping
    public Result<PageResult<InterviewSessionListItemVO>> listMySessions(@RequestParam(required = false) Integer page,
                                                                        @RequestParam(required = false) Integer size,
                                                                        @RequestParam(required = false) Long projectId,
                                                                        @RequestParam(required = false) String keyword) {
        return Result.success(interviewService.listMySessions(page, size, projectId, keyword));
    }
}
