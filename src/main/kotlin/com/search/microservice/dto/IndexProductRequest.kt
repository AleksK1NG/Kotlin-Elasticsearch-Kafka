package com.search.microservice.dto

import javax.validation.constraints.Min
import javax.validation.constraints.Size

data class IndexProductRequest(
    @get:Size(min = 4, max = 60) val title: String,
    @get:Size(min = 6, max = 5000) val description: String,
    @get:Size(min = 6, max = 1000) val imageUrl: String,
    @get:Min(1) val countInStock: Long,
    @get:Size(min = 6, max = 60) val shop: String,
)
