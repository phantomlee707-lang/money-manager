package com.rainweaver.moneymgr.data

/**
 * Credit/Debit classification for reporting.
 *
 * Only INCOME is a credit and only EXPENSE is a debit. TRANSFER (money moved
 * between the user's own accounts) is neither — it must never be counted as
 * income or expense, so it is excluded from both credit and debit totals.
 */
object TxClass {

    fun isCredit(tx: Transaction): Boolean = tx.type == TxType.INCOME

    fun isDebit(tx: Transaction): Boolean = tx.type == TxType.EXPENSE

    /** True when the transaction belongs in neither column (e.g. a transfer). */
    fun isExcluded(tx: Transaction): Boolean = !isCredit(tx) && !isDebit(tx)

    fun creditMinor(transactions: List<Transaction>): Long =
        transactions.filter { isCredit(it) }.sumOf { it.amountMinor }

    fun debitMinor(transactions: List<Transaction>): Long =
        transactions.filter { isDebit(it) }.sumOf { it.amountMinor }

    /** Closing balance with no opening balance: credits − debits. */
    fun closingBalance(transactions: List<Transaction>): Long =
        creditMinor(transactions) - debitMinor(transactions)

    /** Closing balance when an opening balance (minor units) is supplied. */
    fun closingBalance(transactions: List<Transaction>, openingMinor: Long): Long =
        openingMinor + creditMinor(transactions) - debitMinor(transactions)
}
