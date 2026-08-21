package com.example.tripzy_backend.common.domain

import java.text.NumberFormat
import java.util.Locale

/**
 * Monetary value representation storing amounts in the smallest currency unit (e.g. Paisa / Poisha for BDT).
 * 1 BDT = 100 Paisa.
 *
 * Floating-point types (Double/Float) are strictly prohibited for monetary calculations.
 */
data class Money(
    val amount: Long = 0L,
    val currency: String = DEFAULT_CURRENCY
) : Comparable<Money> {

    init {
        require(currency.isNotBlank()) { "Currency code must not be blank" }
    }

    operator fun plus(other: Money): Money {
        requireSameCurrency(other)
        return Money(this.amount + other.amount, currency)
    }

    operator fun minus(other: Money): Money {
        requireSameCurrency(other)
        return Money(this.amount - other.amount, currency)
    }

    operator fun times(multiplier: Long): Money {
        return Money(this.amount * multiplier, currency)
    }

    operator fun unaryMinus(): Money {
        return Money(-this.amount, currency)
    }

    override fun compareTo(other: Money): Int {
        requireSameCurrency(other)
        return this.amount.compareTo(other.amount)
    }

    fun isPositive(): Boolean = amount > 0L
    fun isNegative(): Boolean = amount < 0L
    fun isZero(): Boolean = amount == 0L

    private fun requireSameCurrency(other: Money) {
        require(this.currency.equals(other.currency, ignoreCase = true)) {
            "Cannot perform monetary operations between different currencies: ${this.currency} and ${other.currency}"
        }
    }

    /**
     * Formats the money into display format with currency symbol (e.g. ৳1,500.00 or ৳1,500).
     */
    fun formatted(showDecimalsAlways: Boolean = false): String {
        val symbol = when (currency.uppercase(Locale.ROOT)) {
            "BDT" -> "৳"
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "INR" -> "₹"
            else -> "$currency "
        }

        val isNeg = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val major = absAmount / 100
        val minor = absAmount % 100

        val formattedMajor = NumberFormat.getNumberInstance(Locale.US).format(major)
        val sign = if (isNeg) "-" else ""

        return if (minor != 0L || showDecimalsAlways) {
            "$sign$symbol$formattedMajor.${minor.toString().padStart(2, '0')}"
        } else {
            "$sign$symbol$formattedMajor"
        }
    }

    companion object {
        const val DEFAULT_CURRENCY = "BDT"

        val ZERO: Money = Money(0L, DEFAULT_CURRENCY)

        fun ofPaisa(paisa: Long, currency: String = DEFAULT_CURRENCY): Money =
            Money(paisa, currency)

        fun ofMajor(major: Long, currency: String = DEFAULT_CURRENCY): Money =
            Money(major * 100L, currency)

        fun of(major: Long, paisa: Long = 0L, currency: String = DEFAULT_CURRENCY): Money =
            Money(major * 100L + paisa, currency)

        fun zero(currency: String = DEFAULT_CURRENCY): Money =
            Money(0L, currency)
    }
}
