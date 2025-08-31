package com.example.yelp;


import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface BusinessRepository extends ElasticsearchRepository<Business, String> { }