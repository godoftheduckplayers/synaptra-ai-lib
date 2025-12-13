package com.ai.agentics.client.openai.data;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token-usage statistics for a Chat Completion.
 *
 * @param promptTokens Number of tokens in the input prompt.
 * @param completionTokens Number of tokens generated in the completion.
 * @param totalTokens Sum of prompt and completion tokens.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record Usage(
    @JsonProperty("prompt_tokens") Integer promptTokens,
    @JsonProperty("completion_tokens") Integer completionTokens,
    @JsonProperty("total_tokens") Integer totalTokens) {}
