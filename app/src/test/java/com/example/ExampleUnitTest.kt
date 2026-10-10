package com.example

import com.example.ads.AdManager
import com.example.data.BulkQuestionParser
import com.example.data.ChapterEntity
import com.example.data.QuestionType
import com.example.data.SeedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun verifyClass9And10SubjectsAndFreeRules() {
        val subjects = SeedData.defaultSubjects()
        val class9 = subjects.filter { it.classLevel == "9" }
        val class10 = subjects.filter { it.classLevel == "10" }

        assertEquals(8, class9.size)
        assertEquals(8, class10.size)

        // First book in both classes is Chemistry and is FREE by default
        assertTrue(class9.first().isFreeByDefault)
        assertTrue(class10.first().isFreeByDefault)
        assertEquals("Chemistry", class9.first().nameEn)
        assertEquals("Chemistry", class10.first().nameEn)

        // Class 9 has Islamiat, Class 10 has Pakistan Studies instead of Islamiat
        assertTrue(class9.any { it.nameEn == "Islamiat" })
        assertTrue(class10.any { it.nameEn == "Pakistan Studies" && it.nameUr == "مطالعہ پاکستان" })

        // First chapter of every book is FREE by default, chapters 2+ are locked by default
        val chapters = SeedData.defaultChapters(subjects)
        assertTrue(chapters.filter { it.chapterNumber == 1 }.all { it.isFreeByDefault })
        assertTrue(chapters.filter { it.chapterNumber > 1 }.all { !it.isFreeByDefault })
    }

    @Test
    fun verifyBulkQuestionParserShortAndMcq() {
        val chapter = ChapterEntity(
            id = "c9_chemistry_ch1",
            subjectId = "c9_chemistry",
            classLevel = "9",
            chapterNumber = 1,
            titleEn = "Ch 1: Fundamentals of Chemistry",
            titleUr = "باب 1: کیمسٹری کے بنیادی اصول"
        )

        val shortRaw = """
            1. Define Industrial Chemistry. | انڈسٹریل کیمسٹری کی تعریف کریں۔
            2. What is Avogadro's Number? | ایووگیڈروز نمبر کیا ہے؟
        """.trimIndent()

        val parsedShorts = BulkQuestionParser.parseShortOrLongQuestions(
            rawText = shortRaw,
            chapter = chapter,
            type = QuestionType.SHORT
        )
        assertEquals(2, parsedShorts.size)
        assertEquals("Define Industrial Chemistry.", parsedShorts[0].questionEn)
        assertEquals("انڈسٹریل کیمسٹری کی تعریف کریں۔", parsedShorts[0].questionUr)

        val mcqRaw = """
            Molar mass of H2SO4 is: :: سلفیورک ایسڈ کا مولر ماس ہے: | 98 g/mol | 49 g/mol | 18 g/mol | 32 g/mol | A
        """.trimIndent()
        val parsedMcqs = BulkQuestionParser.parseMcqQuestions(mcqRaw, chapter)
        assertEquals(1, parsedMcqs.size)
        assertEquals("A", parsedMcqs[0].correctOption)
        assertEquals("98 g/mol", parsedMcqs[0].optionAEn)
    }

    @Test
    fun verifyAdManagerTestVsLiveIdDetection() {
        assertTrue(AdManager.isGoogleTestUnitId(AdManager.GOOGLE_TEST_REWARDED_ID))
        assertFalse(AdManager.isGoogleTestUnitId("ca-app-pub-1234567890123456/9876543210"))
    }

    @Test
    fun verifyUrduOnlyAutoTranslatesToEnglishAndStripsGlosses() {
        val chapter = ChapterEntity(
            id = "c10_biology_ch1",
            subjectId = "c10_biology",
            classLevel = "10",
            chapterNumber = 1,
            titleEn = "Ch 10: Gaseous Exchange",
            titleUr = "باب 10: گیسوں کا تبادلہ"
        )

        val urduOnlyShorts = """
            س-1: سلیولر ریسپائریشن اور تنفس (Breathing) میں کیا فرق ہے؟
            س-2: گیسوں کا تبادلہ (Gaseous exchange) کیا ہے؟
            س-3: سٹومیٹا اور لینٹی سیلز میں کیا فرق ہے؟
        """.trimIndent()

        val parsedShorts = BulkQuestionParser.parseShortOrLongQuestions(
            rawText = urduOnlyShorts,
            chapter = chapter,
            type = QuestionType.SHORT
        )
        assertEquals(3, parsedShorts.size)

        // Question 1: Urdu is clean (no س-1: and no redundant (Breathing) gloss), English is auto-generated!
        assertEquals(
            "What is the difference between cellular respiration and breathing?",
            parsedShorts[0].resolvedQuestionEn()
        )
        assertEquals(
            "سلیولر ریسپائریشن اور تنفس میں کیا فرق ہے؟",
            parsedShorts[0].resolvedQuestionUr()
        )
        assertFalse(BulkQuestionParser.containsUrdu(parsedShorts[0].resolvedQuestionEn()))
        assertFalse(BulkQuestionParser.containsEnglish(parsedShorts[0].resolvedQuestionUr()))

        // Question 2:
        assertEquals(
            "What is gaseous exchange?",
            parsedShorts[1].resolvedQuestionEn()
        )
        assertEquals(
            "گیسوں کا تبادلہ کیا ہے؟",
            parsedShorts[1].resolvedQuestionUr()
        )

        // Urdu-only MCQ auto-translation test
        val urduOnlyMcq = """
            1. ٹریکیا کی لمبائی تقریباً کتنی ہوتی ہے؟
            (الف) 10 سینٹی میٹر (ب) 12 سینٹی میٹر (ج) 15 سینٹی میٹر (د) 20 سینٹی میٹر | B
        """.trimIndent()

        val parsedMcqs = BulkQuestionParser.parseMcqQuestions(urduOnlyMcq, chapter)
        assertEquals(1, parsedMcqs.size)
        assertEquals("What is the approximate length of the trachea?", parsedMcqs[0].resolvedQuestionEn())
        assertEquals("ٹریکیا کی لمبائی تقریباً کتنی ہوتی ہے؟", parsedMcqs[0].resolvedQuestionUr())
        assertEquals("10 cm", parsedMcqs[0].resolvedOptionAEn())
        assertEquals("10 سینٹی میٹر", parsedMcqs[0].resolvedOptionAUr())
        assertEquals("12 cm", parsedMcqs[0].resolvedOptionBEn())
        assertEquals("12 سینٹی میٹر", parsedMcqs[0].resolvedOptionBUr())
        assertEquals("B", parsedMcqs[0].correctOption)
    }

    @Test
    fun verifyAndNormalizePaperBankJson() {
        val fileCandidates = listOf(
            java.io.File("../data/shami_paper_bank.json"),
            java.io.File("data/shami_paper_bank.json")
        )
        val bankFile = fileCandidates.firstOrNull { it.exists() } ?: return
        val moshi = com.squareup.moshi.Moshi.Builder()
            .addLast(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(com.example.data.GitHubPaperBankPayload::class.java).indent("  ")
        val payload = adapter.fromJson(bankFile.readText()) ?: return

        val normalizedQuestions = payload.questions
            .groupBy { it.chapterId to it.type }
            .flatMap { (_, group) ->
                BulkQuestionParser.normalizeAndPairChapterQuestions(group)
            }

        val updatedPayload = payload.copy(questions = normalizedQuestions)
        bankFile.writeText(adapter.toJson(updatedPayload))

        // Verify that science/general subjects have clean English (no Urdu script) and clean Urdu
        val scienceQuestions = normalizedQuestions.filter { !it.subjectId.contains("urdu") }
        assertTrue(scienceQuestions.isNotEmpty())
        for (q in scienceQuestions) {
            val en = q.resolvedQuestionEn()
            val ur = q.resolvedQuestionUr()
            assertTrue("Expected non-blank English for ${q.id}", en.isNotBlank())
            assertTrue("Expected non-blank Urdu for ${q.id}", ur.isNotBlank())
            assertFalse("Unexpected Urdu script in English question ${q.id}: $en", BulkQuestionParser.containsUrdu(en))
            assertTrue("Expected Urdu script in Urdu question ${q.id}: $ur", BulkQuestionParser.containsUrdu(ur))
        }
    }

    @Test
    fun verifyEnglishOnlyAutoTranslatesToUrdu() {
        val chapter = ChapterEntity(
            id = "c10_biology_ch1",
            subjectId = "c10_biology",
            classLevel = "10",
            chapterNumber = 1,
            titleEn = "Ch 10: Gaseous Exchange",
            titleUr = "باب 10: گیسوں کا تبادلہ"
        )

        val englishOnlyShorts = """
            Q1: What is the difference between cellular respiration and breathing?
            Q2: What is gaseous exchange?
            Q3: Define bronchioles.
            Q4: Differentiate between speed and velocity.
        """.trimIndent()

        val parsedShorts = BulkQuestionParser.parseShortOrLongQuestions(
            rawText = englishOnlyShorts,
            chapter = chapter,
            type = QuestionType.SHORT
        )
        assertEquals(4, parsedShorts.size)

        // Q1: English-only -> Urdu auto-generated!
        assertEquals(
            "What is the difference between cellular respiration and breathing?",
            parsedShorts[0].resolvedQuestionEn()
        )
        assertEquals(
            "سلیولر ریسپائریشن اور تنفس میں کیا فرق ہے؟",
            parsedShorts[0].resolvedQuestionUr()
        )
        assertTrue(BulkQuestionParser.containsUrdu(parsedShorts[0].resolvedQuestionUr()))
        assertFalse(BulkQuestionParser.containsEnglish(parsedShorts[0].resolvedQuestionUr()))

        // Q2:
        assertEquals("What is gaseous exchange?", parsedShorts[1].resolvedQuestionEn())
        assertEquals("گیسوں کا تبادلہ کیا ہے؟", parsedShorts[1].resolvedQuestionUr())

        // Q3:
        assertEquals("Define bronchioles.", parsedShorts[2].resolvedQuestionEn())
        assertEquals("برونکیولز کی تعریف لکھیں۔", parsedShorts[2].resolvedQuestionUr())

        // Q4: Rule-based novel English question -> Urdu auto-generated!
        assertEquals("Differentiate between speed and velocity.", parsedShorts[3].resolvedQuestionEn())
        assertEquals("سپیڈ اور ویلاسٹی میں فرق لکھیں۔", parsedShorts[3].resolvedQuestionUr())

        // English-only MCQ -> Urdu stem and options auto-generated!
        val englishOnlyMcq = """
            1. What is the approximate length of the trachea?
            (A) 10 cm (B) 12 cm (C) 15 cm (D) 20 cm | B
        """.trimIndent()

        val parsedMcqs = BulkQuestionParser.parseMcqQuestions(englishOnlyMcq, chapter)
        assertEquals(1, parsedMcqs.size)
        assertEquals("What is the approximate length of the trachea?", parsedMcqs[0].resolvedQuestionEn())
        assertEquals("ٹریکیا کی لمبائی تقریباً کتنی ہوتی ہے؟", parsedMcqs[0].resolvedQuestionUr())
        assertEquals("10 cm", parsedMcqs[0].resolvedOptionAEn())
        assertEquals("10 سینٹی میٹر", parsedMcqs[0].resolvedOptionAUr())
        assertEquals("12 cm", parsedMcqs[0].resolvedOptionBEn())
        assertEquals("12 سینٹی میٹر", parsedMcqs[0].resolvedOptionBUr())
        assertEquals("B", parsedMcqs[0].correctOption)
    }
}
