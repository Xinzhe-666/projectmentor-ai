package com.xinzhe.projectmentor.indexing.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.indexing.service.VectorIndexTaskSubmissionService;
import com.xinzhe.projectmentor.indexing.vo.VectorIndexStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/vector-index")
@RequiredArgsConstructor
public class VectorIndexController {
    private final VectorIndexTaskSubmissionService service;

    @GetMapping
    public Result<VectorIndexStatusVO> status(@PathVariable long projectId) {
        return Result.success(service.status(projectId));
    }

    @PostMapping("/rebuild")
    public Result<VectorIndexStatusVO> rebuild(@PathVariable long projectId) {
        return Result.success(service.rebuild(projectId));
    }
}
