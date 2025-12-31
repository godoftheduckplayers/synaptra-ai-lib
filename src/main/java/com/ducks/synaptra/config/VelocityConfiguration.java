package com.ducks.synaptra.config;

import java.util.Properties;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for Apache Velocity.
 *
 * <p>This configuration provides a {@link VelocityEngine} bean configured for in-memory template
 * evaluation. It is intended to be used by components that render prompts and contextual system
 * messages dynamically (e.g., agent prompts, episodic memory, handoff contexts).
 *
 * <p>The engine is initialized with UTF-8 input encoding and does not rely on any file-based
 * template loaders, making it suitable for runtime-generated templates.
 *
 * <p><strong>Design notes:</strong>
 *
 * <ul>
 *   <li>Templates are evaluated via {@link VelocityEngine#evaluate}, not loaded from files.
 *   <li>This configuration keeps Velocity isolated from I/O concerns.
 *   <li>The returned engine is thread-safe after initialization.
 * </ul>
 *
 * @author Leandro Marques
 * @since 1.0.0
 */
@Configuration
public class VelocityConfiguration {

  /**
   * Creates and initializes the {@link VelocityEngine} bean.
   *
   * <p>The engine is configured with UTF-8 input encoding and immediately initialized so it can be
   * safely reused across the application.
   *
   * @return an initialized {@link VelocityEngine} instance
   */
  @Bean
  public VelocityEngine velocityEngine() {
    Properties props = new Properties();
    props.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");

    VelocityEngine engine = new VelocityEngine(props);
    engine.init();
    return engine;
  }
}
