package com.search.microservice.consumer

import com.search.microservice.domain.Product
import com.search.microservice.exceptions.SerializationException
import com.search.microservice.repository.ProductElasticRepository
import com.search.microservice.utils.SerializationUtils
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
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
import javax.annotation.PreDestroy


@Service
class ProductKafkaConsumer(private val productRepository: ProductElasticRepository, private val tracer: Tracer) {
    private val batchQueue = LinkedBlockingDeque<Product>()
    private val mutex = Mutex()

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
                    val product = SerializationUtils.deserializeFromJsonBytes(data, Product::class.java)
                    handleBatchIndex(product)
                    ack.acknowledge().also {
                        span.tag("message", product.toString())
                        log.info("<<<commit>>> process index for productID: ${product.id}")
                    }
                } catch (ex: SerializationException) {
                    ack.acknowledge().also { log.error("<<<commit error>>> serialization error", ex) }.also { span.error(ex) }
                } catch (ex: Exception) {
                    log.error("processIndexProduct Exception", ex).also { span.error(ex) }
                } finally {
                    span.end()
                }
            }
        }
    }

    private suspend fun handleBatchIndex(product: Product) = withContext(tracer.asContextElement() + Dispatchers.IO) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductKafkaConsumer.handleBatchIndex")

        try {
            mutex.lock()
            batchQueue.add(product)
            if (batchQueue.size >= batchQueueSize) productRepository.bulkInsert(batchQueue).also {
                log.info("(handleBatchIndex) saved queueSize: >>>>>>>>>>>>>> ${batchQueue.size}")
                batchQueue.clear()
            }
        } finally {
            mutex.unlock()
            span.end()
        }
    }


    @Scheduled(initialDelay = 25000, fixedRate = 10000)
    fun flushBulkInsert() = runBlocking(errorhandler + tracer.asContextElement()) {
        withContext(Dispatchers.IO) {
            log.info("(Scheduled) running scheduled insert queueSize: >>>>>>>>>, ${batchQueue.size} \n")
            val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductKafkaConsumer.flushBulkInsert")

            try {
                mutex.lock()
                if (batchQueue.isNotEmpty()) productRepository.bulkInsert(batchQueue).also {
                    log.info("(Scheduled) saved queueSize: >>>>>>>>>>>>>> ${batchQueue.size} \n\n\n\n")
                    batchQueue.clear()
                }
            } catch (ex: Exception) {
                span.error(ex)
                throw ex
            } finally {
                mutex.unlock()
                span.end()
            }
        }
    }

    @PreDestroy
    fun flushBatchQueue() = runBlocking {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductKafkaConsumer.flushBatchQueue")

        try {
            mutex.lock()
            if (batchQueue.isNotEmpty()) productRepository.bulkInsert(batchQueue)
                .also { batchQueue.clear().also { log.info("batch queue saved") } }
        } catch (ex: Exception) {
            log.error("flushBatchQueue", ex).also { span.error(ex) }
            throw ex
        } finally {
            mutex.unlock().also { span.end() }
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