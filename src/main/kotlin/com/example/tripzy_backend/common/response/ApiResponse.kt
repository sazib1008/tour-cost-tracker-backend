package com.example.tripzy_backend.common.response

import java.time.Instant

data class ApiResponse<T>(
    val success: Boolean = true,
    val message: String? = null,
    val data: T? = null,
    val timestamp: Instant = Instant.now()
) {
    companion object {
        fun <T> success(data: T, message: String? = null): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = data)

        fun <T> successMessage(message: String): ApiResponse<T> =
            ApiResponse(success = true, message = message, data = null)
    }
}
