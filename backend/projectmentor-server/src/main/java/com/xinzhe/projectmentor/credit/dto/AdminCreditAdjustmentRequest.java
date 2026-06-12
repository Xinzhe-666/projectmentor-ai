package com.xinzhe.projectmentor.credit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminCreditAdjustmentRequest {

    @NotNull(message = "额度不能为空")
    @Min(value = 1, message = "额度必须为正整数")
    @Max(value = 10000, message = "单次调整额度不能超过 10000")
    private Integer amount;

    @NotBlank(message = "调整原因不能为空")
    @Size(min = 2, max = 200, message = "调整原因长度必须在 2 到 200 个字符之间")
    private String reason;
}
