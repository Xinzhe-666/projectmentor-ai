package com.xinzhe.projectmentor.analysis.service;

import com.xinzhe.projectmentor.ai.AiServiceException;
import com.xinzhe.projectmentor.common.BusinessException;
import com.xinzhe.projectmentor.common.ErrorCode;
import org.junit.jupiter.api.Test;

import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisFailureClassifierTests {

    private final AnalysisFailureClassifier classifier = new AnalysisFailureClassifier();

    @Test
    void transientNetworkAndAiFailuresAreRetryable() {
        assertThat(classifier.classify(new RuntimeException(new SocketTimeoutException("timeout"))))
                .isEqualTo(AnalysisFailureClassifier.FailureType.RETRYABLE);
        assertRetryable(AiServiceException.timeout(new SocketTimeoutException("timeout")));
        assertRetryable(AiServiceException.fromHttpStatus(408, null));
        assertRetryable(AiServiceException.fromHttpStatus(429, null));
        assertRetryable(AiServiceException.fromHttpStatus(500, null));
        assertRetryable(AiServiceException.fromHttpStatus(503, null));
    }

    @Test
    void validationCreditAndPermanentConfigurationFailuresAreNotRetryable() {
        assertThat(classifier.classify(new BusinessException(ErrorCode.CREDIT_NOT_ENOUGH, "insufficient")))
                .isEqualTo(AnalysisFailureClassifier.FailureType.NON_RETRYABLE);
        assertNonRetryable(AiServiceException.disabled());
        assertNonRetryable(AiServiceException.missingApiKey());
        assertNonRetryable(AiServiceException.fromHttpStatus(400, null));
        assertNonRetryable(AiServiceException.fromHttpStatus(401, null));
        assertNonRetryable(AiServiceException.fromHttpStatus(403, null));
        assertNonRetryable(AiServiceException.fromHttpStatus(404, null));
        assertNonRetryable(AiServiceException.invalidResponse());
        assertThat(classifier.classify(new IllegalArgumentException("invalid state")))
                .isEqualTo(AnalysisFailureClassifier.FailureType.NON_RETRYABLE);
    }

    private void assertRetryable(Throwable failure) {
        assertThat(classifier.classify(failure)).isEqualTo(AnalysisFailureClassifier.FailureType.RETRYABLE);
    }

    private void assertNonRetryable(Throwable failure) {
        assertThat(classifier.classify(failure)).isEqualTo(AnalysisFailureClassifier.FailureType.NON_RETRYABLE);
    }
}
