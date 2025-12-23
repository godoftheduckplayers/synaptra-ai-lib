package com.ducks.synaptra.annotation;

import com.ducks.synaptra.config.AIAgenticConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Enables AI Agentic autoconfiguration for a Spring application.
 *
 * <p>This annotation is used to activate the {@link AIAgenticConfiguration} class, which
 * initializes and registers all required beans for AI Agentic integration within the Spring
 * context.
 *
 * <p>Typical usage involves adding this annotation to a Spring Boot application class or
 * configuration class to enable AI Agentic capabilities such as agent orchestration, task
 * execution, and AI service connectivity.
 *
 * <pre>{@code
 * @SpringBootApplication
 * @EnableAIAgentic
 * public class Application {
 *     public static void main(String[] args) {
 *         SpringApplication.run(Application.class, args);
 *     }
 * }
 * }</pre>
 *
 * @see AIAgenticConfiguration
 * @author Leandro Marques
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(AIAgenticConfiguration.class)
public @interface EnableAIAgentic {}
