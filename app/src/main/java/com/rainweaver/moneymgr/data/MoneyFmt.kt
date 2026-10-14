package com.rainweaver.moneymgr.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.roundToLong

/**
 * Money is stored as integer minor units (cents). This formats a cents value
 * for display with a currency label, decimal count and thousands separator.
 */
object MoneyFmt {

    /** Parses user text (major units, e.g. "12.50") into integer cents. */
    fun parseToMinor(text: String): Long {
        val v = text.trim().replace(",", "").toDoubleOrNull() ?: return 0L
        return (v * 100).roundToLong()
    }

    fun format(minor: Long, currency: String, decimals: Int, separator: String): String {
        val symbols = DecimalFormatSymbols.getInstance(Locale.US)
        symbols.groupingSeparator = when (separator) {
            "dot" -> '.'
            "space" -> ' '
            else -> ','
        }
        symbols.decimalSeparator = if (separator == "dot") ',' else '.'

        val dec = decimals.coerceIn(0, 6)
        val pattern = if (dec == 0) "#,##0" else "#,##0." + "0".repeat(dec)
        val df = DecimalFormat(pattern, symbols)
        if (separator == "none") df.isGroupingUsed = false

        val value = minor / 100.0 // display only; storage & sums stay integer
        val sym = if (currency.isBlank()) "" else currency.trim() + " "
        return sym + df.format(value)
    }
}
