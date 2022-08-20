package com.search.microservice.controller

import com.search.microservice.domain.Product
import com.search.microservice.dto.IndexProductRequest
import com.search.microservice.service.ProductService
import kotlinx.coroutines.withTimeout
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import reactor.util.Loggers


@RestController
@RequestMapping(path = ["/api/v1/products"])
class ProductController(private val productService: ProductService) {

    @PostMapping
    suspend fun indexAsync(@RequestBody request: IndexProductRequest): ResponseEntity<*> = withTimeout(timeoutMillis) {
        val product = Product.of(request)
        productService.index(product).let { ResponseEntity.ok(product).also { log.info("indexed product: $product") } }
    }


    @GetMapping("/search")
    suspend fun search(
        @RequestParam(name = "term") term: String,
        @RequestParam(name = "page", required = false, defaultValue = "0") page: Int,
        @RequestParam(name = "size", required = false, defaultValue = "10") size: Int,
    ) = withTimeout(timeoutMillis) {
        productService.search(term, page, size).let { ResponseEntity.ok(it) }.also { log.info("response: $it") }
    }

    companion object {
        private val log = Loggers.getLogger(ProductController::class.java)
        private const val timeoutMillis = 5555000L
    }
}