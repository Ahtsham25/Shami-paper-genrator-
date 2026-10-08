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
}
