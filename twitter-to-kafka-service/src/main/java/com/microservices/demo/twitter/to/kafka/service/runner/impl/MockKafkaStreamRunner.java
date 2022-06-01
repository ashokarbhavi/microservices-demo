/**
 * 
 */
package com.microservices.demo.twitter.to.kafka.service.runner.impl;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.microservices.demo.config.TwitterToKafkaServiceConfigData;
import com.microservices.demo.twitter.to.kafka.service.exception.TwitterToKafkaServiceException;
import com.microservices.demo.twitter.to.kafka.service.listener.TwitterKafkaStatusListener;
import com.microservices.demo.twitter.to.kafka.service.runner.StreamRunner;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

/**
 * @author ashok mock twitter stream
 *
 */
@Component
@ConditionalOnProperty(name = "twitter-to-kafka-service.enable-mock-tweets", havingValue = "true")
public class MockKafkaStreamRunner implements StreamRunner {

	private static final Logger LOG = LoggerFactory.getLogger(MockKafkaStreamRunner.class);

	private final TwitterToKafkaServiceConfigData twitterToKafkaServiceConfigData;
	private final TwitterKafkaStatusListener twitterKafkaStatusListener;
	private static final Random RANDOM = new Random();
	private static final String[] WORDS = new String[] { "Lorem", "ipsum", "dolor", "sit", "amet", "consectetuer",
			"adipiscing", "elit", "Maecenas", "porttitor", "congue", "massa", "Fusce", "posuere", "magna", "sed",
			"pulvinar", "ultricies", "purus", "lectus", "malesuada", "libero" };

	private static final String tweetAsRawJson = "{" + "\"created_at\":\"{0}\"," + "\"id\":\"{1}\","
			+ "\"text\":\"{2}\"," + "\"user\":{\"id\":\"{3}\"}" + "}";

	private static final String TWITTER_STATUS_DATE_FORMAT = "EEE MMM dd HH:mm:ss zzz yyyy";

	public MockKafkaStreamRunner(TwitterToKafkaServiceConfigData configData, TwitterKafkaStatusListener listener) {
		super();
		this.twitterToKafkaServiceConfigData = configData;
		this.twitterKafkaStatusListener = listener;
	}

	@Override
	public void start() throws TwitterException {
		String[] keywords = twitterToKafkaServiceConfigData.getTwitterKeywords().toArray(new String[0]);
		Integer mockMaxTweetLength = twitterToKafkaServiceConfigData.getMockMaxTweetLength();
		Integer mockMinTweetLength = twitterToKafkaServiceConfigData.getMockMinTweetLength();
		Long mockSleepMs = twitterToKafkaServiceConfigData.getMockSleepMs();
		LOG.info("stated mock Filtering twitter stream for keywords {}", Arrays.toString(keywords));
		simulateTwitterStream(keywords, mockMaxTweetLength, mockMinTweetLength, mockSleepMs);

	}

	private void simulateTwitterStream(String[] keywords, Integer mockMaxTweetLength, Integer mockMinTweetLength,
			Long mockSleepMs) {
		Executors.newSingleThreadExecutor().submit(() -> {
			try {
				while (true) {
					String formattedTweetAsRawJson = getFormattedTweet(keywords, mockMinTweetLength,
							mockMaxTweetLength);
					Status status = TwitterObjectFactory.createStatus(formattedTweetAsRawJson);
					twitterKafkaStatusListener.onStatus(status);
					sleep(mockSleepMs);
				}
			} catch (TwitterException e) {
				LOG.error("error creating twitter status!", e);
			}
		});

	}

	private String getFormattedTweet(String[] keywords, Integer mockMinTweetLength, Integer mockMaxTweetLength) {
		String[] param = new String[] {
				ZonedDateTime.now().format(DateTimeFormatter.ofPattern(TWITTER_STATUS_DATE_FORMAT)),
				String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)),
				getRandomTweets(keywords, mockMinTweetLength, mockMaxTweetLength),
				String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)) };
		return formatTweetAsJsonWithParams(param);
	}

	private String formatTweetAsJsonWithParams(String[] param) {
		String tweet = tweetAsRawJson;

		for (int i = 0; i < param.length; i++) {
			tweet = tweet.replace("{" + i + "}", param[i]);
		}
		return tweet;
	}

	private String getRandomTweets(String[] keywords, Integer mockMinTweetLength, Integer mockMaxTweetLength) {
		// TODO Auto-generated method stub
		StringBuilder tweet = new StringBuilder();
		int tweetLength = RANDOM.nextInt(mockMaxTweetLength - mockMinTweetLength + 1) + mockMinTweetLength;
		return constructRandomTweet(keywords, tweet, tweetLength);
	}

	private String constructRandomTweet(String[] keywords, StringBuilder tweet, int tweetLength) {
		for (int i = 0; i < tweetLength; i++) {
			tweet.append(WORDS[RANDOM.nextInt(WORDS.length)]).append(" ");
			if (i == tweetLength / 2) {
				tweet.append(keywords[RANDOM.nextInt(keywords.length)]).append(" ");
			}
		}
		return tweet.toString().trim();
	}

	private void sleep(Long mockSleepMs) {
		// TODO Auto-generated method stub
		try {
			Thread.sleep(mockSleepMs);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			throw new TwitterToKafkaServiceException("Error while sleeping for waiting new status to create");
		}
	}

}
