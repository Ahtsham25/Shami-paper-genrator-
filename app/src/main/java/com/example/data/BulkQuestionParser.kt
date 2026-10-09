package com.example.data

import java.util.UUID

object BulkQuestionParser {

    private val urduRegex = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")
    private val anyLatinRegex = Regex("[A-Za-z]")
    private val latinWordRegex = Regex("[A-Za-z]{2,}")

    private val leadingNumberRegex = Regex(
        "^\\s*(?:[•*\\-–—]+\\s*)?(?:Q(?:uestion)?\\.?\\s*#?\\s*\\d+[:.)\\-]?|سوال\\s*نمبر\\s*[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[:.)\\-۔]?|سوال\\s*[\\d\\u06F0-\\u06F9\\u0660-\\u0669]*[:.)\\-۔]?|\\([\\d\\u06F0-\\u06F9\\u0660-\\u0669]+\\)|\\[[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+\\]|[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[.):\\-۔]|\\([ivxIVX]+\\)|[ivxIVX]+[.):])\\s*",
        RegexOption.IGNORE_CASE
    )

    private val trailingMarksRegex = Regex(
        "\\s*(?:\\[\\s*\\d+\\s*(?:Marks|نمبر)?\\s*\\]|\\(\\s*\\d+\\s*(?:Marks|نمبر)\\s*\\))\\s*$",
        RegexOption.IGNORE_CASE
    )

    private val trailingOrphanNumberBeforeUrduRegex = Regex(
        "\\s*(?:Q(?:uestion)?\\.?\\s*#?\\s*\\d+[:.)\\-]?|\\(?[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[.):\\-]?|\\([ivxIVX]+\\)|\\([A-Da-d]\\)|[/:|\\-–—]|\\()\\s*$",
        RegexOption.IGNORE_CASE
    )

    private val optionSinglePrefixRegex = Regex(
        "^\\s*(?:\\([A-Da-d]\\)|\\[[A-Da-d]\\]|[A-Da-d][.):\\-]|\\((?:الف|ا|ب|ج|د)\\)|(?:الف|ا|ب|ج|د)[.):\\-۔])\\s*(.+)$"
    )

    private val enInlineOptionsRegex = Regex(
        "(?:^|\\s)(?:\\([Aa]\\)|[Aa][.):\\-])\\s*(.+?)\\s+(?:\\([Bb]\\)|[Bb][.):\\-])\\s*(.+?)\\s+(?:\\([Cc]\\)|[Cc][.):\\-])\\s*(.+?)\\s+(?:\\([Dd]\\)|[Dd][.):\\-])\\s*(.+)$"
    )

    private val urInlineOptionsRegex = Regex(
        "(?:^|\\s)(?:\\((?:الف|ا)\\)|(?:الف|ا)[.):\\-۔])\\s*(.+?)\\s+(?:\\(ب\\)|ب[.):\\-۔])\\s*(.+?)\\s+(?:\\(ج\\)|ج[.):\\-۔])\\s*(.+?)\\s+(?:\\(د\\)|د[.):\\-۔])\\s*(.+)$"
    )

    private val answerLineRegex = Regex(
        "^\\s*(?:Ans(?:wer)?|Correct(?:\\s*Option)?|Key|درست\\s*جواب|جواب)\\s*[:=\\-]?\\s*(.+)$",
        RegexOption.IGNORE_CASE
    )

    private val trailingInlineAnswerRegex = Regex(
        "\\s*(?:\\[?\\s*(?:Ans(?:wer)?|Correct|جواب)\\s*[:=\\-]\\s*([A-Da-d]|الف|ا|ب|ج|د)\\s*\\]?|\\(\\s*(?:Ans|جواب)\\s*[:=\\-]?\\s*([A-Da-d]|الف|ا|ب|ج|د)\\s*\\))\\s*$",
        RegexOption.IGNORE_CASE
    )

    fun containsUrdu(text: String): Boolean = urduRegex.containsMatchIn(text)

    fun containsEnglish(text: String): Boolean = anyLatinRegex.containsMatchIn(text)

    fun cleanLeadingNumber(line: String): String {
        var current = line.trim()
        repeat(3) {
            val next = current.replaceFirst(leadingNumberRegex, "").trim()
            if (next == current) return@repeat
            current = next
        }
        current = current.replace(trailingMarksRegex, "").trim()
        return current
    }

