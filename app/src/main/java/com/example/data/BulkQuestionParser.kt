package com.example.data

import java.util.UUID

object BulkQuestionParser {

    private val urduRegex = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")
    private val anyLatinRegex = Regex("[A-Za-z]")
    private val latinWordRegex = Regex("[A-Za-z]{2,}")

    private val leadingNumberRegex = Regex(
        "^\\s*(?:[•*\\-–—]+\\s*)?(?:Q(?:uestion)?[\\s.#\\-–—]*\\d+[.):\\-]*|سوال\\s*(?:نمبر)?[\\s.:\\-–—]*[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[.):\\-۔]*|س[\\s.:\\-–—]+[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[.):\\-۔]*|سوال\\s*[.):\\-۔]+|\\([\\d\\u06F0-\\u06F9\\u0660-\\u0669]+\\)|\\[[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+\\]|[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[.):\\-۔]|\\([ivxIVX]+\\)|[ivxIVX]+[.):\\-])\\s*",
        RegexOption.IGNORE_CASE
    )

    private val trailingMarksRegex = Regex(
        "\\s*(?:\\[\\s*\\d+\\s*(?:Marks|نمبر)?\\s*\\]|\\(\\s*\\d+\\s*(?:Marks|نمبر)\\s*\\))\\s*$",
        RegexOption.IGNORE_CASE
    )

    private val trailingOrphanNumberBeforeUrduRegex = Regex(
        "\\s*(?:Q(?:uestion)?\\.?\\s*#?\\s*\\d+[.):\\-]?|سوال\\s*(?:نمبر)?[\\s.:\\-–—]*[\\d\\u06F0-\\u06F9\\u0660-\\u0669]*[.):\\-۔]?|س[\\s.:\\-–—]+[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[.):\\-۔]?|\\(?[\\d\\u06F0-\\u06F9\\u0660-\\u0669]+[.):\\-]?|\\([ivxIVX]+\\)|\\([A-Da-d]\\)|[/|:\\-–—]|\\()\\s*$",
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
        "^\\s*(?:Ans(?:wer)?|Correct(?:\\s*Option)?|Key|درست\\s*جواب|جواب)\\s*[=:\\-]?\\s*(.+)$",
        RegexOption.IGNORE_CASE
    )

    private val trailingInlineAnswerRegex = Regex(
        "\\s*(?:\\[?\\s*(?:Ans(?:wer)?|Correct|جواب)\\s*[=:\\-]\\s*([A-Da-d]|الف|ا|ب|ج|د)\\s*\\]?|\\(\\s*(?:Ans|جواب)\\s*[=:\\-]?\\s*([A-Da-d]|الف|ا|ب|ج|د)\\s*\\)|\\|\\s*([A-Da-d]|الف|ا|ب|ج|د))\\s*$",
        RegexOption.IGNORE_CASE
    )

    private val urduEnglishGlossRegex = Regex(
        "([\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF]+)\\s*\\(\\s*[A-Za-z][A-Za-z0-9\\s\\-/.,']*\\)"
    )

    fun containsUrdu(text: String): Boolean = urduRegex.containsMatchIn(text)

    fun containsEnglish(text: String): Boolean = anyLatinRegex.containsMatchIn(text)

