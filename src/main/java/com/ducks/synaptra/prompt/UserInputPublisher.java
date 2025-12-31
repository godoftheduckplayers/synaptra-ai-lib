package com.ducks.synaptra.prompt;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.memory.EpisodeMemory;
import com.ducks.synaptra.orchestration.event.agent.contract.AgentRequestEvent;
import com.ducks.synaptra.prompt.contract.RecordEvent;
import com.ducks.synaptra.velocity.VelocityTemplateService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Publishes user input as an agent execution request, enriching it with episodic execution context
 * when applicable.
 *
 * <p>This class is responsible for handling raw user input and transforming it into an {@link
 * AgentRequestEvent}. When an agent is already in execution, the publisher restores the episodic
 * memory associated with the current session and injects it into the agent request to ensure
 * execution continuity.
 *
 * <p>The emitted event represents the canonical entry point from user interaction into the agentic
 * execution pipeline. It guarantees that:
 *
 * <ul>
 *   <li>User input is always propagated as a {@code user} message
 *   <li>Episodic memory is loaded and injected only when available
 *   <li>Agents can resume execution without repeating completed steps
 *   <li>Execution state remains consistent across multi-turn interactions
 * </ul>
 *
 * <p>This component acts as the boundary between the interaction layer and the orchestration layer,
 * ensuring that user intent is contextualized before triggering agent execution.
 *
 * @author Leandro Marques
 * @version 1.0.0
 */
@RequiredArgsConstructor
@Service
public class UserInputPublisher {

  private final EpisodeMemory episodeMemory;
  private final VelocityTemplateService velocityTemplateService;
  private final ApplicationEventPublisher publisher;

  /**
   * Publishes a new agent execution request based on user input.
   *
   * <p>This method receives raw user input and emits an {@link AgentRequestEvent} enriched with
   * episodic execution context when the agent has already processed previous steps in the same
   * session.
   *
   * @param sessionId the unique identifier of the execution session
   * @param agent the agent responsible for handling the user input
   * @param userInput the raw input provided by the user
   */
  @LogTracer(spanName = "publish_agent_execution_event")
  public void publishEvent(String sessionId, Agent agent, String userInput) {
    publisher.publishEvent(buildAgentRequestEvent(sessionId, agent, userInput));
  }

  private AgentRequestEvent buildAgentRequestEvent(
      String sessionId, Agent agent, String userInput) {
    Message episodicContext = getEpisodicContext(sessionId, agent);
    return new AgentRequestEvent(
        sessionId, agent, null, episodicContext, new Message("user", userInput, null, null, null));
  }

  /**
   * Builds a system {@link Message} containing the episodic memory context for an agent that is
   * already in execution.
   *
   * <p>This method is responsible for restoring historical execution context from the episodic
   * memory associated with the given session and agent. The generated system message summarizes
   * previously processed events and is injected into the agent prompt to ensure continuity of the
   * execution flow.
   *
   * <p>The episodic context allows the agent to:
   *
   * <ul>
   *   <li>Be aware of what has already been processed in the current session
   *   <li>Avoid repeating completed steps or previously asked questions
   *   <li>Resume execution from the last known state
   *   <li>Prevent infinite loops or redundant interactions
   * </ul>
   *
   * <p>This method is intended to be invoked <strong>only when an agent is already running</strong>
   * and needs to rehydrate its execution context. If no episodic memory is found, the method
   * returns {@code null}, indicating that no historical context should be injected.
   *
   * @param sessionId the unique identifier of the current execution session
   * @param agent the agent whose episodic memory should be loaded
   * @return a system {@link Message} containing the episodic execution context, or {@code null} if
   *     no episodic memory exists for the given session and agent
   */
  private Message getEpisodicContext(String sessionId, Agent agent) {
    List<RecordEvent> recordEventList = episodeMemory.getEpisodeMemory(sessionId, agent);

    if (!CollectionUtils.isEmpty(recordEventList)) {
      Map<String, Object> velocityContext = new HashMap<>();
      velocityContext.put("records", recordEventList);

      String episodicContextPrompt =
          """
                  EPISODIC_MEMORY_CONTEXT
                  Purpose:
                  This section summarizes what has already happened in the current session.
                  It exists to ensure continuity and avoid repetition.

                  The last events:
                  #foreach($record in $records)
                   # Event - status: $record.status(), content: $record.content()
                  #end

                  Constraints:
                  - Do not repeat previously asked questions.
                  - Do not redo completed steps.
                  - Resume execution from the last known state.
                  """;

      episodicContextPrompt =
          velocityTemplateService.render(episodicContextPrompt, velocityContext);

      return new Message("system", episodicContextPrompt, null, null, null);
    }
    return null;
  }
}
