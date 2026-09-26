package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.ai.AiServiceException;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
@Component
public class AnalysisFailureClassifier {

    public enum FailureType {
        RETRYABLE,
        NON_RETRYABLE
    }

    public FailureType classify(Throwable throwable) {
        AiServiceException aiFailure = findCause(throwable, AiServiceException.class);
        if (aiFailure != null) {
            return aiFailure.isRetryable() ? FailureType.RETRYABLE : FailureType.NON_RETRYABLE;
        }
        if (containsCause(throwable, SocketTimeoutException.class)
                || containsCause(throwable, ConnectException.class)
                || containsCause(throwable, ResourceAccessException.class)
                || containsCause(throwable, TransientDataAccessException.class)) {
            return FailureType.RETRYABLE;
        }

        if (throwable instanceof BusinessException businessException) {
            if (businessException.getCode() == ErrorCode.AI_SERVICE_ERROR.getCode()) {
                return FailureType.NON_RETRYABLE;
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
        return findCause(throwable, type) != null;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }
        return null;
    }
}
