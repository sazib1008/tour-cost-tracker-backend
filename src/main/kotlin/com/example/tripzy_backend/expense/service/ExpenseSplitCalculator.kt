package com.example.tripzy_backend.expense.service

import com.example.tripzy_backend.common.exception.InvalidExpenseException
import com.example.tripzy_backend.expense.model.Allocation
import org.springframework.stereotype.Component

@Component
class ExpenseSplitCalculator {

    fun calculateEqualSplit(totalAmount: Long, participantUserIds: List<String>): List<Allocation> {
        if (participantUserIds.isEmpty()) {
            throw InvalidExpenseException("At least one participant is required for an equal split")
        }
        val count = participantUserIds.size
        val baseAmount = totalAmount / count
        val remainder = (totalAmount % count).toInt()

        return participantUserIds.mapIndexed { index, userId ->
            val amount = if (index < remainder) baseAmount + 1L else baseAmount
            Allocation(userId = userId, amount = amount)
        }
    }

    fun calculateExactSplit(totalAmount: Long, exactAllocations: Map<String, Long>): List<Allocation> {
        if (exactAllocations.isEmpty()) {
            throw InvalidExpenseException("Exact allocations map cannot be empty")
        }
        val sum = exactAllocations.values.sum()
        if (sum != totalAmount) {
            throw InvalidExpenseException("Sum of exact allocations ($sum paisa) does not match total amount ($totalAmount paisa)")
        }
        return exactAllocations.map { (userId, amount) ->
            Allocation(userId = userId, amount = amount)
        }
    }

    fun calculatePercentageSplit(totalAmount: Long, percentages: Map<String, Double>): List<Allocation> {
        if (percentages.isEmpty()) {
            throw InvalidExpenseException("Percentages map cannot be empty")
        }
        val totalPercentage = percentages.values.sum()
        if (kotlin.math.abs(totalPercentage - 100.0) > 0.01) {
            throw InvalidExpenseException("Sum of percentages must equal 100.0%, current sum: $totalPercentage%")
        }

        val sortedEntries = percentages.entries.sortedByDescending { it.value }
        val rawAllocations = sortedEntries.map { (userId, pct) ->
            userId to (totalAmount * (pct / 100.0)).toLong()
        }.toMap().toMutableMap()

        val computedSum = rawAllocations.values.sum()
        var diff = totalAmount - computedSum

        val iterator = sortedEntries.iterator()
        while (diff > 0 && iterator.hasNext()) {
            val key = iterator.next().key
            rawAllocations[key] = (rawAllocations[key] ?: 0L) + 1L
            diff--
        }

        return rawAllocations.map { (userId, amount) ->
            Allocation(userId = userId, amount = amount)
        }
    }

    fun calculateSharesSplit(totalAmount: Long, shares: Map<String, Int>): List<Allocation> {
        if (shares.isEmpty()) {
            throw InvalidExpenseException("Shares map cannot be empty")
        }
        val totalShares = shares.values.sum()
        if (totalShares <= 0) {
            throw InvalidExpenseException("Total shares must be greater than zero")
        }

        val sortedEntries = shares.entries.sortedByDescending { it.value }
        val rawAllocations = sortedEntries.map { (userId, share) ->
            userId to (totalAmount * share) / totalShares
        }.toMap().toMutableMap()

        val computedSum = rawAllocations.values.sum()
        var diff = totalAmount - computedSum

        val iterator = sortedEntries.iterator()
        while (diff > 0 && iterator.hasNext()) {
            val key = iterator.next().key
            rawAllocations[key] = (rawAllocations[key] ?: 0L) + 1L
            diff--
        }

        return rawAllocations.map { (userId, amount) ->
            Allocation(userId = userId, amount = amount)
        }
    }
}
