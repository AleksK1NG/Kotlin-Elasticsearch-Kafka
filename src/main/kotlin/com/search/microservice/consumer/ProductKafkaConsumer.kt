package com.search.microservice.consumer

import com.search.microservice.domain.Product
import com.search.microservice.exceptions.SerializationException
import com.search.microservice.repository.ProductElasticRepository
import com.search.microservice.utils.SerializationUtils.deserializeFromJsonBytes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.util.Loggers
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.Semaphore


@Service
class ProductKafkaConsumer(private val productRepository: ProductElasticRepository) {
    private val batchQueue = LinkedBlockingDeque<Product>()
    private val semaphore = Semaphore(1)

    @Value(value = "\${spring.kafka.consumer.batch-queue-size}")
    private val batchQueueSize: Int = 100

    @KafkaListener(
        topics = ["\${microservice.kafka.topics.index-product}"], groupId = "\${microservice.kafka.groupId}", concurrency = "\${microservice.kafka.default-concurrency}"
    )
    fun processIndexProduct(@Payload data: ByteArray, ack: Acknowledgment) = runBlocking {
        try {
            log.info("process kafka message: ${String(data)}")
            val deserializedProduct = deserializeFromJsonBytes(data, Product::class.java)
            log.info("process deserialized product: $deserializedProduct")
            batchQueue.add(deserializedProduct)
            if (batchQueue.size >= batchQueueSize) handleBatchIndex()
            ack.acknowledge().also { log.info("<<<commit>>> batch insert message") }
        } catch (ex: SerializationException) {
            ack.acknowledge().also { log.error("<<<commit error>>> serialization exception", ex) }
        } catch (ex: Exception) {
            log.error("processIndexProduct Exception", ex)
        }
    }

    private suspend fun handleBatchIndex() = withContext(Dispatchers.IO) {
        try {
            semaphore.acquire().also { log.info("batch insert semaphore acquired: ${Thread.currentThread().name}") }
            if (batchQueue.size >= batchQueueSize) productRepository.bulkInsert(batchQueue).also { batchQueue.clear() }
            semaphore.release().also { log.info("batch insert semaphore released: ${Thread.currentThread().name}") }
        } catch (ex: Exception) {
            semaphore.release()
            throw ex
        }
    }


    @Scheduled(initialDelay = 15000, fixedRate = 25000)
    private fun flushBulkInsert() = runBlocking(errorhandler) {
        withContext(Dispatchers.IO) {
            try {
                semaphore.acquire()
                if (batchQueue.isNotEmpty()) productRepository.bulkInsert(batchQueue).also { batchQueue.clear() }
                semaphore.release()
            } catch (ex: Exception) {
                log.error("Scheduled flushBulkInsert error", ex)
                semaphore.release()
                throw ex
            }
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