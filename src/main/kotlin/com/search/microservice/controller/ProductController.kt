package com.search.microservice.controller

import kotlinx.coroutines.withTimeout
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.util.Loggers


@RestController
@RequestMapping(path = ["/api/v1/products"])
class ProductController {

    @PostMapping
    suspend fun indexAsync(): ResponseEntity<String> = withTimeout(timeoutMillis) {
        ResponseEntity.ok("OK").also { log.info("index product") }
    }

    companion object {
        private val log = Loggers.getLogger(ProductController::class.java)
        private const val timeoutMillis = 5000L
    }
}