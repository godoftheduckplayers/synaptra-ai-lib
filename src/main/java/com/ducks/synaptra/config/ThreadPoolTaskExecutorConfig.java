package com.ducks.synaptra.config;

import io.micrometer.tracing.CurrentTraceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolTaskExecutorConfig {

  @Bean(name = "agentExecutionExecutor")
  public ThreadPoolTaskExecutor agentExecutionExecutor(CurrentTraceContext currentTraceContext) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);

    executor.setThreadNamePrefix("ai-event-exec-");
    executor.setTaskDecorator(new TraceTaskDecorator(currentTraceContext));

    executor.initialize();
    return executor;
  }
}
