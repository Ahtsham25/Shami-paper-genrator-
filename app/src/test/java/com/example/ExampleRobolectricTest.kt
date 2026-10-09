package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.PaperHeaderConfig
import com.example.data.SavedPaperPayload
import com.example.data.SeedData
import com.example.pdf.ExamPdfGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Paper Maker", appName)
        assertEquals("Paper Maker by Shami Academy", context.getString(R.string.brand_header_title))
    }

    @Test
    fun `launch MainActivity without crashing`() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }

    @Test
    fun `render A4 and Legal full paper preview bitmaps with font size table values`() {
        val subjects = SeedData.defaultSubjects()
        val chapters = SeedData.defaultChapters(subjects)
        val questions = SeedData.defaultQuestions(chapters, subjects)
        val mcqs = questions.filter { it.type == "MCQ" }.take(6)
        val shorts = questions.filter { it.type == "SHORT" }.take(5)
        val longs = questions.filter { it.type == "LONG" }.take(2)

        val a4Payload = SavedPaperPayload(
            header = PaperHeaderConfig(paperSize = "A4", fontSizePt = 10, paperVersion = 1),
            mcqs = mcqs,
            shortQuestions = shorts,
            longQuestions = longs
        )
        val legalPayload = SavedPaperPayload(
            header = PaperHeaderConfig(paperSize = "LEGAL", fontSizePt = 16, paperVersion = 2),
            mcqs = mcqs,
            shortQuestions = shorts,
            longQuestions = longs
        )

        val a4Preview = ExamPdfGenerator.renderPaperPagesToBitmaps(a4Payload, scaleFactor = 1.2f)
        val legalPreview = ExamPdfGenerator.renderPaperPagesToBitmaps(legalPayload, scaleFactor = 1.2f)

        assertEquals(595, a4Preview.pageWidthPt)
        assertEquals(842, a4Preview.pageHeightPt)
        assertEquals(10, a4Preview.fontSizePt)
        assertTrue(a4Preview.pageBitmaps.isNotEmpty())

        assertEquals(612, legalPreview.pageWidthPt)
        assertEquals(1008, legalPreview.pageHeightPt)
        assertEquals(16, legalPreview.fontSizePt)
        assertTrue(legalPreview.pageBitmaps.isNotEmpty())
    }
}
