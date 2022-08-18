package com.search.microservice.repository

import com.search.microservice.domain.Product

interface ProductElasticRepository {
    suspend fun index(product: Product): Unit
}