package com.search.microservice.configuration

import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.KafkaAdmin
import reactor.util.Loggers

@Configuration
class KafkaTopicConfiguration(
    @Value(value = "\${spring.kafka.bootstrap-servers:localhost:9093}")
    private val bootstrapServers: String,
    @Value(value = "\${microservice.kafka.topics.index-product:index-product}")
    private val indexProductTopicName: String
) {

    @Bean
    fun kafkaAdmin(): KafkaAdmin {
        val configs: MutableMap<String, Any> = HashMap()
        configs[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        return KafkaAdmin(configs)
    }

    @Bean
    fun indexProductTopicInitializer(kafkaAdmin: KafkaAdmin): NewTopic? {
        return try {
            val topic = NewTopic(indexProductTopicName, partitionsCount, replicationFactor.toShort())
            kafkaAdmin.createOrModifyTopics(topic)
            log.info("(indexProductTopicInitializer) topic: $topic")
            topic
        } catch (ex: Exception) {
            log.error("indexProductTopicInitializer", ex)
            throw ex
        }
    }

    companion object {
        private val log = Loggers.getLogger(KafkaTopicConfiguration::class.java)
        private const val partitionsCount = 3
        private const val replicationFactor = 1
    }
}