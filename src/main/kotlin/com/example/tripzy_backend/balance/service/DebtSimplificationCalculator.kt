package com.example.tripzy_backend.balance.service

import com.example.tripzy_backend.balance.model.SuggestedTransfer
import com.example.tripzy_backend.balance.model.TourBalanceSummary
import com.example.tripzy_backend.balance.model.UserBalance
import com.example.tripzy_backend.expense.model.Expense
import org.springframework.stereotype.Component
import java.util.PriorityQueue

@Component
class DebtSimplificationCalculator {

    data class BalanceEntry(
        val userId: String,
        val amount: Long
    )

    /**
     * Pure calculation function:
     * Computes member balances and minimal cash flow settlement transfers.
     */
    fun calculate(
        tourId: String,
        allMemberIds: Set<String>,
        expenses: List<Expense>,
        settlements: List<com.example.tripzy_backend.settlement.model.Settlement> = emptyList()
    ): TourBalanceSummary {
        var totalCost = 0L
        var totalSharedCost = 0L
        var totalPersonalCost = 0L

        val totalPaidMap = mutableMapOf<String, Long>().withDefault { 0L }
        val totalResponsibleMap = mutableMapOf<String, Long>().withDefault { 0L }
        val personalSpendMap = mutableMapOf<String, Long>().withDefault { 0L }
        val sharedPaidMap = mutableMapOf<String, Long>().withDefault { 0L }
        val sharedResponsibleMap = mutableMapOf<String, Long>().withDefault { 0L }

        // Initialize all members with 0
        allMemberIds.forEach { uid ->
            totalPaidMap[uid] = 0L
            totalResponsibleMap[uid] = 0L
            personalSpendMap[uid] = 0L
            sharedPaidMap[uid] = 0L
            sharedResponsibleMap[uid] = 0L
        }

        for (expense in expenses) {
            totalCost += expense.amount

            if (expense.isPersonal) {
                totalPersonalCost += expense.amount
                expense.payments.forEach { payment ->
                    val currentPaid = totalPaidMap.getValue(payment.userId)
                    totalPaidMap[payment.userId] = currentPaid + payment.amount
                    val currentPers = personalSpendMap.getValue(payment.userId)
                    personalSpendMap[payment.userId] = currentPers + payment.amount
                }
                expense.allocations.forEach { alloc ->
                    val currentResp = totalResponsibleMap.getValue(alloc.userId)
                    totalResponsibleMap[alloc.userId] = currentResp + alloc.amount
                }
            } else {
                totalSharedCost += expense.amount
                expense.payments.forEach { payment ->
                    val currentPaid = totalPaidMap.getValue(payment.userId)
                    totalPaidMap[payment.userId] = currentPaid + payment.amount
                    val currentSharedPaid = sharedPaidMap.getValue(payment.userId)
                    sharedPaidMap[payment.userId] = currentSharedPaid + payment.amount
                }
                expense.allocations.forEach { alloc ->
                    val currentResp = totalResponsibleMap.getValue(alloc.userId)
                    totalResponsibleMap[alloc.userId] = currentResp + alloc.amount
                    val currentSharedResp = sharedResponsibleMap.getValue(alloc.userId)
                    sharedResponsibleMap[alloc.userId] = currentSharedResp + alloc.amount
                }
            }
        }

        // Apply settlements: fromUserId paid toUserId, so fromUserId's debt is reduced (+sharedPaid), and toUserId's credit is reduced (+sharedResponsible)
        for (settlement in settlements) {
            val fromPaid = sharedPaidMap.getValue(settlement.fromUserId)
            sharedPaidMap[settlement.fromUserId] = fromPaid + settlement.amount

            val toResp = sharedResponsibleMap.getValue(settlement.toUserId)
            sharedResponsibleMap[settlement.toUserId] = toResp + settlement.amount

            val totalFromPaid = totalPaidMap.getValue(settlement.fromUserId)
            totalPaidMap[settlement.fromUserId] = totalFromPaid + settlement.amount
        }

        val allInvolvedUserIds = (allMemberIds + totalPaidMap.keys + totalResponsibleMap.keys).toSet()

        val balances = allInvolvedUserIds.associateWith { userId ->
            val sPaid = sharedPaidMap.getValue(userId)
            val sResp = sharedResponsibleMap.getValue(userId)
            UserBalance(
                userId = userId,
                totalPaid = totalPaidMap.getValue(userId),
                totalResponsible = totalResponsibleMap.getValue(userId),
                personalSpend = personalSpendMap.getValue(userId),
                sharedPaid = sPaid,
                sharedResponsible = sResp,
                netBalance = sPaid - sResp
            )
        }

        val suggestedTransfers = simplifyDebts(balances.values)

        return TourBalanceSummary(
            tourId = tourId,
            totalCost = totalCost,
            totalSharedCost = totalSharedCost,
            totalPersonalCost = totalPersonalCost,
            balances = balances,
            suggestedTransfers = suggestedTransfers
        )
    }

    /**
     * Greedy min-cash-flow algorithm using two max-heaps:
     * - Creditor max-heap (netBalance > 0)
     * - Debtor max-heap (netBalance < 0, stored as positive magnitude)
     * Ties are broken by userId lexicographically for 100% deterministic results.
     */
    fun simplifyDebts(userBalances: Collection<UserBalance>): List<SuggestedTransfer> {
        val creditorComparator = compareByDescending<BalanceEntry> { it.amount }
            .thenBy { it.userId }
        val debtorComparator = compareByDescending<BalanceEntry> { it.amount }
            .thenBy { it.userId }

        val creditors = PriorityQueue(creditorComparator)
        val debtors = PriorityQueue(debtorComparator)

        for (balance in userBalances) {
            when {
                balance.netBalance > 0L -> creditors.add(BalanceEntry(balance.userId, balance.netBalance))
                balance.netBalance < 0L -> debtors.add(BalanceEntry(balance.userId, -balance.netBalance))
            }
        }

        val transfers = mutableListOf<SuggestedTransfer>()

        while (creditors.isNotEmpty() && debtors.isNotEmpty()) {
            val creditor = creditors.poll()
            val debtor = debtors.poll()

            val settledAmount = minOf(creditor.amount, debtor.amount)
            if (settledAmount > 0L) {
                transfers.add(
                    SuggestedTransfer(
                        fromUserId = debtor.userId,
                        toUserId = creditor.userId,
                        amount = settledAmount
                    )
                )
            }

            val creditorRemaining = creditor.amount - settledAmount
            val debtorRemaining = debtor.amount - settledAmount

            if (creditorRemaining > 0L) {
                creditors.add(BalanceEntry(creditor.userId, creditorRemaining))
            }
            if (debtorRemaining > 0L) {
                debtors.add(BalanceEntry(debtor.userId, debtorRemaining))
            }
        }

        return transfers
    }
}
