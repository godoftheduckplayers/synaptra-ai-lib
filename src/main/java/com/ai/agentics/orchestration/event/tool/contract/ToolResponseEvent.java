package com.ai.agentics.orchestration.event.tool.contract;

import com.ai.agentics.agent.Agent;
import com.ai.agentics.client.openai.data.ChatCompletionResponse;
import com.ai.agentics.client.openai.data.Message;
import com.ai.agentics.client.openai.data.ToolCall;
import org.springframework.lang.Nullable;

/**
 * Event representing the result of an {@link Agent} execution.
 *
 * <p>This event is published after an agent has completed a chat completion request and produced a
 * {@link ChatCompletionResponse}. It serves as the terminal or intermediate output of an agent
 * execution and may be consumed by orchestrators, supervisors, or response delivery components.
 *
 * <p>An {@code AgentExecutionResponseEvent} may be published:
 *
 * <ul>
 *   <li>As the final response to a user interaction
 *   <li>As an intermediate response in a multi-agent or multi-step orchestration flow
 *   <li>In reaction to tool execution results or agent coordination logic
 * </ul>
 *
 * <p>The event optionally carries:
 *
 * <ul>
 *   <li>The {@link Agent} that produced the response
 *   <li>The original {@link Message} representing the user input
 * </ul>
 *
 * <p>Both {@code agent} and {@code user} are nullable to support:
 *
 * <ul>
 *   <li>Deferred agent attribution
 *   <li>Supervisor or aggregator agents
 *   <li>Event propagation across multiple orchestration stages
 * </ul>
 *
 * <p>The {@link ChatCompletionResponse} is mandatory and represents the raw response returned by
 * the AI provider.
 *
 * @param agent the agent that produced the response, or {@code null} if not applicable
 * @param user the original user message associated with this execution, or {@code null}
 * @param chatCompletionResponse the chat completion response returned by the AI provider
 * @author Leandro Marques
 * @since 1.0.0
 */
public record ToolResponseEvent(
    String sessionId, @Nullable Agent agent, @Nullable Message user, ToolCall toolCall) {}
