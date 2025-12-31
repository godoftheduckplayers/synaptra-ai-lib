package com.ducks.synaptra.config;

import io.micrometer.tracing.CurrentTraceContext;
import lombok.NonNull;
import org.springframework.core.task.TaskDecorator;

public class TraceTaskDecorator implements TaskDecorator {
  private final CurrentTraceContext currentTraceContext;

  public TraceTaskDecorator(CurrentTraceContext currentTraceContext) {
    this.currentTraceContext = currentTraceContext;
  }

  @Override
  public @NonNull Runnable decorate(@NonNull Runnable runnable) {
    return currentTraceContext.wrap(runnable);
  }
}
