package com.example.tripzy_backend.user.service

import com.example.tripzy_backend.common.exception.ResourceNotFoundException
import com.example.tripzy_backend.user.dto.UpdateUserRequest
import com.example.tripzy_backend.user.model.User
import com.example.tripzy_backend.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class UserServiceTest {

    private val userRepository: UserRepository = mockk()
    private lateinit var userService: UserService

    @BeforeEach
    fun setUp() {
        userService = UserService(userRepository)
    }

    @Test
    fun `getProfile should return UserDto when user exists`() {
        val user = User(id = "u1", email = "test@example.com", name = "Test User", avatarUrl = "http://pic.jpg")
        every { userRepository.findById("u1") } returns Optional.of(user)

        val result = userService.getProfile("u1")

        assertEquals("u1", result.id)
        assertEquals("test@example.com", result.email)
        assertEquals("Test User", result.name)
        assertEquals("http://pic.jpg", result.avatarUrl)
    }

    @Test
    fun `getProfile should throw ResourceNotFoundException when user not found`() {
        every { userRepository.findById("non-existent") } returns Optional.empty()

        assertThrows(ResourceNotFoundException::class.java) {
            userService.getProfile("non-existent")
        }
    }

    @Test
    fun `updateProfile should update name and avatarUrl`() {
        val user = User(id = "u1", email = "test@example.com", name = "Old Name", avatarUrl = "old.jpg")
        val updatedUser = User(id = "u1", email = "test@example.com", name = "New Name", avatarUrl = "new.jpg")

        every { userRepository.findById("u1") } returns Optional.of(user)
        every { userRepository.save(any()) } returns updatedUser

        val req = UpdateUserRequest(name = "New Name", avatarUrl = "new.jpg")
        val result = userService.updateProfile("u1", req)

        assertEquals("New Name", result.name)
        assertEquals("new.jpg", result.avatarUrl)
        verify { userRepository.save(match { it.name == "New Name" && it.avatarUrl == "new.jpg" }) }
    }

    @Test
    fun `findOrCreateGoogleUser should return existing user when googleId matches`() {
        val existing = User(id = "u1", email = "test@example.com", name = "Existing Name", googleId = "google-123")
        every { userRepository.findByGoogleId("google-123") } returns Optional.of(existing)

        val result = userService.findOrCreateGoogleUser("google-123", "test@example.com", "Existing Name", null)

        assertEquals("u1", result.id)
        assertEquals("google-123", result.googleId)
    }

    @Test
    fun `findOrCreateGoogleUser should create new user when not found`() {
        every { userRepository.findByGoogleId("google-new") } returns Optional.empty()
        every { userRepository.findByEmail("new@example.com") } returns Optional.empty()
        val created = User(id = "u2", email = "new@example.com", name = "New User", googleId = "google-new")
        every { userRepository.save(any()) } returns created

        val result = userService.findOrCreateGoogleUser("google-new", "new@example.com", "New User", null)

        assertNotNull(result)
        assertEquals("u2", result.id)
        verify { userRepository.save(match { it.email == "new@example.com" && it.googleId == "google-new" }) }
    }
}
