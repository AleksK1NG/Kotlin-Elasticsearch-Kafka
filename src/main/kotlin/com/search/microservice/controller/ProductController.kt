package com.search.microservice.controller

import com.search.microservice.domain.Product
import com.search.microservice.dto.IndexProductRequest
import com.search.microservice.repository.ProductElasticRepository
import kotlinx.coroutines.withTimeout
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.util.Loggers


@RestController
@RequestMapping(path = ["/api/v1/products"])
class ProductController(private val productElasticRepository: ProductElasticRepository) {

    @PostMapping
    suspend fun indexAsync(@RequestBody request: IndexProductRequest): ResponseEntity<*> = withTimeout(timeoutMillis) {
        log.info("request: $request")
        productElasticRepository.index(Product.of(request))
        ResponseEntity.ok(Product.of(request)).also { log.info("index product") }
    }

    @GetMapping("/search")
    suspend fun search(
        @RequestParam(name = "term") term: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "10") size: Int,
    ) = withTimeout(timeoutMillis) {
        val paginationResponse = productElasticRepository.search(term, page, size)
        ResponseEntity.ok(paginationResponse).also { log.info("search term success: $term") }
    }

    companion object {
        private val log = Loggers.getLogger(ProductController::class.java)
        private const val timeoutMillis = 5555000L
    }
}