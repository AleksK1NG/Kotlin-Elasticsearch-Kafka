package com.search.microservice.service

import com.search.microservice.domain.Product
import com.search.microservice.repository.ProductElasticRepository
import com.search.microservice.utils.PaginationResponse
import com.search.microservice.utils.publisher.KafkaPublisher
import kotlinx.coroutines.coroutineScope
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class ProductServiceImpl(
    private val productElasticRepository: ProductElasticRepository,
    private val publisher: KafkaPublisher,
    @Value(value = "\${microservice.kafka.topics.index-product:index-product}")
    private val indexProductTopicName: String,
) : ProductService {

    override suspend fun index(product: Product) = coroutineScope {
        publisher.publish(indexProductTopicName, product)
    }

    override suspend fun search(term: String, page: Int, size: Int): PaginationResponse<Product> = coroutineScope {
        productElasticRepository.search(term, page, size)
    }
}