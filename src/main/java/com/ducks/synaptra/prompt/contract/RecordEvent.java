package com.ducks.synaptra.prompt.contract;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a semantic execution record produced by an agent or orchestration step.
 *
 * <p>A {@code RecordEvent} captures a meaningful state transition during the lifecycle of an agent
 * execution (for example, waiting for user input, waiting for tool execution, or completion of a
 * task). These records are persisted in episodic memory and later used to rehydrate context, drive
 * routing decisions, or deliver responses.
 *
 * <p>The {@code status} field expresses the execution state, while {@code content} provides
 * human-readable or agent-readable details associated with that state.
 *
 * <p>This record can also expose a Velocity-compatible context via {@link #velocityContext()},
 * enabling it to be rendered into prompts or system messages.
 *
 * @param content a textual description or summary associated with the execution state
 * @param status the execution status identifier (e.g. WAIT_USER_INPUT, FINISHED)
 * @author Leandro Marques
 * @since 1.0.0
 */
public record RecordEvent(String content, String status) {

  /**
   * Builds a Velocity template context from this record.
   *
   * <p>The returned map can be used directly by {@code VelocityTemplateService} to render prompts
   * or system messages that include record-specific information.
   *
   * @return a map containing {@code content} and {@code status} entries
   */
  public Map<String, Object> velocityContext() {
    Map<String, Object> context = new HashMap<>();
    context.put("content", content);
    context.put("status", status);
    return context;
  }
}
