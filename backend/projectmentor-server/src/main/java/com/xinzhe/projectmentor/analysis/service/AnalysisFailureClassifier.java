package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.Locale;

@Component
public class AnalysisFailureClassifier {

    public enum FailureType {
        RETRYABLE,
        NON_RETRYABLE
    }

    public FailureType classify(Throwable throwable) {
        if (containsCause(throwable, SocketTimeoutException.class)
                || containsCause(throwable, ConnectException.class)
                || containsCause(throwable, ResourceAccessException.class)
                || containsCause(throwable, TransientDataAccessException.class)) {
            return FailureType.RETRYABLE;
        }

        if (throwable instanceof BusinessException businessException) {
            if (businessException.getCode() == ErrorCode.AI_SERVICE_ERROR.getCode()) {
                String message = String.valueOf(businessException.getMessage()).toLowerCase(Locale.ROOT);
                if (message.contains("未启用") || message.contains("未配置")
                        || message.contains("返回为空") || message.contains("内容为空")) {
                    return FailureType.NON_RETRYABLE;
                }
                return FailureType.RETRYABLE;
            }
            if (businessException.getCode() == ErrorCode.CREDIT_NOT_ENOUGH.getCode()
                    || businessException.getCode() == ErrorCode.PARAM_ERROR.getCode()
                    || businessException.getCode() == ErrorCode.NOT_FOUND.getCode()
                    || businessException.getCode() == ErrorCode.FORBIDDEN.getCode()
                    || businessException.getCode() == ErrorCode.UNAUTHORIZED.getCode()) {
                return FailureType.NON_RETRYABLE;
            }
        }
        return FailureType.NON_RETRYABLE;
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
