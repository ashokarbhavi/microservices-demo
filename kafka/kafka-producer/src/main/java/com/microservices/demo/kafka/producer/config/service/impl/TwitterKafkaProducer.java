package com.microservices.demo.kafka.producer.config.service.impl;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import com.microservices.demo.kafka.avro.model.TwitterAvroModel;
import com.microservices.demo.kafka.producer.config.service.KafkaProducer;
import javax.annotation.PreDestroy;

@Service
public class TwitterKafkaProducer implements KafkaProducer<Long, TwitterAvroModel> {

	private static final Logger LOG = LoggerFactory.getLogger(TwitterKafkaProducer.class);

	private KafkaTemplate<Long, TwitterAvroModel> kafkaTemplate;

	public TwitterKafkaProducer(KafkaTemplate<Long, TwitterAvroModel> kafkaTemplate) {
		super();
		this.kafkaTemplate = kafkaTemplate;
	}

	@Override
	public void send(String topicName, Long key, TwitterAvroModel message) {
		LOG.info("sending message='{}' to topic='{}'", message, topicName);
		ListenableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture = kafkaTemplate.send(topicName, key,
				message);
		addCallback(topicName, message, kafkaResultFuture);

	}
	
	 @PreDestroy
	    public void close() {
	        if (kafkaTemplate != null) {
	            LOG.info("Closing kafka producer!");
	            kafkaTemplate.destroy();
	        }
	    }

	private void addCallback(String topicName, TwitterAvroModel message,
			ListenableFuture<SendResult<Long, TwitterAvroModel>> kafkaResultFuture) {
		kafkaResultFuture.addCallback(new ListenableFutureCallback<>() {

			@Override
			public void onSuccess(SendResult<Long, TwitterAvroModel> sendResult) {
				RecordMetadata metadata = sendResult.getRecordMetadata();
				LOG.debug("Received new metadata. Topic: {}; Partition {}; Offset {}; Timestamp {}, at time {}",
						metadata.topic(), metadata.partition(), metadata.offset(), metadata.timestamp(),
						System.nanoTime());

			}

			@Override
			public void onFailure(Throwable ex) {
				LOG.error("Error while sending message {} to topic {}", message.toString(), topicName, ex);

			}
		});
	}

}
