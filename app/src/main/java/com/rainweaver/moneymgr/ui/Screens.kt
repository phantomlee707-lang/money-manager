package com.rainweaver.moneymgr.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rainweaver.moneymgr.data.Settings
import com.rainweaver.moneymgr.data.Transaction
import com.rainweaver.moneymgr.data.TxType
import kotlinx.coroutines.launch

// ---------------- DASHBOARD ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(vm: MoneyViewModel, onEdit: (Transaction?) -> Unit, onRequestExport: () -> Unit, onDelete: (Transaction) -> Unit) {
    val transactions by vm.transactions.collectAsState()
    val money: (Long) -> String = { vm.money(it) }
    val dateStr: (Long) -> String = { vm.formatDate(it) }
    val periodTx = remember(transactions, vm.selectedMonth) { vm.txInMonth(vm.selectedMonth) }
    val inc = remember(periodTx) { periodTx.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor } }
    val exp = remember(periodTx) { periodTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor } }
    val bal = inc - exp
    val monthly = remember(transactions) { vm.monthlySummary(6) }
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Column {
            Text("Money Manager")
            Text("Rainweaver Studio", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
        } }, actions = { TextButton(onClick = onRequestExport) { Text("Export as PDF") } })
        Column(Modifier.padding(16.dp)) {
            MonthSelector(vm.selectedMonth, { vm.shiftMonth(-1) }, { vm.shiftMonth(1) })
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Income", money(inc), Modifier.weight(1f))
                SummaryCard("Expense", money(exp), Modifier.weight(1f))
                SummaryCard("Balance", money(bal), Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            Text("Last 6 months", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            MonthlyBarChart(monthly, money)
            Spacer(Modifier.height(16.dp))
            Text("Recent transactions", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(periodTx) { tx -> TxRow(tx, money, dateStr, { onEdit(tx) }, { onDelete(tx) }) }
            }
        }
    }
}

