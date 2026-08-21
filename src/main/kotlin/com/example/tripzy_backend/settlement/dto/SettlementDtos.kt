package com.example.tripzy_backend.settlement.dto

import com.example.tripzy_backend.settlement.model.PaymentMethod
import com.example.tripzy_backend.settlement.model.Settlement
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class RecordSettlementRequest(
    @field:NotBlank(message = "Payee toUserId is required")
    val toUserId: String,

    @field:Min(value = 1, message = "Settlement amount must be greater than zero")
    val amount: Long, // in Paisa

    val paymentMethod: PaymentMethod = PaymentMethod.CASH,

    val notes: String? = null,

    val referenceId: String? = null,

    val settledAt: Instant? = null
)

data class SettlementDto(
    val id: String,
    val tourId: String,
    val fromUserId: String,
    val fromUserName: String,
    val fromUserAvatarUrl: String?,
    val toUserId: String,
    val toUserName: String,
    val toUserAvatarUrl: String?,
    val amount: Long, // in Paisa
    val paymentMethod: PaymentMethod,
    val notes: String?,
    val referenceId: String?,
    val settledAt: Instant,
    val createdBy: String,
    val createdAt: Instant
) {
    companion object {
        fun fromEntity(
            settlement: Settlement,
            fromUserName: String = "Unknown User",
            fromUserAvatarUrl: String? = null,
            toUserName: String = "Unknown User",
            toUserAvatarUrl: String? = null
        ): SettlementDto = SettlementDto(
            id = settlement.id ?: "",
            tourId = settlement.tourId,
            fromUserId = settlement.fromUserId,
            fromUserName = fromUserName,
            fromUserAvatarUrl = fromUserAvatarUrl,
            toUserId = settlement.toUserId,
            toUserName = toUserName,
            toUserAvatarUrl = toUserAvatarUrl,
            amount = settlement.amount,
            paymentMethod = settlement.paymentMethod,
            notes = settlement.notes,
            referenceId = settlement.referenceId,
            settledAt = settlement.settledAt,
            createdBy = settlement.createdBy,
            createdAt = settlement.createdAt
        )
    }
}
