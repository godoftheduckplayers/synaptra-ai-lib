package com.ducks.synaptra.memory.episode;

import com.ducks.synaptra.model.agent.Agent;
import com.ducks.synaptra.memory.episode.model.RecordEvent;
import com.ducks.synaptra.log.LogTracer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class EpisodeMemory {

  private static final Map<String, Map<String, List<RecordEvent>>> MEMORY = new HashMap<>();

  @LogTracer(spanName = "episode_memory_register_event")
  public void registerEvent(String sessionId, Agent agent, RecordEvent recordEvent) {
    Assert.hasText(sessionId, "sessionId must not be null or blank");
    Assert.notNull(agent, "agent must not be null");
    Assert.notNull(recordEvent, "recordEvent must not be null");

    Map<String, List<RecordEvent>> events = getOrCreateSessionEvents(sessionId);
    List<RecordEvent> agentEvents = getOrCreateAgentEvents(agent, events);
    agentEvents.add(recordEvent);
  }

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
