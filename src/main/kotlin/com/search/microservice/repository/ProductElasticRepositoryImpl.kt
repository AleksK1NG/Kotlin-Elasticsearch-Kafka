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
            throw ex
        }
    }

    override suspend fun search(term: String, page: Int, size: Int): PaginationResponse<Product> = withTimeout(65000) {
        try {
            esClient.search({
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
            }, Product::class.java)
                .await()
                .let {
                    val productList = it.hits().hits().mapNotNull { hit -> hit.source() }
                    val totalHits = it.hits().total()?.value() ?: 0
                    PaginationResponse.of(page, size, totalHits, productList).also { response -> log.info("search result: $response") }
                }
        } catch (ex: Exception) {
            log.error("search error", ex)
            throw ex
        }
    }

    override suspend fun bulkInsert(products: BlockingDeque<Product>) = withContext(Dispatchers.IO) {
        try {
            if (products.isNotEmpty()) {
                val br = BulkRequest.Builder().index(productIndexName)

                products.forEach { product ->
                    br.operations {
                        it.index<Product> { indexOpsBuilder ->
                            indexOpsBuilder.document(product).id(product.id)
                        }
                    }
                }

                esClient.bulk(br.build()).await().also { log.info("bulk insert response: $it") }
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