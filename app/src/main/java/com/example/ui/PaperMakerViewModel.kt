package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ads.AdManager
import com.example.data.BulkQuestionParser
import com.example.data.ChapterEntity
import com.example.data.GitHubPaperBankPayload
import com.example.data.GitHubRepoConfig
import com.example.data.GitHubSyncResult
import com.example.data.GitHubSyncService
import com.example.data.PaperDatabase
import com.example.data.PaperHeaderConfig
import com.example.data.PaperLanguage
import com.example.data.QuestionEntity
import com.example.data.QuestionType
import com.example.data.RemoteAdConfig
import com.example.data.SavedPaperEntity
import com.example.data.SavedPaperPayload
import com.example.data.SeedData
import com.example.data.SubjectEntity
import com.example.pdf.ExamPdfGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

import android.app.Activity

enum class BottomNavTab {
    HOME,
    SAVED_DOWNLOADS,
    MORE
}

enum class ActiveScreen {
    MAIN_TABS,
    SUBJECT_CHAPTERS,
    QUESTION_PICKER_AND_BUILDER,
    PAPER_PREVIEW,
    PRIVACY_POLICY,
    OWNER_ADMIN_APP
}

data class DownloadedPdfInfo(
    val file: File,
    val fileName: String,
    val publicFolderDisplay: String = ExamPdfGenerator.PUBLIC_DOWNLOAD_FOLDER_DISPLAY
)

class PaperMakerViewModel(application: Application) : AndroidViewModel(application) {

    private val db = PaperDatabase.getInstance(application)
    private val dao = db.paperDao()
    private val prefs = application.getSharedPreferences("shami_paper_prefs", Context.MODE_PRIVATE)
    val gitHubService = GitHubSyncService(application)

    val allSubjects: StateFlow<List<SubjectEntity>> = dao.getAllSubjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChapters: StateFlow<List<ChapterEntity>> = dao.getAllChaptersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allQuestions: StateFlow<List<QuestionEntity>> = dao.getAllQuestionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedPapers: StateFlow<List<SavedPaperEntity>> = dao.getAllSavedPapersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTab = MutableStateFlow(BottomNavTab.HOME)
    val currentTab: StateFlow<BottomNavTab> = _currentTab.asStateFlow()

    private val _activeScreen = MutableStateFlow(ActiveScreen.MAIN_TABS)
    val activeScreen: StateFlow<ActiveScreen> = _activeScreen.asStateFlow()

    private val _selectedClassLevel = MutableStateFlow("9")
    val selectedClassLevel: StateFlow<String> = _selectedClassLevel.asStateFlow()

    private val _selectedSubject = MutableStateFlow<SubjectEntity?>(null)
    val selectedSubject: StateFlow<SubjectEntity?> = _selectedSubject.asStateFlow()

    private val _selectedChapterIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedChapterIds: StateFlow<Set<String>> = _selectedChapterIds.asStateFlow()

    private val _unlockedSubjectIds = MutableStateFlow<Set<String>>(
        prefs.getStringSet("unlocked_subjects", emptySet()) ?: emptySet()
    )
    val unlockedSubjectIds: StateFlow<Set<String>> = _unlockedSubjectIds.asStateFlow()

    private val _unlockedChapterIds = MutableStateFlow<Set<String>>(
        prefs.getStringSet("unlocked_chapters", emptySet()) ?: emptySet()
    )
    val unlockedChapterIds: StateFlow<Set<String>> = _unlockedChapterIds.asStateFlow()

    private val _selectedMcqs = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val selectedMcqs: StateFlow<List<QuestionEntity>> = _selectedMcqs.asStateFlow()

    private val _selectedShortQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val selectedShortQuestions: StateFlow<List<QuestionEntity>> = _selectedShortQuestions.asStateFlow()

    private val _selectedLongQuestions = MutableStateFlow<List<QuestionEntity>>(emptyList())
    val selectedLongQuestions: StateFlow<List<QuestionEntity>> = _selectedLongQuestions.asStateFlow()

