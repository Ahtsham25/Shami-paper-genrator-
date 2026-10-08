package com.example.data

enum class PaperLanguage(val code: String, val labelEn: String) {
    ENGLISH("ENGLISH", "English"),
    URDU("URDU", "Urdu"),
    BILINGUAL("BILINGUAL", "Bilingual");

    companion object {
        fun fromCode(code: String): PaperLanguage =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
    }
}

enum class QuestionType(
    val code: String,
    val titleEn: String,
    val defaultMarks: Int
) {
    MCQ("MCQ", "MCQs (Objective)", 1),
    SHORT("SHORT", "Short Questions", 2),
    LONG("LONG", "Long Questions", 5);

    companion object {
        fun fromCode(code: String): QuestionType =
            entries.find { it.code.equals(code, ignoreCase = true) } ?: SHORT
    }
}

data class SubjectEntity(
    val id: String,
    val classLevel: String, // "9" or "10"
    val orderIndex: Int,    // 0 = first book (FREE by default)
    val nameEn: String,
    val nameUr: String,
    val iconKey: String,
    val colorHex: String,
    val isFreeByDefault: Boolean = (orderIndex == 0)
)

data class ChapterEntity(
    val id: String,
    val subjectId: String,
    val classLevel: String, // "9" or "10"
    val chapterNumber: Int, // 1 = first chapter (FREE by default)
    val titleEn: String,
    val titleUr: String,
    val isFreeByDefault: Boolean = (chapterNumber == 1)
)

data class QuestionEntity(
    val id: String,
    val chapterId: String,
    val subjectId: String,
    val classLevel: String,
    val type: String, // "MCQ", "SHORT", "LONG"
    val questionEn: String,
    val questionUr: String,
    val optionAEn: String = "",
    val optionBEn: String = "",
    val optionCEn: String = "",
    val optionDEn: String = "",
    val optionAUr: String = "",
    val optionBUr: String = "",
    val optionCUr: String = "",
    val optionDUr: String = "",
    val correctOption: String = "A",
    val marks: Int = 1,
    val sortOrder: Int = 0
)

data class PaperHeaderConfig(
    val institutionName: String = "SHAMI ACADEMY",
    val examTitle: String = "Term Examination",
    val studentNameValue: String = "",
    val rollNumberValue: String = "",
    val classLabel: String = "Class 9th",
    val subjectNameEn: String = "Chemistry",
    val subjectNameUr: String = "کیمسٹری",
    val timeAllowed: String = "2:00 Hours",
    val languageMode: String = PaperLanguage.ENGLISH.code,
    val paperVersion: Int = 1,          // 1 = Version 1 (Table Layout), 2 = Version 2 (Classic Layout)
    val paperSize: String = "A4",       // "A4" or "LEGAL"
    val fontSizeScale: String = "MEDIUM", // "SMALL", "MEDIUM", "LARGE"
    val includeAnswerKey: Boolean = true
)

data class SavedPaperPayload(
    val header: PaperHeaderConfig,
    val mcqs: List<QuestionEntity>,
    val shortQuestions: List<QuestionEntity>,
    val longQuestions: List<QuestionEntity>
)

data class SavedPaperEntity(
    val id: Long = 0,
    val institutionName: String,
    val paperTitle: String,
    val classLevel: String,
    val subjectId: String,
    val subjectNameEn: String,
    val subjectNameUr: String,
    val timeAllowed: String,
    val languageMode: String,
    val totalMarks: Int,
    val mcqCount: Int,
    val shortCount: Int,
    val longCount: Int,
    val payloadJson: String,
    val pdfFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class RemoteAdConfig(
    val useTestAds: Boolean = true,
    val appId: String = "ca-app-pub-3940256099942544~3347511713",
    val bannerAdUnitId: String = "ca-app-pub-3940256099942544/6300978111",
    val interstitialAdUnitId: String = "ca-app-pub-3940256099942544/1033173712",
    val rewardedAdUnitId: String = "ca-app-pub-3940256099942544/5224354917"
)

data class GitHubPaperBankPayload(
    val version: Int = 1,
    val updatedBy: String = "Paper Maker by Shami Academy Admin",
    val updatedAt: Long = System.currentTimeMillis(),
    val adConfig: RemoteAdConfig? = null,
    val subjects: List<SubjectEntity> = emptyList(),
    val chapters: List<ChapterEntity> = emptyList(),
    val questions: List<QuestionEntity> = emptyList()
)
