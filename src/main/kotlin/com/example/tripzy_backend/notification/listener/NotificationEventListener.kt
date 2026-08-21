package com.example.tripzy_backend.notification.listener

import com.example.tripzy_backend.common.domain.Money
import com.example.tripzy_backend.notification.dto.NotificationPayload
import com.example.tripzy_backend.notification.event.ExpenseCreatedEvent
import com.example.tripzy_backend.notification.event.MemberJoinedEvent
import com.example.tripzy_backend.notification.event.SettlementRecordedEvent
import com.example.tripzy_backend.notification.event.TourStatusChangedEvent
import com.example.tripzy_backend.notification.service.FcmPushNotificationService
import com.example.tripzy_backend.tour.repository.TourMemberRepository
import com.example.tripzy_backend.tour.repository.TourRepository
import com.example.tripzy_backend.user.repository.UserRepository
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class NotificationEventListener(
    private val fcmPushNotificationService: FcmPushNotificationService,
    private val tourMemberRepository: TourMemberRepository,
    private val tourRepository: TourRepository,
    private val userRepository: UserRepository
) {

    @Async
    @EventListener
    fun handleExpenseCreated(event: ExpenseCreatedEvent) {
        val tour = tourRepository.findById(event.tourId).orElse(null) ?: return
        val creator = userRepository.findById(event.createdByUserId).orElse(null)
        val creatorName = creator?.name ?: "A member"

        val members = tourMemberRepository.findByTourId(event.tourId)
        val recipientUserIds = members.map { it.userId }.filter { it != event.createdByUserId }

        val formattedAmount = Money.ofPaisa(event.amount, event.currency).formatted()
        val payload = NotificationPayload(
            title = "New Expense in ${tour.title}",
            body = "$creatorName added \"${event.title}\" ($formattedAmount)",
            data = mapOf(
                "type" to "EXPENSE_CREATED",
                "tourId" to event.tourId,
                "expenseId" to event.expenseId
            )
        )
        fcmPushNotificationService.sendNotificationToUsers(recipientUserIds, payload)
    }

    @Async
    @EventListener
    fun handleSettlementRecorded(event: SettlementRecordedEvent) {
        val tour = tourRepository.findById(event.tourId).orElse(null) ?: return
        val fromUser = userRepository.findById(event.fromUserId).orElse(null)
        val fromUserName = fromUser?.name ?: "A member"

        val formattedAmount = Money.ofPaisa(event.amount).formatted()
        val payload = NotificationPayload(
            title = "Settlement in ${tour.title}",
            body = "$fromUserName recorded a payment of $formattedAmount via ${event.paymentMethod}",
            data = mapOf(
                "type" to "SETTLEMENT_RECORDED",
                "tourId" to event.tourId,
                "settlementId" to event.settlementId
            )
        )
        // Notify the recipient directly
        fcmPushNotificationService.sendNotificationToUsers(listOf(event.toUserId), payload)
    }

    @Async
    @EventListener
    fun handleMemberJoined(event: MemberJoinedEvent) {
        val tour = tourRepository.findById(event.tourId).orElse(null) ?: return
        val members = tourMemberRepository.findByTourId(event.tourId)
        val recipientUserIds = members.map { it.userId }.filter { it != event.userId }

        val payload = NotificationPayload(
            title = "New Member in ${tour.title}",
            body = "${event.memberName} joined the tour!",
            data = mapOf(
                "type" to "MEMBER_JOINED",
                "tourId" to event.tourId
            )
        )
        fcmPushNotificationService.sendNotificationToUsers(recipientUserIds, payload)
    }

    @Async
    @EventListener
    fun handleTourStatusChanged(event: TourStatusChangedEvent) {
        val tour = tourRepository.findById(event.tourId).orElse(null) ?: return
        val members = tourMemberRepository.findByTourId(event.tourId)
        val recipientUserIds = members.map { it.userId }.filter { it != event.changedByUserId }

        val payload = NotificationPayload(
            title = "${tour.title} status updated",
            body = "Tour status is now ${event.newStatus}",
            data = mapOf(
                "type" to "TOUR_STATUS_CHANGED",
                "tourId" to event.tourId,
                "newStatus" to event.newStatus.name
            )
        )
        fcmPushNotificationService.sendNotificationToUsers(recipientUserIds, payload)
    }
}