    fun cleanOptionPrefix(raw: String): String {
        var current = raw.trim()
        repeat(2) {
            val m = optionSinglePrefixRegex.find(current)
            if (m != null) {
                current = m.groupValues[1].trim()
            }
        }
        return current.trim()
    }

    private fun isSectionHeadingLine(line: String): Boolean {
        val cleaned = cleanLeadingNumber(line).lowercase()
            .removeSuffix(":")
            .removeSuffix("۔")
            .removeSuffix("-")
            .trim()
        val headings = setOf(
            "short questions",
            "short question",
            "long questions",
            "long question",
            "mcqs",
            "mcq",
            "multiple choice questions",
            "objective",
            "subjective",
            "english medium",
            "urdu medium",
            "bilingual",
            "مختصر سوالات",
            "تفصیلی سوالات",
            "لانگ کوسچن",
            "شارٹ کوسچن",
            "ایم سی کیوز",
            "کثیر الانتخابی سوالات",
            "معروضی سوالات",
            "انشائیہ سوالات",
            "اردو میڈیم",
            "انگلش میڈیم"
        )
        return cleaned in headings
    }

    /**
     * Intelligently splits any string into (EnglishPart, UrduPart).
     * - If the string has both English and Urdu (via ::, |, /, -, or adjacent on the same line),
     *   returns Pair(cleanEnglish, cleanUrdu).
     * - If the string is pure English, returns Pair(cleanEnglish, "").
     * - If the string is pure Urdu (even with inline chemical/physics symbols like pH or SI),
     *   returns Pair("", cleanUrdu).
     */
    fun splitBilingualText(raw: String, isOption: Boolean = false): Pair<String, String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "" to ""

        val cleanFn: (String) -> String = { s ->
            if (isOption) cleanOptionPrefix(s) else cleanLeadingNumber(s)
        }

        // 1. Check explicit delimiters (::, |, Tab)
        val explicitSep = when {
            trimmed.contains("::") -> "::"
            trimmed.contains("|") -> "|"
            trimmed.contains("\t") -> "\t"
            else -> null
        }
        if (explicitSep != null) {
            val parts = trimmed.split(explicitSep).map { it.trim() }.filter { it.isNotBlank() }
            val enParts = parts.filter { !containsUrdu(it) }
            val urParts = parts.filter { containsUrdu(it) }
            if (enParts.isNotEmpty() && urParts.isNotEmpty()) {
                val en = cleanFn(enParts.joinToString(" "))
                val ur = cleanFn(urParts.joinToString(" "))
                return en to ur
            }
            if (urParts.isEmpty() && enParts.isNotEmpty()) {
                return cleanFn(enParts.first()) to ""
            }
            if (enParts.isEmpty() && urParts.isNotEmpty()) {
                return "" to cleanFn(urParts.first())
            }
        }

        val hasUr = containsUrdu(trimmed)
        val hasEn = containsEnglish(trimmed)

        // 2. Pure Urdu (no Latin letters at all)
        if (hasUr && !hasEn) {
            return "" to cleanFn(trimmed)
        }

        // 3. Pure English / numbers (no Urdu characters at all)
        if (!hasUr) {
            return cleanFn(trimmed) to ""
        }

        // 4. Contains BOTH English and Urdu characters on the same line!
        // 4a. Check if separated by " / ", " - ", " – ", " — ", or "/"
        for (sep in listOf(" / ", " - ", " – ", " — ", "/")) {
            if (trimmed.contains(sep)) {
                val parts = trimmed.split(sep).map { it.trim() }.filter { it.isNotBlank() }
                if (parts.size == 2) {
                    val p0Ur = containsUrdu(parts[0])
                    val p1Ur = containsUrdu(parts[1])
                    if (!p0Ur && p1Ur && containsEnglish(parts[0])) {
                        return cleanFn(parts[0]) to cleanFn(parts[1])
                    } else if (p0Ur && !p1Ur && containsEnglish(parts[1])) {
                        return cleanFn(parts[1]) to cleanFn(parts[0])
                    }
                }
            }
        }

