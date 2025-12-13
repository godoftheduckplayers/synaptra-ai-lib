package com.ai.agentics.model.config;

/**
 * Defines the configuration parameters for an AI model provider execution.
 *
 * <p>This record encapsulates the core generation settings used when invoking a language model,
 * such as the model identifier and sampling parameters that influence randomness, diversity, and
 * response length.
 *
 * <p>The configuration is provider-agnostic and may be reused across different execution contexts
 * and AI providers, as long as the underlying provider supports the specified parameters.
 *
 * @param model The identifier of the language model to be used (e.g. {@code "gpt-4.1"}, {@code
 *     "gpt-4o-mini"}).
 * @param temperature Controls the randomness of the generated output. Higher values produce more
 *     creative responses, while lower values yield more deterministic results.
 * @param maxTokens The maximum number of tokens that the model is allowed to generate in the
 *     response.
 * @param topP Controls nucleus sampling by limiting token selection to those within the top
 *     cumulative probability mass.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record ProviderConfig(String model, double temperature, int maxTokens, double topP) {}
