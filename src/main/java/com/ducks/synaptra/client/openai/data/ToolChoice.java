package com.ducks.synaptra.client.openai.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Defines the strategy used by the model to select and invoke tools during a Chat Completion
 * execution.
 *
 * <p>This enum represents the available tool selection modes supported by the OpenAI
 * Function-Calling interface. Each value maps directly to the corresponding string expected by the
 * OpenAI API.
 *
 * <p>The selected {@code ToolChoice} determines whether the model can automatically choose a tool,
 * is forced to call a specific tool, or avoids tool invocation entirely.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@AllArgsConstructor
@Getter
public enum ToolChoice {

  /**
   * Allows the model to automatically decide whether to invoke a tool and which tool should be
   * called, based on the current prompt and conversation context.
   *
   * <p>This value maps to {@code "auto"} in the OpenAI API.
   */
  AUTO("auto");

  /** The raw value expected by the OpenAI API for tool selection. */
  private final String value;
}
