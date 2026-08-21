package com.example.tripzy_backend.auth.dto

import com.example.tripzy_backend.user.dto.UserDto
import jakarta.validation.constraints.NotBlank

data class GoogleAuthRequest(
    @field:NotBlank(message = "idToken is required")
    val idToken: String
)

data class AuthResponse(
    val token: String,
    val user: UserDto,
    val expiresInMs: Long
)

data class GoogleTokenPayload(
    val sub: String,
    val email: String,
    val name: String,
    val picture: String? = null,
    val emailVerified: Boolean = true
)
