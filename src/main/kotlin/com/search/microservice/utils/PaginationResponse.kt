package com.search.microservice.utils

import kotlin.math.ceil


data class PaginationResponse<T>(
    val page: Int,
    val size: Int,
    val totalCount: Long,
    val totalPages: Int,
    val hasMore: Boolean,
    val list: List<T>
) {

    companion object {
        fun <T> of(page: Int, size: Int, totalCount: Long, list: List<T>): PaginationResponse<T> {
            val total = ceil((totalCount.toInt().toDouble() / size.toDouble())).toInt()
            val hasMore = (page + 1) < total
            return PaginationResponse(page, size, totalCount, total, hasMore, list)
        }
    }

    override fun toString(): String {
        return "PaginationResponse(page=$page, size=$size, totalCount=$totalCount, totalPages=$totalPages, hasMore=$hasMore, listSize=${list.size})"
    }
}