        // 4b. Inspect character boundary between English and Urdu
        val allUrduMatches = urduRegex.findAll(trimmed).toList()
        val firstUrduIdx = allUrduMatches.first().range.first
        val lastUrduIdx = allUrduMatches.last().range.last

        val rawBeforeUrdu = trimmed.substring(0, firstUrduIdx)
            .replace(trailingOrphanNumberBeforeUrduRegex, "")
            .trim()
        val rawFromFirstUrdu = trimmed.substring(firstUrduIdx).trim()

        var afterIdx = lastUrduIdx + 1
        while (afterIdx < trimmed.length && trimmed[afterIdx] in listOf(')', ']', '۔', '؟', '.', '?', '!', ':', '-', '–', '/')) {
            afterIdx++
        }
        val rawAfterLastUrdu = if (afterIdx < trimmed.length) trimmed.substring(afterIdx).trim() else ""
        val rawUpToLastUrdu = if (afterIdx <= trimmed.length) trimmed.substring(0, afterIdx).trim() else trimmed

        val beforeHasEn = containsEnglish(rawBeforeUrdu)
        val afterHasEn = containsEnglish(rawAfterLastUrdu)

        if (isOption) {
            if (beforeHasEn && !afterHasEn) {
                val en = cleanOptionPrefix(rawBeforeUrdu)
                val ur = cleanOptionPrefix(rawFromFirstUrdu)
                if (en.isNotBlank() && ur.isNotBlank()) return en to ur
            }
            if (afterHasEn && !beforeHasEn) {
                val ur = cleanOptionPrefix(rawUpToLastUrdu)
                val en = cleanOptionPrefix(rawAfterLastUrdu)
                if (en.isNotBlank() && ur.isNotBlank()) return en to ur
            }
        } else {
            // Question stem: ensure English part is an actual English phrase/question
            // and not a single technical token (like "pH") at the start of an Urdu sentence.
            if (beforeHasEn && !afterHasEn) {
                val candidateEn = cleanLeadingNumber(rawBeforeUrdu)
                val wordCount = latinWordRegex.findAll(candidateEn).count()
                val endsWithPunct = candidateEn.lastOrNull() in listOf('?', '.', '!', ':', ';')
                if (wordCount >= 2 || (wordCount >= 1 && endsWithPunct)) {
                    val candidateUr = cleanLeadingNumber(rawFromFirstUrdu)
                    if (candidateEn.isNotBlank() && candidateUr.isNotBlank()) {
                        return candidateEn to candidateUr
                    }
                }
            }
            if (afterHasEn && !beforeHasEn) {
                val candidateEn = cleanLeadingNumber(rawAfterLastUrdu)
                val wordCount = latinWordRegex.findAll(candidateEn).count()
                val endsWithPunct = candidateEn.lastOrNull() in listOf('?', '.', '!', ':', ';')
                if (wordCount >= 2 || (wordCount >= 1 && endsWithPunct)) {
                    val candidateUr = cleanLeadingNumber(rawUpToLastUrdu)
                    if (candidateEn.isNotBlank() && candidateUr.isNotBlank()) {
                        return candidateEn to candidateUr
                    }
                }
            }
        }

