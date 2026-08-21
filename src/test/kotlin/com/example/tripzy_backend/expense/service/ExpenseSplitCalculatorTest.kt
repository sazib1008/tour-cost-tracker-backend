package com.example.tripzy_backend.expense.service

import com.example.tripzy_backend.common.exception.InvalidExpenseException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ExpenseSplitCalculatorTest {

    private lateinit var calculator: ExpenseSplitCalculator

    @BeforeEach
    fun setUp() {
        calculator = ExpenseSplitCalculator()
    }

    @Test
    fun `equal split should divide cleanly when divisible`() {
        val total = 30000L // 300 BDT
        val members = listOf("u1", "u2", "u3")
        val allocations = calculator.calculateEqualSplit(total, members)

        assertEquals(3, allocations.size)
        assertEquals(10000L, allocations[0].amount)
        assertEquals(10000L, allocations[1].amount)
        assertEquals(10000L, allocations[2].amount)
        assertEquals(total, allocations.sumOf { it.amount })
    }

    @Test
    fun `equal split should distribute remainder paisa to first members without loss`() {
        val total = 10000L // 100 BDT split 3 ways: 33.33, 33.33, 33.34
        val members = listOf("u1", "u2", "u3")
        val allocations = calculator.calculateEqualSplit(total, members)

        assertEquals(3, allocations.size)
        assertEquals(3334L, allocations[0].amount) // remainder of 1 paisa given to first member
        assertEquals(3333L, allocations[1].amount)
        assertEquals(3333L, allocations[2].amount)
        assertEquals(total, allocations.sumOf { it.amount })
    }

    @Test
    fun `exact split should succeed when sum matches total`() {
        val total = 5000L
        val exactMap = mapOf("u1" to 2000L, "u2" to 3000L)
        val allocations = calculator.calculateExactSplit(total, exactMap)

        assertEquals(2, allocations.size)
        assertEquals(total, allocations.sumOf { it.amount })
    }

    @Test
    fun `exact split should throw InvalidExpenseException when sum mismatches`() {
        val total = 5000L
        val exactMap = mapOf("u1" to 2000L, "u2" to 2000L) // sum is 4000L

        assertThrows(InvalidExpenseException::class.java) {
            calculator.calculateExactSplit(total, exactMap)
        }
    }

    @Test
    fun `percentage split should distribute rounding remainder to ensure exact sum`() {
        val total = 10000L // 100 BDT: 33.33%, 33.33%, 33.34%
        val percentages = mapOf("u1" to 33.33, "u2" to 33.33, "u3" to 33.34)
        val allocations = calculator.calculatePercentageSplit(total, percentages)

        assertEquals(total, allocations.sumOf { it.amount })
    }

    @Test
    fun `shares split should distribute amounts proportionally and balance exact sum`() {
        val total = 10000L // 100 BDT: shares 1, 2, 2 (total 5 shares)
        val shares = mapOf("u1" to 1, "u2" to 2, "u3" to 2)
        val allocations = calculator.calculateSharesSplit(total, shares)

        assertEquals(3, allocations.size)
        assertEquals(2000L, allocations.first { it.userId == "u1" }.amount)
        assertEquals(4000L, allocations.first { it.userId == "u2" }.amount)
        assertEquals(4000L, allocations.first { it.userId == "u3" }.amount)
        assertEquals(total, allocations.sumOf { it.amount })
    }
}
