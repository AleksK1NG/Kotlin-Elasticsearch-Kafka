package com.search.microservice.exceptions

data class ErrorHttpResponse(
    val status: Int,
    val message: String,
    val timestamp: String
)
