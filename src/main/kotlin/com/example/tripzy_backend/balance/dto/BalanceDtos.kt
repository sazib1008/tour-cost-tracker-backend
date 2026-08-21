package com.example.tripzy_backend.balance.dto

data class UserBalanceDetailDto(
    val userId: String,
    val name: String,
    val email: String,
    val avatarUrl: String?,
    val totalPaid: Long, // in Paisa
    val totalResponsible: Long, // in Paisa
    val personalSpend: Long, // in Paisa
    val sharedPaid: Long, // in Paisa
    val sharedResponsible: Long, // in Paisa
    val netBalance: Long // in Paisa (positive = receives, negative = owes)
)

data class SuggestedTransferDetailDto(
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Long // in Paisa
)

data class TourBalanceResponseDto(
    val tourId: String,
    val totalCost: Long,
    val totalSharedCost: Long,
    val totalPersonalCost: Long,
    val myBalance: UserBalanceDetailDto?,
    val memberBalances: List<UserBalanceDetailDto>,
    val suggestedTransfers: List<SuggestedTransferDetailDto>
)
