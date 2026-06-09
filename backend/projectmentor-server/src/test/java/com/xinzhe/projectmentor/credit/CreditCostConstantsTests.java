package com.xinzhe.projectmentor.credit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreditCostConstantsTests {

    @Test
    void exposesUnifiedAiCreditCosts() {
        assertThat(CreditCostConstants.REGISTER_GIFT).isEqualTo(10);
        assertThat(CreditCostConstants.AI_AUDIT_REPORT).isEqualTo(2);
        assertThat(CreditCostConstants.AI_CLAIM_EVIDENCE).isEqualTo(2);
        assertThat(CreditCostConstants.AI_PROJECT_QA).isEqualTo(1);
        assertThat(CreditCostConstants.AI_HALLUCINATION_CHECK).isEqualTo(1);
        assertThat(CreditCostConstants.AI_INTERVIEW_SESSION).isEqualTo(2);
        assertThat(CreditCostConstants.AI_RESUME_OPTIMIZE).isEqualTo(1);
    }
}
