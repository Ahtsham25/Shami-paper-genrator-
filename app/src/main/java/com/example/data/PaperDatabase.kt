package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

interface PaperDao {
    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>>
    suspend fun getAllSubjectsOnce(): List<SubjectEntity>
    fun getAllChaptersFlow(): Flow<List<ChapterEntity>>
    suspend fun getAllChaptersOnce(): List<ChapterEntity>
    fun getAllQuestionsFlow(): Flow<List<QuestionEntity>>
    suspend fun getAllQuestionsOnce(): List<QuestionEntity>
    fun getAllSavedPapersFlow(): Flow<List<SavedPaperEntity>>

    suspend fun insertSubjects(subjects: List<SubjectEntity>)
    suspend fun insertSubject(subject: SubjectEntity)
    suspend fun insertChapters(chapters: List<ChapterEntity>)
    suspend fun insertChapter(chapter: ChapterEntity)
    suspend fun updateChapter(chapter: ChapterEntity)
    suspend fun deleteChapterById(chapterId: String)
    suspend fun deleteQuestionsByChapter(chapterId: String)
    suspend fun deleteQuestionsByChapterAndType(chapterId: String, type: String)
    suspend fun insertQuestions(questions: List<QuestionEntity>)
    suspend fun insertQuestion(question: QuestionEntity)
    suspend fun deleteQuestionById(questionId: String)
    suspend fun insertSavedPaper(paper: SavedPaperEntity): Long
    suspend fun deleteSavedPaperById(paperId: Long)
    suspend fun clearAllSubjects()
    suspend fun clearAllChapters()
    suspend fun clearAllQuestions()
}

class PaperDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "shami_paper_maker.db", null, 2),
    PaperDao {

    private val subjectsFlow = MutableStateFlow<List<SubjectEntity>>(emptyList())
    private val chaptersFlow = MutableStateFlow<List<ChapterEntity>>(emptyList())
    private val questionsFlow = MutableStateFlow<List<QuestionEntity>>(emptyList())
    private val savedPapersFlow = MutableStateFlow<List<SavedPaperEntity>>(emptyList())

    init {
        refreshAllFlowsSync()
    }

    fun paperDao(): PaperDao = this

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS subjects (
                id TEXT PRIMARY KEY NOT NULL,
                classLevel TEXT NOT NULL,
                orderIndex INTEGER NOT NULL,
                nameEn TEXT NOT NULL,
                nameUr TEXT NOT NULL,
                iconKey TEXT NOT NULL,
                colorHex TEXT NOT NULL,
                isFreeByDefault INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chapters (
                id TEXT PRIMARY KEY NOT NULL,
                subjectId TEXT NOT NULL,
                classLevel TEXT NOT NULL,
                chapterNumber INTEGER NOT NULL,
                titleEn TEXT NOT NULL,
                titleUr TEXT NOT NULL,
                isFreeByDefault INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS questions (
                id TEXT PRIMARY KEY NOT NULL,
                chapterId TEXT NOT NULL,
                subjectId TEXT NOT NULL,
                classLevel TEXT NOT NULL,
                type TEXT NOT NULL,
                questionEn TEXT NOT NULL,
                questionUr TEXT NOT NULL,
                optionAEn TEXT NOT NULL,
                optionBEn TEXT NOT NULL,
                optionCEn TEXT NOT NULL,
                optionDEn TEXT NOT NULL,
                optionAUr TEXT NOT NULL,
                optionBUr TEXT NOT NULL,
                optionCUr TEXT NOT NULL,
                optionDUr TEXT NOT NULL,
                correctOption TEXT NOT NULL,
                marks INTEGER NOT NULL,
                sortOrder INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS saved_papers (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                institutionName TEXT NOT NULL,
                paperTitle TEXT NOT NULL,
                classLevel TEXT NOT NULL,
                subjectId TEXT NOT NULL,
                subjectNameEn TEXT NOT NULL,
                subjectNameUr TEXT NOT NULL,
                timeAllowed TEXT NOT NULL,
                languageMode TEXT NOT NULL,
                totalMarks INTEGER NOT NULL,
                mcqCount INTEGER NOT NULL,
                shortCount INTEGER NOT NULL,
                longCount INTEGER NOT NULL,
                payloadJson TEXT NOT NULL,
                pdfFilePath TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onCreate(db)
    }

    @Synchronized
    private fun refreshAllFlowsSync() {
        subjectsFlow.value = querySubjectsSync()
        chaptersFlow.value = queryChaptersSync()
        questionsFlow.value = queryQuestionsSync()
        savedPapersFlow.value = querySavedPapersSync()
    }

    @Synchronized
    private fun querySubjectsSync(): List<SubjectEntity> {
        val list = mutableListOf<SubjectEntity>()
        val db = writableDatabase
        db.rawQuery("SELECT id, classLevel, orderIndex, nameEn, nameUr, iconKey, colorHex, isFreeByDefault FROM subjects ORDER BY classLevel ASC, orderIndex ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    SubjectEntity(
                        id = c.getString(0),
                        classLevel = c.getString(1),
                        orderIndex = c.getInt(2),
                        nameEn = c.getString(3),
                        nameUr = c.getString(4),
                        iconKey = c.getString(5),
                        colorHex = c.getString(6),
                        isFreeByDefault = c.getInt(7) != 0
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    private fun queryChaptersSync(): List<ChapterEntity> {
        val list = mutableListOf<ChapterEntity>()
        val db = writableDatabase
        db.rawQuery("SELECT id, subjectId, classLevel, chapterNumber, titleEn, titleUr, isFreeByDefault FROM chapters ORDER BY classLevel ASC, subjectId ASC, chapterNumber ASC", null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    ChapterEntity(
                        id = c.getString(0),
                        subjectId = c.getString(1),
                        classLevel = c.getString(2),
                        chapterNumber = c.getInt(3),
                        titleEn = c.getString(4),
                        titleUr = c.getString(5),
                        isFreeByDefault = c.getInt(6) != 0
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    private fun queryQuestionsSync(): List<QuestionEntity> {
        val list = mutableListOf<QuestionEntity>()
        val db = writableDatabase
        db.rawQuery(
            "SELECT id, chapterId, subjectId, classLevel, type, questionEn, questionUr, optionAEn, optionBEn, optionCEn, optionDEn, optionAUr, optionBUr, optionCUr, optionDUr, correctOption, marks, sortOrder FROM questions ORDER BY sortOrder ASC, id ASC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    QuestionEntity(
                        id = c.getString(0),
                        chapterId = c.getString(1),
                        subjectId = c.getString(2),
                        classLevel = c.getString(3),
                        type = c.getString(4),
                        questionEn = c.getString(5),
                        questionUr = c.getString(6),
                        optionAEn = c.getString(7),
                        optionBEn = c.getString(8),
                        optionCEn = c.getString(9),
                        optionDEn = c.getString(10),
                        optionAUr = c.getString(11),
                        optionBUr = c.getString(12),
                        optionCUr = c.getString(13),
                        optionDUr = c.getString(14),
                        correctOption = c.getString(15),
                        marks = c.getInt(16),
                        sortOrder = c.getInt(17)
                    )
                )
            }
        }
        return list
    }

    @Synchronized
    private fun querySavedPapersSync(): List<SavedPaperEntity> {
        val list = mutableListOf<SavedPaperEntity>()
        val db = writableDatabase
        db.rawQuery(
            "SELECT id, institutionName, paperTitle, classLevel, subjectId, subjectNameEn, subjectNameUr, timeAllowed, languageMode, totalMarks, mcqCount, shortCount, longCount, payloadJson, pdfFilePath, createdAt FROM saved_papers ORDER BY createdAt DESC",
            null
        ).use { c ->
            while (c.moveToNext()) {
                list.add(
                    SavedPaperEntity(
                        id = c.getLong(0),
                        institutionName = c.getString(1),
                        paperTitle = c.getString(2),
                        classLevel = c.getString(3),
                        subjectId = c.getString(4),
                        subjectNameEn = c.getString(5),
                        subjectNameUr = c.getString(6),
                        timeAllowed = c.getString(7),
                        languageMode = c.getString(8),
                        totalMarks = c.getInt(9),
                        mcqCount = c.getInt(10),
                        shortCount = c.getInt(11),
                        longCount = c.getInt(12),
                        payloadJson = c.getString(13),
                        pdfFilePath = if (c.isNull(14)) null else c.getString(14),
                        createdAt = c.getLong(15)
                    )
                )
            }
        }
        return list
    }

    private fun subjectValues(s: SubjectEntity) = ContentValues().apply {
        put("id", s.id)
        put("classLevel", s.classLevel)
        put("orderIndex", s.orderIndex)
        put("nameEn", s.nameEn)
        put("nameUr", s.nameUr)
        put("iconKey", s.iconKey)
        put("colorHex", s.colorHex)
        put("isFreeByDefault", if (s.isFreeByDefault) 1 else 0)
    }

    private fun chapterValues(ch: ChapterEntity) = ContentValues().apply {
        put("id", ch.id)
        put("subjectId", ch.subjectId)
        put("classLevel", ch.classLevel)
        put("chapterNumber", ch.chapterNumber)
        put("titleEn", ch.titleEn)
        put("titleUr", ch.titleUr)
        put("isFreeByDefault", if (ch.isFreeByDefault) 1 else 0)
    }

    private fun questionValues(q: QuestionEntity) = ContentValues().apply {
        put("id", q.id)
        put("chapterId", q.chapterId)
        put("subjectId", q.subjectId)
        put("classLevel", q.classLevel)
        put("type", q.type)
        put("questionEn", q.questionEn)
        put("questionUr", q.questionUr)
        put("optionAEn", q.optionAEn)
        put("optionBEn", q.optionBEn)
        put("optionCEn", q.optionCEn)
        put("optionDEn", q.optionDEn)
        put("optionAUr", q.optionAUr)
        put("optionBUr", q.optionBUr)
        put("optionCUr", q.optionCUr)
        put("optionDUr", q.optionDUr)
        put("correctOption", q.correctOption)
        put("marks", q.marks)
        put("sortOrder", q.sortOrder)
    }

    override fun getAllSubjectsFlow(): Flow<List<SubjectEntity>> = subjectsFlow.asStateFlow()

    override suspend fun getAllSubjectsOnce(): List<SubjectEntity> = withContext(Dispatchers.IO) {
        querySubjectsSync().also { subjectsFlow.value = it }
    }

    override fun getAllChaptersFlow(): Flow<List<ChapterEntity>> = chaptersFlow.asStateFlow()

    override suspend fun getAllChaptersOnce(): List<ChapterEntity> = withContext(Dispatchers.IO) {
        queryChaptersSync().also { chaptersFlow.value = it }
    }

    override fun getAllQuestionsFlow(): Flow<List<QuestionEntity>> = questionsFlow.asStateFlow()

    override suspend fun getAllQuestionsOnce(): List<QuestionEntity> = withContext(Dispatchers.IO) {
        queryQuestionsSync().also { questionsFlow.value = it }
    }

    override fun getAllSavedPapersFlow(): Flow<List<SavedPaperEntity>> = savedPapersFlow.asStateFlow()

    override suspend fun insertSubjects(subjects: List<SubjectEntity>) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (s in subjects) {
                    db.insertWithOnConflict("subjects", null, subjectValues(s), SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            subjectsFlow.value = querySubjectsSync()
        }
    }

    override suspend fun insertSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.insertWithOnConflict("subjects", null, subjectValues(subject), SQLiteDatabase.CONFLICT_REPLACE)
            subjectsFlow.value = querySubjectsSync()
        }
    }

    override suspend fun insertChapters(chapters: List<ChapterEntity>) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (ch in chapters) {
                    db.insertWithOnConflict("chapters", null, chapterValues(ch), SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            chaptersFlow.value = queryChaptersSync()
        }
    }

    override suspend fun insertChapter(chapter: ChapterEntity) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.insertWithOnConflict("chapters", null, chapterValues(chapter), SQLiteDatabase.CONFLICT_REPLACE)
            chaptersFlow.value = queryChaptersSync()
        }
    }

    override suspend fun updateChapter(chapter: ChapterEntity) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.insertWithOnConflict("chapters", null, chapterValues(chapter), SQLiteDatabase.CONFLICT_REPLACE)
            chaptersFlow.value = queryChaptersSync()
        }
    }

    override suspend fun deleteChapterById(chapterId: String) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.delete("chapters", "id = ?", arrayOf(chapterId))
            chaptersFlow.value = queryChaptersSync()
        }
    }

    override suspend fun deleteQuestionsByChapter(chapterId: String) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.delete("questions", "chapterId = ?", arrayOf(chapterId))
            questionsFlow.value = queryQuestionsSync()
        }
    }

    override suspend fun deleteQuestionsByChapterAndType(chapterId: String, type: String) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.delete("questions", "chapterId = ? AND type = ?", arrayOf(chapterId, type))
            questionsFlow.value = queryQuestionsSync()
        }
    }

    override suspend fun insertQuestions(questions: List<QuestionEntity>) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (q in questions) {
                    db.insertWithOnConflict("questions", null, questionValues(q), SQLiteDatabase.CONFLICT_REPLACE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            questionsFlow.value = queryQuestionsSync()
        }
    }

    override suspend fun insertQuestion(question: QuestionEntity) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.insertWithOnConflict("questions", null, questionValues(question), SQLiteDatabase.CONFLICT_REPLACE)
            questionsFlow.value = queryQuestionsSync()
        }
    }

    override suspend fun deleteQuestionById(questionId: String) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.delete("questions", "id = ?", arrayOf(questionId))
            questionsFlow.value = queryQuestionsSync()
        }
    }

    override suspend fun insertSavedPaper(paper: SavedPaperEntity): Long = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            val values = ContentValues().apply {
                if (paper.id > 0L) put("id", paper.id)
                put("institutionName", paper.institutionName)
                put("paperTitle", paper.paperTitle)
                put("classLevel", paper.classLevel)
                put("subjectId", paper.subjectId)
                put("subjectNameEn", paper.subjectNameEn)
                put("subjectNameUr", paper.subjectNameUr)
                put("timeAllowed", paper.timeAllowed)
                put("languageMode", paper.languageMode)
                put("totalMarks", paper.totalMarks)
                put("mcqCount", paper.mcqCount)
                put("shortCount", paper.shortCount)
                put("longCount", paper.longCount)
                put("payloadJson", paper.payloadJson)
                if (paper.pdfFilePath != null) put("pdfFilePath", paper.pdfFilePath) else putNull("pdfFilePath")
                put("createdAt", paper.createdAt)
            }
            val rowId = writableDatabase.insertWithOnConflict("saved_papers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            savedPapersFlow.value = querySavedPapersSync()
            rowId
        }
    }

    override suspend fun deleteSavedPaperById(paperId: Long) = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.delete("saved_papers", "id = ?", arrayOf(paperId.toString()))
            savedPapersFlow.value = querySavedPapersSync()
        }
    }

    override suspend fun clearAllSubjects() = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.delete("subjects", null, null)
            subjectsFlow.value = emptyList()
        }
    }

    override suspend fun clearAllChapters() = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.delete("chapters", null, null)
            chaptersFlow.value = emptyList()
        }
    }

    override suspend fun clearAllQuestions() = withContext(Dispatchers.IO) {
        synchronized(this@PaperDatabase) {
            writableDatabase.delete("questions", null, null)
            questionsFlow.value = emptyList()
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: PaperDatabase? = null

        fun getInstance(context: Context): PaperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = PaperDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
