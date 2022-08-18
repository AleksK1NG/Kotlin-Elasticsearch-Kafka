package com.search.microservice.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.search.microservice.dto.IndexProductRequest
import java.time.Instant
import java.util.*


@JsonIgnoreProperties(ignoreUnknown = true)
class Product(
    @JsonProperty(value = "id") var id: String = "",
    @JsonProperty(value = "title") var title: String = "",
    @JsonProperty(value = "description") var description: String = "",
    @JsonProperty(value = "image_url") var imageUrl: String = "",
    @JsonProperty(value = "count_in_stock") var countInStock: Long = 0L,
    @JsonProperty(value = "shop") var shop: String = "",
    @JsonProperty(value = "created_at") var createdAt: Date = Date.from(Instant.now())
) {

    companion object {
        fun of(indexProductRequest: IndexProductRequest): Product {
            return Product(
                id = UUID.randomUUID().toString(),
                title = indexProductRequest.title,
                description = indexProductRequest.description,
                imageUrl = indexProductRequest.imageUrl,
                shop = indexProductRequest.shop,
                countInStock = indexProductRequest.countInStock,
                createdAt = Date.from(Instant.now())
            )
        }
    }


    override fun toString(): String {
        return "Product(id='$id', title='$title', description='$description', imageUrl='$imageUrl', countInStock='$countInStock', shop='$shop', createdAt=$createdAt)"
    }
}
