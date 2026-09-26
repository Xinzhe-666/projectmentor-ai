package com.xinzhe.projectmentor.ai;

import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;

/**
 * Internal, safely classified AI failure. Messages deliberately exclude upstream response bodies.
 */
public class AiServiceException extends BusinessException {

    public enum Kind {
        DISABLED,
        MISSING_API_KEY,
        TIMEOUT,
        NETWORK,
        RATE_LIMITED,
        TRANSIENT_HTTP,
        PERMANENT_HTTP,
        INVALID_RESPONSE,
        INVALID_REQUEST
    }

    private final Kind kind;
    private final boolean retryable;
    private final Integer httpStatus;

    private AiServiceException(Kind kind,
                               boolean retryable,
                               Integer httpStatus,
                               String safeMessage,
                               Throwable cause) {
        super(ErrorCode.AI_SERVICE_ERROR, safeMessage);
        this.kind = kind;
        this.retryable = retryable;
        this.httpStatus = httpStatus;
        if (cause != null) {
            initCause(cause);
        }
    }

    public static AiServiceException disabled() {
        return permanent(Kind.DISABLED, "AI 服务未启用");
    }

    public static AiServiceException missingApiKey() {
        return permanent(Kind.MISSING_API_KEY, "AI API Key 未配置");
    }

    public static AiServiceException timeout(Throwable cause) {
        return retryable(Kind.TIMEOUT, "AI 服务请求超时", cause);
    }

    public static AiServiceException network(Throwable cause) {
        return retryable(Kind.NETWORK, "AI 服务网络暂时不可用", cause);
    }

    public static AiServiceException invalidResponse() {
        return permanent(Kind.INVALID_RESPONSE, "AI 返回结构无效");
    }

    public static AiServiceException invalidRequest(Throwable cause) {
        return new AiServiceException(
                Kind.INVALID_REQUEST, false, null, "AI 请求参数或配置无效", cause
        );
    }

    public static AiServiceException fromHttpStatus(int status, Throwable cause) {
        if (status == 408) {
            return new AiServiceException(Kind.TIMEOUT, true, status, "AI HTTP 408", cause);
        }
        if (status == 429) {
            return new AiServiceException(Kind.RATE_LIMITED, true, status, "AI HTTP 429", cause);
        }
        if (status >= 500 && status <= 599) {
            return new AiServiceException(Kind.TRANSIENT_HTTP, true, status, "AI HTTP " + status, cause);
        }
        return new AiServiceException(Kind.PERMANENT_HTTP, false, status, "AI HTTP " + status, cause);
    }

    private static AiServiceException retryable(Kind kind, String safeMessage, Throwable cause) {
        return new AiServiceException(kind, true, null, safeMessage, cause);
    }

    private static AiServiceException permanent(Kind kind, String safeMessage) {
        return new AiServiceException(kind, false, null, safeMessage, null);
    }

    public Kind getKind() {
        return kind;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public String safeDescriptor() {
        return httpStatus == null ? kind.name() : kind.name() + ":" + httpStatus;
    }
}
