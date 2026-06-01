package com.xinzhe.projectmentor.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FeedbackSubmitRequest {

    @NotBlank(message = "不能为空")
    private String type;

    @NotBlank(message = "不能为空")
    @Size(min = 5, max = 2000, message = "长度必须在 5 到 2000 个字符之间")
    private String content;

    @Size(max = 255, message = "长度不能超过 255 个字符")
    private String contact;

    @Size(max = 500, message = "长度不能超过 500 个字符")
    private String pageUrl;
}
