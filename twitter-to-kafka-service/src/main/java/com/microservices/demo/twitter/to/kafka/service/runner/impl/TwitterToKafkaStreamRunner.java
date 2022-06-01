package com.microservices.demo.twitter.to.kafka.service.runner.impl;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import com.microservices.demo.config.TwitterToKafkaServiceConfigData;
import com.microservices.demo.twitter.to.kafka.service.listener.TwitterKafkaStatusListener;
import com.microservices.demo.twitter.to.kafka.service.runner.StreamRunner;

import twitter4j.FilterQuery;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;

@Component
@ConditionalOnExpression("${twitter-to-kafka-service.enable-v1-tweets} && not ${twitter-to-kafka-service.enable-mock-tweets} && not ${twitter-to-kafka-service.enable-v2-tweets}")
public class TwitterToKafkaStreamRunner implements StreamRunner {

	private static final Logger LOG = LoggerFactory.getLogger(TwitterToKafkaStreamRunner.class);

	private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
	private final TwitterKafkaStatusListener twitterKafkaStatusListener;
	private TwitterStream twitterStream;

	public TwitterToKafkaStreamRunner(TwitterToKafkaServiceConfigData configData,
			TwitterKafkaStatusListener statusListener) {
		super();
		this.twitterToKafkaServiceConfigData = configData;
		this.twitterKafkaStatusListener = statusListener;
	}

	@Override
	public void start() throws TwitterException {
		List<String> searchtweets = searchtweets();
		searchtweets.stream().forEach(n -> LOG.info(n));
		twitterStream = new TwitterStreamFactory().getInstance();
		twitterStream.addListener(twitterKafkaStatusListener);
		// addFilter();

	}

	@PreDestroy
	private void shutdown() {
		if (twitterStream != null) {
			LOG.info("closing twitter stream");
			twitterStream.shutdown();
		}

	}

	private void addFilter() {
		String[] keywords = twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[0]);
		FilterQuery filterQuery = new FilterQuery(keywords);
		twitterStream.filter(filterQuery);
		LOG.info("stated Filtering twitter stream for keywords {}", Arrays.toString(keywords));
	}

	public static List<String> searchtweets() throws TwitterException {

		Twitter twitter = new TwitterFactory().getInstance();
		// twitter.lan
		Query query = new Query("lang:en AND #Mbappe");
		QueryResult result = twitter.search(query);

		return result.getTweets().stream().map(item -> item.getText()).collect(Collectors.toList());
	}

}
