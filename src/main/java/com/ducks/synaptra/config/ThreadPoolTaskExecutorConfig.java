package com.ducks.synaptra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class ThreadPoolTaskExecutorConfig {

  @Bean(name = "agentExecutionExecutor")
  public ThreadPoolTaskExecutor agentExecutionExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);

    executor.setThreadNamePrefix("ai-event-exec-");

    executor.initialize();
    return executor;
  }
}
