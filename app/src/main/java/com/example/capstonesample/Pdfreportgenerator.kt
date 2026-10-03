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
        val marginLeft = 55f
        val maxWidth = pageWidth - (marginLeft * 2)
        var y = 70f
        var pageNumber = 1

        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas

        val titlePaint = Paint().apply {
            textSize = 20f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.CENTER
        }

        val sectionHeaderPaint = Paint().apply {
            textSize = 14f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.LEFT
        }

        val labelPaint = Paint().apply {
            textSize = 11.5f
            isFakeBoldText = true
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.LEFT
        }

        val bodyPaint = Paint().apply {
            textSize = 11.5f
            isFakeBoldText = false
            color = android.graphics.Color.BLACK
            textAlign = Paint.Align.LEFT
        }

        fun checkPageBreak(requiredSpace: Float) {
            if (y + requiredSpace > pageHeight - 50f) {
                document.finishPage(page)
                pageNumber++
                val nextInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(nextInfo)
                canvas = page.canvas
                y = 60f
            }
        }

        val lines = reportText.split("\n")
        var i = 0

        while (i < lines.size) {
            val rawLine = lines[i].trim()

            if (rawLine.equals("Daily Site Report", ignoreCase = true)) {
                checkPageBreak(35f)
                canvas.drawText("Daily Site Report", pageWidth / 2f, y, titlePaint)
                y += 32f
            } else if (rawLine.equals("Manpower", ignoreCase = true) ||
                       rawLine.equals("Work Progress", ignoreCase = true) ||
                       rawLine.equals("Ongoing Scope of works", ignoreCase = true) ||
                       rawLine.equals("Ongoing Scope of work", ignoreCase = true)) {
                checkPageBreak(32f)
                y += 10f
                canvas.drawText(rawLine, marginLeft, y, sectionHeaderPaint)
                y += 20f
            } else if (rawLine.startsWith("Date:", ignoreCase = true) ||
                       rawLine.startsWith("Project Name:", ignoreCase = true) ||
                       rawLine.startsWith("Location:", ignoreCase = true)) {
                checkPageBreak(20f)
                val colonIdx = rawLine.indexOf(':')
                if (colonIdx != -1) {
                    val label = rawLine.substring(0, colonIdx + 1) + " "
                    val value = rawLine.substring(colonIdx + 1).trim()
                    canvas.drawText(label, marginLeft, y, labelPaint)
                    val labelWidth = labelPaint.measureText(label)
                    canvas.drawText(value, marginLeft + labelWidth, y, bodyPaint)
                } else {
                    canvas.drawText(rawLine, marginLeft, y, bodyPaint)
                }
                y += 18f
            } else if (rawLine.isBlank()) {
                y += 10f
            } else {
                checkPageBreak(18f)
                val wrapped = wrapText(rawLine, bodyPaint, maxWidth)
                for (wLine in wrapped) {
                    checkPageBreak(18f)
                    canvas.drawText(wLine, marginLeft, y, bodyPaint)
                    y += 16f
                }
            }
            i++
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