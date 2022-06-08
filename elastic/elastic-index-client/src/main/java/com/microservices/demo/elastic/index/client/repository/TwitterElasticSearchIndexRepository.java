package com.microservices.demo.elastic.index.client.repository;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import com.microservices.demo.elastic.model.index.impl.TwitterIndexModel;

public interface TwitterElasticSearchIndexRepository extends ElasticsearchRepository<TwitterIndexModel, String> {

}
