package com.search.microservice.consumer

import com.search.microservice.domain.Product
import com.search.microservice.exceptions.SerializationException
import com.search.microservice.utils.SerializationUtils.deserializeFromJsonBytes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.runBlocking
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service
import reactor.util.Loggers


@Service
class ProductKafkaConsumer {

    @KafkaListener(
        topics = ["\${microservice.kafka.topics.index-product}"],
        groupId = "\${microservice.kafka.groupId}",
        concurrency = "\${microservice.kafka.default-concurrency}"
    )
    fun processIndexProduct(@Payload data: ByteArray, ack: Acknowledgment) = runBlocking {
        try {
            log.info("process kafka message: ${String(data)}")
            val deserializedProduct = deserializeFromJsonBytes(data, Product::class.java)
            log.info("process deserialized product: $deserializedProduct")
        } catch (ex: SerializationException) {
            ack.acknowledge().also { log.error("ack serialization exception", ex) }
        } catch (ex: Exception) {
            log.error("processIndexProduct Exception", ex)
        }
    }


    companion object {
        private val log = Loggers.getLogger(ProductKafkaConsumer::class.java)
        private const val handleTimeoutMillis = 5000L
        private val errorhandler = CoroutineExceptionHandler { _, throwable ->
            run { log.error("(ProductKafkaConsumer) CoroutineExceptionHandler", throwable) }
        }
    }
}