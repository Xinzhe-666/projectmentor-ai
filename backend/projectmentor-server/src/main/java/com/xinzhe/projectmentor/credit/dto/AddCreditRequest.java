package com.xinzhe.projectmentor.credit.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddCreditRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "增加额度不能为空")
    @Min(value = 1, message = "增加额度必须大于 0")
    @Max(value = 10000, message = "单次增加额度不能超过 10000")
    private Integer amount;

    @Size(max = 255, message = "备注不能超过 255 个字符")
    private String remark;
}