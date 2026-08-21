package com.example.tripzy_backend.user.service

import com.example.tripzy_backend.common.exception.ResourceNotFoundException
import com.example.tripzy_backend.user.dto.UpdateUserRequest
import com.example.tripzy_backend.user.dto.UserDto
import com.example.tripzy_backend.user.model.User
import com.example.tripzy_backend.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository
) {

    fun getProfile(userId: String): UserDto {
        val user = findById(userId)
        return UserDto.fromEntity(user)
    }

    @Transactional
    fun updateProfile(userId: String, request: UpdateUserRequest): UserDto {
        val user = findById(userId)
        user.name = request.name
        request.avatarUrl?.let { user.avatarUrl = it }
        val saved = userRepository.save(user)
        return UserDto.fromEntity(saved)
    }

    @Transactional
    fun findOrCreateGoogleUser(
        googleId: String,
        email: String,
        name: String,
        avatarUrl: String?
    ): User {
        val existingByGoogleId = userRepository.findByGoogleId(googleId)
        if (existingByGoogleId.isPresent) {
            val user = existingByGoogleId.get()
            var modified = false
            if (user.avatarUrl != avatarUrl && avatarUrl != null) {
                user.avatarUrl = avatarUrl
                modified = true
            }
            if (user.name != name && name.isNotBlank()) {
                user.name = name
                modified = true
            }
            return if (modified) userRepository.save(user) else user
        }

        val existingByEmail = userRepository.findByEmail(email)
        if (existingByEmail.isPresent) {
            val user = existingByEmail.get()
            user.googleId = googleId
            if (avatarUrl != null && user.avatarUrl == null) {
                user.avatarUrl = avatarUrl
            }
            return userRepository.save(user)
        }

        val newUser = User(
            email = email,
            name = name,
            avatarUrl = avatarUrl,
            googleId = googleId
        )
        return userRepository.save(newUser)
    }

    fun findById(userId: String): User =
        userRepository.findById(userId).orElseThrow {
            ResourceNotFoundException("User not found with id: $userId")
        }

    fun findByEmail(email: String): User =
        userRepository.findByEmail(email).orElseThrow {
            ResourceNotFoundException("User not found with email: $email")
        }
}
