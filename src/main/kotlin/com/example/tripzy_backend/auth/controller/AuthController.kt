package com.example.tripzy_backend.auth.controller

import com.example.tripzy_backend.auth.dto.AuthResponse
import com.example.tripzy_backend.auth.dto.GoogleAuthRequest
import com.example.tripzy_backend.auth.service.AuthService
import com.example.tripzy_backend.common.response.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/google")
    fun authenticateWithGoogle(
        @Valid @RequestBody request: GoogleAuthRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val authResponse = authService.authenticateWithGoogle(request)
        return ResponseEntity.ok(ApiResponse.success(data = authResponse, message = "Authentication successful"))
    }
}
