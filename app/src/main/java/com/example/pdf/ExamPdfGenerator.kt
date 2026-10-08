package com.example.pdf

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.PaperLanguage
import com.example.data.SavedPaperPayload
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object ExamPdfGenerator {

    private const val MARGIN_H = 34f
    private const val MARGIN_TOP = 34f
    private const val MARGIN_BOTTOM = 42f

    suspend fun generateExamPdf(
        context: Context,
        payload: SavedPaperPayload,
        fileNamePrefix: String = "Shami_Paper"
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()
        val lang = PaperLanguage.fromCode(payload.header.languageMode)
        val isLegal = payload.header.paperSize.equals("LEGAL", ignoreCase = true)
        val pageWidth = if (isLegal) 612 else 595
        val pageHeight = if (isLegal) 1008 else 842
        val contentWidth = (pageWidth - (MARGIN_H * 2)).toInt()

        val fontScale = when (payload.header.fontSizeScale.uppercase()) {
            "SMALL" -> 0.90f
            "LARGE" -> 1.15f
            else -> 1.0f
        }

        val totalMcqMarks = payload.mcqs.sumOf { it.marks }
        val totalShortMarks = payload.shortQuestions.sumOf { it.marks }
        val totalLongMarks = payload.longQuestions.sumOf { it.marks }
        val grandTotalMarks = totalMcqMarks + totalShortMarks + totalLongMarks

        var pageNumber = 1
        var currentPage = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        )
        var canvas = currentPage.canvas
        var yPos = MARGIN_TOP

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(11, 36, 71)
            style = Paint.Style.STROKE
            strokeWidth = 1.4f
        }
        val fillHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }
        val sectionFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(11, 36, 71)
            style = Paint.Style.FILL
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(11, 36, 71)
            textSize = 18f * fontScale
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val subHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10.5f * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val sectionHeaderPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 11f * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyBoldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyRegularPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f * fontScale
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 8.5f
        }

        fun drawFooter(c: Canvas, pNum: Int) {
            c.drawLine(
                MARGIN_H,
                pageHeight - 30f,
                pageWidth - MARGIN_H,
                pageHeight - 30f,
                borderPaint
            )
            c.drawText(
                "Paper Maker by Shami Academy",
                MARGIN_H,
                pageHeight - 16f,
                footerPaint
            )
            val pageText = "Page $pNum"
            val pw = footerPaint.measureText(pageText)
            c.drawText(
                pageText,
                pageWidth - MARGIN_H - pw,
                pageHeight - 16f,
                footerPaint
            )
        }

        fun ensureSpace(neededHeight: Float) {
            if (yPos + neededHeight > pageHeight - MARGIN_BOTTOM) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                pageNumber++
                currentPage = pdfDocument.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = currentPage.canvas
                yPos = MARGIN_TOP
            }
        }

        fun drawMultilineText(
            text: String,
            paint: TextPaint,
            width: Int = contentWidth,
            xOffset: Float = MARGIN_H,
            alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
        ): Float {
            if (text.isBlank()) return 0f
            val staticLayout = StaticLayout.Builder
                .obtain(text, 0, text.length, paint, width)
                .setAlignment(alignment)
                .setLineSpacing(2f, 1.05f)
                .setIncludePad(false)
                .build()
            val height = staticLayout.height.toFloat()
            ensureSpace(height + 6f)
            canvas.save()
            canvas.translate(xOffset, yPos)
            staticLayout.draw(canvas)
            canvas.restore()
            yPos += height + 4f
            return height
        }

        // ====================================================================
        // 1. EXAM PAPER HEADER BLOCK (Version 1: Table Grid vs Version 2: Classic)
        // ====================================================================
        val instName = payload.header.institutionName.ifBlank { "SHAMI ACADEMY" }.uppercase()
        val subjectDisplay = when (lang) {
            PaperLanguage.ENGLISH -> payload.header.subjectNameEn
            PaperLanguage.URDU -> payload.header.subjectNameUr
            PaperLanguage.BILINGUAL -> "${payload.header.subjectNameEn} (${payload.header.subjectNameUr})"
        }
        val studentVal = payload.header.studentNameValue.ifBlank { "____________________" }
        val rollVal = payload.header.rollNumberValue.ifBlank { "____________" }

        if (payload.header.paperVersion == 1) {
            // VERSION 1: STRUCTURED TABLE HEADER
            val top = yPos
            val row1H = 30f
            val row2H = 24f
            val row3H = 24f
            val totalH = row1H + row2H + row3H
            val left = MARGIN_H
            val right = pageWidth - MARGIN_H
            val midX = (left + right) / 2f
            val col1 = left + (contentWidth * 0.36f)
            val col2 = left + (contentWidth * 0.68f)

            canvas.drawRect(left, top, right, top + row1H, fillHeaderPaint)
            canvas.drawRect(left, top, right, top + totalH, borderPaint)
            canvas.drawLine(left, top + row1H, right, top + row1H, borderPaint)
            canvas.drawLine(left, top + row1H + row2H, right, top + row1H + row2H, borderPaint)

            // Vertical cell dividers
            canvas.drawLine(midX, top + row1H, midX, top + row1H + row2H, borderPaint)
            canvas.drawLine(col1, top + row1H + row2H, col1, top + totalH, borderPaint)
            canvas.drawLine(col2, top + row1H + row2H, col2, top + totalH, borderPaint)

            val instW = titlePaint.measureText(instName)
            canvas.drawText(instName, ((pageWidth - instW) / 2f).coerceAtLeast(left + 8f), top + 21f, titlePaint)

            // Row 2: Student Name | Roll Number
            canvas.drawText("Student Name: $studentVal", left + 8f, top + row1H + 16f, bodyBoldPaint)
            canvas.drawText("Roll No: $rollVal", midX + 8f, top + row1H + 16f, bodyBoldPaint)

            // Row 3: Subject & Class | Time Allowed | Total Marks
            canvas.drawText("${payload.header.classLabel} - $subjectDisplay", left + 8f, top + row1H + row2H + 16f, bodyBoldPaint)
            canvas.drawText("Time: ${payload.header.timeAllowed}", col1 + 8f, top + row1H + row2H + 16f, bodyBoldPaint)
            canvas.drawText("Total Marks: $grandTotalMarks", col2 + 8f, top + row1H + row2H + 16f, bodyBoldPaint)

            yPos = top + totalH + 14f
        } else {
            // VERSION 2: CLASSIC BANNER HEADER
            val top = yPos
            val boxH = 84f
            canvas.drawRect(MARGIN_H, top, pageWidth - MARGIN_H, top + boxH, borderPaint)
            val instW = titlePaint.measureText(instName)
            canvas.drawText(instName, ((pageWidth - instW) / 2f).coerceAtLeast(MARGIN_H + 8f), top + 22f, titlePaint)

            val subLine = "${payload.header.classLabel}  •  Subject: $subjectDisplay  •  ${payload.header.examTitle}"
            val subW = subHeaderPaint.measureText(subLine)
            canvas.drawText(subLine, ((pageWidth - subW) / 2f).coerceAtLeast(MARGIN_H + 8f), top + 40f, subHeaderPaint)

            canvas.drawLine(MARGIN_H + 8f, top + 46f, pageWidth - MARGIN_H - 8f, top + 46f, borderPaint)
            canvas.drawText("Name: $studentVal", MARGIN_H + 10f, top + 62f, bodyBoldPaint)
            canvas.drawText("Roll No: $rollVal", pageWidth - MARGIN_H - 180f, top + 62f, bodyBoldPaint)
            canvas.drawText("Time Allowed: ${payload.header.timeAllowed}", MARGIN_H + 10f, top + 78f, bodyBoldPaint)
            canvas.drawText("Total Marks: $grandTotalMarks", pageWidth - MARGIN_H - 180f, top + 78f, bodyBoldPaint)

            yPos = top + boxH + 14f
        }

        var mainSectionQuestionNumber = 1

        fun drawSectionBanner(titleEn: String, titleUr: String, marksText: String) {
            ensureSpace(28f)
            canvas.drawRect(
                MARGIN_H,
                yPos,
                pageWidth - MARGIN_H,
                yPos + 22f,
                sectionFillPaint
            )
            val label = when (lang) {
                PaperLanguage.ENGLISH -> "Q.$mainSectionQuestionNumber: $titleEn"
                PaperLanguage.URDU -> "سوال نمبر $mainSectionQuestionNumber: $titleUr"
                PaperLanguage.BILINGUAL -> "Q.$mainSectionQuestionNumber: $titleEn  |  سوال نمبر $mainSectionQuestionNumber: $titleUr"
            }
            canvas.drawText(label, MARGIN_H + 8f, yPos + 15f, sectionHeaderPaint)
            val mw = sectionHeaderPaint.measureText(marksText)
            canvas.drawText(
                marksText,
                pageWidth - MARGIN_H - mw - 8f,
                yPos + 15f,
                sectionHeaderPaint
            )
            yPos += 28f
            mainSectionQuestionNumber++
        }

        // ====================================================================
        // 2. SECTION: MCQs (Q.1)
        // ====================================================================
        if (payload.mcqs.isNotEmpty()) {
            drawSectionBanner(
                titleEn = "Choose the correct option.",
                titleUr = "درست جواب کا انتخاب کریں۔",
                marksText = "(${payload.mcqs.size} x 1 = $totalMcqMarks)"
            )

            payload.mcqs.forEachIndexed { index, q ->
                ensureSpace(48f)
                val num = index + 1
                when (lang) {
                    PaperLanguage.ENGLISH -> {
                        drawMultilineText(
                            text = "$num. ${q.questionEn} (${q.marks})",
                            paint = bodyBoldPaint
                        )
                        val opts = "(A) ${q.optionAEn}    (B) ${q.optionBEn}    (C) ${q.optionCEn}    (D) ${q.optionDEn}"
                        drawMultilineText(text = "    $opts", paint = bodyRegularPaint)
                    }
                    PaperLanguage.URDU -> {
                        drawMultilineText(
                            text = "$num. ${q.questionUr} (${q.marks})",
                            paint = bodyBoldPaint,
                            alignment = Layout.Alignment.ALIGN_OPPOSITE
                        )
                        val optsUr = "(الف) ${q.optionAUr}    (ب) ${q.optionBUr}    (ج) ${q.optionCUr}    (د) ${q.optionDUr}"
                        drawMultilineText(
                            text = optsUr,
                            paint = bodyRegularPaint,
                            alignment = Layout.Alignment.ALIGN_OPPOSITE
                        )
                    }
                    PaperLanguage.BILINGUAL -> {
                        drawMultilineText(
                            text = "$num. ${q.questionEn}",
                            paint = bodyBoldPaint
                        )
                        if (q.questionUr.isNotBlank() && q.questionUr != q.questionEn) {
                            drawMultilineText(
                                text = q.questionUr,
                                paint = bodyBoldPaint,
                                alignment = Layout.Alignment.ALIGN_OPPOSITE
                            )
                        }
                        val optsBilingual =
                            "(A) ${q.optionAEn} / ${q.optionAUr}   (B) ${q.optionBEn} / ${q.optionBUr}   (C) ${q.optionCEn} / ${q.optionCUr}   (D) ${q.optionDEn} / ${q.optionDUr}"
                        drawMultilineText(text = "    $optsBilingual", paint = bodyRegularPaint)
                    }
                }
                if (payload.header.paperVersion == 1) {
                    canvas.drawLine(MARGIN_H, yPos, pageWidth - MARGIN_H, yPos, borderPaint)
                }
                yPos += 4f
            }
            yPos += 8f
        }

        // ====================================================================
        // 3. SECTION: SHORT QUESTIONS (Q.2)
        // ====================================================================
        if (payload.shortQuestions.isNotEmpty()) {
            drawSectionBanner(
                titleEn = "Write short answers to the following questions.",
                titleUr = "مندرجہ ذیل مختصر سوالات کے جوابات لکھیں۔",
                marksText = "(Marks: $totalShortMarks)"
            )

            payload.shortQuestions.forEachIndexed { index, q ->
                ensureSpace(32f)
                val roman = toRoman(index + 1)
                when (lang) {
                    PaperLanguage.ENGLISH -> {
                        drawMultilineText(
                            text = "($roman) ${q.questionEn}  [${q.marks}]",
                            paint = bodyRegularPaint
                        )
                    }
                    PaperLanguage.URDU -> {
                        drawMultilineText(
                            text = "(${index + 1}) ${q.questionUr}  [${q.marks}]",
                            paint = bodyRegularPaint,
                            alignment = Layout.Alignment.ALIGN_OPPOSITE
                        )
                    }
                    PaperLanguage.BILINGUAL -> {
                        drawMultilineText(
                            text = "($roman) ${q.questionEn}  [${q.marks}]",
                            paint = bodyBoldPaint
                        )
                        if (q.questionUr.isNotBlank() && q.questionUr != q.questionEn) {
                            drawMultilineText(
                                text = q.questionUr,
                                paint = bodyRegularPaint,
                                alignment = Layout.Alignment.ALIGN_OPPOSITE
                            )
                        }
                    }
                }
                yPos += 3f
            }
            yPos += 8f
        }

        // ====================================================================
        // 4. SECTION: LONG QUESTIONS (Q.3)
        // ====================================================================
        if (payload.longQuestions.isNotEmpty()) {
            drawSectionBanner(
                titleEn = "Answer the following detailed / long questions.",
                titleUr = "مندرجہ ذیل تفصیلی سوالات کے جوابات دیں۔",
                marksText = "(Marks: $totalLongMarks)"
            )

            payload.longQuestions.forEachIndexed { index, q ->
                ensureSpace(36f)
                val num = index + 1
                when (lang) {
                    PaperLanguage.ENGLISH -> {
                        drawMultilineText(
                            text = "($num) ${q.questionEn}  [${q.marks} Marks]",
                            paint = bodyRegularPaint
                        )
                    }
                    PaperLanguage.URDU -> {
                        drawMultilineText(
                            text = "($num) ${q.questionUr}  [${q.marks} نمبر]",
                            paint = bodyRegularPaint,
                            alignment = Layout.Alignment.ALIGN_OPPOSITE
                        )
                    }
                    PaperLanguage.BILINGUAL -> {
                        drawMultilineText(
                            text = "($num) ${q.questionEn}  [${q.marks} Marks]",
                            paint = bodyBoldPaint
                        )
                        if (q.questionUr.isNotBlank() && q.questionUr != q.questionEn) {
                            drawMultilineText(
                                text = q.questionUr,
                                paint = bodyRegularPaint,
                                alignment = Layout.Alignment.ALIGN_OPPOSITE
                            )
                        }
                    }
                }
                yPos += 4f
            }
        }

        // ====================================================================
        // 5. OPTIONAL ANSWER KEY FOR MCQs
        // ====================================================================
        if (payload.header.includeAnswerKey && payload.mcqs.isNotEmpty()) {
            ensureSpace(50f)
            yPos += 10f
            canvas.drawLine(MARGIN_H, yPos, pageWidth - MARGIN_H, yPos, borderPaint)
            yPos += 10f
            val keySummary = payload.mcqs.mapIndexed { i, q -> "${i + 1}:${q.correctOption}" }.joinToString("   |   ")
            drawMultilineText(
                text = "Teacher Answer Key (MCQs):   $keySummary",
                paint = footerPaint
            )
        }

        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)

        val papersDir = File(context.filesDir, "papers")
        if (!papersDir.exists()) papersDir.mkdirs()

        val safeSubject = payload.header.subjectNameEn.replace(Regex("[^A-Za-z0-9]"), "_")
        val fileName = "${fileNamePrefix}_Class${payload.header.classLabel.filter { it.isDigit() }}_${safeSubject}_${System.currentTimeMillis()}.pdf"
        val outFile = File(papersDir, fileName)

        FileOutputStream(outFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        outFile
    }

    fun shareOrOpenPdf(context: Context, file: File, share: Boolean = true) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = if (share) {
                Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Exam Paper - Paper Maker by Shami Academy")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(
                if (share) Intent.createChooser(intent, "Share Exam Paper PDF") else intent
            )
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "PDF saved at: ${file.name}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun toRoman(num: Int): String {
        val romans = listOf(
            "i", "ii", "iii", "iv", "v", "vi", "vii", "viii", "ix", "x",
            "xi", "xii", "xiii", "xiv", "xv", "xvi", "xvii", "xviii", "xix", "xx"
        )
        return romans.getOrElse(num - 1) { num.toString() }
    }
}
