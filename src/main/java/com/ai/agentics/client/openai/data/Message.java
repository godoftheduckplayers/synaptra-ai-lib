package com.ai.agentics.client.openai.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Represents a single message in the Chat Completions conversation.
 *
 * <p>Messages are exchanged between the {@code system}, {@code user}, {@code assistant}, and {@code
 * tool} roles. Assistant messages may include {@code tool_calls} when the model decides to invoke a
 * function, and tool messages must include the corresponding {@code tool_call_id}.
 *
 * @param role The role of the message author ("system", "user", "assistant", or "tool").
 * @param content Natural-language text or JSON content produced by a tool.
 * @param name Optional function or username (used when {@code role="function"} or for
 *     identification).
 * @param toolCallId Identifier linking a tool’s output to the {@code tool_calls} entry it fulfills.
 * @param toolCalls List of tool invocations returned by the model.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record Message(
    String role,
    String content,
    String name,
    @JsonProperty("tool_call_id") String toolCallId,
    @JsonProperty("tool_calls") List<ToolCall> toolCalls) {}
