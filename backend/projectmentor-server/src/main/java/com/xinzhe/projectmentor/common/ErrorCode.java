package com.xinzhe.projectmentor.common;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(0, "success"),

    PARAM_ERROR(40000, "请求参数错误"),

    UNAUTHORIZED(40100, "用户未登录或登录已过期"),

    FORBIDDEN(40300, "无权限访问"),

    NOT_FOUND(40400, "资源不存在"),

    SYSTEM_ERROR(50000, "系统内部异常"),

    OPERATION_ERROR(50001, "操作失败"),

    CREDIT_NOT_ENOUGH(60001, "额度不足"),

    AI_SERVICE_ERROR(70001, "AI 服务调用失败");

    private final int code;

    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}