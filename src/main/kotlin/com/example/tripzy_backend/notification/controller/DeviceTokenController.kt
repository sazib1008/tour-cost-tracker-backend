package com.example.tripzy_backend.notification.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.response.ApiResponse
import com.example.tripzy_backend.notification.dto.RegisterDeviceTokenRequest
import com.example.tripzy_backend.notification.service.DeviceTokenService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users/me/device-tokens")
class DeviceTokenController(
    private val deviceTokenService: DeviceTokenService
) {

    @PostMapping
    fun registerDeviceToken(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: RegisterDeviceTokenRequest
    ): ResponseEntity<ApiResponse<Unit>> {
        deviceTokenService.registerToken(principal.id, request)
        return ResponseEntity.ok(ApiResponse.successMessage("Device token registered successfully"))
    }

    @DeleteMapping("/{token}")
    fun deleteDeviceToken(
        @AuthenticationPrincipal principal: UserPrincipal,
        @PathVariable token: String
    ): ResponseEntity<ApiResponse<Unit>> {
        deviceTokenService.removeToken(principal.id, token)
        return ResponseEntity.ok(ApiResponse.successMessage("Device token removed successfully"))
    }
}
