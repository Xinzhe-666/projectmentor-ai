package com.xinzhe.projectmentor.project.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.project.dto.CreateProjectRequest;
import com.xinzhe.projectmentor.project.service.ProjectService;
import com.xinzhe.projectmentor.project.vo.ProjectVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    public Result<ProjectVO> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return Result.success(projectService.createProject(request));
    }

    @GetMapping
    public Result<List<ProjectVO>> listMyProjects() {
        return Result.success(projectService.listMyProjects());
    }

    @GetMapping("/{id}")
    public Result<ProjectVO> getProjectDetail(@PathVariable("id") Long id) {
        return Result.success(projectService.getProjectDetail(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable("id") Long id) {
        projectService.deleteProject(id);
        return Result.success();
    }
}