package com.search.microservice.utils.publisher

import com.search.microservice.controller.ProductController
import com.search.microservice.utils.SerializationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import reactor.util.Loggers
import java.util.concurrent.TimeUnit

@Service
class KafkaPublisherImpl(private val kafkaTemplate: KafkaTemplate<String, ByteArray>) : KafkaPublisher {

    override suspend fun publish(topicName: String, data: Any) = withContext(Dispatchers.IO) {
        try {
            val jsonBytes = SerializationUtils.serializeToJsonBytes(data)
            val record = ProducerRecord<String, ByteArray>(topicName, jsonBytes)
            kafkaTemplate.send(record).get(timeoutMillis, TimeUnit.MILLISECONDS)
            log.info("publishing kafka record value >>>>> ${String(record.value())}")
        } catch (ex: Exception) {
            log.error("KafkaPublisherImpl publish", ex)
            throw RuntimeException("KafkaPublisherImpl publish", ex)
        }
    }

    companion object {
        private val log = Loggers.getLogger(ProductController::class.java)
        private const val timeoutMillis = 5555000L
    }
}