package com.ducks.synaptra.orchestration.event.record.contract;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.prompt.contract.RecordEvent;
import org.springframework.lang.Nullable;

/**
 * Event representing a record produced during an agent execution.
 *
 * <p>This event is emitted whenever an agent produces a {@link RecordEvent} that represents a
 * meaningful execution state change (e.g., waiting for user input, tool completion, or execution
 * finished).
 *
 * <p>The event is consumed by the orchestration layer to:
 *
 * <ul>
 *   <li>Persist the record into episodic memory
 *   <li>Drive state transitions in the execution flow
 *   <li>Trigger follow-up events such as agent resumption or answer delivery
 * </ul>
 *
 * <p>The {@code agent} and {@code user} fields are nullable to support advanced orchestration
 * scenarios, including supervisor agents, deferred attribution, or intermediate propagation across
 * execution stages.
 *
 * <p>The {@link RecordEvent} is mandatory and represents the semantic execution signal produced by
 * the agent.
 *
 * @param sessionId the unique identifier of the execution session
 * @param agent the agent that produced the record, or {@code null} if not applicable
 * @param user the original user message associated with this execution, or {@code null}
 * @param recordEvent the execution record describing the current state and content (required)
 * @author Leandro Marques
 * @since 1.0.0
 */
public record RecordRequestEvent(
    String sessionId, @Nullable Agent agent, @Nullable Message user, RecordEvent recordEvent) {}
