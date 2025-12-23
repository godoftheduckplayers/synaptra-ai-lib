package com.ducks.synaptra.agent;

import com.ducks.synaptra.client.openai.data.Tool;
import com.ducks.synaptra.client.openai.data.ToolChoice;
import java.util.List;
import java.util.Map;

/**
 * Defines the contract for an executable AI agent within the Agentics execution framework.
 *
 * <p>An {@code Agent} encapsulates all the information required to perform a model execution,
 * including the provider configuration, the base prompt, the set of callable tools, and the
 * strategy for tool selection.
 *
 * <p>This interface is designed to be provider-agnostic, allowing different implementations to
 * supply configurations for various AI providers (e.g. OpenAI, Azure OpenAI, or others) while
 * maintaining a consistent execution model.
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
   * <p>The identifier should be:
   *
   * <ul>
   *   <li>Unique within the application or orchestration scope
   *   <li>Stable and deterministic (must not change between executions)
   *   <li>Human-readable when possible (to simplify debugging and observability)
   * </ul>
   *
   * @return a unique, non-null identifier for this agent
   */
  String identifier();

  String name();

  String goal();

  AgentType agentType();

  /**
   * Returns the configuration of the AI provider used by this agent.
   *
   * <p>This configuration typically includes model details, credentials, timeouts, and other
   * provider-specific settings required to execute the agent.
   *
   * @return the provider configuration associated with this agent
   */
  ProviderConfig providerConfig();

  /**
   * Returns the base prompt that defines the agent's behavior and context.
   *
   * <p>The prompt serves as the initial instruction or system message that guides the model's
   * responses during execution.
   *
   * @return the agent's base prompt
   */
  String prompt();

  /**
   * Returns the list of callable tools available to the agent.
   *
   * <p>These tools may be invoked by the model during execution when using the Function-Calling
   * interface, depending on the selected {@link ToolChoice} strategy.
   *
   * @return the list of tools available to the agent
   */
  List<Tool> tools();

  Agent parent();

  void setParent(Agent agent);

  List<Agent> agents();

  /**
   * Defines how the model should choose and invoke tools during execution.
   *
   * <p>This setting controls whether the model may automatically select a tool, must call a
   * specific tool, or should avoid tool invocation altogether.
   *
   * @return the tool selection strategy for this agent
   */
  ToolChoice toolChoice();

  Map<String, Object> velocityContext();
}