    private val _paperHeader = MutableStateFlow(
        PaperHeaderConfig(
            institutionName = prefs.getString("default_institution", "SHAMI ACADEMY") ?: "SHAMI ACADEMY",
            timeAllowed = prefs.getString("default_time_allowed", "2:00 Hours") ?: "2:00 Hours",
            languageMode = prefs.getString("default_lang_mode", PaperLanguage.ENGLISH.code) ?: PaperLanguage.ENGLISH.code,
            paperVersion = prefs.getInt("default_paper_version", 1),
            paperSize = prefs.getString("default_paper_size", "A4") ?: "A4",
            fontSizeScale = prefs.getString("default_font_scale", "MEDIUM") ?: "MEDIUM",
            fontSizePt = prefs.getInt("default_font_size_pt", 12)
        )
    )
    val paperHeader: StateFlow<PaperHeaderConfig> = _paperHeader.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _lastDownloadedPdf = MutableStateFlow<DownloadedPdfInfo?>(null)
    val lastDownloadedPdf: StateFlow<DownloadedPdfInfo?> = _lastDownloadedPdf.asStateFlow()

    private val _isSyncingGitHub = MutableStateFlow(false)
    val isSyncingGitHub: StateFlow<Boolean> = _isSyncingGitHub.asStateFlow()

    init {
        AdManager.initialize(application)
        seedDatabaseIfNeeded()
        autoSyncFromGitHubIfConfigured()
    }

    private fun seedDatabaseIfNeeded() {
        viewModelScope.launch {
            val existingSubjects = dao.getAllSubjectsOnce()
            if (existingSubjects.isEmpty()) {
                val subjects = SeedData.defaultSubjects()
                val chapters = SeedData.defaultChapters(subjects)
                val questions = SeedData.defaultQuestions(chapters, subjects)
                dao.insertSubjects(subjects)
                dao.insertChapters(chapters)
                dao.insertQuestions(questions)
            } else {
                normalizeAllExistingQuestionsInDatabase()
            }
        }
    }

    private suspend fun normalizeAllExistingQuestionsInDatabase() {
        val allQs = dao.getAllQuestionsOnce()
        if (allQs.isEmpty()) return
        val grouped = allQs.groupBy { "${it.chapterId}::${it.type}" }
        val normalizedAll = mutableListOf<QuestionEntity>()
        var changed = false
        for ((_, group) in grouped) {
            val norm = BulkQuestionParser.normalizeAndPairChapterQuestions(group)
            if (norm != group) changed = true
            normalizedAll.addAll(norm)
        }
        if (changed) {
            dao.clearAllQuestions()
            dao.insertQuestions(normalizedAll)
        }
    }

