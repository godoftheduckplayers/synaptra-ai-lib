package com.ducks.synaptra.tool.route;

import static com.ducks.synaptra.memory.episode.model.StatusType.FINISHED_AGENT_EXECUTION;

import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.event.agent.model.AgentRequestEvent;
import com.ducks.synaptra.event.answer.AnswerExecutionListener;
import com.ducks.synaptra.event.answer.model.AnswerRequestEvent;
import com.ducks.synaptra.event.tool.model.ToolExecutionType;
import com.ducks.synaptra.event.tool.model.ToolRequestEvent;
import com.ducks.synaptra.memory.episode.EpisodeMemory;
import com.ducks.synaptra.memory.episode.model.RecordEvent;
import com.ducks.synaptra.model.agent.Agent;
import com.ducks.synaptra.state.AgentExecutionState;
import com.ducks.synaptra.tool.ToolExecution;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class RouteToParentToolExecutionImpl implements ToolExecution {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // Objective is fixed: ask the parent to self-reflect and verify whether anything is still
  // missing.
  private static final String BASE_PROMPT =
      """
       HANDOFF: SELF-REFLECTION (PARENT)

       Objective:
       Analyze whether there is still anything missing to be executed in order to fully complete the request.

       Summary (what was done by the child agent):
       $summary

       Completion rule:
       - If no further actions, inputs, or delegations are required, produce a final closing message
         summarizing the outcome and confirming that the request has been fully completed.

       Constraints:
       - Do not assume missing data.
       - Use the summary and episodic context to determine what is missing or if execution is complete.
       - Do not expand or redefine the original objective.
       """;

  private final RouteToAgentToolExecutionImpl routeToAgentToolExecution;
  private final EpisodeMemory episodeMemory;
  private final ApplicationEventPublisher publisher;
  private final List<AnswerExecutionListener> answerExecutionListenerList;

  @Override
  public ToolExecutionType toolExecutionType() {
    return ToolExecutionType.ROUTE_TO_PARENT;
  }

  @Override
  public void resolve(ToolRequestEvent toolRequestEvent) {
    ToolRequestEvent nextToolRequestEvent = AgentExecutionState.getNextToolRequestEvent();
    if (nextToolRequestEvent != null) {
      routeToAgentToolExecution.resolve(nextToolRequestEvent);
    } else {
      try {
        RouteParentMapper routeMapper =
            MAPPER.readValue(
                toolRequestEvent.getToolCall().function().arguments(), RouteParentMapper.class);

        if (toolRequestEvent.getAgent().isSupportsInterimMessages()) {
          answerExecutionListenerList.forEach(
              answerExecutionListener ->
                  answerExecutionListener.onAnswerExecutionResponseEvent(
                      new AnswerRequestEvent(
                          toolRequestEvent.getSessionId(),
                          toolRequestEvent.getAgent(),
                          toolRequestEvent.getUser(),
                          routeMapper.response())));
        }

        Agent parentAgent = toolRequestEvent.getAgent().getParent();
        if (parentAgent == null) {
          throw new RuntimeException(
              "Failed to route execution to parent: current agent has no parent configured.");
        }

        // Record a human-readable event for the parent, so history shows what the child completed.
        episodeMemory.registerEvent(
            toolRequestEvent.getSessionId(),
            parentAgent,
            new RecordEvent(
                "Agent execution finished. Summary: " + safe(routeMapper.summary()),
                FINISHED_AGENT_EXECUTION));

        String handoffPrompt = BASE_PROMPT.replace("$summary", safe(routeMapper.summary()));
        Message handoffContext = new Message("system", handoffPrompt, null, null, null);

        List<RecordEvent> recordEventList =
            episodeMemory.getEpisodeMemory(toolRequestEvent.getSessionId(), parentAgent);
        String episodePrompt = buildDynamicEpisodePrompt(recordEventList);
        Message episodeContext = new Message("system", episodePrompt, null, null, null);

        publisher.publishEvent(
            new AgentRequestEvent(
                toolRequestEvent.getSessionId(),
                parentAgent,
                handoffContext,
                null,
                toolRequestEvent.getUser()));

      } catch (JsonProcessingException e) {
        throw new RuntimeException(
            "Failed to parse route_to_parent arguments: "
                + toolRequestEvent.getToolCall().function().arguments(),
            e);
      }
    }
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
