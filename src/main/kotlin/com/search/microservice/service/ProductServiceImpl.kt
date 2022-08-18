package com.search.microservice.service

import com.search.microservice.domain.Product
import com.search.microservice.repository.ProductElasticRepository
import com.search.microservice.utils.PaginationResponse
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service


@Service
class ProductServiceImpl(private val productElasticRepository: ProductElasticRepository): ProductService {
    override suspend fun index(product: Product) = coroutineScope {
        productElasticRepository.index(product)
    }

    override suspend fun search(term: String, page: Int, size: Int): PaginationResponse<Product> = coroutineScope {
        productElasticRepository.search(term, page, size)
    }
}