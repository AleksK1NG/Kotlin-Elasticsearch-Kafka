package com.search.microservice.utils.publisher

interface KafkaPublisher {
    suspend fun publish(topicName: String, data: Any)
}