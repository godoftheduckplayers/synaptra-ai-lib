package com.ducks.synaptra.orchestration.event.tool;

import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.orchestration.event.record.RecordExecutionEvent;
import com.ducks.synaptra.orchestration.event.record.contract.RecordRequestEvent;
import com.ducks.synaptra.orchestration.event.tool.contract.ToolResponseEvent;
import com.ducks.synaptra.prompt.HandoffContextPublisher;
import com.ducks.synaptra.prompt.RecordEventPublisher;
import com.ducks.synaptra.prompt.contract.RecordEvent;
import java.util.List;
import java.util.Objects;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Dispatches tool execution signals produced during agent execution.
 *
 * <p>This component listens for {@link ToolResponseEvent} events and determines how each tool call
 * should be handled. It distinguishes between internal orchestration tools (used to control the
 * agentic workflow) and external tools (which must be executed by dedicated tool listeners).
 *
 * <p>Internal system tools include:
 *
 * <ul>
 *   <li>{@code route_to_agent}: triggers a handoff to another agent
 *   <li>{@code record_event}: persists an orchestration record into episodic memory
 * </ul>
 *
 * <p>For any external tool call, a {@code WAIT_TOOL_EXECUTION} record is emitted and the execution
 * request is forwarded to all registered {@link ToolExecutionListener}s.
 *
 * <p>This class does not execute tools directly; it only coordinates routing and notification.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Service
public class ToolExecutionEvent {

  /** Internal system function used to route execution back to another agent. */
  private static final String ROUTE_TO_AGENT = "route_to_agent";

  /** Internal system function used to persist orchestration records. */
  private static final String RECORD_EVENT = "record_event";

  private final HandoffContextPublisher handoffContextPublisher;
  private final RecordEventPublisher recordEventPublisher;
  private final List<ToolExecutionListener> toolExecutionListenerList;

  public ToolExecutionEvent(
      HandoffContextPublisher handoffContextPublisher,
      RecordEventPublisher recordEventPublisher,
      List<ToolExecutionListener> toolExecutionListenerList) {
    this.handoffContextPublisher = Objects.requireNonNull(handoffContextPublisher);
    this.recordEventPublisher = Objects.requireNonNull(recordEventPublisher);
    this.toolExecutionListenerList = Objects.requireNonNull(toolExecutionListenerList);
  }

  /**
   * Handles a tool response event, routing internal system tools and notifying external tool
   * execution listeners when required.
   *
   * @param toolResponseEvent the tool response event produced by the agent execution layer
   */
  @LogTracer(spanName = "tool_execution_event")
  @Async("agentExecutionExecutor")
  @EventListener
  public void onToolExecutionEvent(ToolResponseEvent toolResponseEvent) {
    String toolName = toolResponseEvent.toolCall().function().name();

    handleInternalOrchestration(toolName, toolResponseEvent);

    if (!isInternalFunction(toolName)) {
      toolExecutionListenerList.forEach(
          listener -> listener.onToolExecutionResponseEvent(toolResponseEvent));
    }
  }

  private void handleInternalOrchestration(String toolName, ToolResponseEvent toolResponseEvent) {

    if (ROUTE_TO_AGENT.equals(toolName)) {
      handoffContextPublisher.publishEvent(toolResponseEvent);
      return;
    }

    if (RECORD_EVENT.equals(toolName)) {
      recordEventPublisher.publishEvent(toolResponseEvent);
      return;
    }

    // External tool call: register a WAIT_TOOL_EXECUTION record
    recordEventPublisher.publishEvent(
        new RecordRequestEvent(
            toolResponseEvent.sessionId(),
            toolResponseEvent.agent(),
            toolResponseEvent.user(),
            new RecordEvent(
                "Waiting for the tool execution.", RecordExecutionEvent.WAIT_TOOL_EXECUTION)));
  }

  /**
   * Determines whether a tool name represents an internal orchestration function.
   *
   * @param toolName the tool/function name
   * @return {@code true} if the function is internal, {@code false} otherwise
   */
  public boolean isInternalFunction(String toolName) {
    return ROUTE_TO_AGENT.equals(toolName) || RECORD_EVENT.equals(toolName);
  }
}
