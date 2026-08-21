package com.example.tripzy_backend.notification.repository

import com.example.tripzy_backend.notification.model.NotificationToken
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NotificationTokenRepository : MongoRepository<NotificationToken, String> {
    fun findByUserId(userId: String): List<NotificationToken>
    fun findByUserIdIn(userIds: Collection<String>): List<NotificationToken>
    fun findByUserIdAndToken(userId: String, token: String): Optional<NotificationToken>
    fun deleteByUserIdAndToken(userId: String, token: String)
    fun deleteByToken(token: String)
}
