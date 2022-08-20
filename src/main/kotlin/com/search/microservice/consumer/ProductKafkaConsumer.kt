package com.search.microservice.consumer

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation
import com.search.microservice.domain.Product
import com.search.microservice.exceptions.SerializationException
import com.search.microservice.utils.SerializationUtils.deserializeFromJsonBytes
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
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
class ProductKafkaConsumer(private val esClient: ElasticsearchAsyncClient) {
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
            if (batchQueue.size >= 100) {
                val br = BulkRequest.Builder()
                batchQueue.forEach {
                    br.operations { op ->
                        op.index { idx: IndexOperation.Builder<Product> ->
                            idx.index("products").id(it.id).document(it)
                        }
                    }
                }
                val response = esClient.bulk(br.build()).await()
                log.info("bulk insert: ${batchQueue.size}, response: $response, thread: ${Thread.currentThread().name}")
                batchQueue.clear()
            }
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
                val br = BulkRequest.Builder()

                if (batchQueue.isNotEmpty()) {
                    batchQueue.forEach { product ->
                        br.operations { it.index <Product>{
                                indexOpsBuilder -> indexOpsBuilder.document(product).id(product.id)
                        } }
                    }

                    br.index("products")
                    val bulkRequest = br.build()
                    log.info("bulkRequest: ${bulkRequest.toString()}")
                    val response = esClient.bulk(bulkRequest).await()

                    log.info("Scheduled bulk insert: ${batchQueue.size}, response: $response")

                    batchQueue.clear()
                }

                semaphore.release()
            } catch (ex: Exception) {
                log.error("flushBulkInsert", ex)
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