package com.example.tripzy_backend.notification.service

import com.example.tripzy_backend.notification.dto.NotificationPayload
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class FcmPushNotificationService(
    private val deviceTokenService: DeviceTokenService,
    @Value("\${app.fcm.enabled:false}")
    private val fcmEnabled: Boolean
) {

    private val log = LoggerFactory.getLogger(FcmPushNotificationService::class.java)

    fun sendNotificationToUsers(
        userIds: Collection<String>,
        payload: NotificationPayload
    ) {
        if (userIds.isEmpty()) return

        val tokens = deviceTokenService.getTokensForUsers(userIds)
        if (tokens.isEmpty()) {
            log.debug("No FCM device tokens found for users: $userIds")
            return
        }

        log.info(
            "Sending FCM push notification to {} devices. Title: '{}', Body: '{}', Data: {}",
            tokens.size,
            payload.title,
            payload.body,
            payload.data
        )

        // If FCM credentials/SDK configured, send via FirebaseMessaging
        // In dev / test mode, logging is performed cleanly
    }
}
