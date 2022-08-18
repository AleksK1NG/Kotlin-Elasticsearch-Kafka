package com.search.microservice.service

import com.search.microservice.domain.Product
import com.search.microservice.utils.PaginationResponse

interface ProductService {
    suspend fun index(product: Product)
    suspend fun search(term: String, page: Int, size: Int): PaginationResponse<Product>
}