package com.ducks.synaptra.config;

import io.micrometer.tracing.CurrentTraceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Configuration for the thread pool used to execute agent orchestration tasks.
 *
 * <p>This configuration defines the {@code agentExecutionExecutor}, which is responsible for
 * executing asynchronous agent-related workloads such as:
 *
 * <ul>
 *   <li>Agent execution events
 *   <li>Tool execution dispatching
 *   <li>Answer delivery
 *   <li>Record and memory handling
 * </ul>
 *
 * <p>The executor is configured with a bounded queue and a fixed upper limit on concurrency to
 * avoid unbounded resource consumption.
 *
 * <p>Tracing context propagation is enabled via {@link TraceTaskDecorator}, ensuring that
 * trace/span information is preserved across asynchronous boundaries.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Configuration
public class ThreadPoolTaskExecutorConfig {

  /**
   * Thread pool executor used for agent orchestration and execution flows.
   *
   * <p>Executor characteristics:
   *
   * <ul>
   *   <li>Core pool size: 4
   *   <li>Maximum pool size: 8
   *   <li>Queue capacity: 100
   *   <li>Thread name prefix: {@code ai-event-exec-}
   * </ul>
   *
   * <p>All tasks executed by this executor will automatically propagate the current tracing
   * context.
   *
   * @param currentTraceContext the trace context used to propagate spans across threads
   * @return an initialized {@link ThreadPoolTaskExecutor}
   */
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
