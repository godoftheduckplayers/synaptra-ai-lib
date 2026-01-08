package com.ducks.synaptra.event;

import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.event.agent.model.AgentRequestEvent;
import com.ducks.synaptra.memory.episode.EpisodeMemory;
import com.ducks.synaptra.memory.episode.model.RecordEvent;
import com.ducks.synaptra.memory.episode.model.StatusType;
import com.ducks.synaptra.model.agent.Agent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class UserInputMessagePublisher {

  private final ApplicationEventPublisher publisher;
  private final EpisodeMemory episodeMemory;

  public void publisherUserMessage(String sessionUUID, Agent agent, String content) {

    episodeMemory.registerEvent(
        sessionUUID,
        agent,
        new RecordEvent("The user input is: " + content, StatusType.USER_INPUT_REQUEST));

    List<RecordEvent> recordEventList = episodeMemory.getEpisodeMemory(sessionUUID, agent);

    Message userMessage = new Message("user", content, null, null, null);

    // If there is no episodic memory yet, do NOT inject anything.
    if (recordEventList == null || recordEventList.isEmpty()) {
      publisher.publishEvent(
          new AgentRequestEvent(
              sessionUUID,
              agent,
              new Message(
                  "system",
                  "Process the user message and identify if the all datas is colleted and finished the execution calling the specific tool",
                  null,
                  null,
                  null),
              null,
              userMessage));
      return;
    }

    String episodicPrompt = buildDynamicEpisodePrompt(recordEventList);

    publisher.publishEvent(
        new AgentRequestEvent(
            sessionUUID,
            agent,
            null,
            new Message("system", episodicPrompt, null, null, null),
            userMessage));
  }

  /**
   * Builds a dynamic episodic-memory prompt with both: - the current state (latest event) - the
   * full history (ordered list of events)
   */
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
