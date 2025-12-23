package com.ducks.synaptra.config.tracer;

import com.ducks.synaptra.tracer.AIAgenticTracer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for AI Agentic tracing integration.
 *
 * <p>This class binds tracing-related settings defined under the {@code ai.agentic.tracing} prefix
 * in the application's configuration file (e.g., {@code application.yml} or {@code
 * application.properties}).
 *
 * <p>It is used to control the tracing behavior of the AI Agentic framework, including enabling or
 * disabling distributed tracing, configuring the OTLP/Jaeger exporter endpoint, and defining the
 * logical service name associated with emitted traces.
 *
 * <h3>Example configuration</h3>
 *
 * <pre>
 * ai:
 *   agentic:
 *     tracing:
 *       enabled: true
 *       jaeger-endpoint: <a href="http://localhost:4318/v1/traces">...</a>
 *       service-name: duck-ledger-gateway
 * </pre>
 *
 * <p>If no {@code service-name} is provided, it defaults to {@code "ai-agentic"}.
 *
 * @see AIAgenticTracer
 * @see ConfigurationProperties
 * @author Leandro Marques
 * @since 1.0.0
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "ai.agentic.tracing")
public class AIAgenticTracingProperties {

  /** Default fallback name for the service if none is explicitly defined. */
  private static final String AI_AGENTIC = "ai-agentic";

  /** Whether distributed tracing is enabled for AI Agentic components. */
  private boolean enabled;

  /** The Jaeger or OTLP endpoint URL to which trace data will be exported. */
  private String jaegerEndpoint;

  /** The logical service name used to identify this application in the trace data. */
  private String serviceName;

  /**
   * Resolves the effective service name.
   *
   * <p>If no {@code serviceName} is configured, this method returns the default value {@code
   * "ai-agentic"}.
   *
   * @return the configured service name or the default value if not set
   */
  public String getServiceName() {
    return (serviceName != null && !serviceName.isBlank()) ? serviceName : AI_AGENTIC;
  }
}
