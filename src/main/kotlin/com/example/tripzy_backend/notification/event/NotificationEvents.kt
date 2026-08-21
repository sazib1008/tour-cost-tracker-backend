package com.example.tripzy_backend.notification.event

import com.example.tripzy_backend.common.domain.TourStatus
import com.example.tripzy_backend.settlement.model.PaymentMethod

data class ExpenseCreatedEvent(
    val tourId: String,
    val expenseId: String,
    val title: String,
    val amount: Long, // in Paisa
    val currency: String,
    val createdByUserId: String
)

data class SettlementRecordedEvent(
    val tourId: String,
    val settlementId: String,
    val fromUserId: String,
    val toUserId: String,
    val amount: Long, // in Paisa
    val paymentMethod: PaymentMethod
)

data class MemberJoinedEvent(
    val tourId: String,
    val userId: String,
    val memberName: String
)

data class TourStatusChangedEvent(
    val tourId: String,
    val oldStatus: TourStatus,
    val newStatus: TourStatus,
    val changedByUserId: String
)
