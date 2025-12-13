package com.ai.agentics.client.openai.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a parameter schema definition used in the OpenAI Function-Calling interface.
 *
 * <p>This class models a JSON Schema–like structure where parameters are defined as an object
 * containing named properties, their respective types and descriptions, and an optional list of
 * required fields.
 *
 * <p>The default {@code type} is {@code "object"}, which aligns with the OpenAI function schema
 * specification. Each property is described using a {@link ParameterProperty}, and required fields
 * are explicitly tracked to ensure proper validation during function invocation.
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Getter
public class Parameter {

  /**
   * The JSON schema type of the parameter definition.
   *
   * <p>Defaults to {@code "object"}, as function parameters are represented as a structured object
   * in the OpenAI Function-Calling API.
   */
  @Setter private String type = "object";

  /**
   * Map containing the parameter properties keyed by their names.
   *
   * <p>Each entry defines the expected type and description of an individual parameter using {@link
   * ParameterProperty}.
   */
  private final Map<String, ParameterProperty> properties = new HashMap<>();

  /**
   * List of property names that are required when invoking the function.
   *
   * <p>Only properties explicitly marked as required are included in this list.
   */
  private final List<String> required = new ArrayList<>();

  /**
   * Adds a new property to the parameter schema.
   *
   * <p>If {@code isRequired} is {@code true}, the property name is also added to the list of
   * required parameters.
   *
   * @param name the name of the parameter property
   * @param property the property definition, including type and description
   * @param isRequired whether the parameter is required for function invocation
   */
  public void addProperty(String name, ParameterProperty property, Boolean isRequired) {
    properties.put(name, property);
    if (isRequired) {
      required.add(name);
    }
  }
}
