package com.arekalov.islab1.dto

/**
 * DTO для пагинированного ответа
 */
data class PagedResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
) {
    companion object {
        fun <T> of(content: List<T>, page: Int, size: Int, totalElements: Long): PagedResponse<T> {
            val totalPages = if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
            return PagedResponse(
                content = content,
                page = page,
                size = size,
                totalElements = totalElements,
                totalPages = totalPages,
                hasNext = page < totalPages - 1,
                hasPrevious = page > 0
            )
        }
    }
}

/**
 * DTO для ответа об ошибке
 */
data class ErrorResponse(
    val message: String,
    val details: List<String>? = null,
    val timestamp: String = java.time.Instant.now().toString()
)
