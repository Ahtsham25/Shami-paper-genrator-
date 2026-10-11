package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.app.Activity
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.ads.AdMobBannerBar
import com.example.data.PaperLanguage
import com.example.data.QuestionEntity
import com.example.data.QuestionType
import com.example.ui.PaperMakerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectChaptersScreen(
    viewModel: PaperMakerViewModel
) {
    BackHandler { viewModel.navigateBack() }

    val context = LocalContext.current
    val activity = context as? Activity
    val subject by viewModel.selectedSubject.collectAsState()
    val allChapters by viewModel.allChapters.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()
    val selectedChapterIds by viewModel.selectedChapterIds.collectAsState()
    val unlockedChapterIds by viewModel.unlockedChapterIds.collectAsState()
    val adState by AdManager.state.collectAsState()

    val currentSubject = subject ?: return
    val chapters = remember(allChapters, currentSubject.id) {
        allChapters.filter { it.subjectId == currentSubject.id }.sortedBy { it.chapterNumber }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = currentSubject.nameEn,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Class ${currentSubject.classLevel}th • Select Chapters",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0B2447),
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chapters, key = { it.id }) { chapter ->
                val isUnlocked = !adState.adsEnabled ||
                    (chapter.chapterNumber == 1) ||
                    chapter.isFreeByDefault ||
                    unlockedChapterIds.contains(chapter.id)

                val isSelected = selectedChapterIds.contains(chapter.id)
                val chQuestions = allQuestions.filter { it.chapterId == chapter.id }
                val mcqCount = chQuestions.count { it.type == QuestionType.MCQ.code }
                val shortCount = chQuestions.count { it.type == QuestionType.SHORT.code }
                val longCount = chQuestions.count { it.type == QuestionType.LONG.code }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (isUnlocked) {
                                viewModel.toggleChapterSelection(chapter)
                            } else {
                                // Immediately launch the real Google AdMob full-screen Rewarded Ad (no intermediate dialog!)
                                AdManager.triggerRewardedUnlock(
                                    activity = activity,
                                    context = context,
                                    onUnlocked = {
                                        viewModel.unlockChapterViaRewardedAd(chapter.id)
                                    }
                                )
                            }
                        }
                        .border(
                            width = if (isSelected && isUnlocked) 2.dp else 1.dp,
                            color = if (isSelected && isUnlocked) Color(0xFF0D9488)
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("chapter_card_${chapter.id}"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (isUnlocked) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { viewModel.toggleChapterSelection(chapter) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }

                            Column {
                                Text(
                                    text = chapter.titleEn,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "MCQs: $mcqCount  •  Short: $shortCount  •  Long: $longCount",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Only show the Lock icon on locked chapters (no "Watch Ad" text!)
                        if (!isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0B2447))
                                    .testTag("unlock_chapter_btn_${chapter.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Locked Chapter",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${selectedChapterIds.size} Chapter(s) Selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Select MCQs, Short & Long Questions next",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.proceedToQuestionPicker() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("proceed_to_questions_button")
                    ) {
                        Text("Pick Questions", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                AdMobBannerBar()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPickerAndBuilderScreen(
    viewModel: PaperMakerViewModel
) {
    BackHandler { viewModel.navigateBack() }

    val subject by viewModel.selectedSubject.collectAsState()
    val selectedChapterIds by viewModel.selectedChapterIds.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()
    val selectedMcqs by viewModel.selectedMcqs.collectAsState()
    val selectedShorts by viewModel.selectedShortQuestions.collectAsState()
    val selectedLongs by viewModel.selectedLongQuestions.collectAsState()
    val paperHeader by viewModel.paperHeader.collectAsState()
    val currentLanguage = PaperLanguage.fromCode(paperHeader.languageMode)

    val availableQuestions = remember(allQuestions, selectedChapterIds, subject) {
        if (selectedChapterIds.isNotEmpty()) {
            allQuestions.filter { it.chapterId in selectedChapterIds }
        } else if (subject != null) {
            allQuestions.filter { it.subjectId == subject!!.id }
        } else {
            allQuestions
        }
    }

    val mcqPool = remember(availableQuestions, currentLanguage) {
        val raw = availableQuestions.filter { it.type == QuestionType.MCQ.code }
        val filtered = raw.filter { it.matchesLanguage(currentLanguage) }
        filtered.ifEmpty { raw }
    }
    val shortPool = remember(availableQuestions, currentLanguage) {
        val raw = availableQuestions.filter { it.type == QuestionType.SHORT.code }
        val filtered = raw.filter { it.matchesLanguage(currentLanguage) }
        filtered.ifEmpty { raw }
    }
    val longPool = remember(availableQuestions, currentLanguage) {
        val raw = availableQuestions.filter { it.type == QuestionType.LONG.code }
        val filtered = raw.filter { it.matchesLanguage(currentLanguage) }
        filtered.ifEmpty { raw }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val totalMarks = selectedMcqs.sumOf { it.marks } +
        selectedShorts.sumOf { it.marks } +
        selectedLongs.sumOf { it.marks }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Pick Questions & Build Paper",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Marks: $totalMarks • MCQs(${selectedMcqs.size}) | Short(${selectedShorts.size}) | Long(${selectedLongs.size})",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD700)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.navigateBack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                Button(
                    onClick = { viewModel.openPaperPreview() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D9488),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = "Full Preview",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Full Preview", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0B2447),
                titleContentColor = Color.White
            )
        )

        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            LanguageModeSelectorRow(
                currentLanguage = currentLanguage,
                onSelectLanguage = { viewModel.setPaperLanguage(it) }
            )
        }

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("Q.1 MCQs (${selectedMcqs.size}/${mcqPool.size})", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_pick_mcqs")
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("Q.2 Short Qs (${selectedShorts.size}/${shortPool.size})", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_pick_shorts")
            )
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("Q.3 Long Qs (${selectedLongs.size}/${longPool.size})", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_pick_longs")
            )
            Tab(
                selected = selectedTabIndex == 3,
                onClick = { selectedTabIndex = 3 },
                text = { Text("Drag & Drop + Header", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_drag_and_header")
            )
            Tab(
                selected = selectedTabIndex == 4,
                onClick = { selectedTabIndex = 4 },
                text = { Text("Full Preview (A4/Legal)", fontWeight = FontWeight.ExtraBold) },
                modifier = Modifier.testTag("tab_live_full_preview")
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTabIndex) {
                0 -> QuestionChecklistTab(
                    questions = mcqPool,
                    selectedIds = selectedMcqs.map { it.id }.toSet(),
                    type = QuestionType.MCQ,
                    language = currentLanguage,
                    onToggleQuestion = { viewModel.toggleQuestionSelection(it) },
                    onSelectAll = { viewModel.selectAllInList(mcqPool, QuestionType.MCQ) },
                    onClearAll = { viewModel.clearSelectedQuestionsByType(QuestionType.MCQ) }
                )
                1 -> QuestionChecklistTab(
                    questions = shortPool,
                    selectedIds = selectedShorts.map { it.id }.toSet(),
                    type = QuestionType.SHORT,
                    language = currentLanguage,
                    onToggleQuestion = { viewModel.toggleQuestionSelection(it) },
                    onSelectAll = { viewModel.selectAllInList(shortPool, QuestionType.SHORT) },
                    onClearAll = { viewModel.clearSelectedQuestionsByType(QuestionType.SHORT) }
                )
                2 -> QuestionChecklistTab(
                    questions = longPool,
                    selectedIds = selectedLongs.map { it.id }.toSet(),
                    type = QuestionType.LONG,
                    language = currentLanguage,
                    onToggleQuestion = { viewModel.toggleQuestionSelection(it) },
                    onSelectAll = { viewModel.selectAllInList(longPool, QuestionType.LONG) },
                    onClearAll = { viewModel.clearSelectedQuestionsByType(QuestionType.LONG) }
                )
                3 -> DragAndDropAndHeaderEditorTab(
                    viewModel = viewModel
                )
                4 -> PaperPreviewWorkspaceContent(
                    viewModel = viewModel,
                    onOpenFullScreen = { viewModel.openPaperPreview() },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Auto Total Marks: $totalMarks",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${paperHeader.paperSize} • Font ${paperHeader.fontSizePt}pt • Q1:${selectedMcqs.size} | Q2:${selectedShorts.size} | Q3:${selectedLongs.size}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { viewModel.openPaperPreview() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.testTag("open_paper_preview_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Full Preview & PDF", fontWeight = FontWeight.Bold)
                    }
                }
                AdMobBannerBar()
            }
        }
    }
}

@Composable
private fun QuestionChecklistTab(
    questions: List<QuestionEntity>,
    selectedIds: Set<String>,
    type: QuestionType,
    language: PaperLanguage,
    onToggleQuestion: (QuestionEntity) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = type.titleEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSelectAll,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("select_all_${type.code}")
                    ) {
                        Icon(Icons.Default.SelectAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pick All", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onClearAll,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ClearAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", fontSize = 12.sp)
                    }
                }
            }
        }

        itemsIndexed(questions, key = { idx, q -> "${q.id}_$idx" }) { idx, q ->
            val checked = q.id in selectedIds
            val cleanEn = remember(q.id, q.questionEn, q.questionUr) { q.resolvedQuestionEn() }
            val cleanUr = remember(q.id, q.questionEn, q.questionUr) { q.resolvedQuestionUr() }
            val optAEn = remember(q.id, q.optionAEn, q.optionAUr) { q.resolvedOptionAEn() }
            val optBEn = remember(q.id, q.optionBEn, q.optionBUr) { q.resolvedOptionBEn() }
            val optCEn = remember(q.id, q.optionCEn, q.optionCUr) { q.resolvedOptionCEn() }
            val optDEn = remember(q.id, q.optionDEn, q.optionDUr) { q.resolvedOptionDEn() }
            val optAUr = remember(q.id, q.optionAEn, q.optionAUr) { q.resolvedOptionAUr() }
            val optBUr = remember(q.id, q.optionBEn, q.optionBUr) { q.resolvedOptionBUr() }
            val optCUr = remember(q.id, q.optionCEn, q.optionCUr) { q.resolvedOptionCUr() }
            val optDUr = remember(q.id, q.optionDEn, q.optionDUr) { q.resolvedOptionDUr() }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onToggleQuestion(q) }
                    .border(
                        width = if (checked) 2.dp else 1.dp,
                        color = if (checked) Color(0xFF0D9488) else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .testTag("question_item_${q.id}"),
                shape = RoundedCornerShape(14.dp)
            ) {
                val cardDirection = if (language == PaperLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides cardDirection) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                            contentDescription = if (checked) "Selected" else "Not Selected",
                            tint = if (checked) Color(0xFF0D9488) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (language == PaperLanguage.URDU) "سوال #${idx + 1}" else "Q #${idx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0D9488)
                                )
                                Text(
                                    text = if (language == PaperLanguage.URDU) "${q.marks} نمبر" else "${q.marks} Marks",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            when (language) {
                                PaperLanguage.ENGLISH -> {
                                    Text(
                                        text = cleanEn.ifBlank { q.questionEn },
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                PaperLanguage.URDU -> {
                                    Text(
                                        text = "\u200F${cleanUr.ifBlank { q.questionUr }}",
                                        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl),
                                        textAlign = TextAlign.Right,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                PaperLanguage.BILINGUAL -> {
                                    if (cleanEn.isNotBlank()) {
                                        Text(
                                            text = cleanEn,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    if (cleanUr.isNotBlank()) {
                                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                            Text(
                                                text = "\u200F$cleanUr",
                                                style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Rtl),
                                                textAlign = TextAlign.Right,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0F766E),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            if (q.type == QuestionType.MCQ.code) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val optionsLine = when (language) {
                                    PaperLanguage.ENGLISH ->
                                        "(A) $optAEn   (B) $optBEn   (C) $optCEn   (D) $optDEn"
                                    PaperLanguage.URDU ->
                                        "\u200F(الف) $optAUr   (ب) $optBUr   (ج) $optCUr   (د) $optDUr"
                                    PaperLanguage.BILINGUAL ->
                                        "(A) $optAEn/$optAUr  (B) $optBEn/$optBUr  (C) $optCEn/$optCUr  (D) $optDEn/$optDUr"
                                }
                                Text(
                                    text = optionsLine,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = if (language == PaperLanguage.URDU) TextAlign.Right else TextAlign.Start,
                                    style = LocalTextStyle.current.copy(
                                        textDirection = if (language == PaperLanguage.URDU) TextDirection.Rtl else TextDirection.Ltr
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DragAndDropAndHeaderEditorTab(
    viewModel: PaperMakerViewModel
) {
    val header by viewModel.paperHeader.collectAsState()
    val mcqs by viewModel.selectedMcqs.collectAsState()
    val shorts by viewModel.selectedShortQuestions.collectAsState()
    val longs by viewModel.selectedLongQuestions.collectAsState()
    val totalMarks = mcqs.sumOf { it.marks } + shorts.sumOf { it.marks } + longs.sumOf { it.marks }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Exam Paper Header Details",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Marks ($totalMarks) & Subject (${header.subjectNameEn}) are automatically calculated. You can also edit these directly on the Paper Preview!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = header.institutionName,
                        onValueChange = { viewModel.updatePaperHeader(header.copy(institutionName = it)) },
                        label = { Text("Institution / Academy Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("header_institution_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = header.studentNameValue,
                            onValueChange = { viewModel.updatePaperHeader(header.copy(studentNameValue = it)) },
                            label = { Text("Student Name") },
                            placeholder = { Text("Blank line if empty") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = header.rollNumberValue,
                            onValueChange = { viewModel.updatePaperHeader(header.copy(rollNumberValue = it)) },
                            label = { Text("Roll No") },
                            placeholder = { Text("Blank if empty") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = header.subjectNameEn,
                            onValueChange = { viewModel.updatePaperHeader(header.copy(subjectNameEn = it)) },
                            label = { Text("Book / Subject") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = header.timeAllowed,
                            onValueChange = { viewModel.updatePaperHeader(header.copy(timeAllowed = it)) },
                            label = { Text("Time Allowed") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Include MCQ Answer Key at bottom of PDF",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Switch(
                            checked = header.includeAnswerKey,
                            onCheckedChange = { viewModel.updatePaperHeader(header.copy(includeAnswerKey = it)) }
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Drag & Drop / Reorder Selected Questions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Hold & drag the handle icon (☰) or tap ↑ / ↓ arrows to reorder questions in Q.1, Q.2, and Q.3.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (mcqs.isNotEmpty()) {
            item {
                SectionReorderHeader("Q.1: Selected MCQs (${mcqs.size} items • ${mcqs.sumOf { it.marks }} Marks)")
            }
            itemsIndexed(mcqs, key = { idx, q -> "reorder_mcq_${q.id}_$idx" }) { idx, q ->
                DraggableSelectedQuestionRow(
                    index = idx,
                    totalCount = mcqs.size,
                    question = q,
                    language = PaperLanguage.fromCode(header.languageMode),
                    onMoveUp = { viewModel.moveSelectedQuestion(QuestionType.MCQ, idx, idx - 1) },
                    onMoveDown = { viewModel.moveSelectedQuestion(QuestionType.MCQ, idx, idx + 1) },
                    onRemove = { viewModel.toggleQuestionSelection(q) }
                )
            }
        }

        if (shorts.isNotEmpty()) {
            item {
                SectionReorderHeader("Q.2: Selected Short Questions (${shorts.size} items • ${shorts.sumOf { it.marks }} Marks)")
            }
            itemsIndexed(shorts, key = { idx, q -> "reorder_short_${q.id}_$idx" }) { idx, q ->
                DraggableSelectedQuestionRow(
                    index = idx,
                    totalCount = shorts.size,
                    question = q,
                    language = PaperLanguage.fromCode(header.languageMode),
                    onMoveUp = { viewModel.moveSelectedQuestion(QuestionType.SHORT, idx, idx - 1) },
                    onMoveDown = { viewModel.moveSelectedQuestion(QuestionType.SHORT, idx, idx + 1) },
                    onRemove = { viewModel.toggleQuestionSelection(q) }
                )
            }
        }

        if (longs.isNotEmpty()) {
            item {
                SectionReorderHeader("Q.3: Selected Long Questions (${longs.size} items • ${longs.sumOf { it.marks }} Marks)")
            }
            itemsIndexed(longs, key = { idx, q -> "reorder_long_${q.id}_$idx" }) { idx, q ->
                DraggableSelectedQuestionRow(
                    index = idx,
                    totalCount = longs.size,
                    question = q,
                    language = PaperLanguage.fromCode(header.languageMode),
                    onMoveUp = { viewModel.moveSelectedQuestion(QuestionType.LONG, idx, idx - 1) },
                    onMoveDown = { viewModel.moveSelectedQuestion(QuestionType.LONG, idx, idx + 1) },
                    onRemove = { viewModel.toggleQuestionSelection(q) }
                )
            }
        }
    }
}

@Composable
private fun SectionReorderHeader(title: String) {
    Surface(
        color = Color(0xFF0B2447),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun DraggableSelectedQuestionRow(
    index: Int,
    totalCount: Int,
    question: QuestionEntity,
    language: PaperLanguage,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    var accumulatedDragY by remember { mutableFloatStateOf(0f) }

    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(index, totalCount) {
                detectVerticalDragGestures(
                    onDragStart = { accumulatedDragY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDragY += dragAmount
                        if (accumulatedDragY < -65f && index > 0) {
                            accumulatedDragY = 0f
                            onMoveUp()
                        } else if (accumulatedDragY > 65f && index < totalCount - 1) {
                            accumulatedDragY = 0f
                            onMoveDown()
                        }
                    }
                )
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.DragIndicator,
                contentDescription = "Drag to reorder",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                val cleanEn = remember(question.id, question.questionEn, question.questionUr) { question.resolvedQuestionEn() }
                val cleanUr = remember(question.id, question.questionEn, question.questionUr) { question.resolvedQuestionUr() }
                when (language) {
                    PaperLanguage.ENGLISH -> {
                        Text(
                            text = "${index + 1}. ${cleanEn.ifBlank { question.questionEn }}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                    }
                    PaperLanguage.URDU -> {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = "\u200F${index + 1}۔ ${cleanUr.ifBlank { question.questionUr }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Right,
                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                                maxLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    PaperLanguage.BILINGUAL -> {
                        if (cleanEn.isNotBlank()) {
                            Text(
                                text = "${index + 1}. $cleanEn",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                        }
                        if (cleanUr.isNotBlank()) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "\u200F${index + 1}۔ $cleanUr",
                                    fontSize = 12.sp,
                                    color = Color(0xFF0D9488),
                                    textAlign = TextAlign.Right,
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                                    maxLines = 2,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            IconButton(
                onClick = onMoveUp,
                enabled = index > 0,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onMoveDown,
                enabled = index < totalCount - 1,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(18.dp))
            }
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Remove Question",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