        // Fallback: if mostly Urdu with an inline English symbol (e.g. "قوت کا SI یونٹ کیا ہے؟"),
        // treat as Urdu question.
        return "" to cleanFn(trimmed)
    }

    /**
     * Parses bulk pasted text into Short or Long QuestionEntity items.
     * Supports ALL ways of pasting English & Urdu:
     * 1) Same line with or without separator ("What is atom? ایٹم کیا ہے؟" or "What is atom? | ایٹم کیا ہے؟")
     * 2) Alternating lines (English line followed by Urdu line, or Urdu line followed by English line)
     * 3) Block of English questions (1..N) followed by Block of Urdu questions (1..N) in the same paste
     * 4) Pure English lines or Pure Urdu lines
     */
    fun parseShortOrLongQuestions(
        rawText: String,
        chapter: ChapterEntity,
        type: QuestionType,
        marksPerQuestion: Int = type.defaultMarks,
        startOrder: Int = 1
    ): List<QuestionEntity> {
        val rawLines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !isSectionHeadingLine(it) }

        if (rawLines.isEmpty()) return emptyList()

        data class SplitLine(val en: String, val ur: String)

        val splitLines = rawLines.mapNotNull { line ->
            val (en, ur) = splitBilingualText(line, isOption = false)
            if (en.isBlank() && ur.isBlank()) null else SplitLine(en, ur)
        }

        if (splitLines.isEmpty()) return emptyList()

        val pairedQuestions = mutableListOf<Pair<String, String>>()
        val pendingPureEn = mutableListOf<String>()
        val pendingPureUr = mutableListOf<String>()

        fun flushPending() {
            val count = maxOf(pendingPureEn.size, pendingPureUr.size)
            for (k in 0 until count) {
                val en = pendingPureEn.getOrElse(k) { "" }
                val ur = pendingPureUr.getOrElse(k) { "" }
                pairedQuestions.add(en to ur)
            }
            pendingPureEn.clear()
            pendingPureUr.clear()
        }

        for (item in splitLines) {
            when {
                item.en.isNotBlank() && item.ur.isNotBlank() -> {
                    flushPending()
                    pairedQuestions.add(item.en to item.ur)
                }
                item.en.isNotBlank() -> {
                    pendingPureEn.add(item.en)
                }
                item.ur.isNotBlank() -> {
                    pendingPureUr.add(item.ur)
                }
            }
        }
        flushPending()

        var order = startOrder
        return pairedQuestions.map { (qEn, qUr) ->
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
        }
    }

    private data class McqDraft(
        val qEn: String = "",
        val qUr: String = "",
        val aEn: String = "",
        val bEn: String = "",
        val cEn: String = "",
        val dEn: String = "",
        val aUr: String = "",
        val bUr: String = "",
        val cUr: String = "",
        val dUr: String = "",
        val correct: String = "A"
    ) {
        val hasEn: Boolean get() = qEn.isNotBlank()
        val hasUr: Boolean get() = qUr.isNotBlank()
        val isBilingual: Boolean get() = hasEn && hasUr
    }

    private fun parseAnswerLetter(raw: String): String {
        val trimmed = raw.trim()
        val upper = trimmed.uppercase()
        for (ch in upper) {
            if (ch in listOf('A', 'B', 'C', 'D')) return ch.toString()
        }
        return when {
            trimmed.contains("الف") || trimmed.contains("ا") -> "A"
            trimmed.contains("ب") -> "B"
            trimmed.contains("ج") -> "C"
            trimmed.contains("د") -> "D"
            else -> "A"
        }
    }

    private data class InlineOptionsExtraction(
        val questionPrefix: String,
        val options: List<String>,
        val isUrduMarkers: Boolean,
        val detectedAnswer: String?
    )

    private fun extractInlineOptionsFromLine(line: String): InlineOptionsExtraction? {
        var workingLine = line.trim()
        var detectedAns: String? = null
        val ansMatch = trailingInlineAnswerRegex.find(workingLine)
        if (ansMatch != null) {
            val rawAns = ansMatch.groupValues[1].ifBlank { ansMatch.groupValues[2] }
            detectedAns = parseAnswerLetter(rawAns)
            workingLine = workingLine.substring(0, ansMatch.range.first).trim()
        }

        val urMatch = urInlineOptionsRegex.find(workingLine)
        val enMatch = enInlineOptionsRegex.find(workingLine)

        val chosenMatch = when {
            urMatch != null && enMatch != null -> {
                if (urMatch.range.first <= enMatch.range.first) urMatch else enMatch
            }
            urMatch != null -> urMatch
            enMatch != null -> enMatch
            else -> null
        } ?: return null

        val isUr = (chosenMatch === urMatch)
        val prefix = workingLine.substring(0, chosenMatch.range.first).trim()
        val opts = listOf(
            chosenMatch.groupValues[1].trim(),
            chosenMatch.groupValues[2].trim(),
            chosenMatch.groupValues[3].trim(),
            chosenMatch.groupValues[4].trim()
        )
        return InlineOptionsExtraction(
            questionPrefix = prefix,
            options = opts,
            isUrduMarkers = isUr,
            detectedAnswer = detectedAns
        )
    }

    /**
     * Parses bulk pasted text into MCQ QuestionEntity items.
     * Supports:
     * 1) Pipe-separated single-line MCQs (English, Urdu, Bilingual ::, or English block + Urdu block)
     * 2) Multi-line or Single-line MCQs with (A)(B)(C)(D) and/or (الف)(ب)(ج)(د)
     * 3) Same-line English+Urdu MCQs, Alternating English+Urdu MCQs, or Block of English MCQs + Block of Urdu MCQs!
     */
    fun parseMcqQuestions(
        rawText: String,
        chapter: ChapterEntity,
        marksPerMcq: Int = 1,
        startOrder: Int = 1
    ): List<QuestionEntity> {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !isSectionHeadingLine(it) }
        if (lines.isEmpty()) return emptyList()

        val drafts = mutableListOf<McqDraft>()

        // Check if pipe-delimited single-line MCQs
        val pipeLines = lines.filter { it.count { c -> c == '|' } >= 2 }
        if (pipeLines.size == lines.size) {
            for (line in pipeLines) {
                val parts = line.split("|").map { it.trim() }
                val (qEn, qUr) = splitBilingualText(parts.getOrElse(0) { "" }, isOption = false)
                val (a1En, a1Ur) = splitBilingualText(parts.getOrElse(1) { "Option A" }, isOption = true)
                val (b1En, b1Ur) = splitBilingualText(parts.getOrElse(2) { "Option B" }, isOption = true)
                val (c1En, c1Ur) = splitBilingualText(parts.getOrElse(3) { "Option C" }, isOption = true)
                val (d1En, d1Ur) = splitBilingualText(parts.getOrElse(4) { "Option D" }, isOption = true)
                val correct = parseAnswerLetter(parts.getOrElse(5) { "A" })

                val isUrOnlyStem = qUr.isNotBlank() && qEn.isBlank()
                val isEnOnlyStem = qEn.isNotBlank() && qUr.isBlank()

                drafts.add(
                    McqDraft(
                        qEn = qEn,
                        qUr = qUr,
                        aEn = if (isUrOnlyStem) "" else a1En.ifBlank { a1Ur },
                        bEn = if (isUrOnlyStem) "" else b1En.ifBlank { b1Ur },
                        cEn = if (isUrOnlyStem) "" else c1En.ifBlank { c1Ur },
                        dEn = if (isUrOnlyStem) "" else d1En.ifBlank { d1Ur },
                        aUr = if (isEnOnlyStem) "" else a1Ur.ifBlank { a1En },
                        bUr = if (isEnOnlyStem) "" else b1Ur.ifBlank { b1En },
                        cUr = if (isEnOnlyStem) "" else c1Ur.ifBlank { c1En },
                        dUr = if (isEnOnlyStem) "" else d1Ur.ifBlank { d1En },
                        correct = correct
                    )
                )
            }
            return finalizeMcqDrafts(drafts, chapter, marksPerMcq, startOrder)
        }

        var idx = 0
        while (idx < lines.size) {
            val currentLine = lines[idx]

            // Skip standalone Answer lines if encountered out of sync
            if (answerLineRegex.containsMatchIn(currentLine) && drafts.isNotEmpty()) {
                val ansMatch = answerLineRegex.find(currentLine)
                val ans = parseAnswerLetter(ansMatch?.groupValues?.get(1) ?: "A")
                val last = drafts.removeAt(drafts.lastIndex)
                drafts.add(last.copy(correct = ans))
                idx++
                continue
            }

            var qEn = ""
            var qUr = ""
            var rawOptsEnOrMixed: List<String>? = null
            var rawOptsUr: List<String>? = null
            var correct = "A"

            // Case 1: The question line ITSELF also has inline options on the same line!
            // e.g. "1. Which gas is lightest? (A) Hydrogen (B) Oxygen (C) Nitrogen (D) Carbon"
            val sameLineExtraction = extractInlineOptionsFromLine(currentLine)
            if (sameLineExtraction != null && sameLineExtraction.questionPrefix.isNotBlank()) {
                val (stemEn, stemUr) = splitBilingualText(sameLineExtraction.questionPrefix, isOption = false)
                qEn = stemEn
                qUr = stemUr
                if (sameLineExtraction.detectedAnswer != null) {
                    correct = sameLineExtraction.detectedAnswer
                }
                if (sameLineExtraction.isUrduMarkers || (qUr.isNotBlank() && qEn.isBlank())) {
                    rawOptsUr = sameLineExtraction.options
                } else {
                    rawOptsEnOrMixed = sameLineExtraction.options
                }
                idx++
            } else {
                // Standard question stem line
                val (stem1En, stem1Ur) = splitBilingualText(currentLine, isOption = false)
                qEn = stem1En
                qUr = stem1Ur
                idx++

                // Check if the very next line is the translation of the question stem
                // (i.e. not an option line and not an answer line, and provides the missing language)
                if (idx < lines.size && (qEn.isBlank() != qUr.isBlank())) {
                    val nextLine = lines[idx]
                    val nextInline = extractInlineOptionsFromLine(nextLine)
                    val isNextSingleOpt = optionSinglePrefixRegex.containsMatchIn(nextLine)
                    val isNextAns = answerLineRegex.containsMatchIn(nextLine)
                    if (nextInline == null && !isNextSingleOpt && !isNextAns) {
                        val (stem2En, stem2Ur) = splitBilingualText(nextLine, isOption = false)
                        if (qEn.isBlank() && stem2En.isNotBlank() && stem2Ur.isBlank()) {
                            qEn = stem2En
                            idx++
                        } else if (qUr.isBlank() && stem2Ur.isNotBlank() && stem2En.isBlank()) {
                            qUr = stem2Ur
                            idx++
                        }
                    }
                }
            }

            // Collect options if not yet collected (or if second language options follow on the next line)
            fun tryCollectOptionsBlock() {
                if (idx >= lines.size) return
                val inlineExt = extractInlineOptionsFromLine(lines[idx])
                if (inlineExt != null && inlineExt.questionPrefix.isBlank()) {
                    if (inlineExt.detectedAnswer != null) correct = inlineExt.detectedAnswer
                    if (inlineExt.isUrduMarkers) {
                        if (rawOptsUr == null) rawOptsUr = inlineExt.options
                        else if (rawOptsEnOrMixed == null) rawOptsEnOrMixed = inlineExt.options
                    } else {
                        if (rawOptsEnOrMixed == null) rawOptsEnOrMixed = inlineExt.options
                        else if (rawOptsUr == null) rawOptsUr = inlineExt.options
                    }
                    idx++
                } else if (optionSinglePrefixRegex.containsMatchIn(lines[idx])) {
                    val collected = mutableListOf<String>()
                    while (idx < lines.size && collected.size < 4 && optionSinglePrefixRegex.containsMatchIn(lines[idx])) {
                        val m = optionSinglePrefixRegex.find(lines[idx])
                        collected.add(m?.groupValues?.get(1)?.trim() ?: lines[idx])
                        idx++
                    }
                    if (collected.isNotEmpty()) {
                        val hasUrOpts = collected.any { containsUrdu(it) }
                        val hasEnOpts = collected.any { containsEnglish(it) }
                        if (hasUrOpts && !hasEnOpts) {
                            if (rawOptsUr == null) rawOptsUr = collected
                            else if (rawOptsEnOrMixed == null) rawOptsEnOrMixed = collected
                        } else {
                            if (rawOptsEnOrMixed == null) rawOptsEnOrMixed = collected
                            else if (rawOptsUr == null) rawOptsUr = collected
                        }
                    }
                }
            }

            tryCollectOptionsBlock()
            // Check if a second options block (in the other language) immediately follows!
            tryCollectOptionsBlock()

            // Check for Answer line after options
            if (idx < lines.size && answerLineRegex.containsMatchIn(lines[idx])) {
                val ansMatch = answerLineRegex.find(lines[idx])
                correct = parseAnswerLetter(ansMatch?.groupValues?.get(1) ?: "A")
                idx++
            }

            // Resolve option strings into En & Ur
            var aEn = ""
            var bEn = ""
            var cEn = ""
            var dEn = ""
            var aUr = ""
            var bUr = ""
            var cUr = ""
            var dUr = ""

            if (rawOptsEnOrMixed != null && rawOptsUr != null) {
                aEn = cleanOptionPrefix(rawOptsEnOrMixed!!.getOrElse(0) { "Option A" })
                bEn = cleanOptionPrefix(rawOptsEnOrMixed!!.getOrElse(1) { "Option B" })
                cEn = cleanOptionPrefix(rawOptsEnOrMixed!!.getOrElse(2) { "Option C" })
                dEn = cleanOptionPrefix(rawOptsEnOrMixed!!.getOrElse(3) { "Option D" })

                aUr = cleanOptionPrefix(rawOptsUr!!.getOrElse(0) { aEn })
                bUr = cleanOptionPrefix(rawOptsUr!!.getOrElse(1) { bEn })
                cUr = cleanOptionPrefix(rawOptsUr!!.getOrElse(2) { cEn })
                cUr = cleanOptionPrefix(rawOptsUr!!.getOrElse(2) { cEn })
                dUr = cleanOptionPrefix(rawOptsUr!!.getOrElse(3) { dEn })
            } else {
                val singleList = rawOptsEnOrMixed ?: rawOptsUr ?: listOf("Option A", "Option B", "Option C", "Option D")
                val (sAEn, sAUr) = splitBilingualText(singleList.getOrElse(0) { "Option A" }, isOption = true)
                val (sBEn, sBUr) = splitBilingualText(singleList.getOrElse(1) { "Option B" }, isOption = true)
                val (sCEn, sCUr) = splitBilingualText(singleList.getOrElse(2) { "Option C" }, isOption = true)
                val (sDEn, sDUr) = splitBilingualText(singleList.getOrElse(3) { "Option D" }, isOption = true)

                val isUrOnlyStem = qUr.isNotBlank() && qEn.isBlank()
                val isEnOnlyStem = qEn.isNotBlank() && qUr.isBlank()

                aEn = if (isUrOnlyStem) "" else sAEn.ifBlank { sAUr }
                bEn = if (isUrOnlyStem) "" else sBEn.ifBlank { sBUr }
                cEn = if (isUrOnlyStem) "" else sCEn.ifBlank { sCUr }
                dEn = if (isUrOnlyStem) "" else sDEn.ifBlank { sDUr }

                aUr = if (isEnOnlyStem) "" else sAUr.ifBlank { sAEn }
                bUr = if (isEnOnlyStem) "" else sBUr.ifBlank { sBEn }
                cUr = if (isEnOnlyStem) "" else sCUr.ifBlank { sCEn }
                dUr = if (isEnOnlyStem) "" else sDUr.ifBlank { sDEn }
            }

            if (qEn.isNotBlank() || qUr.isNotBlank()) {
                drafts.add(
                    McqDraft(
                        qEn = qEn,
                        qUr = qUr,
                        aEn = aEn,
                        bEn = bEn,
                        cEn = cEn,
                        dEn = dEn,
                        aUr = aUr,
                        bUr = bUr,
                        cUr = cUr,
                        dUr = dUr,
                        correct = correct
                    )
                )
            }
        }

        return finalizeMcqDrafts(drafts, chapter, marksPerMcq, startOrder)
    }

    /**
     * Pairs pure-English MCQ drafts and pure-Urdu MCQ drafts (whether alternating or in two blocks)
     * into unified Bilingual QuestionEntity objects.
     */
    private fun finalizeMcqDrafts(
        drafts: List<McqDraft>,
        chapter: ChapterEntity,
        marksPerMcq: Int,
        startOrder: Int
    ): List<QuestionEntity> {
        val merged = mutableListOf<McqDraft>()
        val pendingEn = mutableListOf<McqDraft>()
        val pendingUr = mutableListOf<McqDraft>()

        fun flushPending() {
            val count = maxOf(pendingEn.size, pendingUr.size)
            for (k in 0 until count) {
                val enD = pendingEn.getOrElse(k) { null }
                val urD = pendingUr.getOrElse(k) { null }
                when {
                    enD != null && urD != null -> {
                        merged.add(
                            McqDraft(
                                qEn = enD.qEn,
                                qUr = urD.qUr,
                                aEn = enD.aEn,
                                bEn = enD.bEn,
                                cEn = enD.cEn,
                                dEn = enD.dEn,
                                aUr = urD.aUr.ifBlank { enD.aEn },
                                bUr = urD.bUr.ifBlank { enD.bEn },
                                cUr = urD.cUr.ifBlank { enD.cEn },
                                dUr = urD.dUr.ifBlank { enD.dEn },
                                correct = if (enD.correct != "A") enD.correct else urD.correct
                            )
                        )
                    }
                    enD != null -> merged.add(enD)
                    urD != null -> merged.add(urD)
                }
            }
            pendingEn.clear()
            pendingUr.clear()
        }

        for (d in drafts) {
            when {
                d.isBilingual -> {
                    flushPending()
                    merged.add(d)
                }
                d.hasEn -> pendingEn.add(d)
                d.hasUr -> pendingUr.add(d)
            }
        }
        flushPending()

        var order = startOrder
        return merged.map { d ->
            QuestionEntity(
                id = "${chapter.id}_mcq_${UUID.randomUUID().toString().take(8)}",
                chapterId = chapter.id,
                subjectId = chapter.subjectId,
                classLevel = chapter.classLevel,
                type = QuestionType.MCQ.code,
                questionEn = d.qEn,
                questionUr = d.qUr,
                optionAEn = d.aEn,
                optionBEn = d.bEn,
                optionCEn = d.cEn,
                optionDEn = d.dEn,
                optionAUr = d.aUr,
                optionBUr = d.bUr,
                optionCUr = d.cUr,
                optionDUr = d.dUr,
                correctOption = d.correct,
                marks = marksPerMcq,
                sortOrder = order++
            )
        }
    }

    /**
     * Normalizes & pairs a list of existing QuestionEntity items in a chapter+type:
     * 1) Splits any question that had English & Urdu mixed in the same field.
     * 2) Pairs pure-English and pure-Urdu questions in the same chapter+type into unified Bilingual items.
     */
    fun normalizeAndPairChapterQuestions(questions: List<QuestionEntity>): List<QuestionEntity> {
        if (questions.isEmpty()) return emptyList()

        val cleaned = questions.sortedBy { it.sortOrder }.map { q ->
            val enResolved = q.resolvedQuestionEn()
            val urResolved = q.resolvedQuestionUr()
            if (q.type == QuestionType.MCQ.code) {
                q.copy(
                    questionEn = enResolved,
                    questionUr = urResolved,
                    optionAEn = q.resolvedOptionAEn(),
                    optionBEn = q.resolvedOptionBEn(),
                    optionCEn = q.resolvedOptionCEn(),
                    optionDEn = q.resolvedOptionDEn(),
                    optionAUr = q.resolvedOptionAUr(),
                    optionBUr = q.resolvedOptionBUr(),
                    optionCUr = q.resolvedOptionCUr(),
                    optionDUr = q.resolvedOptionDUr()
                )
            } else {
                q.copy(
                    questionEn = enResolved,
                    questionUr = urResolved
                )
            }
        }

        val pureEn = cleaned.filter { it.questionEn.isNotBlank() && it.questionUr.isBlank() }
        val pureUr = cleaned.filter { it.questionUr.isNotBlank() && it.questionEn.isBlank() }

        // If there are both pure-English and pure-Urdu questions in the same chapter & type, pair them!
        if (pureEn.isEmpty() || pureUr.isEmpty()) {
            return cleaned
        }

        val bilingual = cleaned.filter { it.questionEn.isNotBlank() && it.questionUr.isNotBlank() }
        val paired = mutableListOf<QuestionEntity>()
        paired.addAll(bilingual)

        val maxPairs = maxOf(pureEn.size, pureUr.size)
        for (i in 0 until maxPairs) {
            val enQ = pureEn.getOrNull(i)
            val urQ = pureUr.getOrNull(i)
            when {
                enQ != null && urQ != null -> {
                    paired.add(
                        enQ.copy(
                            questionUr = urQ.questionUr,
                            optionAUr = urQ.optionAUr.ifBlank { enQ.optionAEn },
                            optionBUr = urQ.optionBUr.ifBlank { enQ.optionBEn },
                            optionCUr = urQ.optionCUr.ifBlank { enQ.optionCEn },
                            optionDUr = urQ.optionDUr.ifBlank { enQ.optionDEn }
                        )
                    )
                }
                enQ != null -> paired.add(enQ)
                urQ != null -> paired.add(urQ)
            }
        }

        return paired.mapIndexed { idx, q -> q.copy(sortOrder = idx + 1) }
    }
}
