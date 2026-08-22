package com.example.tripzy_backend.balance.service

import com.example.tripzy_backend.balance.model.SuggestedTransfer
import com.example.tripzy_backend.balance.model.TourBalanceSummary
import com.example.tripzy_backend.balance.model.UserBalance
import com.example.tripzy_backend.expense.model.Allocation
import com.example.tripzy_backend.expense.model.Expense
import com.example.tripzy_backend.expense.model.Payment
import com.example.tripzy_backend.settlement.model.Settlement
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
     *
     * Net Balance Formula:
     *   netBalance = totalPaid - (sharedShare + personalShare)
     *
     * Classification:
     *   netBalance > 0: Member receives money (Creditor)
     *   netBalance < 0: Member owes money (Debtor)
     */
    fun calculate(
        tourId: String,
        allMemberIds: Set<String>,
        expenses: List<Expense>,
        settlements: List<Settlement> = emptyList()
    ): TourBalanceSummary {
        var totalCost = 0L
        var totalSharedCost = 0L
        var totalPersonalCost = 0L

        val totalPaidMap = mutableMapOf<String, Long>().withDefault { 0L }
        val personalSpendMap = mutableMapOf<String, Long>().withDefault { 0L }
        val sharedPaidMap = mutableMapOf<String, Long>().withDefault { 0L }
        val sharedResponsibleMap = mutableMapOf<String, Long>().withDefault { 0L }

        // Initialize all active members with 0
        allMemberIds.forEach { uid ->
            totalPaidMap[uid] = 0L
            personalSpendMap[uid] = 0L
            sharedPaidMap[uid] = 0L
            sharedResponsibleMap[uid] = 0L
        }

        for (expense in expenses) {
            totalCost += expense.amount

            // Resolve payments: if payments list is empty or unpopulated, fallback to createdBy
            val payments = if (expense.payments.isNotEmpty()) {
                expense.payments
            } else {
                listOf(Payment(userId = expense.createdBy, amount = expense.amount))
            }

            if (expense.isPersonal) {
                totalPersonalCost += expense.amount

                payments.forEach { payment ->
                    val currentPaid = totalPaidMap.getValue(payment.userId)
                    totalPaidMap[payment.userId] = currentPaid + payment.amount
                }

                // Resolve personal allocations: member who incurred this personal expense
                val allocations = if (expense.allocations.isNotEmpty()) {
                    expense.allocations
                } else {
                    listOf(Allocation(userId = expense.createdBy, amount = expense.amount))
                }

                allocations.forEach { alloc ->
                    val currentPers = personalSpendMap.getValue(alloc.userId)
                    personalSpendMap[alloc.userId] = currentPers + alloc.amount
                }
            } else {
                totalSharedCost += expense.amount

                payments.forEach { payment ->
                    val currentPaid = totalPaidMap.getValue(payment.userId)
                    totalPaidMap[payment.userId] = currentPaid + payment.amount
                    val currentSharedPaid = sharedPaidMap.getValue(payment.userId)
                    sharedPaidMap[payment.userId] = currentSharedPaid + payment.amount
                }
            }
        }

        // Shared Share Calculation:
        // Distribute totalSharedCost equally among all active members
        if (allMemberIds.isNotEmpty()) {
            val memberCount = allMemberIds.size
            val baseShare = totalSharedCost / memberCount
            val remainder = (totalSharedCost % memberCount).toInt()

            allMemberIds.sorted().forEachIndexed { index, uid ->
                val share = if (index < remainder) baseShare + 1L else baseShare
                sharedResponsibleMap[uid] = share
            }
        }

        // Apply settlements:
        // fromUserId (Debtor) paid toUserId (Creditor)
        // fromUserId's totalPaid and sharedPaid increase by settlement amount (+amount)
        // toUserId's responsible/received increases by settlement amount (+amount)
        for (settlement in settlements) {
            val fromPaid = sharedPaidMap.getValue(settlement.fromUserId)
            sharedPaidMap[settlement.fromUserId] = fromPaid + settlement.amount

            val toResp = sharedResponsibleMap.getValue(settlement.toUserId)
            sharedResponsibleMap[settlement.toUserId] = toResp + settlement.amount

            val totalFromPaid = totalPaidMap.getValue(settlement.fromUserId)
            totalPaidMap[settlement.fromUserId] = totalFromPaid + settlement.amount
        }

        val allInvolvedUserIds = (allMemberIds + totalPaidMap.keys + personalSpendMap.keys + sharedPaidMap.keys + sharedResponsibleMap.keys).toSet()

        val balances = allInvolvedUserIds.associateWith { userId ->
            val paid = totalPaidMap.getValue(userId)
            val sPaid = sharedPaidMap.getValue(userId)
            val sShare = sharedResponsibleMap.getValue(userId)
            val pShare = personalSpendMap.getValue(userId)
            val totalResp = sShare + pShare
            val net = paid - totalResp

            UserBalance(
                userId = userId,
                totalPaid = paid,
                totalResponsible = totalResp,
                personalSpend = pShare,
                sharedPaid = sPaid,
                sharedResponsible = sShare,
                netBalance = net
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
     * - Creditor max-heap (netBalance > 0, member receives money)
     * - Debtor max-heap (netBalance < 0, member owes money, stored as positive magnitude)
     * Direction: Debtor (fromUserId) pays Creditor (toUserId).
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
