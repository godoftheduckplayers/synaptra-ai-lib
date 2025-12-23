package com.ducks.synaptra.client.openai.data;

/**
 * Represents a tool invocation issued by the model inside a message’s {@code tool_calls} array.
 *
 * @param id Unique identifier of the tool call.
 * @param type Type of tool being invoked (typically {@code "function"}).
 * @param function Details of the function call including name and arguments.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record ToolCall(String id, String type, FunctionCall function) {}
