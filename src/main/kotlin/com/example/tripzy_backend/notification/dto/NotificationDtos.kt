package com.example.tripzy_backend.notification.dto

import jakarta.validation.constraints.NotBlank

data class RegisterDeviceTokenRequest(
    @field:NotBlank(message = "FCM token is required")
    val token: String,

    val deviceType: String = "ANDROID"
)

data class NotificationPayload(
    val title: String,
    val body: String,
    val data: Map<String, String> = emptyMap()
)
