package com.microservices.demo.twitter.to.kafka.service;

import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import com.microservices.demo.twitter.to.kafka.service.init.StreamInitializer;
import com.microservices.demo.twitter.to.kafka.service.runner.StreamRunner;

/**
 * Hello world!
 *
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.microservices.demo")
public class TwittertoKafakaServiceApplication implements CommandLineRunner {

	private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(TwittertoKafakaServiceApplication.class);

	private final StreamRunner streamRunner;

	private final StreamInitializer streamInitializer;

	public TwittertoKafakaServiceApplication(StreamRunner runner, StreamInitializer streamInitializer) {
		super();
		this.streamRunner = runner;
		this.streamInitializer = streamInitializer;
	}

	public static void main(String[] args) {
		SpringApplication.run(TwittertoKafakaServiceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("app starts.....");
		streamInitializer.init();
		streamRunner.start();
	}
}
