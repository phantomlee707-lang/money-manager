package com.rainweaver.moneymgr.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rainweaver.moneymgr.data.Category
import com.rainweaver.moneymgr.data.MoneyFmt
import com.rainweaver.moneymgr.data.Settings
import com.rainweaver.moneymgr.data.Transaction
import com.rainweaver.moneymgr.data.TxType
import java.time.LocalDate
import java.time.YearMonth

val ChartIncome = Color(0xFF5EEAD4)
val ChartExpense = Color(0xFFFB923C)
val ChartCat = Color(0xFFA78BFA)

@Composable
fun SummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(14.dp)) {
            Text(label, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

/** Period selector row: ‹ Month Name YYYY › */
@Composable
fun MonthSelector(ym: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = onPrev) { Text("‹") }
        Text("${ym.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${ym.year}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        TextButton(onClick = onNext) { Text("›") }
    }
}

// ---------- bank-statement style components ----------

@Composable
fun ProfileHeader(name: String) {
    val initial = name.trim().firstOrNull()?.uppercase() ?: "M"
    val greeting = if (name.isBlank()) "Your personal ledger" else "Hi, ${name.trim().split(" ").first()}"
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
            Text(initial, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 22.sp)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(greeting, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Fully offline · data stays on this device", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
}

/** Bank-statement style period summary: opening / in / out / closing. */
@Composable
fun StatementSummaryCard(opening: Long, income: Long, expense: Long, closing: Long, money: (Long) -> String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            StatementLine("Opening balance", money(opening))
            StatementLine("Money in", "+${money(income)}", MaterialTheme.colorScheme.primary)
            StatementLine("Money out", "-${money(expense)}", MaterialTheme.colorScheme.error)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Closing balance", fontWeight = FontWeight.Bold)
                Text(money(closing), fontWeight = FontWeight.Bold, fontSize = 18.sp,
                    color = if (closing < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StatementLine(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

/** A ledger line showing the signed amount and the running balance after it. */
@Composable
fun StatementTxRow(tx: Transaction, running: Long, money: (Long) -> String, dateStr: (Long) -> String, onClick: () -> Unit, onDelete: () -> Unit) {
    val isIncome = tx.type == TxType.INCOME
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 14.dp, end = 4.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(tx.categoryName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text("${dateStr(tx.dateEpochDay)}${if (tx.note.isNotBlank()) " · ${tx.note}" else ""}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                Text("${if (isIncome) "+" else "-"}${money(tx.amountMinor)}", fontWeight = FontWeight.Bold,
                    color = if (isIncome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Text("bal ${money(running)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun TxRow(tx: Transaction, money: (Long) -> String, dateStr: (Long) -> String, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("${if (tx.type == TxType.INCOME) "+" else "-"}${money(tx.amountMinor)}",
                    fontWeight = FontWeight.Bold,
                    color = if (tx.type == TxType.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Text("${tx.categoryName} · ${dateStr(tx.dateEpochDay)}${if (tx.note.isNotBlank()) " · ${tx.note}" else ""}", fontSize = 12.sp)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ---------- charts (simple Canvas, no external deps) ----------

@Composable
fun MonthlyBarChart(data: List<Triple<YearMonth, Long, Long>>, money: (Long) -> String) {
    if (data.isEmpty()) return
    val maxV = data.maxOf { maxOf(it.second, it.third) }.coerceAtLeast(1L)
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(160.dp)) {
            val slot = size.width / data.size
            val barW = slot / 3.2f
            data.forEachIndexed { i, (_, inc, exp) ->
                val x = i * slot + slot / 2f - barW
                val incH = size.height * (inc.toFloat() / maxV)
                val expH = size.height * (exp.toFloat() / maxV)
                drawRect(ChartIncome, topLeft = Offset(x, size.height - incH), size = Size(barW, incH))
                drawRect(ChartExpense, topLeft = Offset(x + barW + 2f, size.height - expH), size = Size(barW, expH))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            data.forEach { (ym, _, _) -> Text(ym.monthValue.toString(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(ChartIncome, "Income")
            LegendDot(ChartExpense, "Expense")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(10.dp)) { drawCircle(color) }
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp)
    }
}

@Composable
fun CategoryBars(data: List<Pair<String, Long>>, money: (Long) -> String) {
    if (data.isEmpty()) { Text("No expenses this period.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp); return }
    val maxV = data.maxOf { it.second }.coerceAtLeast(1L)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        data.forEach { (name, minor) ->
            Column {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(name, fontSize = 13.sp)
                    Text(money(minor), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(4.dp))
                Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                    val w = size.width * (minor.toFloat() / maxV)
                    drawRect(ChartCat, size = Size(w, size.height))
                }
            }
        }
    }
}

// ---------- dialogs ----------

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TxDialog(
    initial: Transaction?,
    categories: List<Category>,
    onSave: (Long?, TxType, String, Long, String, String, LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf(initial?.type ?: TxType.EXPENSE) }
    var amount by remember { mutableStateOf(initial?.let { MoneyFmt.format(it.amountMinor, "", 2, "none").trim() } ?: "") }
    var note by remember { mutableStateOf(initial?.note ?: "") }
    var dateStr by remember { mutableStateOf(initial?.let { LocalDate.ofEpochDay(it.dateEpochDay).toString() } ?: LocalDate.now().toString()) }
    val cats = categories.filter { it.type == type }.ifEmpty { listOf(Category(name = "General", type = type)) }
    var selectedCat by remember { mutableStateOf(cats.firstOrNull { it.id == initial?.categoryId } ?: cats.first()) }
    LaunchedEffect(type) { selectedCat = cats.firstOrNull { it.id == initial?.categoryId } ?: cats.first() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add transaction" else "Edit transaction") },
        text = {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == TxType.EXPENSE, onClick = { type = TxType.EXPENSE }, label = { Text("Expense") })
                    FilterChip(selected = type == TxType.INCOME, onClick = { type = TxType.INCOME }, label = { Text("Income") })
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount") }, singleLine = true)
                Spacer(Modifier.height(6.dp))
                Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    cats.forEach { c -> FilterChip(selected = selectedCat?.id == c.id, onClick = { selectedCat = c }, label = { Text(c.name) }) }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(note, { note = it }, label = { Text("Note (optional)") }, singleLine = true)
                OutlinedTextField(dateStr, { dateStr = it }, label = { Text("Date (yyyy-MM-dd)") }, singleLine = true)
            }
        },
        confirmButton = {
            val cat = selectedCat ?: cats.first()
            TextButton(onClick = {
                val date = runCatching { LocalDate.parse(dateStr) }.getOrDefault(LocalDate.now())
                onSave(initial?.id, type, amount, cat.id, cat.name, note, date)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportDialog(
    currency: String, onCurrency: (String) -> Unit,
    decimals: Int, onDecimals: (Int) -> Unit,
    separator: String, onSeparator: (String) -> Unit,
    preview: (Long) -> String,
    onDismiss: () -> Unit,
    onExport: (period: String, from: String, to: String) -> Unit
) {
    var period by remember { mutableStateOf("month") }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export as PDF") },
        text = {
            Column {
                Text("Period", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = period == "month", onClick = { period = "month" }, label = { Text("Month") })
                    FilterChip(selected = period == "all", onClick = { period = "all" }, label = { Text("All time") })
                    FilterChip(selected = period == "custom", onClick = { period = "custom" }, label = { Text("Range") })
                }
                if (period == "custom") {
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(from, { from = it }, label = { Text("From (yyyy-MM-dd)") }, singleLine = true)
                    OutlinedTextField(to, { to = it }, label = { Text("To (yyyy-MM-dd)") }, singleLine = true)
                }
                Spacer(Modifier.height(12.dp))
                Text("Currency label", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                OutlinedTextField(currency, onCurrency, label = { Text("Symbol") }, singleLine = true)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Settings.CURRENCIES.take(6).forEach { c -> AssistChip(onClick = { onCurrency(c) }, label = { Text(c) }) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Decimals", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(0, 1, 2).forEach { d -> FilterChip(selected = decimals == d, onClick = { onDecimals(d) }, label = { Text("$d") }) }
                }
                Spacer(Modifier.height(12.dp))
                Text("Number separator", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Settings.SEPARATORS.forEach { (key, example) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = separator == key, onClick = { onSeparator(key) })
                        Text("$key  ($example)", fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Preview: ${preview(123456789L)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { TextButton(onClick = { onExport(period, from, to) }) { Text("Choose folder & export") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ManageCategoriesDialog(
    categories: List<Category>,
    onAdd: (String, TxType) -> Unit,
    onDelete: (Category) -> Unit,
    onDismiss: () -> Unit
) {
    var newIncome by remember { mutableStateOf("") }
    var newExpense by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Categories") },
        text = {
            Column(Modifier.heightIn(max = 440.dp)) {
                Text("Income", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                categories.filter { it.type == TxType.INCOME }.forEach { c -> CatRow(c, onDelete) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(newIncome, { newIncome = it }, placeholder = { Text("New income category") }, singleLine = true, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (newIncome.isNotBlank()) { onAdd(newIncome, TxType.INCOME); newIncome = "" } }) { Text("Add") }
                }
                Spacer(Modifier.height(12.dp))
                Text("Expense", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                categories.filter { it.type == TxType.EXPENSE }.forEach { c -> CatRow(c, onDelete) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(newExpense, { newExpense = it }, placeholder = { Text("New expense category") }, singleLine = true, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (newExpense.isNotBlank()) { onAdd(newExpense, TxType.EXPENSE); newExpense = "" } }) { Text("Add") }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
private fun CatRow(c: Category, onDelete: (Category) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(c.name + if (c.isDefault) "  (default)" else "", fontSize = 13.sp, modifier = Modifier.weight(1f))
        IconButton(onClick = { onDelete(c) }) { Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error) }
    }
}
