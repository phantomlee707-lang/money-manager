package com.rainweaver.moneymgr.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainweaver.moneymgr.data.Category
import com.rainweaver.moneymgr.data.MoneyFmt
import com.rainweaver.moneymgr.data.MoneyRepository
import com.rainweaver.moneymgr.data.Settings
import com.rainweaver.moneymgr.data.Transaction
import com.rainweaver.moneymgr.data.TxType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MoneyViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MoneyRepository(app)
    private val settings = Settings(app)

    val transactions: StateFlow<List<Transaction>> =
        repo.observeTransactions().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> =
        repo.observeCategories().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // money-format settings (persisted)
    var currency by mutableStateOf(settings.currency)
    var decimals by mutableStateOf(settings.decimals)
    var separator by mutableStateOf(settings.separator)
    var dateFormat by mutableStateOf(settings.dateFormat)
    var defaultPeriod by mutableStateOf(settings.defaultPeriod)

    // selected dashboard period (a calendar month)
    var selectedMonth by mutableStateOf(YearMonth.now())

    init { viewModelScope.launch { repo.seedDefaultsIfEmpty() } }

    fun saveSettings() {
        settings.currency = currency; settings.decimals = decimals
        settings.separator = separator; settings.dateFormat = dateFormat
        settings.defaultPeriod = defaultPeriod
    }

    fun money(minor: Long): String = MoneyFmt.format(minor, currency, decimals, separator)

    fun formatDate(epochDay: Long): String {
        val d = LocalDate.ofEpochDay(epochDay)
        val fmt = when (dateFormat) {
            "dmy" -> DateTimeFormatter.ofPattern("dd/MM/yyyy")
            "mdy" -> DateTimeFormatter.ofPattern("MM/dd/yyyy")
            else -> DateTimeFormatter.ofPattern("yyyy-MM-dd")
        }
        return d.format(fmt)
    }

    // ---- period-scoped data ----
    fun txInMonth(ym: YearMonth): List<Transaction> =
        transactions.value.filter { YearMonth.from(LocalDate.ofEpochDay(it.dateEpochDay)) == ym }

    // Period-scoped totals are computed in the composables via remember(transactions,
    // selectedMonth) so they stay memoized yet redraw the instant the DB changes.
    fun shiftMonth(delta: Long) { selectedMonth = selectedMonth.plusMonths(delta) }
    fun setMonth(ym: YearMonth) { selectedMonth = ym }

    /** Last [count] months (oldest→newest) with income/expense totals, for charts. */
    fun monthlySummary(count: Int = 6): List<Triple<YearMonth, Long, Long>> {
        val base = YearMonth.now()
        return (count - 1 downTo 0).map { back ->
            val ym = base.minusMonths(back.toLong())
            val txs = txInMonth(ym)
            val inc = txs.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor }
            val exp = txs.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }
            Triple(ym, inc, exp)
        }
    }

    /** Expense totals grouped by category for a month (desc). */
    fun expenseByCategory(ym: YearMonth): List<Pair<String, Long>> =
        txInMonth(ym).filter { it.type == TxType.EXPENSE }
            .groupBy { it.categoryName }
            .map { it.key to it.value.sumOf { t -> t.amountMinor } }
            .sortedByDescending { it.second }

    // ---- transactions ----
    fun saveTransaction(
        id: Long?, type: TxType, amountText: String,
        categoryId: Long, categoryName: String, note: String, date: LocalDate
    ) {
        val minor = MoneyFmt.parseToMinor(amountText)
        if (minor <= 0) return
        viewModelScope.launch {
            val existing = id?.let { e -> transactions.value.firstOrNull { it.id == e } }
            if (existing != null) {
                repo.updateTx(existing.copy(
                    type = type, amountMinor = minor, categoryId = categoryId,
                    categoryName = categoryName, note = note, dateEpochDay = date.toEpochDay(),
                    updatedAt = System.currentTimeMillis()
                ))
            } else {
                repo.addTx(Transaction(
                    type = type, amountMinor = minor, categoryId = categoryId,
                    categoryName = categoryName, note = note, dateEpochDay = date.toEpochDay()
                ))
            }
        }
    }

    fun delete(tx: Transaction) { viewModelScope.launch { repo.deleteTx(tx) } }
    suspend fun allForReport(): List<Transaction> = repo.allTx()
    suspend fun txBetween(from: LocalDate, to: LocalDate): List<Transaction> =
        repo.txInRange(from.toEpochDay(), to.toEpochDay())

    /** Wipes all transactions (data management). Categories are kept. */
    fun deleteAllTransactions() {
        viewModelScope.launch { transactions.value.forEach { repo.deleteTx(it) } }
    }

    // ---- categories ----
    fun addCategory(name: String, type: TxType) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.addCategory(Category(name = name.trim(), type = type)) }
    }

    fun deleteCategory(cat: Category, onBlocked: (Int) -> Unit) {
        viewModelScope.launch {
            val used = repo.categoryUsage(cat.id)
            if (used > 0) onBlocked(used) else repo.deleteCategory(cat)
        }
    }

    fun categoriesFor(type: TxType): List<Category> = categories.value.filter { it.type == type }
}
