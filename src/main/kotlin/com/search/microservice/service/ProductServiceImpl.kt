package com.search.microservice.service

import com.search.microservice.domain.Product
import com.search.microservice.repository.ProductElasticRepository
import com.search.microservice.utils.PaginationResponse
import com.search.microservice.utils.publisher.KafkaPublisher
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.stereotype.Service


@Service
class ProductServiceImpl(
    private val productElasticRepository: ProductElasticRepository,
    private val publisher: KafkaPublisher,
    @Value(value = "\${microservice.kafka.topics.index-product:index-product}")
    private val indexProductTopicName: String,
    private val tracer: Tracer
) : ProductService {


    override suspend fun index(product: Product) = withContext(tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductService.index")
        try {
            publisher.publish(indexProductTopicName, product)
        } finally {
            span.end()
        }
    }

    override suspend fun search(term: String, page: Int, size: Int): PaginationResponse<Product> = withContext(tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductService.search")
        try {
            productElasticRepository.search(term, page, size)
        } finally {
            span.end()
        }
    }
}