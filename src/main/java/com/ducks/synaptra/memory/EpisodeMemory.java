package com.ducks.synaptra.memory;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.log.LogTracer;
import com.ducks.synaptra.publisher.contract.RecordEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * In-memory episodic memory store for agent execution sessions.
 *
 * <p>This service keeps a lightweight, session-scoped timeline of {@link RecordEvent}s per agent.
 * It is designed to support orchestration continuity (e.g., "waiting for user input", "waiting for
 * tool execution", "agent finished") by allowing the runtime to persist and later rehydrate what
 * has already happened during the current session.
 *
 * <h2>Storage model</h2>
 *
 * <ul>
 *   <li>Keyed by {@code sessionId}
 *   <li>Inside each session, keyed by {@code agent.getIdentifier()}
 *   <li>Each agent key maps to an append-only {@link List} of {@link RecordEvent}
 * </ul>
 *
 * <h2>Important notes</h2>
 *
 * <ul>
 *   <li>This is an <strong>in-memory</strong> implementation (non-persistent).
 *   <li>It is intended for development or single-node usage unless replaced by a distributed store.
 *   <li>Concurrency: this implementation uses plain {@link HashMap} and {@link ArrayList}. If your
 *       orchestration is truly concurrent across threads, consider using concurrent collections or
 *       a persistent store.
 * </ul>
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Service
public class EpisodeMemory {

  /**
   * Memory structure:
   *
   * <pre>
   * sessionId -> ( agentIdentifier -> [RecordEvent, RecordEvent, ...] )
   * </pre>
   */
  private static final Map<String, Map<String, List<RecordEvent>>> MEMORY = new HashMap<>();

  /**
   * Appends a new {@link RecordEvent} to the episodic memory timeline of the given agent within the
   * provided session.
   *
   * <p>This is typically used by orchestration components to record execution state transitions,
   * for example:
   *
   * <ul>
   *   <li>Waiting for user input
   *   <li>Waiting for tool execution
   *   <li>Waiting for agent execution
   *   <li>Finished
   * </ul>
   *
   * @param sessionId the unique identifier of the current execution session (must not be blank)
   * @param agent the agent whose episodic timeline will be updated (must not be {@code null})
   * @param recordEvent the event to append (must not be {@code null})
   */
  @LogTracer(spanName = "episode_memory_register_event")
  public void registerEvent(String sessionId, Agent agent, RecordEvent recordEvent) {
    Assert.hasText(sessionId, "sessionId must not be null or blank");
    Assert.notNull(agent, "agent must not be null");
    Assert.notNull(recordEvent, "recordEvent must not be null");

    Map<String, List<RecordEvent>> events = getOrCreateSessionEvents(sessionId);
    List<RecordEvent> agentEvents = getOrCreateAgentEvents(agent, events);
    agentEvents.add(recordEvent);
  }

  /**
   * Retrieves the episodic memory timeline for a given agent within the provided session.
   *
   * <p>If no events exist yet, an empty list is returned and also initialized in memory.
   *
   * @param sessionId the unique identifier of the current execution session (must not be blank)
   * @param agent the agent whose episodic timeline should be loaded (must not be {@code null})
   * @return the (mutable) list of recorded events for the agent in the given session
   */
  @LogTracer(spanName = "episode_memory_get_events")
  public List<RecordEvent> getEpisodeMemory(String sessionId, Agent agent) {
    Assert.hasText(sessionId, "sessionId must not be null or blank");
    Assert.notNull(agent, "agent must not be null");

    Map<String, List<RecordEvent>> events = getOrCreateSessionEvents(sessionId);

    return getOrCreateAgentEvents(agent, events);
  }

  private List<RecordEvent> getOrCreateAgentEvents(
      Agent agent, Map<String, List<RecordEvent>> events) {
    Assert.notNull(agent, "agent must not be null");
    Assert.notNull(events, "events must not be null");

    return events.computeIfAbsent(agent.getIdentifier(), key -> new ArrayList<>());
  }

  private Map<String, List<RecordEvent>> getOrCreateSessionEvents(String sessionId) {
    Assert.hasText(sessionId, "sessionId must not be null or blank");

    return MEMORY.computeIfAbsent(sessionId, key -> new HashMap<>());
  }
}
