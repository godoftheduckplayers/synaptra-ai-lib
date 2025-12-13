package com.ai.agentics.config;

import com.ai.agentics.annotation.EnableAIAgentic;
import com.ai.agentics.config.tracer.AIAgenticTracingProperties;
import com.ai.agentics.tracer.AIAgenticTracer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Core configuration class for enabling AI Agentic components and tracing support.
 *
 * <p>This class provides the foundational Spring configuration for the AI Agentic framework. It is
 * automatically imported when {@link EnableAIAgentic} is present on a configuration or application
 * class.
 *
 * <p>Responsibilities include:
 *
 * <ul>
 *   <li>Scanning the base package <b>com.ai.agentics</b> for components and services
 *   <li>Loading and binding tracing-related properties via {@link AIAgenticTracingProperties}
 *   <li>Registering the {@link AIAgenticTracer} bean for OpenTelemetry-based tracing
 * </ul>
 *
 * <p>The tracing system enables distributed trace collection and propagation across AI Agentic
 * modules and integrated Spring applications.
 *
 * <pre>{@code
 * @Configuration
 * @EnableAIAgentic
 * public class ApplicationConfiguration {
 *     // AI Agentic tracing and core components are now active
 * }
 * }</pre>
 *
 * @see EnableAIAgentic
 * @see AIAgenticTracer
 * @see AIAgenticTracingProperties
 * @author Leandro Marques
 * @since 1.0.0
 */
@Configuration
@EnableAsync
@ComponentScan(basePackages = "com.ai.agentics")
@EnableConfigurationProperties(AIAgenticTracingProperties.class)
public class AIAgenticConfiguration {

  /**
   * Creates and registers the {@link AIAgenticTracer} bean used for distributed tracing.
   *
   * <p>This tracer integrates with OpenTelemetry to emit trace data to the configured exporter
   * (e.g., Jaeger, OTLP), enabling detailed visibility across agent orchestration flows and AI
   * service operations.
   *
   * @param AIAgenticTracingProperties configuration properties for tracing setup
   * @return an initialized {@link AIAgenticTracer} instance
   */
  @Bean
  public AIAgenticTracer agenticAiTracer(AIAgenticTracingProperties AIAgenticTracingProperties) {
    return new AIAgenticTracer(AIAgenticTracingProperties);
  }
}
