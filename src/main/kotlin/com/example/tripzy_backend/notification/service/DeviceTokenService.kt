package com.example.tripzy_backend.notification.service

import com.example.tripzy_backend.notification.dto.RegisterDeviceTokenRequest
import com.example.tripzy_backend.notification.model.NotificationToken
import com.example.tripzy_backend.notification.repository.NotificationTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class DeviceTokenService(
    private val notificationTokenRepository: NotificationTokenRepository
) {

    @Transactional
    fun registerToken(userId: String, request: RegisterDeviceTokenRequest) {
        val existing = notificationTokenRepository.findByUserIdAndToken(userId, request.token)
        if (existing.isPresent) {
            val tokenEntity = existing.get()
            tokenEntity.lastUpdatedAt = Instant.now()
            tokenEntity.deviceType = request.deviceType
            notificationTokenRepository.save(tokenEntity)
        } else {
            val newToken = NotificationToken(
                userId = userId,
                token = request.token,
                deviceType = request.deviceType,
                lastUpdatedAt = Instant.now()
            )
            notificationTokenRepository.save(newToken)
        }
    }

    @Transactional
    fun removeToken(userId: String, token: String) {
        notificationTokenRepository.deleteByUserIdAndToken(userId, token)
    }

    fun getTokensForUsers(userIds: Collection<String>): List<String> {
        return notificationTokenRepository.findByUserIdIn(userIds).map { it.token }
    }
}
