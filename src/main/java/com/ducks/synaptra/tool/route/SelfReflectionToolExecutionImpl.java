package com.ducks.synaptra.tool.route;

import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.event.agent.model.AgentRequestEvent;
import com.ducks.synaptra.event.tool.model.ToolExecutionType;
import com.ducks.synaptra.event.tool.model.ToolRequestEvent;
import com.ducks.synaptra.memory.episode.EpisodeMemory;
import com.ducks.synaptra.memory.episode.model.RecordEvent;
import com.ducks.synaptra.tool.ToolExecution;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class SelfReflectionToolExecutionImpl implements ToolExecution {

  private static final String BASE_PROMPT =
      """
    HANDOFF: SELF-REFLECTION

    You are performing an internal self-reflection step within an agentic execution flow.

    Your role is to evaluate the current execution state based on:
    - The objective you were assigned
    - What has already been executed
    - The current and historical execution context

    Guidelines:
    - Reflect only on what has already happened and what is still missing.
    - Do not perform domain work, calculations, or data transformations.
    - Do not assume or infer missing information.
    - Do not expand or redefine the objective.
    - Do not communicate decisions directly to the user.

    Focus:
    - Identify gaps, incomplete steps, or missing information.
    - Analyze whether there are pending actions waiting to be executed (e.g., agents selected but not yet executed, queued or pending executions).
    - Determine if execution can continue, must pause, or must wait for a pending execution to be processed.
    - Decide whether the next step should be triggered immediately or deferred based on pending or blocked executions.

    This step exists solely to support correct continuation or completion of the execution flow.
    """;

  private final EpisodeMemory episodeMemory;
  private final ApplicationEventPublisher publisher;

  @Override
  public ToolExecutionType toolExecutionType() {
    return ToolExecutionType.SELF_REFLECTION;
  }

  @Override
  public void resolve(ToolRequestEvent toolRequestEvent) {
    List<RecordEvent> recordEventList =
        episodeMemory.getEpisodeMemory(
            toolRequestEvent.getSessionId(), toolRequestEvent.getAgent());

    Message handoffContext = new Message("system", BASE_PROMPT, null, null, null);

    Message episodicContext =
        new Message("system", buildDynamicEpisodePrompt(recordEventList), null, null, null);

    publisher.publishEvent(
        new AgentRequestEvent(
            toolRequestEvent.getSessionId(),
            toolRequestEvent.getAgent(),
            handoffContext,
            episodicContext,
            toolRequestEvent.getUser()));
  }

  private String buildDynamicEpisodePrompt(List<RecordEvent> events) {
    RecordEvent last = events.getLast();

    String currentStatus = last.getStatus() != null ? last.getStatus().name() : "UNKNOWN";
    String currentContent = safe(last.getContent());

    String history = buildHistory(events);

    return """
        # EPISODIC MEMORY.

        episodic memory purpose:
        - Capture the current state and the execution history so the system can resume correctly:
          what happened so far, where it stopped, and what is expected next.

        StatusType usage:
         - USER_INPUT_REQUEST: The system is explicitly requesting information from the user to continue execution.
         - WAIT_USER_INPUT: Execution is blocked while waiting for the user to provide the requested information.
         - AGENT_EXECUTION: An agent has started executing the assigned objective.
         - WAIT_AGENT_EXECUTION: The system delegated work to an agent and is actively waiting for its execution or result.
         - FINISHED_AGENT_EXECUTION: The agent has completed execution of its assigned objective.
         - WAIT_TOOL_EXECUTION: A tool execution was triggered and the system is waiting for its result.
         - FINISHED_TOOL_EXECUTION: The tool finished execution and its result has been received.
         - FINISHED: The overall objective has been fully completed.

        Content guidelines:
        - Clearly reflect the latest state while considering the full history.

        Current execution state (most recent):
        - status: %s
        - context: %s

        Execution history (oldest -> newest):
        %s
        """
        .formatted(currentStatus, currentContent, history)
        .trim();
  }

  private String buildHistory(List<RecordEvent> events) {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < events.size(); i++) {
      RecordEvent e = events.get(i);

      String status = e.getStatus() != null ? e.getStatus().name() : "UNKNOWN";
      String content = safe(e.getContent());

      sb.append(i + 1).append(". ").append(status).append(" - ").append(content);

      if (i < events.size() - 1) sb.append("\n");
    }
    return sb.toString();
  }

  private String safe(String s) {
    return s == null ? "" : s.trim();
  }
}
