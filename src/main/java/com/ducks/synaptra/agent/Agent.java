package com.ducks.synaptra.agent;

import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.client.openai.data.ToolChoice;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;
import java.util.Map;

/**
 * Defines the contract for an executable AI agent within the Synaptra Agentics execution framework.
 *
 * <p>An {@code Agent} represents a self-contained unit of AI behavior, encapsulating all the
 * information required to perform a model execution, including:
 *
 * <ul>
 *   <li>Its identity and purpose
 *   <li>The base prompt that defines its behavior
 *   <li>The AI provider configuration
 *   <li>The set of callable tools
 *   <li>The strategy for tool selection
 * </ul>
 *
 * <p>This interface is intentionally provider-agnostic, allowing different implementations to
 * target multiple AI providers (e.g. OpenAI, Azure OpenAI, local models, etc.) while preserving a
 * consistent execution and orchestration model.
 *
 * <p>{@code Agent} instances are primarily consumed by the execution and orchestration layers of
 * the framework, which are responsible for routing user input, invoking tools, handling agent
 * hierarchies, and managing execution state across sessions.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
public interface Agent {

  /**
   * Returns the unique identifier of this agent.
   *
   * <p>This identifier is used by the orchestration and execution layers to:
   *
   * <ul>
   *   <li>Uniquely identify the agent within an orchestration plan
   *   <li>Route execution and result events to the correct agent
   *   <li>Track agent participation and execution state across sessions
   * </ul>
   *
   * <p>The identifier must be:
   *
   * <ul>
   *   <li>Unique within the application or orchestration scope
   *   <li>Stable and deterministic (must not change between executions)
   *   <li>Human-readable when possible, to improve observability and debugging
   * </ul>
   *
   * @return a unique, non-null identifier for this agent
   */
  String getIdentifier();

  /**
   * Returns the human-readable name of this agent.
   *
   * <p>This value is typically used for:
   *
   * <ul>
   *   <li>Logging and tracing
   *   <li>User-facing explanations or summaries
   *   <li>Debugging and observability tools
   * </ul>
   *
   * @return the agent name
   */
  String getName();

  /**
   * Returns the primary goal or responsibility of this agent.
   *
   * <p>The goal describes what the agent is expected to accomplish and is commonly injected into
   * prompts, routing decisions, or supervisor agent logic.
   *
   * @return a short description of the agent's goal
   */
  String getGoal();

  /**
   * Indicates whether this agent supports emitting interim or processing messages.
   *
   * <p>When enabled, the execution framework may send intermediate messages to the user while
   * long-running or multi-step operations are being performed, such as:
   *
   * <ul>
   *   <li>Agent routing or delegation
   *   <li>Tool execution
   *   <li>Waiting for asynchronous workers
   *   <li>Multi-agent orchestration flows
   * </ul>
   *
   * <p>This improves user experience by providing feedback during processing rather than remaining
   * silent until execution completes.
   *
   * @return {@code true} if the agent supports interim processing messages; {@code false} otherwise
   */
  boolean isSupportsInterimMessages();

  /**
   * Returns the type of this agent.
   *
   * <p>The agent type is typically used by the orchestration layer to determine execution strategy,
   * such as routing, supervision, or direct execution.
   *
   * @return the agent type
   */
  AgentType getAgentType();

  /**
   * Returns the AI provider configuration associated with this agent.
   *
   * <p>This configuration contains all provider-specific parameters required to execute the model,
   * such as:
   *
   * <ul>
   *   <li>Model name
   *   <li>Temperature and token limits
   *   <li>Timeouts and execution constraints
   * </ul>
   *
   * @return the provider configuration
   */
  ProviderConfig getProviderConfig();

  /**
   * Returns the base prompt that defines the agent's behavior.
   *
   * <p>This prompt is typically used as the system message during model execution and establishes
   * the context, tone, and responsibilities of the agent.
   *
   * @return the agent's base prompt
   */
  String getPrompt();

  /**
   * Returns the list of callable tools available to this agent.
   *
   * <p>These tools may be invoked by the model during execution when using function-calling,
   * depending on the configured {@link ToolChoice} strategy.
   *
   * <p>This method is ignored during JSON serialization.
   *
   * @return the list of available tools
   */
  @JsonIgnore
  List<Tool> getTools();

  /**
   * Returns the parent agent, if this agent is part of a hierarchical orchestration.
   *
   * <p>Parent agents are typically supervisor or orchestrator agents responsible for delegating
   * execution to child agents.
   *
   * @return the parent agent, or {@code null} if none exists
   */
  @JsonIgnore
  Agent getParent();

  /**
   * Assigns a parent agent to this agent.
   *
   * @param agent the parent agent
   */
  void setParent(Agent agent);

  /**
   * Returns the list of child agents managed or delegated by this agent.
   *
   * <p>This is commonly used in supervisor or orchestrator agent patterns.
   *
   * @return the list of child agents
   */
  @JsonIgnore
  List<Agent> getAgents();

  /**
   * Defines how the model should select and invoke tools during execution.
   *
   * <p>This setting controls whether the model:
   *
   * <ul>
   *   <li>Automatically selects tools
   *   <li>Is forced to call a specific tool
   *   <li>Is prevented from calling tools
   * </ul>
   *
   * @return the tool selection strategy
   */
  ToolChoice getToolChoice();

  /**
   * Returns the Velocity context used for prompt rendering.
   *
   * <p>This context provides dynamic variables that may be injected into prompts before execution,
   * enabling template-based prompt generation.
   *
   * <p>This method is ignored during JSON serialization.
   *
   * @return a map representing the Velocity context
   */
  @JsonIgnore
  Map<String, Object> getVelocityContext();
}
