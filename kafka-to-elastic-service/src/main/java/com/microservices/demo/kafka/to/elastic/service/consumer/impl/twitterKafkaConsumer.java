package com.microservices.demo.kafka.to.elastic.service.consumer.impl;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.KafkaConsumerConfigData;
import com.microservices.demo.elastic.index.client.service.ElasticIndexClient;
import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;
import com.microservices.demo.kafka.admin.config.client.KafkaAdminClient;
import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import com.microservices.demo.kafka.to.elastic.service.consumer.KafkaConsumer;
import com.microservices.demo.kafka.to.elastic.service.transformer.AvroToElasticModelTransformer;

@Service
public class twitterKafkaConsumer implements KafkaConsumer<Long, TwitterAvroModel> {

	private static final Logger LOG = LoggerFactory.getLogger(twitterKafkaConsumer.class);

	private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

	private final KafkaAdminClient kafkaAdminClient;

	private final KafkaConfigData kafkaConfigData;

	private final KafkaConsumerConfigData consumerConfigData;

	private final AvroToElasticModelTransformer avroToElasticModelTransformer;

	private final ElasticIndexClient<TwitterIndexModel> elasticIndexClient;

	public twitterKafkaConsumer(KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry,
			KafkaAdminClient kafkaAdminClient, KafkaConfigData kafkaConfigData,
			ElasticIndexClient<TwitterIndexModel> elasticIndexClient,
			AvroToElasticModelTransformer avroToElasticModelTransformer, KafkaConsumerConfigData consumerConfigData) {
		super();
		this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
		this.kafkaAdminClient = kafkaAdminClient;
		this.kafkaConfigData = kafkaConfigData;
		this.consumerConfigData = consumerConfigData;
		this.avroToElasticModelTransformer = avroToElasticModelTransformer;
		this.elasticIndexClient = elasticIndexClient;
	}

	@Override
	@KafkaListener(id = "${kafka-consumer-config.consumer-group-id}", topics = "${kafka-config.topic-name}")
	public void receive(@Payload List<TwitterAvroModel> messages,
			@Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) List<Long> keys,
			@Header(KafkaHeaders.RECEIVED_PARTITION_ID) List<Integer> partitions, List<Long> offsets) {
		LOG.info(
				"{} number of message received with keys {}, partitions {} and offsets {}, "
						+ "sending it to elastic: Thread id {}",
				messages.size(), keys.toString(), partitions.toString(), offsets.toString(),
				Thread.currentThread().getId());
		List<TwitterIndexModel> elasticModels = avroToElasticModelTransformer.getElasticModels(messages);
		List<String> documentIds = elasticIndexClient.save(elasticModels);
		LOG.info("Documents saved to elasticsearch with ids {}", documentIds.toArray());
	}

	@EventListener
	public void onAppStarted(ApplicationEvent applicationEvent) {
		kafkaAdminClient.checkTopicsCreated();
		LOG.info("Topics with name {} are ready for operations", kafkaConfigData.getTopicNamesToCreate().toArray());
		Objects.requireNonNull(
				kafkaListenerEndpointRegistry.getListenerContainer(consumerConfigData.getConsumerGroupId())).start();
	}

}
