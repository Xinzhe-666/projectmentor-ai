package com.xinzhe.projectmentor.qa.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.qa.dto.ProjectQaRequest;
import com.xinzhe.projectmentor.qa.service.ProjectQaService;
import com.xinzhe.projectmentor.qa.vo.ProjectQaResponseVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/qa")
@RequiredArgsConstructor
public class ProjectQaController {

    private final ProjectQaService projectQaService;

    @PostMapping
    public Result<ProjectQaResponseVO> ask(@PathVariable Long projectId,
                                           @Valid @RequestBody ProjectQaRequest request) {
        return Result.success(projectQaService.ask(projectId, request.getQuestion()));
    }
}