    private fun autoSyncFromGitHubIfConfigured() {
        val cfg = gitHubService.getConfig()
        if ((cfg.owner.isNotBlank() && cfg.repo.isNotBlank()) || cfg.customRawUrl.isNotBlank()) {
            viewModelScope.launch {
                val res = gitHubService.fetchBankFromGitHub(cfg)
                if (res is GitHubSyncResult.Success && res.payload != null) {
                    applyBankPayloadToDatabase(res.payload)
                }
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun clearLastDownloadedPdf() {
        _lastDownloadedPdf.value = null
    }

    fun showStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun selectBottomTab(tab: BottomNavTab) {
        _currentTab.value = tab
        _activeScreen.value = ActiveScreen.MAIN_TABS
    }

    fun openPrivacyPolicy() {
        _activeScreen.value = ActiveScreen.PRIVACY_POLICY
    }

    fun openOwnerAdminApp() {
        _activeScreen.value = ActiveScreen.OWNER_ADMIN_APP
    }

    fun selectClassLevel(classLevel: String) {
        _selectedClassLevel.value = classLevel
    }

    fun openSubject(subject: SubjectEntity) {
        _selectedSubject.value = subject
        _selectedClassLevel.value = subject.classLevel
        val firstChId = "${subject.id}_ch1"
        _selectedChapterIds.value = setOf(firstChId)
        _paperHeader.value = _paperHeader.value.copy(
            classLabel = if (subject.classLevel == "9") "Class 9th" else "Class 10th",
            subjectNameEn = subject.nameEn,
            subjectNameUr = subject.nameUr
        )
        _activeScreen.value = ActiveScreen.SUBJECT_CHAPTERS
    }

    fun proceedToQuestionPicker() {
        if (_selectedChapterIds.value.isEmpty()) {
            _statusMessage.value = "Please select at least one chapter first."
            return
        }
        _activeScreen.value = ActiveScreen.QUESTION_PICKER_AND_BUILDER
    }

    fun openPaperPreview() {
        val totalSelected = _selectedMcqs.value.size + _selectedShortQuestions.value.size + _selectedLongQuestions.value.size
        if (totalSelected == 0) {
            val chIds = _selectedChapterIds.value
            val subj = _selectedSubject.value
            val pool = when {
                chIds.isNotEmpty() -> allQuestions.value.filter { it.chapterId in chIds }
                subj != null -> allQuestions.value.filter { it.subjectId == subj.id }
                else -> allQuestions.value
            }
            val autoMcqs = pool.filter { it.type == QuestionType.MCQ.code }.take(5)
            val autoShorts = pool.filter { it.type == QuestionType.SHORT.code }.take(4)
            val autoLongs = pool.filter { it.type == QuestionType.LONG.code }.take(2)
            if (autoMcqs.isNotEmpty() || autoShorts.isNotEmpty() || autoLongs.isNotEmpty()) {
                _selectedMcqs.value = autoMcqs
                _selectedShortQuestions.value = autoShorts
                _selectedLongQuestions.value = autoLongs
            }
        }
        _activeScreen.value = ActiveScreen.PAPER_PREVIEW
    }

    fun navigateBack(): Boolean {
        return when (_activeScreen.value) {
            ActiveScreen.PAPER_PREVIEW -> {
                _activeScreen.value = ActiveScreen.QUESTION_PICKER_AND_BUILDER
                true
            }
            ActiveScreen.QUESTION_PICKER_AND_BUILDER -> {
                _activeScreen.value = ActiveScreen.SUBJECT_CHAPTERS
                true
            }
            ActiveScreen.SUBJECT_CHAPTERS -> {
                _activeScreen.value = ActiveScreen.MAIN_TABS
                true
            }
            ActiveScreen.PRIVACY_POLICY -> {
                _activeScreen.value = ActiveScreen.MAIN_TABS
                true
            }
            ActiveScreen.OWNER_ADMIN_APP -> {
                _activeScreen.value = ActiveScreen.MAIN_TABS
                true
            }
            ActiveScreen.MAIN_TABS -> {
                if (_currentTab.value != BottomNavTab.HOME) {
                    _currentTab.value = BottomNavTab.HOME
                    true
                } else {
                    false
                }
            }
        }
    }

    fun isSubjectUnlocked(subject: SubjectEntity): Boolean {
        return subject.orderIndex == 0 ||
            subject.isFreeByDefault ||
            _unlockedSubjectIds.value.contains(subject.id)
    }

    fun isChapterUnlocked(chapter: ChapterEntity): Boolean {
        return chapter.chapterNumber == 1 ||
            chapter.isFreeByDefault ||
            _unlockedChapterIds.value.contains(chapter.id)
    }

    fun unlockSubjectViaRewardedAd(subjectId: String) {
        val updated = _unlockedSubjectIds.value + subjectId
        _unlockedSubjectIds.value = updated
        prefs.edit().putStringSet("unlocked_subjects", updated).apply()
    }

    fun unlockChapterViaRewardedAd(chapterId: String) {
        val updated = _unlockedChapterIds.value + chapterId
        _unlockedChapterIds.value = updated
        prefs.edit().putStringSet("unlocked_chapters", updated).apply()
        _selectedChapterIds.value = _selectedChapterIds.value + chapterId
    }

    fun resetAllRewardedAdLocks() {
        _unlockedSubjectIds.value = emptySet()
        _unlockedChapterIds.value = emptySet()
        prefs.edit()
            .remove("unlocked_subjects")
            .remove("unlocked_chapters")
            .apply()
        _statusMessage.value = "All locks reset to default."
    }

    fun toggleChapterSelection(chapter: ChapterEntity) {
        val current = _selectedChapterIds.value
        _selectedChapterIds.value = if (current.contains(chapter.id)) {
            current - chapter.id
        } else {
            current + chapter.id
        }
    }

    fun toggleQuestionSelection(question: QuestionEntity) {
        when (QuestionType.fromCode(question.type)) {
            QuestionType.MCQ -> {
                val list = _selectedMcqs.value
                _selectedMcqs.value = if (list.any { it.id == question.id }) {
                    list.filterNot { it.id == question.id }
                } else {
                    list + question
                }
            }
            QuestionType.SHORT -> {
                val list = _selectedShortQuestions.value
                _selectedShortQuestions.value = if (list.any { it.id == question.id }) {
                    list.filterNot { it.id == question.id }
                } else {
                    list + question
                }
            }
            QuestionType.LONG -> {
                val list = _selectedLongQuestions.value
                _selectedLongQuestions.value = if (list.any { it.id == question.id }) {
                    list.filterNot { it.id == question.id }
                } else {
                    list + question
                }
            }
        }
    }

    fun selectAllInList(questions: List<QuestionEntity>, type: QuestionType) {
        when (type) {
            QuestionType.MCQ -> {
                val existingIds = _selectedMcqs.value.map { it.id }.toSet()
                val toAdd = questions.filterNot { it.id in existingIds }
                _selectedMcqs.value = _selectedMcqs.value + toAdd
            }
            QuestionType.SHORT -> {
                val existingIds = _selectedShortQuestions.value.map { it.id }.toSet()
                val toAdd = questions.filterNot { it.id in existingIds }
                _selectedShortQuestions.value = _selectedShortQuestions.value + toAdd
            }
            QuestionType.LONG -> {
                val existingIds = _selectedLongQuestions.value.map { it.id }.toSet()
                val toAdd = questions.filterNot { it.id in existingIds }
                _selectedLongQuestions.value = _selectedLongQuestions.value + toAdd
            }
        }
    }

    fun clearSelectedQuestionsByType(type: QuestionType?) {
        when (type) {
            QuestionType.MCQ -> _selectedMcqs.value = emptyList()
            QuestionType.SHORT -> _selectedShortQuestions.value = emptyList()
            QuestionType.LONG -> _selectedLongQuestions.value = emptyList()
            null -> {
                _selectedMcqs.value = emptyList()
                _selectedShortQuestions.value = emptyList()
                _selectedLongQuestions.value = emptyList()
            }
        }
    }

    fun moveSelectedQuestion(type: QuestionType, fromIndex: Int, toIndex: Int) {
        when (type) {
            QuestionType.MCQ -> {
                val list = _selectedMcqs.value.toMutableList()
                if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                    val item = list.removeAt(fromIndex)
                    list.add(toIndex, item)
                    _selectedMcqs.value = list
                }
            }
            QuestionType.SHORT -> {
                val list = _selectedShortQuestions.value.toMutableList()
                if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                    val item = list.removeAt(fromIndex)
                    list.add(toIndex, item)
                    _selectedShortQuestions.value = list
                }
            }
            QuestionType.LONG -> {
                val list = _selectedLongQuestions.value.toMutableList()
                if (fromIndex in list.indices && toIndex in list.indices && fromIndex != toIndex) {
                    val item = list.removeAt(fromIndex)
                    list.add(toIndex, item)
                    _selectedLongQuestions.value = list
                }
            }
        }
    }

    fun updatePaperHeader(newHeader: PaperHeaderConfig) {
        _paperHeader.value = newHeader
        prefs.edit()
            .putString("default_institution", newHeader.institutionName)
            .putString("default_time_allowed", newHeader.timeAllowed)
            .putString("default_lang_mode", newHeader.languageMode)
            .putInt("default_paper_version", newHeader.paperVersion)
            .putString("default_paper_size", newHeader.paperSize)
            .putString("default_font_scale", newHeader.fontSizeScale)
            .putInt("default_font_size_pt", newHeader.fontSizePt)
            .apply()
    }

    fun setPaperLanguage(lang: PaperLanguage) {
        updatePaperHeader(_paperHeader.value.copy(languageMode = lang.code))
    }

    fun setPaperVersion(version: Int) {
        updatePaperHeader(_paperHeader.value.copy(paperVersion = version))
    }

    fun setPaperSize(size: String) {
        updatePaperHeader(_paperHeader.value.copy(paperSize = size))
    }

    fun setFontSizeScale(scale: String) {
        val mappedPt = when (scale.uppercase()) {
            "SMALL" -> 10
            "LARGE" -> 15
            else -> 12
        }
        updatePaperHeader(
            _paperHeader.value.copy(
                fontSizeScale = scale,
                fontSizePt = mappedPt
            )
        )
    }

    fun setFontSizePt(pt: Int) {
        val clamped = pt.coerceIn(8, 24)
        val scaleLabel = when {
            clamped <= 10 -> "SMALL"
            clamped >= 14 -> "LARGE"
            else -> "MEDIUM"
        }
        updatePaperHeader(
            _paperHeader.value.copy(
                fontSizePt = clamped,
                fontSizeScale = scaleLabel
            )
        )
    }

    fun calculateTotalMarks(): Int {
        return _selectedMcqs.value.sumOf { it.marks } +
            _selectedShortQuestions.value.sumOf { it.marks } +
            _selectedLongQuestions.value.sumOf { it.marks }
    }

    fun buildCurrentPaperPayload(): SavedPaperPayload {
        return SavedPaperPayload(
            header = _paperHeader.value,
            mcqs = _selectedMcqs.value,
            shortQuestions = _selectedShortQuestions.value,
            longQuestions = _selectedLongQuestions.value
        )
    }

    fun savePaperAndOptionallyExportPdf(
        exportPdf: Boolean,
        sharePdfAfterExport: Boolean = false
    ) {
        viewModelScope.launch {
            val payload = buildCurrentPaperPayload()
            val totalMarks = calculateTotalMarks()
            val subject = _selectedSubject.value

            var generatedPdfFile: File? = null
            if (exportPdf) {
                try {
                    generatedPdfFile = ExamPdfGenerator.generateExamPdf(
                        context = getApplication(),
                        payload = payload
                    )
                } catch (e: Exception) {
                    _statusMessage.value = "Error creating PDF: ${e.localizedMessage}"
                }
            }

            val payloadJson = gitHubService.encodeSavedPaperPayload(payload)
            val entity = SavedPaperEntity(
                institutionName = payload.header.institutionName.ifBlank { "SHAMI ACADEMY" },
                paperTitle = "${payload.header.classLabel} - ${payload.header.subjectNameEn}",
                classLevel = _selectedClassLevel.value,
                subjectId = subject?.id ?: "custom",
                subjectNameEn = payload.header.subjectNameEn,
                subjectNameUr = payload.header.subjectNameUr,
                timeAllowed = payload.header.timeAllowed,
                languageMode = payload.header.languageMode,
                totalMarks = totalMarks,
                mcqCount = payload.mcqs.size,
                shortCount = payload.shortQuestions.size,
                longCount = payload.longQuestions.size,
                payloadJson = payloadJson,
                pdfFilePath = generatedPdfFile?.absolutePath
            )
            dao.insertSavedPaper(entity)

            if (generatedPdfFile != null) {
                _lastDownloadedPdf.value = DownloadedPdfInfo(
                    file = generatedPdfFile,
                    fileName = generatedPdfFile.name
                )
                _statusMessage.value = "Saved in Phone: ${ExamPdfGenerator.PUBLIC_DOWNLOAD_FOLDER_DISPLAY}/${generatedPdfFile.name}"
                if (sharePdfAfterExport) {
                    ExamPdfGenerator.shareOrOpenPdf(getApplication(), generatedPdfFile, share = true)
                }
            } else {
                _statusMessage.value = "Paper saved to Saved & Downloads tab!"
            }
        }
    }

    fun printCurrentPaper(activity: Activity) {
        viewModelScope.launch {
            try {
                val payload = buildCurrentPaperPayload()
                val pdfFile = ExamPdfGenerator.generateExamPdf(getApplication(), payload)
                ExamPdfGenerator.printExamPdf(activity, pdfFile, pdfFile.nameWithoutExtension)
            } catch (e: Exception) {
                _statusMessage.value = "Print failed: ${e.localizedMessage}"
            }
        }
    }

    fun printSavedPaper(activity: Activity, savedPaper: SavedPaperEntity) {
        viewModelScope.launch {
            val payload = gitHubService.decodeSavedPaperPayload(savedPaper.payloadJson) ?: return@launch
            try {
                val pdfFile = ExamPdfGenerator.generateExamPdf(getApplication(), payload)
                dao.insertSavedPaper(savedPaper.copy(pdfFilePath = pdfFile.absolutePath))
                ExamPdfGenerator.printExamPdf(activity, pdfFile, pdfFile.nameWithoutExtension)
            } catch (e: Exception) {
                _statusMessage.value = "Print failed: ${e.localizedMessage}"
            }
        }
    }

    fun loadSavedPaperIntoEditor(savedPaper: SavedPaperEntity, openPreviewDirectly: Boolean = true) {
        val payload = gitHubService.decodeSavedPaperPayload(savedPaper.payloadJson) ?: return
        _paperHeader.value = payload.header
        _selectedMcqs.value = payload.mcqs
        _selectedShortQuestions.value = payload.shortQuestions
        _selectedLongQuestions.value = payload.longQuestions
        _selectedClassLevel.value = savedPaper.classLevel
        _activeScreen.value = if (openPreviewDirectly) {
            ActiveScreen.PAPER_PREVIEW
        } else {
            ActiveScreen.QUESTION_PICKER_AND_BUILDER
        }
    }

    fun exportExistingSavedPaperToPdf(savedPaper: SavedPaperEntity, share: Boolean = true) {
        viewModelScope.launch {
            val payload = gitHubService.decodeSavedPaperPayload(savedPaper.payloadJson) ?: return@launch
            try {
                val pdfFile = ExamPdfGenerator.generateExamPdf(getApplication(), payload)
                dao.insertSavedPaper(savedPaper.copy(pdfFilePath = pdfFile.absolutePath))
                _statusMessage.value = "Saved in Phone: ${ExamPdfGenerator.PUBLIC_DOWNLOAD_FOLDER_DISPLAY}/${pdfFile.name}"
                ExamPdfGenerator.shareOrOpenPdf(getApplication(), pdfFile, share = share)
            } catch (e: Exception) {
                _statusMessage.value = "PDF generation failed: ${e.localizedMessage}"
            }
        }
    }

    fun deleteSavedPaper(paperId: Long) {
        viewModelScope.launch {
            dao.deleteSavedPaperById(paperId)
            _statusMessage.value = "Saved paper deleted."
        }
    }

    fun syncDatabaseFromGitHub(config: GitHubRepoConfig) {
        viewModelScope.launch {
            _isSyncingGitHub.value = true
            gitHubService.saveConfig(config)
            when (val res = gitHubService.fetchBankFromGitHub(config)) {
                is GitHubSyncResult.Success -> {
                    val payload = res.payload
                    if (payload != null) {
                        applyBankPayloadToDatabase(payload)
                    }
                    _statusMessage.value = res.message
                }
                is GitHubSyncResult.Error -> {
                    _statusMessage.value = res.errorMessage
                }
            }
            _isSyncingGitHub.value = false
        }
    }

    fun addOrUpdateChapter(
        existingChapterId: String?,
        subject: SubjectEntity,
        chapterNumber: Int,
        titleEn: String,
        titleUr: String
    ) {
        viewModelScope.launch {
            val cleanEn = titleEn.trim().ifBlank { "Ch $chapterNumber: New Topic" }
            val cleanUr = titleUr.trim().ifBlank { cleanEn }
            val chapterId = existingChapterId ?: "${subject.id}_ch_${System.currentTimeMillis()}"
            val entity = ChapterEntity(
                id = chapterId,
                subjectId = subject.id,
                classLevel = subject.classLevel,
                chapterNumber = chapterNumber,
                titleEn = cleanEn,
                titleUr = cleanUr,
                isFreeByDefault = (chapterNumber == 1)
            )
            dao.insertChapter(entity)
            _statusMessage.value = "Chapter saved: $cleanEn"
        }
    }

    fun deleteChapter(chapterId: String) {
        viewModelScope.launch {
            dao.deleteQuestionsByChapter(chapterId)
            dao.deleteChapterById(chapterId)
            _statusMessage.value = "Chapter deleted."
        }
    }

    fun bulkAddOrReplaceQuestions(
        chapter: ChapterEntity,
        type: QuestionType,
        rawText: String,
        replaceExisting: Boolean
    ) {
        if (rawText.isBlank()) {
            _statusMessage.value = "Please paste questions text first."
            return
        }
        viewModelScope.launch {
            val parsed = if (type == QuestionType.MCQ) {
                BulkQuestionParser.parseMcqQuestions(
                    rawText = rawText,
                    chapter = chapter
                )
            } else {
                BulkQuestionParser.parseShortOrLongQuestions(
                    rawText = rawText,
                    chapter = chapter,
                    type = type
                )
            }
            if (parsed.isEmpty()) {
                _statusMessage.value = "Could not parse questions from text."
                return@launch
            }
            if (replaceExisting) {
                val normalized = BulkQuestionParser.normalizeAndPairChapterQuestions(parsed)
                val enriched = com.example.data.UrduEnglishAutoTranslator.autoTranslateQuestionsSuspend(normalized)
                dao.deleteQuestionsByChapterAndType(chapter.id, type.code)
                dao.insertQuestions(enriched)
                _statusMessage.value = "Updated ${enriched.size} ${type.titleEn} (Auto-English, Auto-Urdu & Bilingual Ready) in ${chapter.titleEn}!"
            } else {
                val existingInChapterType = dao.getAllQuestionsOnce()
                    .filter { it.chapterId == chapter.id && it.type == type.code }
                val combined = BulkQuestionParser.normalizeAndPairChapterQuestions(existingInChapterType + parsed)
                val enriched = com.example.data.UrduEnglishAutoTranslator.autoTranslateQuestionsSuspend(combined)
                dao.deleteQuestionsByChapterAndType(chapter.id, type.code)
                dao.insertQuestions(enriched)
                _statusMessage.value = "Updated ${enriched.size} ${type.titleEn} (Auto-English, Auto-Urdu & Bilingual Ready) in ${chapter.titleEn}!"
            }
        }
    }

    fun deleteSingleQuestion(questionId: String) {
        viewModelScope.launch {
            dao.deleteQuestionById(questionId)
            _statusMessage.value = "Question removed."
        }
    }

    fun uploadCurrentDatabaseToGitHub(config: GitHubRepoConfig) {
        viewModelScope.launch {
            _isSyncingGitHub.value = true
            gitHubService.saveConfig(config)
            val adState = AdManager.state.value
            val payload = GitHubPaperBankPayload(
                adConfig = RemoteAdConfig(
                    adsEnabled = adState.adsEnabled,
                    useTestAds = adState.useTestAds,
                    appId = adState.appId,
                    bannerAdUnitId = adState.bannerAdUnitId,
                    interstitialAdUnitId = adState.interstitialAdUnitId,
                    rewardedAdUnitId = adState.rewardedAdUnitId,
                    updatedAt = System.currentTimeMillis()
                ),
                subjects = dao.getAllSubjectsOnce(),
                chapters = dao.getAllChaptersOnce(),
                questions = dao.getAllQuestionsOnce()
            )
            when (val res = gitHubService.pushBankToGitHub(config, payload)) {
                is GitHubSyncResult.Success -> _statusMessage.value = res.message
                is GitHubSyncResult.Error -> _statusMessage.value = res.errorMessage
            }
            _isSyncingGitHub.value = false
        }
    }

    private suspend fun applyBankPayloadToDatabase(payload: GitHubPaperBankPayload) {
        if (payload.subjects.isNotEmpty()) {
            dao.clearAllSubjects()
            dao.insertSubjects(payload.subjects)
        }
        if (payload.chapters.isNotEmpty()) {
            dao.clearAllChapters()
            dao.insertChapters(payload.chapters)
        }
        if (payload.questions.isNotEmpty()) {
            dao.clearAllQuestions()
            val grouped = payload.questions.groupBy { "${it.chapterId}::${it.type}" }
            val normalizedAll = grouped.values.flatMap { BulkQuestionParser.normalizeAndPairChapterQuestions(it) }
            dao.insertQuestions(normalizedAll)
        }
        AdManager.applyRemoteConfigIfPresent(getApplication(), payload.adConfig)
    }
}
