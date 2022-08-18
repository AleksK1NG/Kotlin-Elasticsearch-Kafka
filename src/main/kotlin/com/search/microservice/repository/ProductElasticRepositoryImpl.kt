package com.search.microservice.repository

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import com.search.microservice.domain.Product
import kotlinx.coroutines.future.await
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import reactor.util.Loggers


@Repository
class ProductElasticRepositoryImpl(private val esClient: ElasticsearchAsyncClient): ProductElasticRepository {

    @Value(value = "\${elasticsearch.mappings-index-name}")
    lateinit var productIndexName: String

    override suspend fun index(product: Product) {
        try {
            esClient.index<Product> { it.index(productIndexName).id(product.id).document(product) }.await()
                .also { log.info("response: $it") }
        } catch (ex: Exception) {
            log.error("index error", ex)
        }
    }

    companion object {
        private val log = Loggers.getLogger(ProductElasticRepositoryImpl::class.java)
    }
}