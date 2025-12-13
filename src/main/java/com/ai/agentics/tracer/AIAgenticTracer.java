package com.ai.agentics.tracer;

import com.ai.agentics.config.tracer.AIAgenticTracingProperties;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides distributed tracing support for AI Agentic components using OpenTelemetry.
 *
 * <p>The {@code AIAgenticTracer} class initializes and configures an OpenTelemetry {@link Tracer}
 * instance to enable end-to-end observability across AI Agentic modules and their interactions with
 * external systems.
 *
 * <p>When tracing is enabled via {@link AIAgenticTracingProperties}, this class builds an OTLP HTTP
 * exporter (e.g., Jaeger, OpenTelemetry Collector), configures the {@link SdkTracerProvider}, and
 * registers it as the global tracer provider.
 *
 * <p>All AI Agentic services can then obtain a shared tracer instance from {@link
 * GlobalOpenTelemetry#getTracer(String)} and use it to create spans that represent key operations
 * or agent interactions.
 *
 * <h3>Key responsibilities:</h3>
 *
 * <ul>
 *   <li>Initialize an OTLP exporter for span data transmission
 *   <li>Attach service metadata (e.g., {@code service.name}) to emitted traces
 *   <li>Provide a globally available {@link Tracer} instance
 *   <li>Gracefully degrade to a no-op tracer when tracing is disabled or fails
 * </ul>
 *
 * <h3>Example usage:</h3>
 *
 * <pre>{@code
 * @Component
 * public class AgentTaskHandler {
 *     private final Tracer tracer;
 *
 *     public AgentTaskHandler(AIAgenticTracer aiTracer) {
 *         this.tracer = aiTracer.getTracer();
 *     }
 *
 *     public void execute() {
 *         var span = tracer.spanBuilder("task-execution").startSpan();
 *         try {
 *             // Perform business logic
 *         } finally {
 *             span.end();
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>If tracing is disabled, the tracer is initialized as a no-op instance from {@link
 * GlobalOpenTelemetry}, ensuring that instrumentation calls remain safe and lightweight.
 *
 * @see AIAgenticTracingProperties
 * @see Tracer
 * @see OtlpHttpSpanExporter
 * @see SdkTracerProvider
 * @author Leandro Marques
 * @since 1.0.0
 */
@Getter
public class AIAgenticTracer {

  private static final Logger logger = LoggerFactory.getLogger(AIAgenticTracer.class);

  /** Default tracer name identifier for AI Agentic instrumentation. */
  private static final String AI_AGENTIC = "ai-agentic";

  /** OpenTelemetry resource attribute key for the logical service name. */
  private static final String SERVICE_NAME = "service.name";

  /** The configured OpenTelemetry {@link Tracer} used for span creation. */
  private Tracer tracer;

  /**
   * Constructs and initializes the AI Agentic tracer instance.
   *
   * <p>Depending on the {@link AIAgenticTracingProperties#isEnabled()} flag, this constructor
   * either initializes a fully configured OTLP tracer or falls back to a no-op tracer to avoid
   * runtime errors in environments where tracing is disabled.
   *
   * @param properties tracing configuration properties (endpoint, service name, etc.)
   */
  public AIAgenticTracer(AIAgenticTracingProperties properties) {
    final String serviceName = properties.getServiceName();
    if (properties.isEnabled()) {
      try {
        OtlpHttpSpanExporter exporter = createExporter(properties);
        Resource resource = createResource(serviceName);
        SdkTracerProvider tracerProvider = createTracerProvider(resource, exporter);

        OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).buildAndRegisterGlobal();

        this.tracer = GlobalOpenTelemetry.getTracer(AI_AGENTIC);
        logger.info(
            "AgenticAiTracer initialized with Jaeger endpoint: {} and service.name={}",
            properties.getJaegerEndpoint(),
            serviceName);
      } catch (Exception e) {
        logger.warn("Failed to initialize OTLP Jaeger tracer, using no-op tracer", e);
        this.tracer = GlobalOpenTelemetry.getTracer(AI_AGENTIC);
      }
    } else {
      this.tracer = GlobalOpenTelemetry.getTracer(AI_AGENTIC);
      logger.info("AgenticAiTracer is disabled, using no-op tracer");
    }
  }

  /**
   * Creates a configured OTLP HTTP span exporter based on the provided properties.
   *
   * @param properties tracing configuration
   * @return a new {@link OtlpHttpSpanExporter} instance
   */
  private OtlpHttpSpanExporter createExporter(AIAgenticTracingProperties properties) {
    return OtlpHttpSpanExporter.builder().setEndpoint(properties.getJaegerEndpoint()).build();
  }

  /**
   * Creates a {@link Resource} that includes the logical service name and default system
   * attributes.
   *
   * @param serviceName logical name of the current service
   * @return the merged {@link Resource} instance
   */
  private Resource createResource(String serviceName) {
    return Resource.getDefault()
        .merge(Resource.create(Attributes.of(AttributeKey.stringKey(SERVICE_NAME), serviceName)));
  }

  /**
   * Builds the {@link SdkTracerProvider} that manages tracer lifecycle and span processing.
   *
   * @param resource the OpenTelemetry resource metadata
   * @param exporter the span exporter to use
   * @return an initialized {@link SdkTracerProvider}
   */
  private SdkTracerProvider createTracerProvider(Resource resource, OtlpHttpSpanExporter exporter) {
    return SdkTracerProvider.builder()
        .setResource(resource)
        .addSpanProcessor(BatchSpanProcessor.builder(exporter).build())
        .build();
  }
}
