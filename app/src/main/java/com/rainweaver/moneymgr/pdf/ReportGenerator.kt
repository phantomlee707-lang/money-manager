package com.rainweaver.moneymgr.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.rainweaver.moneymgr.data.MoneyFmt
import com.rainweaver.moneymgr.data.Transaction
import com.rainweaver.moneymgr.data.TxType
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ReportGenerator {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * Writes a PDF report of [transactions] into [out] (a user-chosen stream).
     * Includes totals, an expense-by-category breakdown, and a transaction table.
     * [periodLabel] describes the covered range (e.g. "October 2026" or a date range).
     */
    fun generate(
        transactions: List<Transaction>,
        out: OutputStream,
        currency: String,
        decimals: Int,
        separator: String,
        periodLabel: String = "All time"
    ) {
        fun money(minor: Long) = MoneyFmt.format(minor, currency, decimals, separator)

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val title = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.BLACK }
        val h = Paint().apply { textSize = 13f; isFakeBoldText = true; color = Color.DKGRAY }
        val body = Paint().apply { textSize = 11f; color = Color.BLACK }
        val small = Paint().apply { textSize = 10f; color = Color.GRAY }

        var y = 58
        canvas.drawText("Money Manager — Report", 40f, y.toFloat(), title); y += 24
        canvas.drawText("Rainweaver Studio", 40f, y.toFloat(), h); y += 16
        canvas.drawText("Period: $periodLabel", 40f, y.toFloat(), body); y += 16
        canvas.drawText("Generated ${LocalDate.now()}", 40f, y.toFloat(), small); y += 26

        val income = transactions.filter { it.type == TxType.INCOME }.sumOf { it.amountMinor }
        val expense = transactions.filter { it.type == TxType.EXPENSE }.sumOf { it.amountMinor }
        val balance = income - expense

        canvas.drawText("Total Income:  ${money(income)}", 40f, y.toFloat(), h); y += 20
        canvas.drawText("Total Expense: ${money(expense)}", 40f, y.toFloat(), h); y += 20
        canvas.drawText("Balance:       ${money(balance)}", 40f, y.toFloat(), h); y += 26

        // expense breakdown by category
        val byCat = transactions.filter { it.type == TxType.EXPENSE }
            .groupBy { it.categoryName }
            .mapValues { e -> e.value.sumOf { it.amountMinor } }
            .toList().sortedByDescending { it.second }
        if (byCat.isNotEmpty()) {
            canvas.drawText("Expense by category", 40f, y.toFloat(), h); y += 18
            for ((name, minor) in byCat) {
                canvas.drawText("${name.take(22)}", 40f, y.toFloat(), body)
                canvas.drawText(money(minor), 400f, y.toFloat(), body)
                y += 15
            }
            y += 12
        }

        // transaction table
        canvas.drawText("Date", 40f, y.toFloat(), h)
        canvas.drawText("Type", 130f, y.toFloat(), h)
        canvas.drawText("Category", 210f, y.toFloat(), h)
        canvas.drawText("Note", 330f, y.toFloat(), h)
        canvas.drawText("Amount", 470f, y.toFloat(), h)
        y += 6
        canvas.drawLine(40f, y.toFloat(), 555f, y.toFloat(), body); y += 16

        for (tx in transactions.sortedBy { it.dateEpochDay }) {
            if (y > 810) break
            val date = LocalDate.ofEpochDay(tx.dateEpochDay).format(fmt)
            canvas.drawText(date, 40f, y.toFloat(), body)
            canvas.drawText(if (tx.type == TxType.INCOME) "Income" else "Expense", 130f, y.toFloat(), body)
            canvas.drawText(tx.categoryName.take(15), 210f, y.toFloat(), body)
            canvas.drawText(tx.note.take(18), 330f, y.toFloat(), body)
            canvas.drawText(money(tx.amountMinor), 470f, y.toFloat(), body)
            y += 15
        }

        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }
}
