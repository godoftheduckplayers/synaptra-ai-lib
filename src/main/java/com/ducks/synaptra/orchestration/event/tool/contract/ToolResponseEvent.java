package com.ducks.synaptra.orchestration.event.tool.contract;

import com.ducks.synaptra.agent.Agent;
import com.ducks.synaptra.client.openai.data.Message;
import com.ducks.synaptra.client.openai.data.ToolCall;
import org.springframework.lang.Nullable;

/**
 * Event representing a tool call requested by an agent.
 *
 * <p>This event is published when an agent execution produces a tool call that must be handled by
 * the orchestration layer. It represents the handoff between the agent reasoning phase and the tool
 * execution phase.
 *
 * <p>{@code ToolResponseEvent} does <strong>not</strong> represent the final result of a tool
 * execution. Instead, it carries the {@link ToolCall} definition produced by the agent, which must
 * be interpreted and executed by {@link
 * com.ducks.synaptra.orchestration.event.tool.ToolExecutionListener} implementations.
 *
 * <p>Typical consumers of this event include:
 *
 * <ul>
 *   <li>Internal orchestration handlers (routing, record persistence)
 *   <li>External tool executors (database access, API calls, file processing, etc.)
 * </ul>
 *
 * <p>The {@code agent} and {@code user} fields are nullable to support supervisor agents,
 * multi-stage orchestration, or delayed attribution.
 *
 * @param sessionId the unique identifier of the execution session
 * @param agent the agent that requested the tool call, or {@code null} if not applicable
 * @param user the original user message associated with this execution, or {@code null}
 * @param toolCall the tool call produced by the agent (required)
 * @author Leandro Marques
 * @since 1.0.0
 */
public record ToolResponseEvent(
    String sessionId, @Nullable Agent agent, @Nullable Message user, ToolCall toolCall) {}
