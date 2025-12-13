package com.ai.agentics.client.openai.data;

import java.util.List;

/**
 * Represents the response body returned by the Chat Completions API.
 *
 * <p>Includes generated choices (messages or tool calls) and token-usage statistics for cost
 * tracking.
 *
 * @param id Unique identifier of the completion request.
 * @param object Type of object returned (usually {@code "chat.completion"}).
 * @param created Unix timestamp of creation.
 * @param model Name of the model that generated the output.
 * @param choices List of generated {@link Choice} entries.
 * @param usage Token usage metrics.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record ChatCompletionResponse(
    String id, String object, Long created, String model, List<Choice> choices, Usage usage) {}
