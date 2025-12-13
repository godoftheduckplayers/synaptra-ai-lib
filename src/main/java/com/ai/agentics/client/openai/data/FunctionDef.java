package com.ai.agentics.client.openai.data;


/**
 * Defines a function that can be invoked by the model via tool-calling.
 *
 * <p>The parameters are expressed as a JSON Schema document describing expected input fields, their
 * types, and validation constraints.
 *
 * @param name Name of the function to be called.
 * @param description Human-readable explanation of what the function does.
 * @param parameters JSON Schema node describing the function parameters.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record FunctionDef(String name, String description, Parameter parameters) {}
