package com.xinzhe.projectmentor.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChatCompletionRequest {

    private String model;

    private List<ChatMessage> messages;

    private Double temperature;

    @JsonProperty("max_tokens")
    private Integer maxTokens;
}