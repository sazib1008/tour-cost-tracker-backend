package com.example.tripzy_backend.user.controller

import com.example.tripzy_backend.auth.security.UserPrincipal
import com.example.tripzy_backend.common.response.ApiResponse
import com.example.tripzy_backend.user.dto.UpdateUserRequest
import com.example.tripzy_backend.user.dto.UserDto
import com.example.tripzy_backend.user.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/me")
    fun getCurrentUser(
        @AuthenticationPrincipal principal: UserPrincipal
    ): ResponseEntity<ApiResponse<UserDto>> {
        val user = userService.getProfile(principal.id)
        return ResponseEntity.ok(ApiResponse.success(user))
    }

    @PutMapping("/me")
    fun updateCurrentUser(
        @AuthenticationPrincipal principal: UserPrincipal,
        @Valid @RequestBody request: UpdateUserRequest
    ): ResponseEntity<ApiResponse<UserDto>> {
        val updated = userService.updateProfile(principal.id, request)
        return ResponseEntity.ok(ApiResponse.success(data = updated, message = "Profile updated successfully"))
    }

    @GetMapping("/{id}")
    fun getUserById(
        @PathVariable id: String
    ): ResponseEntity<ApiResponse<UserDto>> {
        val user = userService.getProfile(id)
        return ResponseEntity.ok(ApiResponse.success(user))
    }
}
