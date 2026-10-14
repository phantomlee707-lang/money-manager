package com.rainweaver.moneymgr

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rainweaver.moneymgr.data.Transaction
import com.rainweaver.moneymgr.data.TxType
import com.rainweaver.moneymgr.pdf.ReportGenerator
import com.rainweaver.moneymgr.ui.DashboardScreen
import com.rainweaver.moneymgr.ui.ExportDialog
import com.rainweaver.moneymgr.ui.HistoryScreen
import com.rainweaver.moneymgr.ui.MoneyManagerTheme
import com.rainweaver.moneymgr.ui.MoneyViewModel
import com.rainweaver.moneymgr.ui.SettingsScreen
import com.rainweaver.moneymgr.ui.SummaryScreen
import com.rainweaver.moneymgr.ui.TxDialog
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoneyManagerTheme {
                val vm: MoneyViewModel = viewModel()
                AppRoot(vm)
            }
        }
    }
}

@Composable
private fun AppRoot(vm: MoneyViewModel) {
    var tab by remember { mutableStateOf(0) }
    var showTx by remember { mutableStateOf(false) }
    var editTx by remember { mutableStateOf<Transaction?>(null) }
    var pendingDelete by remember { mutableStateOf<Transaction?>(null) }
    var showExport by remember { mutableStateOf(false) }
    var exportPeriod by remember { mutableStateOf("month") }
    var exportFrom by remember { mutableStateOf("") }
    var exportTo by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val onEdit: (Transaction?) -> Unit = { tx -> editTx = tx; showTx = true }
    val onDelete: (Transaction) -> Unit = { tx -> pendingDelete = tx }
    val onRequestExport: () -> Unit = { showExport = true }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val from = runCatching { LocalDate.parse(exportFrom) }.getOrDefault(LocalDate.of(2000, 1, 1))
                    val to = runCatching { LocalDate.parse(exportTo) }.getOrDefault(LocalDate.of(2100, 1, 1))
                    val (list, label) = when (exportPeriod) {
                        "all" -> vm.allForReport() to "All time"
                        "custom" -> vm.txBetween(from, to) to "$from – $to"
                        else -> {
                            val ym = vm.selectedMonth
                            val s = ym.atDay(1)
                            vm.txBetween(s, ym.atEndOfMonth()) to
                                "${ym.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${ym.year}"
                        }
                    }
                    context.contentResolver.openOutputStream(uri)?.use { os ->
                        ReportGenerator.generate(list, os, vm.currency, vm.decimals, vm.separator, label)
                    }
                    snackbar.showSnackbar("PDF exported")
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, "Share report"))
                } catch (e: Exception) {
                    snackbar.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.Home, "Dashboard") }, label = { Text("Dashboard") })
                NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.AutoMirrored.Filled.List, "History") }, label = { Text("History") })
                NavigationBarItem(tab == 2, { tab = 2 }, icon = { Icon(Icons.Default.PieChart, "Summary") }, label = { Text("Summary") })
                NavigationBarItem(tab == 3, { tab = 3 }, icon = { Icon(Icons.Default.Settings, "Settings") }, label = { Text("Settings") })
            }
        },
        floatingActionButton = {
            if (tab == 0 || tab == 1) FloatingActionButton(onClick = { onEdit(null) }) { Icon(Icons.Default.Add, "Add") }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            when (tab) {
                0 -> DashboardScreen(vm, onEdit, onRequestExport, onDelete)
                1 -> HistoryScreen(vm, onEdit, onDelete)
                2 -> SummaryScreen(vm, onRequestExport)
                else -> SettingsScreen(vm, scope, onRequestExport) { msg -> scope.launch { snackbar.showSnackbar(msg) } }
            }
        }
    }

    if (showTx) TxDialog(editTx, vm.categories.value,
        onSave = { id, t, a, c, n, nt, d -> vm.saveTransaction(id, t, a, c, n, nt, d); showTx = false },
        onDismiss = { showTx = false })

    pendingDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete transaction?") },
            text = { Text("${if (tx.type == TxType.INCOME) "Income" else "Expense"} · ${tx.categoryName} · ${vm.money(tx.amountMinor)}") },
            confirmButton = { TextButton(onClick = { vm.delete(tx); pendingDelete = null }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } }
        )
    }

    if (showExport) ExportDialog(vm.currency, { vm.currency = it }, vm.decimals, { vm.decimals = it },
        vm.separator, { vm.separator = it }, { vm.money(it) }, { showExport = false },
        { period, from, to ->
            vm.saveSettings(); exportPeriod = period; exportFrom = from; exportTo = to; showExport = false
            exportLauncher.launch("money_report_${LocalDate.now()}.pdf")
        })
}
