package com.example.tripzy_backend.expense.model

/**
 * Represents who actually paid the money out-of-pocket for an expense.
 * @param userId the user who disbursed the funds
 * @param amount the exact amount paid in smallest currency unit (Paisa)
 */
data class Payment(
    val userId: String,
    val amount: Long
) {
    init {
        require(amount > 0L) { "Payment amount must be greater than zero" }
        require(userId.isNotBlank()) { "Payment userId must not be blank" }
    }
}

/**
 * Represents who is financially responsible / allocated for the cost.
 * @param userId the user who bears this portion of the cost
 * @param amount the exact allocated amount in smallest currency unit (Paisa)
 */
data class Allocation(
    val userId: String,
    val amount: Long
) {
    init {
        require(amount >= 0L) { "Allocation amount must not be negative" }
        require(userId.isNotBlank()) { "Allocation userId must not be blank" }
    }
}
