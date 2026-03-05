package com.trainingsplan.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "pacr.ai.enabled", havingValue = "true")
@ComponentScan(basePackages = "com.trainingsplan.pacr.ai")
public class AIConfiguration {
}
