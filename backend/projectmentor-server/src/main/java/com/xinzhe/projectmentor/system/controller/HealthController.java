package com.xinzhe.projectmentor.system.controller;

import com.xinzhe.projectmentor.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Result<Map<String, Object>> health() {
        return Result.success(Map.of(
                "status", "UP",
                "service", "ProjectMentor AI",
                "version", "0.0.1"
        ));
    }
}