package com.ducks.synaptra.client.openai.data;

/**
 * Describes a callable tool that the model may invoke during a Chat Completion when using the
 * Function-Calling interface.
 *
 * @param type The tool type; for functions this is always {@code "function"}.
 * @param function Definition of the callable function.
 * @author Leandro Marques
 * @since 1.0.0
 */
public record Tool(String type, FunctionDef function) {

  public Tool(FunctionDef function) {
    this("function", function);
  }
}
