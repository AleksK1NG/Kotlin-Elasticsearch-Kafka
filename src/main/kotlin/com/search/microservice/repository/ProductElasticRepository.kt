package com.search.microservice.repository

import com.search.microservice.domain.Product
import com.search.microservice.utils.PaginationResponse

interface ProductElasticRepository {
    suspend fun index(product: Product)
    suspend fun search(term: String, page: Int, size: Int): PaginationResponse<Product>
}