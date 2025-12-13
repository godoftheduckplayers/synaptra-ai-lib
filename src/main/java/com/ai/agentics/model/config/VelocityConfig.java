package com.ai.agentics.model.config;

import java.util.Properties;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VelocityConfig {

  @Bean
  public VelocityEngine velocityEngine() {
    Properties props = new Properties();

    props.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");

    VelocityEngine engine = new VelocityEngine(props);
    engine.init();
    return engine;
  }
}
