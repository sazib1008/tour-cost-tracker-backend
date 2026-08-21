package com.example.tripzy_backend.notification.model

import com.example.tripzy_backend.common.domain.BaseEntity
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "notification_tokens")
@CompoundIndex(name = "user_token_unique_idx", def = "{'userId': 1, 'token': 1}", unique = true)
data class NotificationToken(
    @Id
    var id: String? = null,

    @Indexed
    var userId: String,

    var token: String,

    var deviceType: String = "ANDROID",

    var lastUpdatedAt: Instant = Instant.now()
) : BaseEntity()
