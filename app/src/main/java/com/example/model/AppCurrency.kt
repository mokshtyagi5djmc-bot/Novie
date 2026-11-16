package com.example.model

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

enum class AppCurrency(
    val code: String,
    val symbol: String,
    val displayName: String,
    val usdRate: Double,
    val fractionDigits: Int = 0
) {
    USD("USD", "$", "US Dollar", 1.0, 2),
    EUR("EUR", "€", "Euro", 0.92, 2),
    GBP("GBP", "£", "British Pound", 0.79, 2),
    INR("INR", "₹", "Indian Rupee", 84.2, 0),
    AED("AED", "د.إ", "UAE Dirham", 3.67, 2),
    SAR("SAR", "﷼", "Saudi Riyal", 3.75, 2),
    JPY("JPY", "¥", "Japanese Yen", 154.0, 0),
    CAD("CAD", "C$", "Canadian Dollar", 1.39, 2),
    AUD("AUD", "A$", "Australian Dollar", 1.53, 2),
    CHF("CHF", "CHF", "Swiss Franc", 0.88, 2);

    companion object {
        fun fromCode(code: String?): AppCurrency {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: USD
        }
    }
}

object CurrencyFormatter {

    fun convertAmount(
        amount: Double,
        fromCode: String,
        toCurrency: AppCurrency,
        convertUsingRates: Boolean
    ): Double {
        if (!convertUsingRates) return amount
        val source = AppCurrency.fromCode(fromCode)
        if (source == toCurrency) return amount
        val amountInUsd = amount / source.usdRate
        return amountInUsd * toCurrency.usdRate
    }

    fun formatFull(
        amount: Double,
        currency: AppCurrency,
        locale: Locale = Locale.US,
        includeDecimals: Boolean = false
    ): String {
        val formatter = NumberFormat.getNumberInstance(locale).apply {
            maximumFractionDigits = if (includeDecimals) currency.fractionDigits else 0
            minimumFractionDigits = 0
        }
        val sign = if (amount < 0) "-" else ""
        val formatted = formatter.format(abs(amount))
        return if (currency == AppCurrency.CHF) {
            "${sign}CHF $formatted"
        } else {
            "$sign${currency.symbol}$formatted"
        }
    }

    fun formatCompact(
        amount: Double,
        currency: AppCurrency,
        locale: Locale = Locale.US
    ): String {
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        val prefix = if (currency == AppCurrency.CHF) "CHF " else currency.symbol
        val (scaled, suffix) = when {
            currency == AppCurrency.INR && absVal >= 10_000_000.0 -> (absVal / 10_000_000.0) to "Cr"
            currency == AppCurrency.INR && absVal >= 100_000.0 -> (absVal / 100_000.0) to "L"
            absVal >= 1_000_000_000.0 -> (absVal / 1_000_000_000.0) to "B"
            absVal >= 1_000_000.0 -> (absVal / 1_000_000.0) to "M"
            absVal >= 1_000.0 -> (absVal / 1_000.0) to "K"
            else -> absVal to ""
        }
        val numText = if (suffix.isEmpty()) {
            String.format(locale, "%.0f", scaled)
        } else if (scaled >= 100.0) {
            String.format(locale, "%.0f", scaled)
        } else {
            String.format(locale, "%.1f", scaled)
        }
        return "$sign$prefix$numText$suffix"
    }
}
