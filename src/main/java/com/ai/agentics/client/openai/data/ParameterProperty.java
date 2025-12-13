package com.ai.agentics.client.openai.data;

/**
 * Describes a property definition for a function parameter used in the OpenAI Function-Calling
 * interface.
 *
 * <p>This record represents a single parameter attribute, including its data type and a
 * human-readable description. It is typically used as part of a structured schema to define the
 * expected input of a callable tool.
 *
 * @param type The data type of the parameter (e.g. {@code "string"}, {@code "number"}, {@code
 *     "boolean"}).
 * @param description A human-readable description explaining the purpose of the parameter.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record ParameterProperty(String type, String description) {}
