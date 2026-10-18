package com.example.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import com.example.data.BulkQuestionParser
import com.example.data.UrduEnglishAutoTranslator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Data structure representing an extracted question draft from a PDF or notes document.
 */
data class ExtractedQuestionDraft(
    val chapterNumber: Int = 1,
    val chapterTitleEn: String = "",
    val chapterTitleUr: String = "",
    val type: String = "SHORT", // "MCQ", "SHORT", "LONG"
    val questionEn: String = "",
    val questionUr: String = "",
    val optionAEn: String = "",
    val optionBEn: String = "",
    val optionCEn: String = "",
    val optionDEn: String = "",
    val optionAUr: String = "",
    val optionBUr: String = "",
    val optionCUr: String = "",
    val optionDUr: String = "",
    val correctOption: String = "A",
    val marks: Int = 2
)

/**
 * Engine for automated extraction of MCQs, Short Questions, and Long Questions
 * from PDF documents and textbook notes using Google Gemini AI and offline rule parsing.
 */
object GeminiPdfQuestionExtractor {

    private const val TAG = "GeminiPdfExtractor"
    private const val GEMINI_MODEL = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Extracts questions from a PDF Uri by rendering pages and sending them to Gemini Vision AI.
     */
    suspend fun extractFromPdfUriViaGemini(
        context: Context,
        pdfUri: Uri,
        apiKey: String,
        onProgress: ((currentPage: Int, totalPages: Int, foundQuestions: Int) -> Unit)? = null
    ): Result<List<ExtractedQuestionDraft>> = withContext(Dispatchers.IO) {
        try {
            var pfd: ParcelFileDescriptor? = null
            try {
                pfd = context.contentResolver.openFileDescriptor(pdfUri, "r")
            } catch (e: Exception) {
                Log.w(TAG, "Cannot open as ParcelFileDescriptor, fallback to stream: ${e.message}")
            }

            if (pfd != null) {
                val allExtracted = mutableListOf<ExtractedQuestionDraft>()
                var renderer: PdfRenderer? = null
                try {
                    renderer = PdfRenderer(pfd)
                    val totalPages = renderer.pageCount
                    val maxPagesToProcess = minOf(totalPages, 20)

                    for (pageIdx in 0 until maxPagesToProcess) {
                        val page = renderer.openPage(pageIdx)
                        // Scale slightly for clear OCR while keeping payload within limits (max width 1200)
                        val scale = if (page.width > 1200) 1200f / page.width else 1.0f
                        val targetW = (page.width * scale).toInt()
                        val targetH = (page.height * scale).toInt()

                        val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        val stream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
                        bitmap.recycle()
                        val base64Image = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

                        onProgress?.invoke(pageIdx + 1, totalPages, allExtracted.size)

                        val pageQuestions = callGeminiVisionForPage(apiKey, base64Image, pageIdx + 1)
                        allExtracted.addAll(pageQuestions)
                    }

                    if (allExtracted.isNotEmpty()) {
                        return@withContext Result.success(allExtracted)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "PdfRenderer vision failed, falling back to stream text: ${e.message}")
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd.close() } catch (_: Exception) {}
                }
            }

            // Fallback: extract text stream and send to text model
            val text = extractTextFromPdfStream(context, pdfUri)
            if (text.isNotBlank()) {
                return@withContext extractFromTextViaGemini(apiKey, text)
            }

            Result.failure(Exception("Unable to read content from the selected PDF file."))
        } catch (e: Exception) {
            Log.e(TAG, "extractFromPdfUriViaGemini error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Extracts questions from raw notes text using Gemini AI.
     */
    suspend fun extractFromTextViaGemini(
        apiKey: String,
        text: String
    ): Result<List<ExtractedQuestionDraft>> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val systemPrompt = """
                You are an expert bilingual Pakistani board examination paper parser and curriculum specialist.
                Analyze the provided textbook or notes text and extract ALL exam questions with extreme thoroughness into a JSON array.
                
                Strict Rules:
                1. Extract ALL Multiple Choice Questions (MCQs), Short Questions (مختصر سوالات), and Long/Detailed Questions (تفصیلی سوالات). Do NOT skip any question.
                2. For every question, ALWAYS provide BOTH English ('questionEn') and Urdu ('questionUr'). If source has only Urdu, accurately translate to English. If source has only English, accurately translate to Urdu.
                3. For MCQs, extract 4 options in English and 4 options in Urdu, and correctOption ('A', 'B', 'C', or 'D').
                4. Detect chapter number ('chapterNumber', integer >= 1) and chapter titles ('chapterTitleEn', 'chapterTitleUr'). Default to 1 if not indicated.
                5. 'type' must be one of: "MCQ", "SHORT", "LONG".
                
                Return JSON matching this exact structure:
                {
                  "questions": [
                    {
                      "chapterNumber": 1,
                      "chapterTitleEn": "Fundamentals of Chemistry",
                      "chapterTitleUr": "کیمسٹری کے بنیادی اصول",
                      "type": "SHORT",
                      "questionEn": "Define Industrial Chemistry.",
                      "questionUr": "انڈسٹریل کیمسٹری کی تعریف کریں۔",
                      "optionAEn": "",
                      "optionBEn": "",
                      "optionCEn": "",
                      "optionDEn": "",
                      "optionAUr": "",
                      "optionBUr": "",
                      "optionCUr": "",
                      "optionDUr": "",
                      "correctOption": "A",
                      "marks": 2
                    }
                  ]
                }
            """.trimIndent()

            // If text is very long, split into chunks up to ~15,000 characters
            val chunks = text.chunked(15000)
            val allQuestions = mutableListOf<ExtractedQuestionDraft>()

            for (chunk in chunks) {
                val prompt = "$systemPrompt\n\nText to analyze:\n$chunk"
                val jsonPayload = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val contentObj = JSONObject().apply {
                            val partsArray = JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            }
                            put("parts", partsArray)
                        }
                        put(contentObj)
                    }
                    put("contents", contentsArray)

                    val genConfig = JSONObject().apply {
                        put("temperature", 0.2)
                        put("responseMimeType", "application/json")
                    }
                    put("generationConfig", genConfig)
                }

