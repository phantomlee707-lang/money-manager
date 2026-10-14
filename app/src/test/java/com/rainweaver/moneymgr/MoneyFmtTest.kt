package com.rainweaver.moneymgr

import com.rainweaver.moneymgr.data.MoneyFmt
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for money parsing/formatting. Money is stored as integer cents,
 * so these verify exactness (no floating-point drift) and display formatting.
 */
class MoneyFmtTest {

    @Test
    fun parseToMinor_basic() {
        assertEquals(1250L, MoneyFmt.parseToMinor("12.50"))
        assertEquals(10000L, MoneyFmt.parseToMinor("100"))
        assertEquals(10L, MoneyFmt.parseToMinor("0.10"))
        assertEquals(1L, MoneyFmt.parseToMinor("0.01"))
    }

    @Test
    fun parseToMinor_stripsThousandsComma() {
        assertEquals(123456L, MoneyFmt.parseToMinor("1,234.56"))
    }

    @Test
    fun parseToMinor_invalidIsZero() {
        assertEquals(0L, MoneyFmt.parseToMinor("abc"))
        assertEquals(0L, MoneyFmt.parseToMinor(""))
        assertEquals(0L, MoneyFmt.parseToMinor("   "))
    }

    @Test
    fun format_commaTwoDecimals() {
        assertEquals("$ 1,234,567.89", MoneyFmt.format(123456789L, "$", 2, "comma"))
    }

    @Test
    fun format_zeroDecimalsRounds() {
        assertEquals("$ 1,234,568", MoneyFmt.format(123456789L, "$", 0, "comma"))
    }

    @Test
    fun format_dotSeparator() {
        assertEquals("€ 1.234.567,89", MoneyFmt.format(123456789L, "€", 2, "dot"))
    }

    @Test
    fun format_spaceSeparator() {
        assertEquals("Rp 1 234 567.89", MoneyFmt.format(123456789L, "Rp", 2, "space"))
    }

    @Test
    fun format_noSeparator() {
        assertEquals("$ 1234567.89", MoneyFmt.format(123456789L, "$", 2, "none"))
    }

    @Test
    fun format_blankCurrency() {
        assertEquals("1,234,567.89", MoneyFmt.format(123456789L, "", 2, "comma"))
    }

    @Test
    fun integerSumsAvoidFloatDrift() {
        // 0.10 added ten times must be exactly 100 cents ($1.00),
        // which binary floating point (0.1 * 10) would get wrong.
        var total = 0L
        repeat(10) { total += MoneyFmt.parseToMinor("0.10") }
        assertEquals(100L, total)
        assertEquals("$ 1.00", MoneyFmt.format(total, "$", 2, "comma"))
    }

    @Test
    fun balanceIsExact() {
        val income = MoneyFmt.parseToMinor("1000.00") + MoneyFmt.parseToMinor("0.55")
        val expense = MoneyFmt.parseToMinor("300.10")
        assertEquals(70045L, income - expense)
        assertEquals("$ 700.45", MoneyFmt.format(income - expense, "$", 2, "comma"))
    }
}
