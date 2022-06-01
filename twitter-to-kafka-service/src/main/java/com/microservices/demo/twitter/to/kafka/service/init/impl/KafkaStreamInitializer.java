package com.microservices.demo.twitter.to.kafka.service.init.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.kafka.admin.config.client.KafkaAdminClient;
import com.microservices.demo.twitter.to.kafka.service.init.StreamInitializer;

@Component
public class KafkaStreamInitializer implements StreamInitializer {

	private static final Logger LOG = LoggerFactory.getLogger(KafkaStreamInitializer.class);

	private final KafkaConfigData kafkaConfigData;

	private final KafkaAdminClient kafkaAdminClient;

	public KafkaStreamInitializer(KafkaConfigData kafkaConfigData, KafkaAdminClient kafkaAdminClient) {
		super();
		this.kafkaConfigData = kafkaConfigData;
		this.kafkaAdminClient = kafkaAdminClient;
	}

	@Override
	public void init() {
		kafkaAdminClient.createTopics();
		kafkaAdminClient.checkSchemaRegistry();
		LOG.info("topics with name {} is ready for operation.", kafkaConfigData.getTopicNamesToCreate().toArray());

	}

}
