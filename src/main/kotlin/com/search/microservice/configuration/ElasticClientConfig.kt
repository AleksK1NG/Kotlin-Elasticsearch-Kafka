package com.search.microservice.configuration

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import co.elastic.clients.json.jackson.JacksonJsonpMapper
import co.elastic.clients.transport.ElasticsearchTransport
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class ElasticClientConfig {

    @Value(value = "\${elasticsearch.host:localhost}")
    val elasticSearchHost: String = "localhost"

    @Value(value = "\${elasticsearch.port:9200}")
    val elasticSearchPort: Int = 9200

    @Bean
    fun restClient(): RestClient = RestClient.builder(HttpHost(elasticSearchHost, elasticSearchPort)).build()

    @Bean
    fun getRestClientTransport(restClient: RestClient): RestClientTransport = RestClientTransport(
        restClient, JacksonJsonpMapper(
            jacksonObjectMapper()
                .registerModule(ParameterNamesModule())
                .registerModule(Jdk8Module())
                .registerModule(JavaTimeModule())
                .registerModule(
                    KotlinModule.Builder()
                        .withReflectionCacheSize(512)
                        .configure(KotlinFeature.NullToEmptyCollection, false)
                        .configure(KotlinFeature.NullToEmptyMap, false)
                        .configure(KotlinFeature.NullIsSameAsDefault, false)
                        .configure(KotlinFeature.SingletonSupport, false)
                        .configure(KotlinFeature.StrictNullChecks, false)
                        .build()
                )
        )
    )

    @Bean
    fun asyncElasticsearchClient(clientTransport: ElasticsearchTransport): ElasticsearchAsyncClient = ElasticsearchAsyncClient(clientTransport)
}