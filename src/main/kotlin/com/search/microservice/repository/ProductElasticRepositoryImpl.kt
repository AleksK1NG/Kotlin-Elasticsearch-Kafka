package com.search.microservice.repository

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.core.BulkRequest
import co.elastic.clients.elasticsearch.core.SearchResponse
import co.elastic.clients.json.JsonData
import com.search.microservice.constants.ProductSearchConstants.COUNT_IN_STOCK
import com.search.microservice.constants.ProductSearchConstants.DESCRIPTION
import com.search.microservice.constants.ProductSearchConstants.SHOP
import com.search.microservice.constants.ProductSearchConstants.TITLE
import com.search.microservice.domain.Product
import com.search.microservice.utils.KeyboardLayoutManager
import com.search.microservice.utils.PaginationResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.cloud.sleuth.Tracer
import org.springframework.cloud.sleuth.instrument.kotlin.asContextElement
import org.springframework.stereotype.Repository
import reactor.util.Loggers
import java.util.concurrent.BlockingDeque


@Repository
class ProductElasticRepositoryImpl(
    private val esClient: ElasticsearchAsyncClient,
    private val keyboardLayoutManager: KeyboardLayoutManager,
    private val tracer: Tracer
) : ProductElasticRepository {

    @Value(value = "\${elasticsearch.mappings-index-name}")
    lateinit var productIndexName: String


    override suspend fun index(product: Product): Unit = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductElasticRepository.index")

        try {
            esClient.index<Product> { it.index(productIndexName).id(product.id).document(product) }
                .await()
                .also {
                    span.tag("response", it.toString())
                    log.info("response: $it")
                }
        } catch (ex: Exception) {
            log.error("index error", ex).also { span.error(ex) }
            throw ex
        } finally {
            span.end()
        }
    }

    override suspend fun search(term: String, page: Int, size: Int): PaginationResponse<Product> = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductElasticRepository.search")

        try {
            esClient.search(
                {
                    it.index(productIndexName)
                        .size(size)
                        .from((page * size))
                        .query { q ->
                            q.bool { b ->
                                b.should { s ->
                                    s.multiMatch { m ->
                                        m.query(term).fields(TITLE, DESCRIPTION, SHOP)
                                    }
                                }.should { s ->
                                    s.multiMatch { m ->
                                        m.query(keyboardLayoutManager.getOppositeKeyboardLayoutTerm(term)).fields(TITLE, DESCRIPTION, SHOP)
                                    }
                                }.mustNot { s ->
                                    s.range { r -> r.field(COUNT_IN_STOCK).lt(JsonData.of(0)) }
                                }
                            }
                        }
                }, Product::class.java
            )
                .await()
                .let {
                    buildSearchPaginatedResponse(it, page, size)
                        .also { response -> log.info("search result: $response") }
                        .also { response -> span.tag("search result", response.toString()) }
                }
        } catch (ex: Exception) {
            log.error("search error", ex).also { span.error(ex) }
            throw ex
        } finally {
            span.end()
        }
    }

    private fun buildSearchPaginatedResponse(searchResponse: SearchResponse<Product>, page: Int, size: Int): PaginationResponse<Product> {
        val productList = searchResponse.hits().hits().mapNotNull { hit -> hit.source() }
        val totalHits = searchResponse.hits().total()?.value() ?: 0
        return PaginationResponse.of(page, size, totalHits, productList)
    }

    override suspend fun bulkInsert(products: BlockingDeque<Product>) = withContext(Dispatchers.IO + tracer.asContextElement()) {
        val span = tracer.nextSpan(tracer.currentSpan()).start().name("ProductElasticRepository.bulkInsert")

        try {
            if (products.isNotEmpty()) {
                val br = BulkRequest.Builder().index(productIndexName)

                products.forEach { product ->
                    br.operations {
                        it.index<Product> { indexOpsBuilder -> indexOpsBuilder.document(product).id(product.id) }
                    }
                }

                esClient.bulk(br.build()).await().also {
                    span.tag("bulk insert response", it.toString())
                    log.info("bulk insert response: ${it.took()}")
                }
            }
        } catch (ex: Exception) {
            log.error("bulkInsert", ex).also { span.error(ex) }
            throw ex
        } finally {
            span.end()
        }
    }

    companion object {
        private val log = Loggers.getLogger(ProductElasticRepositoryImpl::class.java)
    }
}