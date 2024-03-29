package com.microservices.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "retry-config")
public class RetryConfigData {
	
	private long initialIntervalMs;
	private long maxIntervalMs;
	private Double multiplier;
	private Integer maxAttempts;
	private long sleepTimeMs;

}
