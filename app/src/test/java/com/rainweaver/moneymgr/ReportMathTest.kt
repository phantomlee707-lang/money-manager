package com.rainweaver.moneymgr

import com.rainweaver.moneymgr.data.Transaction
import com.rainweaver.moneymgr.data.TxClass
import com.rainweaver.moneymgr.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies credit/debit classification and financial totals. Transfers must be
 * excluded from both credits and debits (never counted as income or expense).
 */
class ReportMathTest {

    private fun tx(type: TxType, minor: Long, note: String = "") =
        Transaction(
            type = type, amountMinor = minor, categoryId = 1,
            categoryName = "General", dateEpochDay = 0, note = note,
        )

    @Test fun incomeIsCredit_expenseIsDebit() {
        assertTrue(TxClass.isCredit(tx(TxType.INCOME, 100)))
        assertFalse(TxClass.isDebit(tx(TxType.INCOME, 100)))
        assertTrue(TxClass.isDebit(tx(TxType.EXPENSE, 100)))
        assertFalse(TxClass.isCredit(tx(TxType.EXPENSE, 100)))
    }

    @Test fun transferIsNeitherCreditNorDebit() {
        val t = tx(TxType.TRANSFER, 5000)
        assertFalse(TxClass.isCredit(t))
        assertFalse(TxClass.isDebit(t))
        assertTrue(TxClass.isExcluded(t))
    }

    @Test fun totalsExcludeTransfers() {
        val list = listOf(
            tx(TxType.INCOME, 100000),    // 1000.00
            tx(TxType.INCOME, 5050),      // 50.50
            tx(TxType.EXPENSE, 30010),    // 300.10
            tx(TxType.TRANSFER, 999999),  // must NOT count toward credits or debits
        )
        assertEquals(105050L, TxClass.creditMinor(list))
        assertEquals(30010L, TxClass.debitMinor(list))
        assertEquals(75040L, TxClass.closingBalance(list))
    }

    @Test fun closingWithOpeningBalance() {
        val list = listOf(tx(TxType.INCOME, 20000), tx(TxType.EXPENSE, 5000))
        // opening + credits - debits
        assertEquals(100000L + 20000L - 5000L, TxClass.closingBalance(list, 100000L))
    }

    @Test fun emptyIsZero() {
        assertEquals(0L, TxClass.creditMinor(emptyList()))
        assertEquals(0L, TxClass.debitMinor(emptyList()))
        assertEquals(0L, TxClass.closingBalance(emptyList()))
    }
}
