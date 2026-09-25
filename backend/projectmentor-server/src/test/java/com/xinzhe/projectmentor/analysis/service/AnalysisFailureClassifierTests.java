package com.xinzhe.projectmentor.analysis.service;

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
        assertThat(classifier.classify(new BusinessException(ErrorCode.AI_SERVICE_ERROR, "upstream 503")))
                .isEqualTo(AnalysisFailureClassifier.FailureType.RETRYABLE);
    }

    @Test
    void validationCreditAndPermanentConfigurationFailuresAreNotRetryable() {
        assertThat(classifier.classify(new BusinessException(ErrorCode.CREDIT_NOT_ENOUGH, "insufficient")))
                .isEqualTo(AnalysisFailureClassifier.FailureType.NON_RETRYABLE);
        assertThat(classifier.classify(new BusinessException(ErrorCode.AI_SERVICE_ERROR, "AI 服务未配置")))
                .isEqualTo(AnalysisFailureClassifier.FailureType.NON_RETRYABLE);
        assertThat(classifier.classify(new IllegalArgumentException("invalid state")))
                .isEqualTo(AnalysisFailureClassifier.FailureType.NON_RETRYABLE);
    }
}
