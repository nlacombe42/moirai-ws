package net.nlacombe.moirai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:/config.properties")
@PropertySource("classpath:/secrets.properties")
public class BaseConfig {
}
