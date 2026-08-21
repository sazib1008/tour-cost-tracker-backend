package com.example.tripzy_backend.settlement.model

import com.example.tripzy_backend.common.domain.BaseEntity
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document(collection = "settlements")
data class Settlement(
    @Id
    var id: String? = null,

    @Indexed
    var tourId: String,

    @Indexed
    var fromUserId: String, // Debtor who pays

    @Indexed
    var toUserId: String, // Creditor who receives

    var amount: Long, // in Paisa

    var paymentMethod: PaymentMethod = PaymentMethod.CASH,

    var notes: String? = null,

    var referenceId: String? = null, // e.g. bKash / Bank transaction reference

    var settledAt: Instant = Instant.now(),

    @Indexed
    var createdBy: String
) : BaseEntity() {
    init {
        require(amount > 0L) { "Settlement amount must be greater than zero" }
        require(fromUserId != toUserId) { "Cannot create a settlement between the same user" }
    }
}
