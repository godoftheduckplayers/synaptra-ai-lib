package com.ai.agentics.client.openai;

import com.ai.agentics.client.AbstractClient;
import com.ai.agentics.client.openai.data.ChatCompletionRequest;
import com.ai.agentics.client.openai.data.ChatCompletionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

/**
 * Reactive HTTP client responsible for communicating with the OpenAI API.
 *
 * <p>This client extends {@link AbstractClient}, inheriting network-level timeout configuration
 * (connection, response, read, and write) and providing a ready-to-use {@link WebClient} for
 * executing non-blocking HTTP requests.
 *
 * <p>The configuration values (base URL and timeouts) are externalized and injected from the
 * application environment using {@link Value} annotations. This enables flexible tuning of
 * connection parameters across different deployment environments.
 *
 * <h2>Configuration Properties:</h2>
 *
 * <ul>
 *   <li><b>ai.agentics.client.openai.url</b> — Base URL of the OpenAI API
 *   <li><b>ai.agentics.client.openai.connect-timeout-ms</b> — Maximum time (in ms) to establish the
 *       TCP connection
 *   <li><b>ai.agentics.client.openai.response-timeout-ms</b> — Maximum time (in ms) to wait for the
 *       HTTP response
 *   <li><b>ai.agentics.client.openai.read-timeout-ms</b> — Maximum time (in ms) without receiving
 *       data after connection
 *   <li><b>ai.agentics.client.openai.write-timeout-ms</b> — Maximum time (in ms) without sending
 *       data during write
 * </ul>
 *
 * <h2>Usage Example:</h2>
 *
 * <pre>{@code
 * @Service
 * public class ChatService {
 *   private final OpenAIClient openAIClient;
 *
 *   public ChatService(OpenAIClient openAIClient) {
 *     this.openAIClient = openAIClient;
 *   }
 *
 *   public Mono<Response> sendPrompt(Request prompt) {
 *     return openAIClient.getWebClient()
 *         .post()
 *         .uri("/v1/chat/completions")
 *         .bodyValue(prompt)
 *         .retrieve()
 *         .bodyToMono(Response.class);
 *   }
 * }
 * }</pre>
 *
 * <p>The {@code OpenAIClient} is a Spring-managed {@link Component}, allowing it to be injected and
 * reused throughout the application while maintaining a single configured {@link WebClient}
 * instance.
 *
 * @see AbstractClient
 * @see WebClient
 * @see HttpClient
 * @author Leandro Marques
 * @since 1.0.0
 */
@Component
public class OpenAIClient extends AbstractClient {

  private static final Logger logger = LoggerFactory.getLogger(OpenAIClient.class);

  private final WebClient webClient;

  @Value("${ai.agentics.client.openai.url}")
  private String url;

  @Value("${ai.agentics.client.openai.api-key}")
  private String apiKey;

  /**
   * Constructs a new {@code OpenAIClient} instance with timeout configuration values provided via
   * application properties.
   *
   * <p>The created client uses a {@link WebClient} configured with a Reactor Netty {@link
   * HttpClient} that enforces all defined timeout limits.
   *
   * @param connectionTimeout maximum time (in ms) to establish the TCP connection
   * @param responseTimeout maximum time (in ms) to wait for the HTTP response
   * @param readTimeout maximum time (in ms) without receiving data after connection
   * @param writeTimeout maximum time (in ms) without sending data during write
   */
  protected OpenAIClient(
      @Value("${ai.agentics.client.openai.connect-timeout-ms:5000}") Integer connectionTimeout,
      @Value("${ai.agentics.client.openai.response-timeout-ms:5000}") Integer responseTimeout,
      @Value("${ai.agentics.client.openai.read-timeout-ms:5000}") Integer readTimeout,
      @Value("${ai.agentics.client.openai.write-timeout-ms:5000}") Integer writeTimeout) {
    super(connectionTimeout, responseTimeout, readTimeout, writeTimeout);
    this.webClient = buildWebClient();
  }

  /**
   * Sends a chat-completion request to the OpenAI API with optional function-calling support.
   *
   * <p>This method performs a POST request to the configured OpenAI endpoint, submitting the
   * specified {@link ChatCompletionRequest} payload. It supports OpenAI’s function-calling (tools)
   * mechanism, allowing the model to return {@code tool_calls} that can be interpreted and executed
   * by the application.
   *
   * <p>The {@code requestId} parameter can be used to trace or correlate this API invocation with
   * upstream requests, distributed traces, or internal logs. It is not sent to OpenAI by default
   * but can be included in headers or structured logging for observability and debugging.
   *
   * <h2>Behavior:</h2>
   *
   * <ul>
   *   <li>Performs a POST to {@code /v1/chat/completions} using {@link WebClient}.
   *   <li>Includes {@code Authorization} and {@code Content-Type} headers automatically.
   *   <li>Serializes the {@link ChatCompletionRequest} as JSON.
   *   <li>Parses the response body into a {@link ChatCompletionResponse}.
   *   <li>Supports network timeouts defined in {@link AbstractClient}.
   * </ul>
   *
   * @param requestId a unique identifier used for tracing or log correlation of this request
   * @param request the {@link ChatCompletionRequest} containing model, messages, and tool
   *     definitions
   * @return a {@link ChatCompletionResponse} containing generated messages or tool calls
   */
  public ChatCompletionResponse call(String requestId, ChatCompletionRequest request) {
    return this.webClient
        .post()
        .uri(this.url)
        .header("Authorization", "Bearer " + apiKey)
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(ChatCompletionResponse.class)
        .onErrorResume(
            e -> {
              logger.error(
                  "message={}, requestId={}, cause={}",
                  "Error calling OpenAI API",
                  requestId,
                  e.getMessage());
              return Mono.empty();
            })
        .block();
  }
}
