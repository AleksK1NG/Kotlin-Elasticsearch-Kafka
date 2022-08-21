package com.search.microservice.configuration

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.springframework.boot.actuate.health.AbstractHealthIndicator
import org.springframework.boot.actuate.health.Health
import org.springframework.stereotype.Component
import reactor.util.Loggers


@Component
class ElasticHealthCheck(private val esClient: ElasticsearchAsyncClient) : AbstractHealthIndicator() {
    override fun doHealthCheck(builder: Health.Builder): Unit = runBlocking {
        try {
            if (esClient.ping().await().value()) {
                builder.up().also { log.info("(doHealthCheck) status up") }
            } else {
                builder.down()
            }
        } catch (ex: Exception) {
            builder.down().also { log.error("doHealthCheck", ex) }
        }
    }

    companion object {
        private val log = Loggers.getLogger(ElasticHealthCheck::class.java)
    }
}