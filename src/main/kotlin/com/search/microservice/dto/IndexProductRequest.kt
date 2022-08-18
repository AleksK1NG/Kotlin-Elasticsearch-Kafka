package com.search.microservice.dto

data class IndexProductRequest(
    val title: String,
    val description: String,
    val imageUrl: String,
    val countInStock: Long,
    val shop: String,
)
