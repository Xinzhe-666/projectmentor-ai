package com.xinzhe.projectmentor.file.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.file.dto.SaveReadmeRequest;
import com.xinzhe.projectmentor.file.service.ProjectFileService;
import com.xinzhe.projectmentor.file.vo.ProjectFileDetailVO;
import com.xinzhe.projectmentor.file.vo.ProjectFileVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectFileController {

    private final ProjectFileService projectFileService;

    @PostMapping("/readme")
    public Result<ProjectFileDetailVO> saveReadme(@PathVariable Long projectId,
                                                  @Valid @RequestBody SaveReadmeRequest request) {
        return Result.success(projectFileService.saveReadme(projectId, request));
    }

    @GetMapping("/files")
    public Result<List<ProjectFileVO>> listProjectFiles(@PathVariable Long projectId) {
        return Result.success(projectFileService.listProjectFiles(projectId));
    }

    @GetMapping("/files/{fileId}")
    public Result<ProjectFileDetailVO> getFileDetail(@PathVariable Long projectId,
                                                     @PathVariable Long fileId) {
        return Result.success(projectFileService.getFileDetail(projectId, fileId));
    }

    @DeleteMapping("/files/{fileId}")
    public Result<Void> deleteFile(@PathVariable Long projectId,
                                   @PathVariable Long fileId) {
        projectFileService.deleteFile(projectId, fileId);
        return Result.success();
    }
}