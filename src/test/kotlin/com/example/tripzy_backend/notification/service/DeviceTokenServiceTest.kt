package com.example.tripzy_backend.notification.service

import com.example.tripzy_backend.notification.dto.RegisterDeviceTokenRequest
import com.example.tripzy_backend.notification.model.NotificationToken
import com.example.tripzy_backend.notification.repository.NotificationTokenRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Optional

class DeviceTokenServiceTest {

    private val notificationTokenRepository: NotificationTokenRepository = mockk()
    private lateinit var deviceTokenService: DeviceTokenService

    @BeforeEach
    fun setUp() {
        deviceTokenService = DeviceTokenService(notificationTokenRepository)
    }

    @Test
    fun `registerToken should save new token if not exists`() {
        every { notificationTokenRepository.findByUserIdAndToken("u1", "fcm-token-1") } returns Optional.empty()
        every { notificationTokenRepository.save(any()) } answers { firstArg() }

        deviceTokenService.registerToken("u1", RegisterDeviceTokenRequest("fcm-token-1", "ANDROID"))

        verify { notificationTokenRepository.save(match { it.userId == "u1" && it.token == "fcm-token-1" }) }
    }

    @Test
    fun `getTokensForUsers should return token list`() {
        val tokenEntity = NotificationToken(userId = "u1", token = "fcm-123")
        every { notificationTokenRepository.findByUserIdIn(listOf("u1")) } returns listOf(tokenEntity)

        val tokens = deviceTokenService.getTokensForUsers(listOf("u1"))

        assertEquals(1, tokens.size)
        assertEquals("fcm-123", tokens[0])
    }
}
