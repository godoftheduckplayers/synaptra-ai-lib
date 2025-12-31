package com.ducks.synaptra.client.openai.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Defines a function that can be invoked by the model via tool-calling.
 *
 * <p>The parameters are expressed as a JSON Schema document describing expected input fields, their
 * types, and validation constraints.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public class FunctionDef {
  private String name;
  private String description;
  private Parameter parameters;
}
