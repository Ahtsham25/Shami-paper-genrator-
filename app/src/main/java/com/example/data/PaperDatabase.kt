package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PaperDao {
    @Query("SELECT * FROM subjects ORDER BY classLevel ASC, orderIndex ASC")
    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY classLevel ASC, orderIndex ASC")
    suspend fun getAllSubjectsOnce(): List<SubjectEntity>

    @Query("SELECT * FROM chapters ORDER BY classLevel ASC, subjectId ASC, chapterNumber ASC")
    fun getAllChaptersFlow(): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters ORDER BY classLevel ASC, subjectId ASC, chapterNumber ASC")
    suspend fun getAllChaptersOnce(): List<ChapterEntity>

    @Query("SELECT * FROM questions ORDER BY sortOrder ASC, id ASC")
    fun getAllQuestionsFlow(): Flow<List<QuestionEntity>>

    @Query("SELECT * FROM questions ORDER BY sortOrder ASC, id ASC")
    suspend fun getAllQuestionsOnce(): List<QuestionEntity>

    @Query("SELECT * FROM saved_papers ORDER BY createdAt DESC")
    fun getAllSavedPapersFlow(): Flow<List<SavedPaperEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE id = :chapterId")
    suspend fun deleteChapterById(chapterId: String)

    @Query("DELETE FROM questions WHERE chapterId = :chapterId")
    suspend fun deleteQuestionsByChapter(chapterId: String)

    @Query("DELETE FROM questions WHERE chapterId = :chapterId AND type = :type")
    suspend fun deleteQuestionsByChapterAndType(chapterId: String, type: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestions(questions: List<QuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QuestionEntity)

    @Query("DELETE FROM questions WHERE id = :questionId")
    suspend fun deleteQuestionById(questionId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedPaper(paper: SavedPaperEntity): Long

    @Query("DELETE FROM saved_papers WHERE id = :paperId")
    suspend fun deleteSavedPaperById(paperId: Long)

    @Query("DELETE FROM subjects")
    suspend fun clearAllSubjects()

    @Query("DELETE FROM chapters")
    suspend fun clearAllChapters()

    @Query("DELETE FROM questions")
    suspend fun clearAllQuestions()
}

@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        QuestionEntity::class,
        SavedPaperEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PaperDatabase : RoomDatabase() {
    abstract fun paperDao(): PaperDao

    companion object {
        @Volatile
        private var INSTANCE: PaperDatabase? = null

        fun getInstance(context: Context): PaperDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PaperDatabase::class.java,
                    "shami_paper_maker.db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
