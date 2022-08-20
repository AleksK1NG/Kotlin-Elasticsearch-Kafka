package com.search.microservice.repository

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.json.JsonData
import com.search.microservice.domain.Product
import com.search.microservice.utils.KeyboardLayoutManager
import com.search.microservice.utils.PaginationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import reactor.util.Loggers
import java.util.concurrent.BlockingDeque
import java.util.concurrent.LinkedBlockingDeque


@Repository
class ProductElasticRepositoryImpl(
    private val esClient: ElasticsearchAsyncClient,
    private val keyboardLayoutManager: KeyboardLayoutManager,
) : ProductElasticRepository {

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

    override suspend fun search(term: String, page: Int, size: Int): PaginationResponse<Product> = withTimeout(65000) {
        LinkedBlockingDeque<Any>(100)
        try {
            val response = esClient.search({
                it.index(productIndexName)
                    .size(size)
                    .from((page * size))
                    .query { q ->
                        q.bool { b ->
                            b.should { s ->
                                s.multiMatch { m ->
                                    m.query(term).fields("title", "description", "shop")
                                }
                            }.should { s ->
                                s.multiMatch { m ->
                                    m.query(keyboardLayoutManager.getOppositeKeyboardLayoutTerm(term)).fields("title", "description", "shop")
                                }
                            }.mustNot { s ->
                                s.range { r ->
                                    r.field("count_in_stock").lt(JsonData.of(0))
                                }
                            }
                        }
                    }
            }, Product::class.java).await()
            log.info("search response: $response")

            val productList = response.hits().hits().mapNotNull { it.source() }
            val totalHits = response.hits().total()?.value() ?: 0

            PaginationResponse.of(page, size, totalHits, productList).also { log.info("search result: $it") }
        } catch (ex: Exception) {
            log.error("search error", ex)
            throw ex
        }
    }

    override suspend fun bulkInsert(products: BlockingDeque<Product>) = withContext(Dispatchers.IO) {
        try {
            val br = BulkRequest.Builder()

            if (products.isNotEmpty()) {
                products.forEach { product ->
                    br.operations {
                        it.index<Product> { indexOpsBuilder ->
                            indexOpsBuilder.document(product).id(product.id)
                        }
                    }
                }

                br.index(productIndexName)
                val bulkRequest = br.build()
                val response = esClient.bulk(bulkRequest).await()

                log.info("bulk insert response: $response")
            }
        } catch (ex: Exception) {
            log.error("bulkInsert", ex)
            throw ex
        }
    }

    companion object {
        private val log = Loggers.getLogger(ProductElasticRepositoryImpl::class.java)
    }
}