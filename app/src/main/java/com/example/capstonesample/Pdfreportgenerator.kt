package com.example.capstonesample.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Builds a simple one-page PDF report from a task's inspection info + the YOLO result text,
 * saves it to app-private storage, and returns a content:// Uri (via FileProvider) that can be
 * shared or opened with any PDF viewer / Drive / Gmail via a chooser Intent.
 */
object PdfReportGenerator {

    fun generate(
        context: Context,
        taskTitle: String,
        engineer: String,
        status: String,
        reportText: String
    ): Uri {

        val pageWidth = 595  // A4 at 72dpi
        val pageHeight = 842
        val marginLeft = 40f
        var y = 60f

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
        }
        val labelPaint = Paint().apply {
            textSize = 12f
            isFakeBoldText = true
            color = android.graphics.Color.DKGRAY
        }
        val bodyPaint = Paint().apply {
            textSize = 12f
        }

        canvas.drawText("SITEPULSE INSPECTION REPORT", marginLeft, y, titlePaint)
        y += 30f

        canvas.drawText("Task: $taskTitle", marginLeft, y, labelPaint)
        y += 20f
        canvas.drawText("Lead Engineer: $engineer", marginLeft, y, labelPaint)
        y += 20f
        canvas.drawText("Status: $status", marginLeft, y, labelPaint)
        y += 30f

        canvas.drawText("AI ANALYSIS RESULT", marginLeft, y, labelPaint)
        y += 20f

        // Wrap long report text across multiple lines so it doesn't run off the page.
        val maxWidth = pageWidth - (marginLeft * 2)
        val lines = wrapText(reportText, bodyPaint, maxWidth)
        for (line in lines) {
            if (y > pageHeight - 60f) break // simple guard — extend to multi-page if reports get long
            canvas.drawText(line, marginLeft, y, bodyPaint)
            y += 18f
        }

        document.finishPage(page)

        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val file = File(reportsDir, "report_${System.currentTimeMillis()}.pdf")
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) > maxWidth) {
                if (current.isNotEmpty()) lines.add(current.toString())
                current = StringBuilder(word)
            } else {
                current = StringBuilder(candidate)
            }
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines
    }
}