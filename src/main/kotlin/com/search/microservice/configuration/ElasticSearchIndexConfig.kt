package com.search.microservice.configuration

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.elasticsearch.indices.Alias
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.elasticsearch.indices.GetIndexRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import reactor.util.Loggers


@Configuration
class ElasticSearchIndexConfig(private val esClient: ElasticsearchAsyncClient) : CommandLineRunner {

    @Value(value = "\${elasticsearch.mappings-path:classpath:mappings.json}")
    lateinit var productsMappingsResourceFile: Resource

    @Value(value = "\${elasticsearch.mappings-index-name}")
    lateinit var productIndexName: String

    @Value(value = "\${elasticsearch.mappings-index-alias-name}")
    lateinit var productIndexAliasName: String

    @Value(value = "\${faker.count:555}")
    val count: Int = 555


    override fun run(vararg args: String?): Unit = runBlocking {
        withContext(Dispatchers.IO) {
            try {
                val mappingBytes = productsMappingsResourceFile.inputStream.readAllBytes() ?: byteArrayOf()
                log.info("creating index: $productIndexName mappings: ${String(mappingBytes)}")

                val exists = esClient.indices().exists(ExistsRequest.Builder().index(productIndexName).build()).await()
                if (!exists.value()) {
                    esClient.indices().create {
                        it.index(productIndexName)
                            .withJson(productsMappingsResourceFile.inputStream)
                            .aliases(mutableMapOf(productIndexAliasName to Alias.of { builder -> builder.routing(productIndexName) }))
                    }.await()
                        .also { log.info("index created: ${it.index()}") }
                    return@withContext
                }
                esClient.indices().get(GetIndexRequest.Builder().index(productIndexName).build()).await()
                    .also { log.info("index already exists: ${it.result()}") }

            } catch (ex: Exception) {
                log.error("error while loading mappings file: ${ex.message}", ex)
            }
        }
    }

    companion object {
        private val log = Loggers.getLogger(ElasticSearchIndexConfig::class.java)
    }
}