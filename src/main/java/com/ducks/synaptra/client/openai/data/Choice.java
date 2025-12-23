package com.ducks.synaptra.client.openai.data;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents one possible completion choice produced by the model.
 *
 * <p>Each choice includes the assistant message (which may contain {@code tool_calls}), its index
 * in the list, and a finish reason.
 *
 * @param index Position of this choice within the response list.
 * @param message The generated {@link Message} from the assistant.
 * @param finishReason Why generation stopped (e.g. "stop", "length", "tool_calls").
 * @param logprobs Optional log-probability data if requested.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record Choice(
    Integer index,
    Message message,
    @JsonProperty("finish_reason") String finishReason,
    Object logprobs) {}
