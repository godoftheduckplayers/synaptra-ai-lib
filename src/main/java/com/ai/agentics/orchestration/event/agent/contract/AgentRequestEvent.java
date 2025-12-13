package com.ai.agentics.orchestration.event.agent.contract;

import com.ai.agentics.client.openai.data.ChatCompletionRequest;
import com.ai.agentics.client.openai.data.Message;
import com.ai.agentics.model.Agent;
import java.util.List;
import org.springframework.lang.Nullable;

/**
 * Event representing a request to execute an {@link Agent}.
 *
 * <p>This event acts as the bridge between the orchestration layer and the execution layer,
 * carrying all contextual information required to perform a chat completion using a configured AI
 * agent.
 *
 * <p>An {@code AgentExecutionRequestEvent} may be published:
 *
 * <ul>
 *   <li>By an orchestrator agent when selecting another agent to handle input
 *   <li>As part of a sequential or parallel execution flow
 *   <li>In response to intermediate tool execution results
 * </ul>
 *
 * <p>The event optionally carries:
 *
 * <ul>
 *   <li>The {@link Agent} responsible for handling the request
 *   <li>The {@link Message} representing the user input or interaction context
 * </ul>
 *
 * <p>Both {@code agent} and {@code user} are nullable to allow:
 *
 * <ul>
 *   <li>Deferred agent resolution
 *   <li>Multi-stage orchestration flows
 *   <li>Event enrichment before execution
 * </ul>
 *
 * <p>The execution layer is expected to validate the presence of the required fields before
 * attempting to execute the agent.
 *
 * @param agent the agent selected to execute the request, or {@code null} if not yet resolved
 * @param user the user message or interaction context, or {@code null} if not yet available
 * @author Leandro Marques
 * @since 1.0.0
 */
public record AgentRequestEvent(
    String sessionId, @Nullable Agent agent, @Nullable Message user) {

  /**
   * Converts this execution event into a {@link ChatCompletionRequest}.
   *
   * <p>This method assembles a complete chat completion request using:
   *
   * <ul>
   *   <li>The agent's provider configuration
   *   <li>The agent's base prompt as a system message
   *   <li>The user message as the conversation input
   *   <li>The agent's available tools
   *   <li>The agent's tool selection strategy
   * </ul>
   *
   * <p><strong>Preconditions:</strong>
   *
   * <ul>
   *   <li>{@code agent} must not be {@code null}
   *   <li>{@code user} must not be {@code null}
   * </ul>
   *
   * <p>If these conditions are not met, an {@link AssertionError} will be thrown.
   *
   * <p>This method is typically invoked by the execution layer immediately before dispatching the
   * request to the AI provider.
   *
   * @return a fully constructed {@link ChatCompletionRequest} ready for execution
   */
  public ChatCompletionRequest toChatCompletionRequest() {
    assert agent != null;
    assert user != null;

    return new ChatCompletionRequest(
        agent.providerConfig().model(),
        List.of(new Message("system", agent.prompt(), null, null, null), user),
        agent.tools(),
        agent.toolChoice().getValue(),
        agent.providerConfig().temperature(),
        agent.providerConfig().maxTokens(),
        agent.providerConfig().topP());
  }
}
