package com.example.tripzy_backend.common.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoneyTest {

    @Test
    fun `should create money correctly from paisa and major units`() {
        val moneyFromPaisa = Money.ofPaisa(150050L) // 1500 BDT 50 Paisa
        assertEquals(150050L, moneyFromPaisa.amount)
        assertEquals("BDT", moneyFromPaisa.currency)

        val moneyFromMajor = Money.ofMajor(500L) // 500 BDT = 50000 Paisa
        assertEquals(50000L, moneyFromMajor.amount)

        val moneyFromMajorAndPaisa = Money.of(100L, 75L) // 100 BDT 75 Paisa
        assertEquals(10075L, moneyFromMajorAndPaisa.amount)
    }

    @Test
    fun `should add money of same currency correctly`() {
        val m1 = Money.of(100L, 50L) // 100.50
        val m2 = Money.of(50L, 75L)  // 50.75
        val result = m1 + m2

        assertEquals(15125L, result.amount)
        assertEquals("BDT", result.currency)
    }

    @Test
    fun `should throw exception when adding different currencies`() {
        val m1 = Money.ofPaisa(1000L, "BDT")
        val m2 = Money.ofPaisa(1000L, "USD")

        assertThrows(IllegalArgumentException::class.java) {
            m1 + m2
        }
    }

    @Test
    fun `should subtract money of same currency correctly`() {
        val m1 = Money.of(100L)
        val m2 = Money.of(40L)
        val result = m1 - m2

        assertEquals(6000L, result.amount)
    }

    @Test
    fun `should multiply money by integer factor`() {
        val m = Money.of(25L, 50L) // 2550
        val result = m * 3

        assertEquals(7650L, result.amount)
    }

    @Test
    fun `should handle unary minus`() {
        val m = Money.of(50L)
        val neg = -m

        assertEquals(-5000L, neg.amount)
        assertTrue(neg.isNegative())
        assertFalse(neg.isPositive())
    }

    @Test
    fun `should compare money correctly`() {
        val m1 = Money.of(100L)
        val m2 = Money.of(200L)
        val m3 = Money.of(100L)

        assertTrue(m1 < m2)
        assertTrue(m2 > m1)
        assertEquals(0, m1.compareTo(m3))
    }

    @Test
    fun `should evaluate zero, positive and negative state`() {
        val zero = Money.ZERO
        val pos = Money.ofPaisa(100L)
        val neg = Money.ofPaisa(-100L)

        assertTrue(zero.isZero())
        assertFalse(zero.isPositive())
        assertFalse(zero.isNegative())

        assertTrue(pos.isPositive())
        assertFalse(pos.isZero())

        assertTrue(neg.isNegative())
        assertFalse(neg.isPositive())
    }

    @Test
    fun `should format display string with BDT symbol and thousands separator`() {
        val m1 = Money.of(1500L, 0L)
        assertEquals("৳1,500", m1.formatted())
        assertEquals("৳1,500.00", m1.formatted(showDecimalsAlways = true))

        val m2 = Money.of(1500L, 50L)
        assertEquals("৳1,500.50", m2.formatted())

        val neg = Money.ofPaisa(-250000L) // -2500 BDT
        assertEquals("-৳2,500", neg.formatted())
    }
}
