package com.ducks.synaptra.client;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Abstract base class for configurable reactive HTTP clients.
 *
 * <p>This class provides common configuration parameters for clients that perform HTTP calls using
 * {@link WebClient} or {@link HttpClient}, allowing fine-grained control over network timeouts and
 * behavior.
 *
 * <p>The attributes define maximum allowed durations for connection, read, and write operations —
 * particularly useful when integrating with external services that require resilience and
 * performance control.
 *
 * <h2>Typical Usage:</h2>
 *
 * <pre>{@code
 * public class MyApiClient extends AbstractClient {
 *   public MyApiClient() {
 *     super(5000L, 5000L, 5000L, 5000L);
 *   }
 * }
 * }</pre>
 *
 * <h2>Fields:</h2>
 *
 * <ul>
 *   <li><b>connectionTimeout</b> — Maximum time (in ms) to establish the TCP connection
 *   <li><b>readTimeout</b> — Maximum time (in ms) to wait for the HTTP response
 *   <li><b>readTimeoutHandler</b> — Maximum time (in ms) without receiving data after connection
 *   <li><b>writeTimeoutHandler</b> — Maximum time (in ms) without sending data during write
 * </ul>
 *
 * <p>Concrete subclasses extending {@code AbstractClient} should implement the specific logic for
 * constructing the HTTP client or executing API requests.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
public abstract class AbstractClient {

  /** Maximum time in milliseconds to establish the TCP connection. */
  private final Integer connectionTimeout;

  /** Maximum time in milliseconds to wait for the HTTP response. */
  private final Integer responseTimeoutMs;

  /** Maximum time in milliseconds without receiving data after connection. */
  private final Integer readTimeoutHandler;

  /** Maximum time in milliseconds without sending data during write. */
  private final Integer writeTimeoutHandler;

  /**
   * Constructs a new {@code AbstractClient} instance.
   *
   * @param connectionTimeout the maximum time (in ms) to establish the TCP connection
   * @param responseTimeoutMs the maximum time (in ms) to wait for the response
   * @param readTimeout the maximum time (in ms) without receiving data after connection
   * @param writeTimeout the maximum time (in ms) without sending data during write
   */
  protected AbstractClient(
      Integer connectionTimeout,
      Integer responseTimeoutMs,
      Integer readTimeout,
      Integer writeTimeout) {
    this.connectionTimeout = connectionTimeout;
    this.responseTimeoutMs = responseTimeoutMs;
    this.readTimeoutHandler = readTimeout;
    this.writeTimeoutHandler = writeTimeout;
  }

  /**
   * Builds a new {@link WebClient} instance backed by a Reactor Netty {@link HttpClient} configured
   * with the timeout settings defined in this client.
   *
   * <p>This method creates a {@link WebClient} using a {@link ReactorClientHttpConnector} that
   * wraps the {@link HttpClient} returned by {@link #buildHttpClient()}. The resulting client
   * supports non-blocking, reactive HTTP communication with connection, response, read, and write
   * timeouts applied at the network level.
   *
   * <p>Each call to this method produces a new {@link WebClient} instance. For performance and
   * tracing consistency, it is recommended that subclasses create the {@code WebClient} once and
   * reuse it across requests.
   *
   * @return a fully configured {@link WebClient} instance with custom timeout handling
   */
  protected WebClient buildWebClient() {
    HttpClient httpClient = buildHttpClient();
    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient)).build();
  }

  /**
   * Builds and configures a Reactor Netty {@link HttpClient} instance with custom timeout settings.
   *
   * <p>This method defines low-level network behavior for all HTTP requests executed by the {@link
   * WebClient} built from this client. It applies connection, response, read, and write timeouts to
   * ensure predictable performance and to prevent hanging connections.
   *
   * <p>Specifically:
   *
   * <ul>
   *   <li><b>Connection timeout</b> — maximum time allowed to establish the TCP connection (via
   *       {@link ChannelOption#CONNECT_TIMEOUT_MILLIS}).
   *   <li><b>Response timeout</b> — maximum time to wait for the server to send an HTTP response
   *       (via {@link HttpClient#responseTimeout(Duration)}).
   *   <li><b>Read timeout</b> — maximum time without receiving data once connected (via {@link
   *       ReadTimeoutHandler}).
   *   <li><b>Write timeout</b> — maximum time without sending data while writing a request (via
   *       {@link WriteTimeoutHandler}).
   * </ul>
   *
   * <p>The resulting {@link HttpClient} is fully non-blocking and suitable for use with {@link
   * WebClient} through {@link ReactorClientHttpConnector}.
   *
   * @return a configured {@link HttpClient} instance with applied timeout handlers
   */
  private HttpClient buildHttpClient() {
    return HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectionTimeout)
        .responseTimeout(Duration.ofMillis(this.responseTimeoutMs))
        .doOnConnected(
            conn ->
                conn.addHandlerLast(
                        new ReadTimeoutHandler(this.readTimeoutHandler, TimeUnit.MILLISECONDS))
                    .addHandlerLast(
                        new WriteTimeoutHandler(this.writeTimeoutHandler, TimeUnit.MILLISECONDS)));
  }
}
