package com.xinzhe.projectmentor.scanner.controller;

import com.xinzhe.projectmentor.common.Result;
import com.xinzhe.projectmentor.scanner.ProjectRuleScanner;
import com.xinzhe.projectmentor.scanner.vo.RuleScanResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectScanController {

    private final ProjectRuleScanner projectRuleScanner;

    @PostMapping("/scan")
    public Result<RuleScanResultVO> scanProject(@PathVariable Long projectId) {
        return Result.success(projectRuleScanner.scanProject(projectId));
    }
}