package com.search.microservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import com.github.javafaker.Faker
import com.search.microservice.dto.IndexProductRequest
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitExchange
import reactor.util.Loggers
import java.util.*

//@SpringBootTest
class SearchMicroserviceApplicationTests(
//    @Autowired private val webClient: WebClient,
//    @Autowired private val faker: Faker,
) {

    private val webClient: WebClient = WebClient.builder().build()
    private val faker = Faker(Locale("en"))
    private val mapper: ObjectMapper = jacksonObjectMapper()
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

    @Test
    fun indexProducts(): Unit = runBlocking {
        repeat(1000) {
            val request = IndexProductRequest(
                title = faker.food().fruit(),
                description = faker.lorem().fixedString(60),
                imageUrl = faker.company().url(),
                countInStock = faker.number().numberBetween(1L, 555555L),
                shop = faker.address().streetAddress(),
            )


            try {
                webClient
                    .post()
                    .uri("http://localhost:8000/api/v1/products")
                    .contentType(APPLICATION_JSON)
                    .body(BodyInserters.fromValue(mapper.writeValueAsString(request)))
                    .awaitExchange { log.info("response: ${it.rawStatusCode()}") }

            } catch (ex: Exception) {
                log.error("indexProducts", ex)
            }
        }
    }

    companion object {
        private val log = Loggers.getLogger(SearchMicroserviceApplicationTests::class.java)
    }
}
