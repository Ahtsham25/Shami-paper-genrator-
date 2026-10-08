package com.example.data

import java.util.UUID

object BulkQuestionParser {

    private val urduRegex = Regex("[\\u0600-\\u06FF]")
    private val leadingNumberRegex = Regex("^\\s*(?:Q\\.?\\s*\\d+[:.)\\-]?|\\(?\\d+[.):\\-]|\\(?[ivxIVX]+\\)|سوال\\s*نمبر\\s*\\d+[:.)\\-]?|سوال\\s*\\d+[:.)\\-]?)\\s*")

    fun containsUrdu(text: String): Boolean = urduRegex.containsMatchIn(text)

    private fun cleanLeadingNumber(line: String): String {
        return line.replace(leadingNumberRegex, "").trim()
    }

    /**
     * Parses bulk pasted text into Short or Long QuestionEntity items.
     * Supports:
     * 1) "English question | اردو سوال" on a single line
     * 2) Alternating English line followed by Urdu line
     * 3) Pure English lines or Pure Urdu lines
     */
    fun parseShortOrLongQuestions(
        rawText: String,
        chapter: ChapterEntity,
        type: QuestionType,
        marksPerQuestion: Int = type.defaultMarks,
        startOrder: Int = 1
    ): List<QuestionEntity> {
        val rawLines = rawText.lines()
            .map { cleanLeadingNumber(it) }
            .filter { it.isNotBlank() }

        if (rawLines.isEmpty()) return emptyList()

        val result = mutableListOf<QuestionEntity>()
        var i = 0
        var order = startOrder

        while (i < rawLines.size) {
            val line = rawLines[i]
            var qEn = ""
            var qUr = ""

            if (line.contains("|")) {
                val parts = line.split("|").map { it.trim() }
                val first = parts.getOrElse(0) { "" }
                val second = parts.getOrElse(1) { "" }
                if (containsUrdu(first) && !containsUrdu(second) && second.isNotBlank()) {
                    qUr = first
                    qEn = second
                } else {
                    qEn = first
                    qUr = second.ifBlank { first }
                }
                i++
            } else if (i + 1 < rawLines.size && !containsUrdu(line) && containsUrdu(rawLines[i + 1]) && !rawLines[i + 1].contains("|")) {
                // Alternating English line + Urdu line
                qEn = line
                qUr = rawLines[i + 1]
                i += 2
            } else if (containsUrdu(line)) {
                qUr = line
                qEn = line
                i++
            } else {
                qEn = line
                qUr = line
                i++
            }

            result.add(
                QuestionEntity(
                    id = "${chapter.id}_${type.code.lowercase()}_${UUID.randomUUID().toString().take(8)}",
                    chapterId = chapter.id,
                    subjectId = chapter.subjectId,
                    classLevel = chapter.classLevel,
                    type = type.code,
                    questionEn = qEn,
                    questionUr = qUr,
                    marks = marksPerQuestion,
                    sortOrder = order++
                )
            )
        }
        return result
    }

