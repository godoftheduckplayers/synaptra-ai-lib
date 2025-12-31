package com.ducks.synaptra.config;

import io.micrometer.tracing.CurrentTraceContext;
import lombok.NonNull;
import org.springframework.core.task.TaskDecorator;

/**
 * {@link TaskDecorator} implementation that propagates the current tracing context to asynchronous
 * tasks.
 *
 * <p>This decorator ensures that Micrometer/OpenTelemetry trace context (e.g. traceId, spanId) is
 * preserved when execution crosses thread boundaries, such as when using {@code @Async}, {@link
 * org.springframework.core.task.TaskExecutor}, or custom thread pools.
 *
 * <p>Without this decorator, asynchronous executions would lose their parent span information,
 * resulting in broken or disconnected traces.
 *
 * <p>This class is typically registered in a {@link org.springframework.core.task.TaskExecutor}
 * configuration and applied to executors that handle agent execution, tool execution, or
 * orchestration events.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
public class TraceTaskDecorator implements TaskDecorator {

  private final CurrentTraceContext currentTraceContext;

  /**
   * Creates a new {@link TraceTaskDecorator}.
   *
   * @param currentTraceContext the current trace context used to wrap asynchronous tasks
   */
  public TraceTaskDecorator(CurrentTraceContext currentTraceContext) {
    this.currentTraceContext = currentTraceContext;
  }

  /**
   * Wraps the given {@link Runnable} so that the current trace context is propagated to the
   * execution thread.
   *
   * @param runnable the original task to be executed
   * @return a wrapped {@link Runnable} with trace context propagation
   */
  @Override
  public @NonNull Runnable decorate(@NonNull Runnable runnable) {
    return currentTraceContext.wrap(runnable);
  }
}
