package com.rainweaver.moneymgr.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.rainweaver.moneymgr.data.MoneyFmt
import com.rainweaver.moneymgr.data.Transaction
import com.rainweaver.moneymgr.data.TxClass
import java.io.OutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ReportGenerator {

    private val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val LEFT = 40f
    private const val RIGHT = 555f
    private const val BOTTOM = 800f

    // column x-positions (CR/DB are right-aligned to these anchors)
    private const val X_DATE = 40f
    private const val X_CAT = 105f
    private const val X_NOTE = 205f
    private const val X_CR = 475f
    private const val X_DB = 555f
    private const val NOTE_WRAP = 28

    /**
     * Writes a PDF report of [transactions] into [out]. Uses separate Credit (CR)
     * and Debit (DB) columns; transfers are excluded from both. Paginates long
     * reports and wraps long notes so nothing is clipped or overlapping.
     * [periodLabel] describes the covered range.
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

        val credits = TxClass.creditMinor(transactions)
        val debits = TxClass.debitMinor(transactions)
        val closing = credits - debits

        val title = Paint().apply { textSize = 22f; isFakeBoldText = true; color = Color.BLACK }
        val h = Paint().apply { textSize = 13f; isFakeBoldText = true; color = Color.DKGRAY }
        val body = Paint().apply { textSize = 11f; color = Color.BLACK }
        val small = Paint().apply { textSize = 10f; color = Color.GRAY }
        val dash = Paint().apply { textSize = 11f; color = Color.LTGRAY; textAlign = Paint.Align.RIGHT }

        val doc = PdfDocument()
        var pageNum = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
        var canvas: Canvas = page.canvas
        var y = 58f

        fun footer() {
            val f = Paint().apply { textSize = 8f; color = Color.GRAY; textAlign = Paint.Align.CENTER }
            canvas.drawText("Rainweaver Studio  •  Money Manager  •  Page $pageNum", PAGE_W / 2f, PAGE_H - 20f, f)
        }
        fun newPage() {
            footer()
            doc.finishPage(page)
            pageNum++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create())
            canvas = page.canvas
            y = 58f
        }
        fun need(extra: Float) { if (y + extra > BOTTOM) newPage() }

        // ---- header ----
        canvas.drawText("Money Manager — Report", LEFT, y, title); y += 24
        canvas.drawText("Rainweaver Studio", LEFT, y, h); y += 16
        canvas.drawText("Period: $periodLabel", LEFT, y, body); y += 16
        canvas.drawText("Generated ${LocalDate.now()}", LEFT, y, small); y += 26

        // ---- summary: credits / debits / closing ----
        need(72f)
        canvas.drawText("Total Credits (CR): ${money(credits)}", LEFT, y, h); y += 20
        canvas.drawText("Total Debits (DB):  ${money(debits)}", LEFT, y, h); y += 20
        canvas.drawText("Closing Balance:    ${money(closing)}", LEFT, y, h); y += 26

        // ---- debit (expense) breakdown by category ----
        val byCat = transactions.filter { TxClass.isDebit(it) }
            .groupBy { it.categoryName }
            .mapValues { e -> e.value.sumOf { it.amountMinor } }
            .toList().sortedByDescending { it.second }
        if (byCat.isNotEmpty()) {
            need(20f)
            canvas.drawText("Expenses by category", LEFT, y, h); y += 18
            for ((name, minor) in byCat) {
                need(16f)
                canvas.drawText(name.take(24), LEFT, y, body)
                drawRight(canvas, money(minor), 400f, y, body)
                y += 15
            }
            y += 12
        }

        // ---- transaction table ----
        need(34f)
        canvas.drawText("Date", X_DATE, y, h)
        canvas.drawText("Category", X_CAT, y, h)
        canvas.drawText("Note", X_NOTE, y, h)
        drawRight(canvas, "CR", X_CR, y, h)
        drawRight(canvas, "DB", X_DB, y, h)
        y += 6
        canvas.drawLine(LEFT, y, RIGHT, y, body); y += 16

        for (tx in transactions.sortedBy { it.dateEpochDay }) {
            val date = LocalDate.ofEpochDay(tx.dateEpochDay).format(fmt)
            val noteLines = wrap(tx.note, NOTE_WRAP)
            val rowH = (14f * noteLines.size).coerceAtLeast(15f)
            need(rowH + 4f)

            canvas.drawText(date, X_DATE, y, body)
            canvas.drawText(tx.categoryName.take(15), X_CAT, y, body)
            var ny = y
            for (ln in noteLines) { canvas.drawText(ln, X_NOTE, ny, body); ny += 14 }

            // Credit column: only income; Debit column: only expense; else a dash.
            if (TxClass.isCredit(tx)) drawRight(canvas, money(tx.amountMinor), X_CR, y, body)
            else drawRight(canvas, "–", X_CR, y, dash)
            if (TxClass.isDebit(tx)) drawRight(canvas, money(tx.amountMinor), X_DB, y, body)
            else drawRight(canvas, "–", X_DB, y, dash)

            y += rowH + 2f
        }

        footer()
        doc.finishPage(page)
        doc.writeTo(out)
        doc.close()
    }

    private fun drawRight(c: Canvas, text: String, x: Float, y: Float, base: Paint) {
        val p = Paint(base).apply { textAlign = Paint.Align.RIGHT }
        c.drawText(text, x, y, p)
    }

    /** Greedy word-wrap into lines of at most [maxChars] characters. */
    private fun wrap(text: String, maxChars: Int): List<String> {
        val t = text.trim()
        if (t.isEmpty()) return listOf("")
        if (t.length <= maxChars) return listOf(t)
        val lines = mutableListOf<String>()
        var i = 0
        while (i < t.length) {
            var end = (i + maxChars).coerceAtMost(t.length)
            if (end < t.length) {
                val sp = t.lastIndexOf(' ', end)
                if (sp > i) end = sp
            }
            lines.add(t.substring(i, end).trim())
            i = end
            while (i < t.length && t[i] == ' ') i++
        }
        return lines
    }
}