// ---------------- HISTORY ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: MoneyViewModel, onEdit: (Transaction?) -> Unit, onDelete: (Transaction) -> Unit) {
    val all by vm.transactions.collectAsState()
    val categories by vm.categories.collectAsState()
    var query by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf<TxType?>(null) }
    var catFilter by remember { mutableStateOf<String?>(null) }
    val money: (Long) -> String = { vm.money(it) }
    val dateStr: (Long) -> String = { vm.formatDate(it) }

    val filtered = remember(all, query, typeFilter, catFilter) {
        all.filter { tx ->
            (typeFilter == null || tx.type == typeFilter) &&
            (catFilter == null || tx.categoryName == catFilter) &&
            (query.isBlank() || tx.note.contains(query, true) || tx.categoryName.contains(query, true))
        }.sortedByDescending { it.dateEpochDay }
    }
    val catNames = remember(categories) { categories.map { it.name }.distinct() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("History", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(query, { query = it }, label = { Text("Search notes / categories") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = typeFilter == null, onClick = { typeFilter = null }, label = { Text("All") })
            FilterChip(selected = typeFilter == TxType.INCOME, onClick = { typeFilter = TxType.INCOME }, label = { Text("Income") })
            FilterChip(selected = typeFilter == TxType.EXPENSE, onClick = { typeFilter = TxType.EXPENSE }, label = { Text("Expense") })
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Category:", fontSize = 13.sp)
            var expanded by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { expanded = true }) { Text(catFilter ?: "All") }
                DropdownMenu(expanded, { expanded = false }) {
                    DropdownMenuItem(text = { Text("All") }, onClick = { catFilter = null; expanded = false })
                    catNames.forEach { n -> DropdownMenuItem(text = { Text(n) }, onClick = { catFilter = n; expanded = false }) }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(Modifier.fillMaxSize()) {
            items(filtered) { tx -> TxRow(tx, money, dateStr, { onEdit(tx) }, { onDelete(tx) }) }
        }
    }
}

// ---------------- SUMMARY ----------------
@Composable
fun SummaryScreen(vm: MoneyViewModel, onRequestExport: () -> Unit) {
    val transactions by vm.transactions.collectAsState()
    val money: (Long) -> String = { vm.money(it) }
    val periodTx = remember(transactions, vm.selectedMonth) { vm.txInMonth(vm.selectedMonth) }
    val inc = remember(periodTx) { periodTx.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor } }
    val exp = remember(periodTx) { periodTx.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor } }
    val bal = inc - exp
    val monthly = remember(transactions) { vm.monthlySummary(6) }
    val byCat = remember(transactions, vm.selectedMonth) { vm.expenseByCategory(vm.selectedMonth) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Summary", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        MonthSelector(vm.selectedMonth, { vm.shiftMonth(-1) }, { vm.shiftMonth(1) })
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Income", money(inc), Modifier.weight(1f))
            SummaryCard("Expense", money(exp), Modifier.weight(1f))
            SummaryCard("Net", money(bal), Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("Income vs expense (6 months)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        MonthlyBarChart(monthly, money)
        Spacer(Modifier.height(20.dp))
        Text("Expense by category", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Spacer(Modifier.height(8.dp))
        CategoryBars(byCat, money)
        Spacer(Modifier.height(20.dp))
        Button(onClick = onRequestExport) { Text("Export as PDF") }
    }
}

// ---------------- SETTINGS ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(vm: MoneyViewModel, scope: kotlinx.coroutines.CoroutineScope, onRequestExport: () -> Unit, onSnack: (String) -> Unit) {
    val categories by vm.categories.collectAsState()
    var showCats by remember { mutableStateOf(false) }
    var confirmWipe by remember { mutableStateOf(false) }
    val money: (Long) -> String = { vm.money(it) }

    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(16.dp))
        Text("Currency label", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(vm.currency, { vm.currency = it }, label = { Text("Symbol") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Settings.CURRENCIES.forEach { c -> AssistChip(onClick = { vm.currency = c }, label = { Text(c) }) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Decimals", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(0, 1, 2).forEach { d -> FilterChip(selected = vm.decimals == d, onClick = { vm.decimals = d }, label = { Text("$d") }) }
        }
        Spacer(Modifier.height(16.dp))
        Text("Number separator", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Settings.SEPARATORS.forEach { (key, example) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = vm.separator == key, onClick = { vm.separator = key })
                Text("$key  ($example)", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Date format", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Settings.DATE_FORMATS.forEach { (key, example) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = vm.dateFormat == key, onClick = { vm.dateFormat = key })
                Text("$key  ($example)", fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Default period", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("month" to "Current month", "all" to "All time").forEach { (k, label) ->
                FilterChip(selected = vm.defaultPeriod == k, onClick = { vm.defaultPeriod = k }, label = { Text(label) })
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Preview: ${money(123456789L)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { vm.saveSettings(); onRequestExport() }) { Text("Export as PDF") }
            OutlinedButton(onClick = { showCats = true }) { Text("Categories") }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = { confirmWipe = true }, colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Default.Delete, null); Spacer(Modifier.width(6.dp)); Text("Delete all transactions")
        }
        Spacer(Modifier.height(20.dp))
        Text("Money Manager by Rainweaver Studio", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Fully offline · data stored on this device only", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }

    if (confirmWipe) AlertDialog(
        onDismissRequest = { confirmWipe = false },
        title = { Text("Delete all transactions?") },
        text = { Text("This permanently removes every transaction. Categories are kept. This cannot be undone.") },
        confirmButton = { TextButton(onClick = { vm.deleteAllTransactions(); confirmWipe = false; onSnack("All transactions deleted") }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete all") } },
        dismissButton = { TextButton(onClick = { confirmWipe = false }) { Text("Cancel") } }
    )
    if (showCats) ManageCategoriesDialog(categories, { n, t -> vm.addCategory(n, t) }, { c -> vm.deleteCategory(c) { used -> scope.launch { onSnack("Can't delete — used by $used transaction(s)") } } }, { showCats = false })
}
