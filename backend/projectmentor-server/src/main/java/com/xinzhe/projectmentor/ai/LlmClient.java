package com.xinzhe.projectmentor.ai;

import com.xinzhe.projectmentor.ai.dto.AiAuditResult;

public interface LlmClient {

    AiAuditResult generateAuditReport(String prompt);

    String chat(String module, String systemPrompt, String userPrompt);
}
