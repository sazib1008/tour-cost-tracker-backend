package com.example.tripzy_backend.balance.model

/**
 * Balance calculation breakdown for a single user in a tour.
 * All amounts are in smallest currency unit (Paisa).
 */
data class UserBalance(
    val userId: String,
    val totalPaid: Long = 0L,
    val totalResponsible: Long = 0L,
    val personalSpend: Long = 0L,
    val sharedPaid: Long = 0L,
    val sharedResponsible: Long = 0L,
    val netBalance: Long = 0L // totalPaid - (sharedResponsible + personalSpend) (positive = receives money, negative = owes money)
) {
    fun isSettled(): Boolean = netBalance == 0L
    fun owes(): Boolean = netBalance < 0L
    fun willReceive(): Boolean = netBalance > 0L
}

/**
 * Suggested minimal settlement transaction produced by debt simplification algorithm.
 */
data class SuggestedTransfer(
    val fromUserId: String,
    val toUserId: String,
    val amount: Long // in Paisa
) {
    init {
        require(amount > 0L) { "Transfer amount must be strictly positive" }
        require(fromUserId != toUserId) { "Cannot transfer money to oneself" }
    }
}

/**
 * Full calculated tour balance overview.
 */
data class TourBalanceSummary(
    val tourId: String,
    val totalCost: Long,
    val totalSharedCost: Long,
    val totalPersonalCost: Long,
    val balances: Map<String, UserBalance>,
    val suggestedTransfers: List<SuggestedTransfer>
)
