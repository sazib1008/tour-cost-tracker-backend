package com.example.tripzy_backend.user.dto

import com.example.tripzy_backend.user.model.User
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UserDto(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String?
) {
    companion object {
        fun fromEntity(user: User): UserDto = UserDto(
            id = user.id ?: "",
            email = user.email,
            name = user.name,
            avatarUrl = user.avatarUrl
        )
    }
}

data class UpdateUserRequest(
    @field:NotBlank(message = "Name cannot be blank")
    @field:Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    val name: String,

    val avatarUrl: String? = null
)
