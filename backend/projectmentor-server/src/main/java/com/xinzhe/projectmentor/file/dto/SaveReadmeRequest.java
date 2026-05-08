package com.xinzhe.projectmentor.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveReadmeRequest {

    @NotBlank(message = "README 内容不能为空")
    @Size(max = 200000, message = "README 内容不能超过 200000 个字符")
    private String content;
}