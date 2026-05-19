package com.xinzhe.projectmentor.file.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.file.service.ProjectZipUploadService;
import com.xinzhe.projectmentor.file.vo.UploadZipResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectUploadController {

    private final ProjectZipUploadService projectZipUploadService;

    @PostMapping(value = "/upload-zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<UploadZipResultVO> uploadZip(@PathVariable Long projectId,
                                               @RequestParam("file") MultipartFile file) {
        return Result.success(projectZipUploadService.uploadZip(projectId, file));
    }
}
