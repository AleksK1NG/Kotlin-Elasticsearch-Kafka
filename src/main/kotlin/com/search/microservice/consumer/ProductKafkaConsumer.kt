package com.search.microservice.consumer

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import com.search.microservice.domain.Product
import com.search.microservice.exceptions.SerializationException
import com.search.microservice.repository.ProductElasticRepository
import com.search.microservice.utils.SerializationUtils.deserializeFromJsonBytes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.util.Loggers
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.Semaphore


@Service
class ProductKafkaConsumer(
    private val esClient: ElasticsearchAsyncClient,
    private val productRepository: ProductElasticRepository
    ) {
    private val batchQueue = LinkedBlockingDeque<Product>(5000)
    private val semaphore = Semaphore(1)

    @KafkaListener(
        topics = ["\${microservice.kafka.topics.index-product}"], groupId = "\${microservice.kafka.groupId}", concurrency = "\${microservice.kafka.default-concurrency}"
    )
    fun processIndexProduct(@Payload data: ByteArray, ack: Acknowledgment) = runBlocking {
        try {
            log.info("process kafka message: ${String(data)}")
            val deserializedProduct = deserializeFromJsonBytes(data, Product::class.java)
            log.info("process deserialized product: $deserializedProduct")
            batchQueue.add(deserializedProduct)
//            if (batchQueue.size >= 100) handleBatchIndex()

        } catch (ex: SerializationException) {
            ack.acknowledge().also { log.error("ack serialization exception", ex) }
        } catch (ex: Exception) {
            log.error("processIndexProduct Exception", ex)
        }
    }

    private suspend fun handleBatchIndex() = withContext(Dispatchers.IO) {
        try {
            semaphore.acquire()
            log.info("batch insert semaphore acquired: ${Thread.currentThread().name}")
            if (batchQueue.size >= 100) productRepository.bulkInsert(batchQueue).also { batchQueue.clear() }
            semaphore.release()
            log.info("batch insert semaphore released: ${Thread.currentThread().name}")
        } catch (ex: Exception) {
            semaphore.release()
            throw ex
        }
    }


    @Scheduled(initialDelay = 15000, fixedRate = 25000)
    private fun flushBulkInsert() = runBlocking {
        withContext(Dispatchers.IO) {
            try {
                semaphore.acquire()
                if (batchQueue.isNotEmpty()) productRepository.bulkInsert(batchQueue).also { batchQueue.clear() }
                semaphore.release()
            } catch (ex: Exception) {
                log.error("Scheduled flushBulkInsert error", ex)
                semaphore.release()
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