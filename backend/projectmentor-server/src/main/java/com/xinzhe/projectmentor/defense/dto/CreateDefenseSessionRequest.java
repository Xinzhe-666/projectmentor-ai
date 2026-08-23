package com.xinzhe.projectmentor.defense.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateDefenseSessionRequest {

    @NotNull(message = "报告ID不能为空")
    private Long reportId;

    @Size(max = 50, message = "Defense 模式不能超过 50 个字符")
    private String mode;
}
