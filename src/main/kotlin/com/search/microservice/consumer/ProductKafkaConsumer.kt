package com.search.microservice.consumer

import com.search.microservice.domain.Product
import com.search.microservice.exceptions.SerializationException
import com.search.microservice.repository.ProductElasticRepository
import com.search.microservice.utils.SerializationUtils
import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import reactor.util.Loggers
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.Semaphore
import javax.annotation.PreDestroy


@Service
class ProductKafkaConsumer(private val productRepository: ProductElasticRepository, private val tracer: Tracer) {
    private val batchQueue = LinkedBlockingDeque<Product>()
    private val semaphore = Semaphore(1)

    @Value(value = "\${spring.kafka.consumer.batch-queue-size}")
    private val batchQueueSize: Int = 500

    @KafkaListener(
        topics = ["\${microservice.kafka.topics.index-product}"], groupId = "\${microservice.kafka.groupId}", concurrency = "\${microservice.kafka.default-concurrency}"
    )
    fun processIndexProduct(@Payload data: ByteArray, ack: Acknowledgment) = runBlocking {
        withContext(tracer.asContextElement()) {
            withTimeout(handleTimeoutMillis) {
                val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductKafkaConsumer.processIndexProduct")

                try {
                    val deserializedProduct = SerializationUtils.deserializeFromJsonBytes(data, Product::class.java)
                    batchQueue.add(deserializedProduct).also { log.info("process deserialized product: $deserializedProduct") }
                    if (batchQueue.size >= batchQueueSize) handleBatchIndex()
                    ack.acknowledge().also {
                        log.info("<<<commit>>> batch insert message")
                        span.tag("message", deserializedProduct.toString())
                    }
                } catch (ex: SerializationException) {
                    ack.acknowledge().also { log.error("<<<commit error>>> serialization ex ception", ex) }.also { span.error(ex) }
                } catch (ex: Exception) {
                    log.error("processIndexProduct Exception", ex).also { span.error(ex) }
                } finally {
                    span.end()
                }
            }
        }
    }

    private suspend fun handleBatchIndex() = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductKafkaConsumer.handleBatchIndex")

        try {
            semaphore.acquire()
            if (batchQueue.size >= batchQueueSize)  productRepository.bulkInsert(batchQueue).also { batchQueue.clear() }
            semaphore.release().also { log.info("batch insert semaphore released >>>>>>>>>>>>>>>>>>>>> \n: ${Thread.currentThread().name}") }
        } catch (ex: Exception) {
            semaphore.release().also { log.error("Scheduled handleBatchIndex error", ex) }.also { span.error(ex) }
            throw ex
        } finally {
            span.end()
        }
    }


    @Scheduled(initialDelay = 60000, fixedRate = 50000)
    fun flushBulkInsert() = runBlocking(errorhandler + tracer.asContextElement()) {
        withContext(Dispatchers.IO) {
            log.info("running scheduled insert >>>>>>>>>, ${batchQueue} \n")
            val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductKafkaConsumer.flushBulkInsert")

            try {
                semaphore.acquire()
                if (batchQueue.isNotEmpty()) productRepository.bulkInsert(batchQueue).also { batchQueue.clear() }
                semaphore.release().also { span.tag("bulkInsert count", batchQueue.size.toString()) }
            } catch (ex: Exception) {
                semaphore.release().also { log.error("Scheduled flushBulkInsert error", ex) }.also { span.error(ex) }
                throw ex
            } finally {
                span.end()
            }
        }
    }

    @PreDestroy
    fun flushBatchQueue() = runBlocking {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductKafkaConsumer.flushBatchQueue")

        try {
            if (batchQueue.isNotEmpty()) productRepository.bulkInsert(batchQueue).also {
                batchQueue.clear().also { log.info("batch queue saved") }
            }
        } catch (ex: Exception) {
            log.error("flushBatchQueue", ex).also { span.error(ex) }
            throw ex
        } finally {
            span.end()
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