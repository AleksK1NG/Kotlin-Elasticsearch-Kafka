package com.search.microservice.configuration

import co.elastic.clients.elasticsearch.ElasticsearchAsyncClient
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import org.springframework.boot.availability.ApplicationAvailability
import org.springframework.boot.availability.AvailabilityChangeEvent
import org.springframework.boot.availability.LivenessState
import org.springframework.boot.availability.ReadinessState
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.util.Loggers


@Component
class ElasticSearchHealthCheck(
    private val esClient: ElasticsearchAsyncClient,
    private val applicationContext: ApplicationContext,
    private val availability: ApplicationAvailability,
) {

    @Scheduled(initialDelay = 15000, fixedRate = 3000)
    fun runElasticHealthCheck(): Unit = runBlocking {
        try {
            val isAvailable = esClient.ping().await().value()
            if (!isAvailable && !availability.livenessState.equals(LivenessState.BROKEN) || !availability.readinessState.equals(ReadinessState.ACCEPTING_TRAFFIC)) {
                AvailabilityChangeEvent.publish(applicationContext, LivenessState.BROKEN)
                AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC)
                log.error("(ElasticSearchHealthCheck) status livenessState: ${availability.livenessState}, readinessState: ${availability.readinessState}")
            }
            if (isAvailable) {
                AvailabilityChangeEvent.publish(applicationContext, LivenessState.CORRECT)
                AvailabilityChangeEvent.publish(applicationContext, ReadinessState.ACCEPTING_TRAFFIC)
                log.info("(ElasticSearchHealthCheck) status livenessState: ${availability.livenessState}, readinessState: ${availability.readinessState}")
            }
        } catch (ex: Exception) {
            AvailabilityChangeEvent.publish(applicationContext, LivenessState.BROKEN)
            AvailabilityChangeEvent.publish(applicationContext, ReadinessState.REFUSING_TRAFFIC)
            log.error("(ElasticSearchHealthCheck) status livenessState: ${availability.livenessState}, readinessState: ${availability.readinessState}", ex)
        }

    }

    companion object {
        private val log = Loggers.getLogger(ElasticSearchHealthCheck::class.java)
    }
}