    /**
     * Strips redundant parenthetical English glosses from Urdu text (e.g. "گیسوں کے تبادلے (Gaseous exchange)" -> "گیسوں کے تبادلے")
     * so Urdu Medium papers show pure Urdu without mixed English words.
     */
    fun stripEnglishGlossFromUrdu(urduText: String): String {
        if (!containsUrdu(urduText)) return urduText.trim()
        return urduText
            .replace(urduEnglishGlossRegex, "$1")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

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
     * - If the string is pure Urdu (even with inline chemical/physics/biology terms like pH, SI, C,
     *   or parenthetical glosses like "(Gaseous exchange)" or "(microvilli)"),
     *   returns Pair("", cleanUrdu).
     */
    fun splitBilingualText(raw: String, isOption: Boolean = false): Pair<String, String> {
        val cleanFn: (String) -> String = { s ->
            if (isOption) cleanOptionPrefix(s) else cleanLeadingNumber(s)
        }
        val trimmed = cleanFn(raw.trim())
        if (trimmed.isBlank()) return "" to ""

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
                val ur = stripEnglishGlossFromUrdu(cleanFn(urParts.joinToString(" ")))
                return en to ur
            }
            if (urParts.isEmpty() && enParts.isNotEmpty()) {
                return cleanFn(enParts.first()) to ""
            }
            if (enParts.isEmpty() && urParts.isNotEmpty()) {
                return "" to stripEnglishGlossFromUrdu(cleanFn(urParts.first()))
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
                        return cleanFn(parts[0]) to stripEnglishGlossFromUrdu(cleanFn(parts[1]))
                    } else if (p0Ur && !p1Ur && containsEnglish(parts[1])) {
                        return cleanFn(parts[1]) to stripEnglishGlossFromUrdu(cleanFn(parts[0]))
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
            if (beforeHasEn && !afterHasEn && !rawBeforeUrdu.startsWith("(") && !rawFromFirstUrdu.startsWith("(")) {
                val en = cleanOptionPrefix(rawBeforeUrdu)
                val ur = stripEnglishGlossFromUrdu(cleanOptionPrefix(rawFromFirstUrdu))
                if (en.isNotBlank() && ur.isNotBlank()) return en to ur
            }
            if (afterHasEn && !beforeHasEn && !rawAfterLastUrdu.startsWith("(")) {
                val ur = stripEnglishGlossFromUrdu(cleanOptionPrefix(rawUpToLastUrdu))
                val en = cleanOptionPrefix(rawAfterLastUrdu)
                if (en.isNotBlank() && ur.isNotBlank()) return en to ur
            }
        } else {
            // Question stem: ensure English part is an actual full English question/sentence
            // and NOT an inline technical term or parenthetical gloss inside an Urdu sentence.
            if (beforeHasEn && !afterHasEn) {
                val candidateEn = cleanLeadingNumber(rawBeforeUrdu)
                val wordCount = latinWordRegex.findAll(candidateEn).count()
                val endsWithPunct = candidateEn.lastOrNull() in listOf('?', '.', '!', ':', ';')
                if (wordCount >= 3 || (wordCount >= 2 && endsWithPunct)) {
                    val candidateUr = stripEnglishGlossFromUrdu(cleanLeadingNumber(rawFromFirstUrdu))
                    if (candidateEn.isNotBlank() && candidateUr.isNotBlank()) {
                        return candidateEn to candidateUr
                    }
                }
            }
            if (afterHasEn && !beforeHasEn && !rawAfterLastUrdu.startsWith("(") && !rawAfterLastUrdu.endsWith(")")) {
                val candidateEn = cleanLeadingNumber(rawAfterLastUrdu)
                val wordCount = latinWordRegex.findAll(candidateEn).count()
                val endsWithPunct = candidateEn.lastOrNull() in listOf('?', '.', '!', ':', ';')
                if (wordCount >= 3 || (wordCount >= 2 && endsWithPunct)) {
                    val candidateUr = stripEnglishGlossFromUrdu(cleanLeadingNumber(rawUpToLastUrdu))
                    if (candidateEn.isNotBlank() && candidateUr.isNotBlank()) {
                        return candidateEn to candidateUr
                    }
                }
            }
        }

        // If mostly English (e.g. an English question referencing an Urdu literary title in quotes/parens),
        // treat as English; otherwise treat as Urdu question with inline English term (e.g. "(Gaseous exchange)", "HCl", "SI").
        val urduCharCount = allUrduMatches.size
        val latinCharCount = anyLatinRegex.findAll(trimmed).count()
        return if (latinCharCount > urduCharCount * 2) {
            cleanFn(trimmed.replace(urduRegex, "").replace(Regex("\\s+"), " ").trim()) to ""
        } else {
            "" to stripEnglishGlossFromUrdu(cleanFn(trimmed))
        }
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
            val finalEn = qEn.ifBlank {
                if (qUr.isNotBlank()) {
                    UrduEnglishAutoTranslator.translateUrduToEnglishOffline(qUr, isOption = false)
                } else ""
            }
            val finalUr = qUr.ifBlank {
                if (qEn.isNotBlank()) {
                    UrduEnglishAutoTranslator.translateEnglishToUrduOffline(qEn, isOption = false)
                } else ""
            }
            QuestionEntity(
                id = "${chapter.id}_${type.code.lowercase()}_${UUID.randomUUID().toString().take(8)}",
                chapterId = chapter.id,
                subjectId = chapter.subjectId,
                classLevel = chapter.classLevel,
                type = type.code,
                questionEn = finalEn,
                questionUr = finalUr,
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
            val rawAns = ansMatch.groupValues[1]
                .ifBlank { ansMatch.groupValues[2] }
                .ifBlank { ansMatch.groupValues[3] }
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
     * into unified Bilingual QuestionEntity objects, and auto-translates Urdu-only MCQs into English.
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
            val finalQEn = d.qEn.ifBlank {
                if (d.qUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(d.qUr, isOption = false) else ""
            }
            val finalQUr = d.qUr.ifBlank {
                if (d.qEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(d.qEn, isOption = false) else ""
            }
            val finalAEn = d.aEn.ifBlank {
                if (d.aUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(d.aUr, isOption = true) else ""
            }
            val finalBEn = d.bEn.ifBlank {
                if (d.bUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(d.bUr, isOption = true) else ""
            }
            val finalCEn = d.cEn.ifBlank {
                if (d.cUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(d.cUr, isOption = true) else ""
            }
            val finalDEn = d.dEn.ifBlank {
                if (d.dUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(d.dUr, isOption = true) else ""
            }
            val finalAUr = d.aUr.ifBlank {
                if (d.aEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(d.aEn, isOption = true) else ""
            }
            val finalBUr = d.bUr.ifBlank {
                if (d.bEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(d.bEn, isOption = true) else ""
            }
            val finalCUr = d.cUr.ifBlank {
                if (d.cEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(d.cEn, isOption = true) else ""
            }
            val finalDUr = d.dUr.ifBlank {
                if (d.dEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(d.dEn, isOption = true) else ""
            }
            QuestionEntity(
                id = "${chapter.id}_mcq_${UUID.randomUUID().toString().take(8)}",
                chapterId = chapter.id,
                subjectId = chapter.subjectId,
                classLevel = chapter.classLevel,
                type = QuestionType.MCQ.code,
                questionEn = finalQEn,
                questionUr = finalQUr,
                optionAEn = finalAEn,
                optionBEn = finalBEn,
                optionCEn = finalCEn,
                optionDEn = finalDEn,
                optionAUr = finalAUr,
                optionBUr = finalBUr,
                optionCUr = finalCUr,
                optionDUr = finalDUr,
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
     * 3) Auto-generates English Medium for any Urdu-only questions AND Urdu Medium for any English-only questions.
     */
    fun normalizeAndPairChapterQuestions(questions: List<QuestionEntity>): List<QuestionEntity> {
        if (questions.isEmpty()) return emptyList()

        fun rawEn(q: QuestionEntity): String {
            val (en1, _) = splitBilingualText(q.questionEn, isOption = false)
            if (en1.isNotBlank()) return en1
            val (en2, _) = splitBilingualText(q.questionUr, isOption = false)
            return en2
        }

        fun rawUr(q: QuestionEntity): String {
            val (_, ur1) = splitBilingualText(q.questionUr, isOption = false)
            if (ur1.isNotBlank()) return ur1
            val (_, ur2) = splitBilingualText(q.questionEn, isOption = false)
            return ur2
        }

        fun rawOptEn(optEn: String, optUr: String): String {
            val (en1, _) = splitBilingualText(optEn, isOption = true)
            if (en1.isNotBlank()) return en1
            val (en2, _) = splitBilingualText(optUr, isOption = true)
            if (en2.isNotBlank()) return en2
            val fallback = optEn.ifBlank { optUr }.trim()
            return if (!containsUrdu(fallback)) fallback else ""
        }

        fun rawOptUr(optEn: String, optUr: String): String {
            val (_, ur1) = splitBilingualText(optUr, isOption = true)
            if (ur1.isNotBlank()) return ur1
            val (_, ur2) = splitBilingualText(optEn, isOption = true)
            if (ur2.isNotBlank()) return ur2
            return ""
        }

        val cleaned = questions.sortedBy { it.sortOrder }.map { q ->
            val enPart = rawEn(q)
            val urPart = rawUr(q)
            if (q.type == QuestionType.MCQ.code) {
                q.copy(
                    questionEn = enPart,
                    questionUr = urPart,
                    optionAEn = rawOptEn(q.optionAEn, q.optionAUr),
                    optionBEn = rawOptEn(q.optionBEn, q.optionBUr),
                    optionCEn = rawOptEn(q.optionCEn, q.optionCUr),
                    optionDEn = rawOptEn(q.optionDEn, q.optionDUr),
                    optionAUr = rawOptUr(q.optionAEn, q.optionAUr),
                    optionBUr = rawOptUr(q.optionBEn, q.optionBUr),
                    optionCUr = rawOptUr(q.optionCEn, q.optionCUr),
                    optionDUr = rawOptUr(q.optionDEn, q.optionDUr)
                )
            } else {
                q.copy(
                    questionEn = enPart,
                    questionUr = urPart
                )
            }
        }

        val pureEn = cleaned.filter { it.questionEn.isNotBlank() && it.questionUr.isBlank() }
        val pureUr = cleaned.filter { it.questionUr.isNotBlank() && it.questionEn.isBlank() }

        val pairedList = if (pureEn.isNotEmpty() && pureUr.isNotEmpty()) {
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
            paired
        } else {
            cleaned
        }

        // Now ensure any question that has Urdu but no English gets auto-translated into English,
        // and any question that has English but no Urdu gets auto-translated into Urdu!
        return pairedList.mapIndexed { idx, q ->
            val autoQEn = q.questionEn.ifBlank {
                if (q.questionUr.isNotBlank()) {
                    UrduEnglishAutoTranslator.translateUrduToEnglishOffline(q.questionUr, isOption = false)
                } else ""
            }
            val autoQUr = q.questionUr.ifBlank {
                if (q.questionEn.isNotBlank()) {
                    UrduEnglishAutoTranslator.translateEnglishToUrduOffline(q.questionEn, isOption = false)
                } else ""
            }
            if (q.type == QuestionType.MCQ.code) {
                val aEn = q.optionAEn.ifBlank {
                    if (q.optionAUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(q.optionAUr, isOption = true) else ""
                }
                val bEn = q.optionBEn.ifBlank {
                    if (q.optionBUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(q.optionBUr, isOption = true) else ""
                }
                val cEn = q.optionCEn.ifBlank {
                    if (q.optionCUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(q.optionCUr, isOption = true) else ""
                }
                val dEn = q.optionDEn.ifBlank {
                    if (q.optionDUr.isNotBlank()) UrduEnglishAutoTranslator.translateUrduToEnglishOffline(q.optionDUr, isOption = true) else ""
                }
                val aUr = q.optionAUr.ifBlank {
                    if (aEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(aEn, isOption = true) else ""
                }
                val bUr = q.optionBUr.ifBlank {
                    if (bEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(bEn, isOption = true) else ""
                }
                val cUr = q.optionCUr.ifBlank {
                    if (cEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(cEn, isOption = true) else ""
                }
                val dUr = q.optionDUr.ifBlank {
                    if (dEn.isNotBlank()) UrduEnglishAutoTranslator.translateEnglishToUrduOffline(dEn, isOption = true) else ""
                }
                q.copy(
                    questionEn = autoQEn,
                    questionUr = autoQUr,
                    optionAEn = aEn,
                    optionBEn = bEn,
                    optionCEn = cEn,
                    optionDEn = dEn,
                    optionAUr = aUr,
                    optionBUr = bUr,
                    optionCUr = cUr,
                    optionDUr = dUr,
                    sortOrder = idx + 1
                )
            } else {
                q.copy(
                    questionEn = autoQEn,
                    questionUr = autoQUr,
                    sortOrder = idx + 1
                )
            }
        }
    }
}
