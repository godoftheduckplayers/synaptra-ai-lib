package com.ai.agentics.client.openai.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a request body for the OpenAI Chat Completions API supporting the Function-Calling
 * (tools) feature.
 *
 * <p>Contains the model name, conversation messages, tool definitions, optional tool-choice
 * behavior, and common generation parameters such as temperature and token limits.
 *
 * @param model The model identifier (for example {@code "gpt-4o-mini"}).
 * @param messages Ordered list of chat messages forming the conversation.
 * @param tools List of {@link Tool} objects describing callable functions.
 * @param toolChoice Either {@code "auto"} or an explicit function-selection object.
 * @param temperature Sampling temperature for creative variability.
 * @param maxTokens Maximum number of tokens to generate in the completion.
 * @param topP Nucleus-sampling probability (alternative to temperature).
 * @author Leandro Marques
 * @since 1.0.0
 */
public record ChatCompletionRequest(
    String model,
    List<Message> messages,
    List<Tool> tools,
    @JsonProperty("tool_choice") Object toolChoice,
    Double temperature,
    @JsonProperty("max_tokens") Integer maxTokens,
    @JsonProperty("top_p") Double topP) {}
