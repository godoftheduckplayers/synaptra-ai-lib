package com.ducks.synaptra.orchestration.event.record;

import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.memory.EpisodeMemory;
import com.ducks.synaptra.orchestration.event.agent.contract.AgentRequestEvent;
import com.ducks.synaptra.orchestration.event.answer.contract.AnswerResponseEvent;
import com.ducks.synaptra.orchestration.event.record.contract.RecordRequestEvent;
import com.ducks.synaptra.prompt.contract.RecordEvent;
import com.ducks.synaptra.velocity.VelocityTemplateService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * Handles record events produced during an agent execution and drives the orchestration flow.
 *
 * <p>This component is responsible for:
 *
 * <ul>
 *   <li>Persisting each {@link RecordEvent} into episodic memory for the current session/agent
 *   <li>Publishing follow-up orchestration events based on the record status (e.g., ask user,
 *       resume parent agent, deliver final answer)
 * </ul>
 *
 * <p>In practice, this service acts as a "state transition" handler:
 *
 * <ul>
 *   <li>{@code WAIT_USER_INPUT} -> emits an {@link AnswerResponseEvent} to request more user input
 *   <li>{@code FINISHED} -> if the agent has a parent, resumes the parent with rehydrated context;
 *       otherwise emits the final {@link AnswerResponseEvent}
 *   <li>{@code FINISHED_TOOL_EXECUTION} -> resumes the parent agent with the tool output as a
 *       system handoff
 * </ul>
 *
 * <p>All events are processed asynchronously using the {@code agentExecutionExecutor}.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Service
public class RecordExecutionEvent {

  private static final Logger logger = LogManager.getLogger(RecordExecutionEvent.class);

  public static final String WAIT_USER_INPUT = "WAIT_USER_INPUT";
  public static final String FINISHED = "FINISHED";
  public static final String WAIT_AGENT_EXECUTION = "WAIT_AGENT_EXECUTION";
  public static final String WAIT_TOOL_EXECUTION = "WAIT_TOOL_EXECUTION";
  public static final String FINISHED_TOOL_EXECUTION = "FINISHED_TOOL_EXECUTION";

  private final ApplicationEventPublisher publisher;
  private final EpisodeMemory episodeMemory;
  private final VelocityTemplateService velocityTemplateService;

  public RecordExecutionEvent(
      ApplicationEventPublisher publisher,
      EpisodeMemory episodeMemory,
      VelocityTemplateService velocityTemplateService) {
    this.publisher = publisher;
    this.episodeMemory = episodeMemory;
    this.velocityTemplateService = velocityTemplateService;
  }

  /**
   * Persists the incoming {@link RecordEvent} to episodic memory and publishes the next
   * orchestration event according to the record status.
   *
   * <p>Suggested tracing/log naming: {@code record_event_received} / {@code
   * record_event_processed}.
   *
   * @param recordRequestEvent the record event emitted by an agent execution pipeline
   */
  @LogTracer(spanName = "record_event_received")
  @Async("agentExecutionExecutor")
  @EventListener
  public void onRecordExecutionEvent(RecordRequestEvent recordRequestEvent) {
    assert recordRequestEvent.agent() != null;

    registerInEpisodicMemory(recordRequestEvent);

    final String status = recordRequestEvent.recordEvent().status();

    switch (status) {
      case WAIT_USER_INPUT -> publishAnswerFromRecord(recordRequestEvent);
      case FINISHED -> handleAgentFinished(recordRequestEvent);
      case FINISHED_TOOL_EXECUTION -> handleToolFinished(recordRequestEvent);
      default -> // Other states (e.g., WAIT_AGENT_EXECUTION / WAIT_TOOL_EXECUTION) may be handled
          // elsewhere,
          // or intentionally ignored here to avoid duplicate transitions.
          logger.debug(
              "[RECORD_EVENT_IGNORED] sessionId={}, agent={}, status={}",
              recordRequestEvent.sessionId(),
              recordRequestEvent.agent().identifier(),
              status);
    }
  }

  private void registerInEpisodicMemory(RecordRequestEvent recordRequestEvent) {
    episodeMemory.registerEvent(
        recordRequestEvent.sessionId(),
        recordRequestEvent.agent(),
        recordRequestEvent.recordEvent());
  }

  private void handleAgentFinished(RecordRequestEvent recordRequestEvent) {
    assert recordRequestEvent.agent() != null;
    if (recordRequestEvent.agent().parent() == null) {
      // Leaf agent finished and there is no parent to resume -> deliver final answer to user.
      publishAnswerFromRecord(recordRequestEvent);
      return;
    }

    // Resume parent agent with updated episodic context + a small handoff instruction.
    Message parentEpisodicContext = buildParentEpisodicContext(recordRequestEvent);

    Message handoffContext =
        new Message(
            "system",
            """
                    Execution update:
                    - A child agent has finished its work.
                    Next steps:
                    - Verify whether all required tasks are completed.
                    - If completed, produce the final user-facing response.
                    - Otherwise, continue processing until completion criteria are met.
                    """,
            null,
            null,
            null);

    publisher.publishEvent(
        new AgentRequestEvent(
            recordRequestEvent.sessionId(),
            recordRequestEvent.agent().parent(),
            handoffContext,
            parentEpisodicContext,
            recordRequestEvent.user()));
  }

  private void handleToolFinished(RecordRequestEvent recordRequestEvent) {
    // Tool execution ended -> resume parent agent with the tool output as system context.
    assert recordRequestEvent.agent() != null;
    publisher.publishEvent(
        new AgentRequestEvent(
            recordRequestEvent.sessionId(),
            recordRequestEvent.agent().parent(),
            new Message("system", recordRequestEvent.recordEvent().content(), null, null, null),
            null,
            recordRequestEvent.user()));
  }

  private void publishAnswerFromRecord(RecordRequestEvent recordRequestEvent) {
    publisher.publishEvent(
        new AnswerResponseEvent(
            recordRequestEvent.sessionId(),
            recordRequestEvent.agent(),
            recordRequestEvent.user(),
            recordRequestEvent.recordEvent().content()));
  }

  /**
   * Builds an episodic context message for the parent agent, summarizing the parent timeline and
   * including a completion note from the child agent that has just finished.
   */
  private Message buildParentEpisodicContext(RecordRequestEvent recordRequestEvent) {
    assert recordRequestEvent.agent() != null;
    List<RecordEvent> parentRecords =
        episodeMemory.getEpisodeMemory(
            recordRequestEvent.sessionId(), recordRequestEvent.agent().parent());

    if (CollectionUtils.isEmpty(parentRecords)) {
      return null;
    }

    Map<String, Object> velocityContext = new HashMap<>();
    velocityContext.put("records", parentRecords);
    velocityContext.put("agent", recordRequestEvent.agent());
    velocityContext.put("content", recordRequestEvent.recordEvent().content());

    String prompt =
        """
            EPISODIC_MEMORY_CONTEXT
            Purpose:
            This section summarizes what has already happened in the current session.
            It exists to ensure continuity and avoid repetition.

            The last events:
            #foreach($record in $records)
             # Event - status: $record.status(), content: $record.content()
            #end

            Child completion:
            - The agent '$agent.name()' has finished. Summary: $content

            Constraints:
            - Do not repeat previously asked questions.
            - Do not redo completed steps.
            - Resume execution from the last known state.
            """;

    String rendered = velocityTemplateService.render(prompt, velocityContext);
    return new Message("system", rendered, null, null, null);
  }
}