    /**
     * Parses bulk pasted text into MCQ QuestionEntity items.
     * Supports:
     * Format 1 (Pipe separated):
     * Question En :: Question Ur | Opt A | Opt B | Opt C | Opt D | C
     * or:
     * Question text | Option A | Option B | Option C | Option D | A
     *
     * Format 2 (Multi-line blocks):
     * Question line
     * A) opt1  B) opt2  C) opt3  D) opt4
     */
    fun parseMcqQuestions(
        rawText: String,
        chapter: ChapterEntity,
        marksPerMcq: Int = 1,
        startOrder: Int = 1
    ): List<QuestionEntity> {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val result = mutableListOf<QuestionEntity>()
        var order = startOrder

        // Check if pipe-delimited single-line MCQs
        val pipeLines = lines.filter { it.contains("|") }
        if (pipeLines.size == lines.size) {
            for (line in pipeLines) {
                val parts = line.split("|").map { it.trim() }
                val qRaw = cleanLeadingNumber(parts.getOrElse(0) { "" })
                val (qEn, qUr) = splitBilingualCell(qRaw)
                val (aEn, aUr) = splitBilingualCell(parts.getOrElse(1) { "Option A" })
                val (bEn, bUr) = splitBilingualCell(parts.getOrElse(2) { "Option B" })
                val (cEn, cUr) = splitBilingualCell(parts.getOrElse(3) { "Option C" })
                val (dEn, dUr) = splitBilingualCell(parts.getOrElse(4) { "Option D" })
                val correct = parts.getOrElse(5) { "A" }.uppercase().take(1).let {
                    if (it in listOf("A", "B", "C", "D")) it else "A"
                }

                result.add(
                    QuestionEntity(
                        id = "${chapter.id}_mcq_${UUID.randomUUID().toString().take(8)}",
                        chapterId = chapter.id,
                        subjectId = chapter.subjectId,
                        classLevel = chapter.classLevel,
                        type = QuestionType.MCQ.code,
                        questionEn = qEn,
                        questionUr = qUr,
                        optionAEn = aEn,
                        optionBEn = bEn,
                        optionCEn = cEn,
                        optionDEn = dEn,
                        optionAUr = aUr,
                        optionBUr = bUr,
                        optionCUr = cUr,
                        optionDUr = dUr,
                        correctOption = correct,
                        marks = marksPerMcq,
                        sortOrder = order++
                    )
                )
            }
            return result
        }

        // Otherwise parse multi-line blocks
        var idx = 0
        val optionPrefixRegex = Regex("^(?:\\(?[A-Da-d][.)\\-:]|الف|ب|ج|د)\\s*(.+)$")
        val inlineOptionsRegex = Regex(
            "\\(?[Aa][.):]\\s*(.+?)\\s+\\(?[Bb][.):]\\s*(.+?)\\s+\\(?[Cc][.):]\\s*(.+?)\\s+\\(?[Dd][.):]\\s*(.+)"
        )

        while (idx < lines.size) {
            val qLine = cleanLeadingNumber(lines[idx])
            val (qEn, qUr) = splitBilingualCell(qLine)
            idx++

            var optA = "Option A"
            var optB = "Option B"
            var optC = "Option C"
            var optD = "Option D"
            var correct = "A"

            if (idx < lines.size) {
                val inlineMatch = inlineOptionsRegex.find(lines[idx])
                if (inlineMatch != null) {
                    optA = inlineMatch.groupValues[1].trim()
                    optB = inlineMatch.groupValues[2].trim()
                    optC = inlineMatch.groupValues[3].trim()
                    optD = inlineMatch.groupValues[4].trim()
                    idx++
                } else {
                    val collected = mutableListOf<String>()
                    while (idx < lines.size && collected.size < 4 && optionPrefixRegex.containsMatchIn(lines[idx])) {
                        val m = optionPrefixRegex.find(lines[idx])
                        collected.add(m?.groupValues?.get(1)?.trim() ?: lines[idx])
                        idx++
                    }
                    if (collected.isNotEmpty()) {
                        optA = collected.getOrElse(0) { "Option A" }
                        optB = collected.getOrElse(1) { "Option B" }
                        optC = collected.getOrElse(2) { "Option C" }
                        optD = collected.getOrElse(3) { "Option D" }
                    }
                }
            }

            if (idx < lines.size && (lines[idx].startsWith("Ans", ignoreCase = true) || lines[idx].startsWith("Correct", ignoreCase = true) || lines[idx].startsWith("جواب"))) {
                val ansChar = lines[idx].uppercase().lastOrNull { it in listOf('A', 'B', 'C', 'D') }
                if (ansChar != null) correct = ansChar.toString()
                idx++
            }

            val (aEn, aUr) = splitBilingualCell(optA)
            val (bEn, bUr) = splitBilingualCell(optB)
            val (cEn, cUr) = splitBilingualCell(optC)
            val (dEn, dUr) = splitBilingualCell(optD)

            result.add(
                QuestionEntity(
                    id = "${chapter.id}_mcq_${UUID.randomUUID().toString().take(8)}",
                    chapterId = chapter.id,
                    subjectId = chapter.subjectId,
                    classLevel = chapter.classLevel,
                    type = QuestionType.MCQ.code,
                    questionEn = qEn,
                    questionUr = qUr,
                    optionAEn = aEn,
                    optionBEn = bEn,
                    optionCEn = cEn,
                    optionDEn = dEn,
                    optionAUr = aUr,
                    optionBUr = bUr,
                    optionCUr = cUr,
                    optionDUr = dUr,
                    correctOption = correct,
                    marks = marksPerMcq,
                    sortOrder = order++
                )
            )
        }

        return result
    }

    private fun splitBilingualCell(raw: String): Pair<String, String> {
        val sep = when {
            raw.contains("::") -> "::"
            raw.contains("|") -> "|"
            else -> null
        }
        return if (sep != null) {
            val parts = raw.split(sep).map { it.trim() }
            val p1 = parts.getOrElse(0) { "" }
            val p2 = parts.getOrElse(1) { p1 }
            p1 to p2.ifBlank { p1 }
        } else {
            val trimmed = raw.trim()
            trimmed to trimmed
        }
    }
}
