package com.microservices.demo.kafka.admin.config.client;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import com.microservices.demo.config.KafkaConfigData;
import com.microservices.demo.config.RetryConfigData;
import com.microservices.demo.kafka.admin.exception.KafkaClientException;

@Component
public class KafkaAdminClient {

	private static final Logger LOG = LoggerFactory.getLogger(KafkaAdminClient.class);

	private final KafkaConfigData kafkaConfigData;

	private final RetryConfigData retryConfigData;

	private final AdminClient adminClient;

	private final RetryTemplate retryTemplate;

	private final WebClient webClient;

	public KafkaAdminClient(KafkaConfigData kafkaConfigData, RetryConfigData retryConfigData, AdminClient adminClient,
			RetryTemplate retryTemplate, WebClient webClient) {
		super();
		this.kafkaConfigData = kafkaConfigData;
		this.retryConfigData = retryConfigData;
		this.adminClient = adminClient;
		this.retryTemplate = retryTemplate;
		this.webClient = webClient;
	}

	public void createTopics() {
		CreateTopicsResult createTopicsResult;
		try {
			createTopicsResult = retryTemplate.execute(this::doCreateTpoics);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			new KafkaClientException("Reached max number of retries for creating kafka topics", e);
		}
		checkTopicsCreated();
	}

	private void checkTopicsCreated() {
		Collection<TopicListing> topics = getTopics();
		int retryCount = 1;
		Integer maxRetry = retryConfigData.getMaxAttempts();
		int multiplier = retryConfigData.getMultiplier().intValue();
		long sleepTimeMs = retryConfigData.getSleepTimeMs();
		for (String topic : kafkaConfigData.getTopicNamesToCreate()) {
			while (!isTopicCreated(topics, topic)) {
				checkMaxEntry(retryCount++, maxRetry);
				sleep(sleepTimeMs);
				sleepTimeMs *= multiplier;
				topics = getTopics();
			}
		}

	}

	public void checkSchemaRegistry() {
		int retryCount = 1;
		Integer maxRetry = retryConfigData.getMaxAttempts();
		int multiplier = retryConfigData.getMultiplier().intValue();
		long sleepTimeMs = retryConfigData.getSleepTimeMs();
		while (!getSchemaRegistryStatus().is2xxSuccessful()) {
			checkMaxEntry(retryCount++, maxRetry);
			sleep(sleepTimeMs);
			sleepTimeMs *= multiplier;
		}

	}

	private HttpStatus getSchemaRegistryStatus() {
		try {
			return webClient.method(HttpMethod.GET).uri(kafkaConfigData.getSchemaRegistryurl()).exchange()
					.map(ClientResponse::statusCode).block();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			return HttpStatus.SERVICE_UNAVAILABLE;
		}
	}

	private void sleep(long sleepTimeMs) {
		try {
			Thread.sleep(sleepTimeMs);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			throw new KafkaClientException("Error while sleeping for wainting new created topics");
		}
	}

	private void checkMaxEntry(int i, Integer maxRetry) {
		if (i > maxRetry) {
			throw new KafkaClientException("Reached max number of retry for reading kafka topics");
		}

	}

	private boolean isTopicCreated(Collection<TopicListing> topics, String topicName) {
		if (topics == null) {
			return false;
		}
		return topics.stream().anyMatch(topic -> topic.name().equals(topicName));
	}

	private CreateTopicsResult doCreateTpoics(RetryContext retryContext) {
		List<String> topicNames = kafkaConfigData.getTopicNamesToCreate();
		LOG.info("creating  {} topics, attempts {}", topicNames.size(), retryContext.getRetryCount());
		List<NewTopic> kafkaTopics = topicNames.stream().map(topic -> new NewTopic(topic.trim(),
				kafkaConfigData.getNumOfPartitions(), kafkaConfigData.getReplicationFactor()))
				.collect(Collectors.toList());
		return adminClient.createTopics(kafkaTopics);
	}

	private Collection<TopicListing> getTopics() {
		Collection<TopicListing> topics = null;
		try {
			topics = retryTemplate.execute(this::dogetTopicListings);
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			new KafkaClientException("Reached max number of retries for reading kafka topics", e);
		}
		return topics;
	}

	private Collection<TopicListing> dogetTopicListings(RetryContext context)
			throws InterruptedException, ExecutionException {
		// TODO Auto-generated method stub
		LOG.info("reading  {} topics, attempts {}", kafkaConfigData.getTopicNamesToCreate().toArray(),
				context.getRetryCount());
		Collection<TopicListing> topics = adminClient.listTopics().listings().get();
		if (topics != null) {
			topics.forEach(n -> LOG.debug("topic with name {}", n));
		}
		return topics;

	}

}