                val endpoint = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
                val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(endpoint)
                    .post(body)
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string() ?: ""
                        throw Exception("Gemini API error HTTP ${response.code}: $errBody")
                    }

                    val respBody = response.body?.string() ?: ""
                    val parsedList = parseGeminiJsonResponse(respBody)
                    allQuestions.addAll(parsedList)
                }
            }

            Result.success(allQuestions)
        } catch (e: Exception) {
            Log.e(TAG, "extractFromTextViaGemini error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Calls Gemini Vision on a single page JPEG image base64.
     */
    private fun callGeminiVisionForPage(
        apiKey: String,
        base64Jpeg: String,
        pageNumber: Int
    ): List<ExtractedQuestionDraft> {
        val prompt = """
            You are an expert bilingual Pakistani board examination paper parser.
            Examine this page (page #$pageNumber) of a textbook/notes document in detail.
            Extract all exam questions (MCQs, Short Questions, Long Questions) visible on this page into a structured JSON array.
            
            Strict Requirements:
            1. Extract every MCQ, Short Question, and Long Question.
            2. For each question, provide BOTH English ('questionEn') and Urdu ('questionUr').
            3. Detect the chapter number and title if present on this page, or infer from context (default to 1).
            4. 'type' must be "MCQ", "SHORT", or "LONG".
            
            Return JSON format:
            {
              "questions": [
                {
                  "chapterNumber": 1,
                  "chapterTitleEn": "Topic Name",
                  "chapterTitleUr": "عنوان",
                  "type": "MCQ",
                  "questionEn": "...",
                  "questionUr": "...",
                  "optionAEn": "...",
                  "optionBEn": "...",
                  "optionCEn": "...",
                  "optionDEn": "...",
                  "optionAUr": "...",
                  "optionBUr": "...",
                  "optionCUr": "...",
                  "optionDUr": "...",
                  "correctOption": "A",
                  "marks": 1
                }
              ]
            }
        """.trimIndent()

        val jsonPayload = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                        val inlineObj = JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Jpeg)
                        }
                        put(JSONObject().put("inlineData", inlineObj))
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val genConfig = JSONObject().apply {
                put("temperature", 0.2)
                put("responseMimeType", "application/json")
            }
            put("generationConfig", genConfig)
        }

        val endpoint = "$BASE_URL/$GEMINI_MODEL:generateContent?key=$apiKey"
        val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.w(TAG, "Gemini Vision page $pageNumber failed with code ${response.code}: $errBody")
                return emptyList()
            }

            val respBody = response.body?.string() ?: ""
            return parseGeminiJsonResponse(respBody)
        }
    }

    /**
     * Parses the response from Gemini generateContent into a List of ExtractedQuestionDraft.
     */
    private fun parseGeminiJsonResponse(responseJsonStr: String): List<ExtractedQuestionDraft> {
        val results = mutableListOf<ExtractedQuestionDraft>()
        try {
            val root = JSONObject(responseJsonStr)
            val candidates = root.optJSONArray("candidates") ?: return emptyList()
            if (candidates.length() == 0) return emptyList()

            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return emptyList()
            val parts = content.optJSONArray("parts") ?: return emptyList()
            if (parts.length() == 0) return emptyList()

            var rawText = parts.getJSONObject(0).optString("text", "")
            rawText = rawText.trim()
            if (rawText.startsWith("```json")) {
                rawText = rawText.removePrefix("```json").trim()
            }
            if (rawText.startsWith("```")) {
                rawText = rawText.removePrefix("```").trim()
            }
            if (rawText.endsWith("```")) {
                rawText = rawText.removeSuffix("```").trim()
            }

            var qArray: JSONArray? = null
            if (rawText.startsWith("[")) {
                qArray = JSONArray(rawText)
            } else if (rawText.startsWith("{")) {
                val obj = JSONObject(rawText)
                qArray = obj.optJSONArray("questions") ?: obj.optJSONArray("items")
            }

            if (qArray != null) {
                for (i in 0 until qArray.length()) {
                    val item = qArray.optJSONObject(i) ?: continue
                    val chNum = item.optInt("chapterNumber", 1).coerceAtLeast(1)
                    val chTitleEn = item.optString("chapterTitleEn", "")
                    val chTitleUr = item.optString("chapterTitleUr", "")

                    val rawType = item.optString("type", "SHORT").uppercase()
                    val type = when {
                        rawType.contains("MCQ") -> "MCQ"
                        rawType.contains("LONG") -> "LONG"
                        else -> "SHORT"
                    }

                    var qEn = item.optString("questionEn", "").trim()
                    var qUr = item.optString("questionUr", "").trim()

                    if (qEn.isBlank() && qUr.isNotBlank()) {
                        qEn = UrduEnglishAutoTranslator.translateUrduToEnglishOffline(qUr, isOption = false)
                    }
                    if (qUr.isBlank() && qEn.isNotBlank()) {
                        qUr = UrduEnglishAutoTranslator.translateEnglishToUrduOffline(qEn, isOption = false)
                    }

                    val optAEn = item.optString("optionAEn", "")
                    val optBEn = item.optString("optionBEn", "")
                    val optCEn = item.optString("optionCEn", "")
                    val optDEn = item.optString("optionDEn", "")

                    val optAUr = item.optString("optionAUr", "")
                    val optBUr = item.optString("optionBUr", "")
                    val optCUr = item.optString("optionCUr", "")
                    val optDUr = item.optString("optionDUr", "")

                    val corr = item.optString("correctOption", "A").uppercase().take(1).ifBlank { "A" }
                    val defaultMarks = when (type) {
                        "MCQ" -> 1
                        "LONG" -> 4
                        else -> 2
                    }
                    val marks = item.optInt("marks", defaultMarks)

                    if (qEn.isNotBlank() || qUr.isNotBlank()) {
                        results.add(
                            ExtractedQuestionDraft(
                                chapterNumber = chNum,
                                chapterTitleEn = chTitleEn,
                                chapterTitleUr = chTitleUr,
                                type = type,
                                questionEn = qEn,
                                questionUr = qUr,
                                optionAEn = optAEn,
                                optionBEn = optBEn,
                                optionCEn = optCEn,
                                optionDEn = optDEn,
                                optionAUr = optAUr,
                                optionBUr = optBUr,
                                optionCUr = optCUr,
                                optionDUr = optDUr,
                                correctOption = corr,
                                marks = marks
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Gemini response: ${e.message}", e)
        }
        return results
    }

    /**
     * Extracts text lines from a PDF stream or text document.
     */
    fun extractTextFromPdfStream(context: Context, uri: Uri): String {
        val sb = StringBuilder()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val l = line?.trim() ?: continue
                    if (l.isNotBlank()) {
                        // Extract printable plain text tokens if reading a raw text or PDF stream
                        if (l.contains("BT") && l.contains("ET")) {
                            // Extract text inside parentheses (text) Tj
                            val matcher = Regex("\\((.*?)\\)\\s*(?:Tj|'|\")").findAll(l)
                            for (m in matcher) {
                                sb.append(m.groupValues[1]).append(" ")
                            }
                            sb.append("\n")
                        } else {
                            sb.append(l).append("\n")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "extractTextFromPdfStream warning: ${e.message}")
        }
        return sb.toString().trim()
    }

    /**
     * Offline rule-based question extractor that operates completely locally
     * without needing an internet connection or Gemini API key.
     */
    fun extractUsingOfflineRuleEngine(text: String): List<ExtractedQuestionDraft> {
        if (text.isBlank()) return emptyList()

        val results = mutableListOf<ExtractedQuestionDraft>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        var currentChapterNum = 1
        var currentChapterTitleEn = "Chapter 1"
        var currentChapterTitleUr = "باب 1"
        var currentType = "SHORT" // Default

        val chapterHeaderRegex = Regex(
            "^(?:Chapter|Unit|Ch|Lesson|باب|یونٹ)[\\s.#\\-–—]*(\\d+)[\\s.:\\-–—]*(.*)$",
            RegexOption.IGNORE_CASE
        )

        val sectionHeadingMcq = setOf("mcqs", "mcq", "multiple choice", "ایم سی کیوز", "کثیر الانتخابی", "معروضی")
        val sectionHeadingShort = setOf("short questions", "short question", "مختصر سوالات", "شارٹ کوسچن")
        val sectionHeadingLong = setOf("long questions", "long question", "detailed questions", "تفصیلی سوالات", "انشائیہ", "لانگ کوسچن")

        var pendingQuestionEn = ""
        var pendingQuestionUr = ""
        var pendingOptAEn: String? = null
        var pendingOptBEn: String? = null
        var pendingOptCEn: String? = null
        var pendingOptDEn: String? = null
        var pendingOptAUr: String? = null
        var pendingOptBUr: String? = null
        var pendingOptCUr: String? = null
        var pendingOptDUr: String? = null
        var pendingCorrect = "A"

        fun flushPendingMcq() {
            if (pendingQuestionEn.isNotBlank() || pendingQuestionUr.isNotBlank()) {
                val qEn = pendingQuestionEn.ifBlank {
                    UrduEnglishAutoTranslator.translateUrduToEnglishOffline(pendingQuestionUr, isOption = false)
                }
                val qUr = pendingQuestionUr.ifBlank {
                    UrduEnglishAutoTranslator.translateEnglishToUrduOffline(pendingQuestionEn, isOption = false)
                }
                results.add(
                    ExtractedQuestionDraft(
                        chapterNumber = currentChapterNum,
                        chapterTitleEn = currentChapterTitleEn,
                        chapterTitleUr = currentChapterTitleUr,
                        type = "MCQ",
                        questionEn = qEn,
                        questionUr = qUr,
                        optionAEn = pendingOptAEn ?: "Option A",
                        optionBEn = pendingOptBEn ?: "Option B",
                        optionCEn = pendingOptCEn ?: "Option C",
                        optionDEn = pendingOptDEn ?: "Option D",
                        optionAUr = pendingOptAUr ?: "آپشن الف",
                        optionBUr = pendingOptBUr ?: "آپشن ب",
                        optionCUr = pendingOptCUr ?: "آپشن ج",
                        optionDUr = pendingOptDUr ?: "آپشن د",
                        correctOption = pendingCorrect,
                        marks = 1
                    )
                )
            }
            pendingQuestionEn = ""
            pendingQuestionUr = ""
            pendingOptAEn = null
            pendingOptBEn = null
            pendingOptCEn = null
            pendingOptDEn = null
            pendingOptAUr = null
            pendingOptBUr = null
            pendingOptCUr = null
            pendingOptDUr = null
            pendingCorrect = "A"
        }

        for (line in lines) {
            // Check chapter headers
            val chMatch = chapterHeaderRegex.find(line)
            if (chMatch != null) {
                flushPendingMcq()
                val num = chMatch.groupValues[1].toIntOrNull() ?: currentChapterNum
                currentChapterNum = num
                val rest = chMatch.groupValues[2].trim()
                if (rest.isNotBlank()) {
                    val (en, ur) = BulkQuestionParser.splitBilingualText(rest)
                    currentChapterTitleEn = en.ifBlank { "Chapter $currentChapterNum: $rest" }
                    currentChapterTitleUr = ur.ifBlank { "باب $currentChapterNum: $rest" }
                } else {
                    currentChapterTitleEn = "Chapter $currentChapterNum"
                    currentChapterTitleUr = "باب $currentChapterNum"
                }
                continue
            }

            // Check section type headings
            val lower = line.lowercase().replace(":", "").replace("-", "").trim()
            if (sectionHeadingMcq.any { lower.contains(it) }) {
                flushPendingMcq()
                currentType = "MCQ"
                continue
            }
            if (sectionHeadingShort.any { lower.contains(it) }) {
                flushPendingMcq()
                currentType = "SHORT"
                continue
            }
            if (sectionHeadingLong.any { lower.contains(it) }) {
                flushPendingMcq()
                currentType = "LONG"
                continue
            }

            if (currentType == "MCQ") {
                // Check if line contains inline options or answer
                if (line.contains("Ans:") || line.contains("Answer:") || line.contains("جواب:")) {
                    val ansMatch = Regex("(?:Ans|Answer|جواب)[\\s.:=]*([A-Da-d]|الف|ب|ج|د)", RegexOption.IGNORE_CASE).find(line)
                    if (ansMatch != null) {
                        val g = ansMatch.groupValues[1].uppercase()
                        pendingCorrect = when (g) {
                            "الف" -> "A"
                            "ب" -> "B"
                            "ج" -> "C"
                            "د" -> "D"
                            else -> g
                        }
                    }
                    flushPendingMcq()
                    continue
                }

                // Check option line (A) ... (B) ... or (الف) ... (ب) ...
                if (line.contains("(A)") || line.contains("A.") || line.contains("(الف)") || line.contains("الف۔")) {
                    val (enA, urA) = BulkQuestionParser.splitBilingualText(line, isOption = true)
                    pendingOptAEn = enA
                    pendingOptAUr = urA
                    continue
                }

                // New question line
                flushPendingMcq()
                val (qEn, qUr) = BulkQuestionParser.splitBilingualText(line)
                pendingQuestionEn = qEn
                pendingQuestionUr = qUr
            } else {
                // Short or Long Question line
                val (qEn, qUr) = BulkQuestionParser.splitBilingualText(line)
                val cleanEn = BulkQuestionParser.cleanLeadingNumber(qEn)
                val cleanUr = BulkQuestionParser.cleanLeadingNumber(qUr)

                if (cleanEn.isNotBlank() || cleanUr.isNotBlank()) {
                    val finalEn = cleanEn.ifBlank {
                        UrduEnglishAutoTranslator.translateUrduToEnglishOffline(cleanUr, isOption = false)
                    }
                    val finalUr = cleanUr.ifBlank {
                        UrduEnglishAutoTranslator.translateEnglishToUrduOffline(cleanEn, isOption = false)
                    }

                    results.add(
                        ExtractedQuestionDraft(
                            chapterNumber = currentChapterNum,
                            chapterTitleEn = currentChapterTitleEn,
                            chapterTitleUr = currentChapterTitleUr,
                            type = currentType,
                            questionEn = finalEn,
                            questionUr = finalUr,
                            correctOption = "A",
                            marks = if (currentType == "LONG") 4 else 2
                        )
                    )
                }
            }
        }

        flushPendingMcq()
        return results
    }
}
