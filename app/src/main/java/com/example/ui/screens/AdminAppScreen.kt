package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ads.AdManager
import com.example.data.ChapterEntity
import com.example.data.GitHubRepoConfig
import com.example.data.QuestionEntity
import com.example.data.QuestionType
import com.example.data.SubjectEntity
import com.example.ui.PaperMakerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAppScreen(
    viewModel: PaperMakerViewModel,
    isStandaloneAdminApk: Boolean = BuildConfig.IS_ADMIN_APK
) {
    if (!isStandaloneAdminApk) {
        BackHandler { viewModel.navigateBack() }
    }

    val allSubjects by viewModel.allSubjects.collectAsState()
    val allChapters by viewModel.allChapters.collectAsState()
    val allQuestions by viewModel.allQuestions.collectAsState()
    val isSyncing by viewModel.isSyncingGitHub.collectAsState()

    var adminTabIndex by remember { mutableIntStateOf(0) }
    var selectedClass by remember { mutableStateOf("9") }

    val classSubjects = remember(allSubjects, selectedClass) {
        allSubjects.filter { it.classLevel == selectedClass }.sortedBy { it.orderIndex }
    }
    var selectedSubjectId by remember(classSubjects) {
        mutableStateOf(classSubjects.firstOrNull()?.id ?: "")
    }
    val activeSubject = classSubjects.find { it.id == selectedSubjectId } ?: classSubjects.firstOrNull()

    val subjectChapters = remember(allChapters, activeSubject) {
        if (activeSubject == null) emptyList()
        else allChapters.filter { it.subjectId == activeSubject.id }.sortedBy { it.chapterNumber }
    }
    var selectedChapterId by remember(subjectChapters) {
        mutableStateOf(subjectChapters.firstOrNull()?.id ?: "")
    }
    val activeChapter = subjectChapters.find { it.id == selectedChapterId } ?: subjectChapters.firstOrNull()

    val savedGitHub = remember { viewModel.gitHubService.getConfig() }
    var ghOwner by remember { mutableStateOf(savedGitHub.owner) }
    var ghRepo by remember { mutableStateOf(savedGitHub.repo) }
    var ghBranch by remember { mutableStateOf(savedGitHub.branch) }
    var ghPath by remember { mutableStateOf(savedGitHub.filePath) }
    var ghToken by remember { mutableStateOf(savedGitHub.token) }

    fun currentConfig(): GitHubRepoConfig = GitHubRepoConfig(
        owner = ghOwner,
        repo = ghRepo,
        branch = ghBranch,
        filePath = ghPath,
        token = ghToken
    )

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Shami Academy — Admin App",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Connected to Main App via GitHub API Token",
                        fontSize = 11.sp,
                        color = Color(0xFFFFD700)
                    )
                }
            },
            navigationIcon = {
                if (!isStandaloneAdminApk) {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Main App",
                            tint = Color.White
                        )
                    }
                }
            },
            actions = {
                Button(
                    onClick = {
                        viewModel.uploadCurrentDatabaseToGitHub(currentConfig())
                    },
                    enabled = !isSyncing,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("admin_top_upload_btn")
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(15.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text("Upload to Main App", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0B2447),
                titleContentColor = Color.White
            )
        )

        ScrollableTabRow(
            selectedTabIndex = adminTabIndex,
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = adminTabIndex == 0,
                onClick = { adminTabIndex = 0 },
                text = { Text("1. GitHub API Key", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = adminTabIndex == 1,
                onClick = { adminTabIndex = 1 },
                text = { Text("2. Edit Chapters", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = adminTabIndex == 2,
                onClick = { adminTabIndex = 2 },
                text = { Text("3. Add Questions", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = adminTabIndex == 3,
                onClick = { adminTabIndex = 3 },
                text = { Text("4. AdMob IDs", fontWeight = FontWeight.Bold) }
            )
        }

        when (adminTabIndex) {
            0 -> AdminGitHubApiSetupTab(
                ghOwner = ghOwner,
                onOwnerChange = { ghOwner = it },
                ghRepo = ghRepo,
                onRepoChange = { ghRepo = it },
                ghBranch = ghBranch,
                onBranchChange = { ghBranch = it },
                ghPath = ghPath,
                onPathChange = { ghPath = it },
                ghToken = ghToken,
                onTokenChange = { ghToken = it },
                isSyncing = isSyncing,
                onSaveApiConfig = {
                    viewModel.gitHubService.saveConfig(currentConfig())
                    viewModel.showStatus("GitHub API Token & Repo attached!")
                },
                onPullFromMainApp = {
                    viewModel.syncDatabaseFromGitHub(currentConfig())
                },
                onPushToMainApp = {
                    viewModel.uploadCurrentDatabaseToGitHub(currentConfig())
                }
            )
            1 -> AdminChaptersEditorTab(
                selectedClass = selectedClass,
                onSelectClass = { selectedClass = it },
                classSubjects = classSubjects,
                activeSubject = activeSubject,
                onSelectSubject = { selectedSubjectId = it.id },
                subjectChapters = subjectChapters,
                viewModel = viewModel,
                onPushToMainApp = {
                    viewModel.uploadCurrentDatabaseToGitHub(currentConfig())
                }
            )
            2 -> AdminQuestionsBulkTab(
                selectedClass = selectedClass,
                onSelectClass = { selectedClass = it },
                classSubjects = classSubjects,
                activeSubject = activeSubject,
                onSelectSubject = { selectedSubjectId = it.id },
                subjectChapters = subjectChapters,
                activeChapter = activeChapter,
                onSelectChapter = { selectedChapterId = it.id },
                allQuestions = allQuestions,
                viewModel = viewModel,
                onPushToMainApp = {
                    viewModel.uploadCurrentDatabaseToGitHub(currentConfig())
                }
            )
            3 -> AdminAdMobConfigTab(
                viewModel = viewModel,
                onPushToMainApp = {
                    viewModel.uploadCurrentDatabaseToGitHub(currentConfig())
                }
            )
        }
    }
}

@Composable
private fun AdminGitHubApiSetupTab(
    ghOwner: String,
    onOwnerChange: (String) -> Unit,
    ghRepo: String,
    onRepoChange: (String) -> Unit,
    ghBranch: String,
    onBranchChange: (String) -> Unit,
    ghPath: String,
    onPathChange: (String) -> Unit,
    ghToken: String,
    onTokenChange: (String) -> Unit,
    isSyncing: Boolean,
    onSaveApiConfig: () -> Unit,
    onPullFromMainApp: () -> Unit,
    onPushToMainApp: () -> Unit
) {
    var showTokenPlain by remember { mutableStateOf(false) }

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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = Color(0xFF0D9488)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Attach GitHub Secret Token & Repository API",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Enter your GitHub Personal Access Token (API Key) once below. Whenever you edit chapters, paste questions, or update AdMob IDs in this Admin App and tap Upload, the Main User App automatically receives all updates via the GitHub API.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = ghToken,
                        onValueChange = onTokenChange,
                        label = { Text("GitHub Secret Token / API Key (ghp_...)") },
                        singleLine = true,
                        visualTransformation = if (showTokenPlain) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            Text(
                                text = if (showTokenPlain) "Hide" else "Show",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0D9488),
                                modifier = Modifier
                                    .clickable { showTokenPlain = !showTokenPlain }
                                    .padding(horizontal = 8.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ghOwner,
                            onValueChange = onOwnerChange,
                            label = { Text("GitHub Username") },
                            placeholder = { Text("e.g. princeshami365") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = ghRepo,
                            onValueChange = onRepoChange,
                            label = { Text("Repository Name") },
                            placeholder = { Text("e.g. paper-maker") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = ghBranch,
                            onValueChange = onBranchChange,
                            label = { Text("Branch") },
                            singleLine = true,
                            modifier = Modifier.weight(0.45f)
                        )
                        OutlinedTextField(
                            value = ghPath,
                            onValueChange = onPathChange,
                            label = { Text("Database File Path") },
                            singleLine = true,
                            modifier = Modifier.weight(0.55f)
                        )
                    }

                    Button(
                        onClick = onSaveApiConfig,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Attach GitHub API Key", fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onPullFromMainApp,
                            enabled = !isSyncing,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Load from GitHub", fontSize = 12.sp)
                        }

                        Button(
                            onClick = onPushToMainApp,
                            enabled = !isSyncing,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload to Main App", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminChaptersEditorTab(
    selectedClass: String,
    onSelectClass: (String) -> Unit,
    classSubjects: List<SubjectEntity>,
    activeSubject: SubjectEntity?,
    onSelectSubject: (SubjectEntity) -> Unit,
    subjectChapters: List<ChapterEntity>,
    viewModel: PaperMakerViewModel,
    onPushToMainApp: () -> Unit
) {
    var editingChapter by remember { mutableStateOf<ChapterEntity?>(null) }
    var chapterNumInput by remember(subjectChapters.size) {
        mutableStateOf((subjectChapters.size + 1).toString())
    }
    var chapterTitleEnInput by remember { mutableStateOf("") }
    var chapterTitleUrInput by remember { mutableStateOf("") }

    LaunchedEffect(editingChapter) {
        val ch = editingChapter
        if (ch != null) {
            chapterNumInput = ch.chapterNumber.toString()
            chapterTitleEnInput = ch.titleEn
            chapterTitleUrInput = ch.titleUr
        } else {
            chapterNumInput = (subjectChapters.size + 1).toString()
            chapterTitleEnInput = ""
            chapterTitleUrInput = ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminClassAndBookSelectorCard(
                selectedClass = selectedClass,
                onSelectClass = onSelectClass,
                classSubjects = classSubjects,
                activeSubject = activeSubject,
                onSelectSubject = onSelectSubject
            )
        }

        if (activeSubject != null) {
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
                            text = if (editingChapter != null) {
                                "Edit Chapter Name (${activeSubject.nameEn})"
                            } else {
                                "Add New Chapter to ${activeSubject.nameEn}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = chapterNumInput,
                                onValueChange = { chapterNumInput = it.filter { c -> c.isDigit() } },
                                label = { Text("Ch #") },
                                singleLine = true,
                                modifier = Modifier.width(86.dp)
                            )
                            OutlinedTextField(
                                value = chapterTitleEnInput,
                                onValueChange = { chapterTitleEnInput = it },
                                label = { Text("Chapter Name (English)") },
                                placeholder = { Text("Ch 1: Fundamentals of Chemistry") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        OutlinedTextField(
                            value = chapterTitleUrInput,
                            onValueChange = { chapterTitleUrInput = it },
                            label = { Text("Chapter Name (Urdu - Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (editingChapter != null) {
                                OutlinedButton(onClick = { editingChapter = null }) {
                                    Text("Cancel")
                                }
                            }
                            Button(
                                onClick = {
                                    val num = chapterNumInput.toIntOrNull() ?: (subjectChapters.size + 1)
                                    viewModel.addOrUpdateChapter(
                                        existingChapterId = editingChapter?.id,
                                        subject = activeSubject,
                                        chapterNumber = num,
                                        titleEn = chapterTitleEnInput,
                                        titleUr = chapterTitleUrInput
                                    )
                                    editingChapter = null
                                    chapterTitleEnInput = ""
                                    chapterTitleUrInput = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (editingChapter != null) "Update Chapter" else "Save Chapter")
                            }

                            Button(
                                onClick = onPushToMainApp,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488))
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload to Main App")
                            }
                        }
                    }
                }
            }

            itemsIndexed(subjectChapters, key = { _, ch -> ch.id }) { _, ch ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ch.titleEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (ch.chapterNumber == 1) "Chapter 1 (Free in Main App)" else "Chapter ${ch.chapterNumber} (Locked by Rewarded Ad)",
                                fontSize = 11.sp,
                                color = Color(0xFF0D9488)
                            )
                        }
                        Row {
                            IconButton(onClick = { editingChapter = ch }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Chapter", tint = Color(0xFF0B2447))
                            }
                            IconButton(onClick = { viewModel.deleteChapter(ch.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Chapter", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminQuestionsBulkTab(
    selectedClass: String,
    onSelectClass: (String) -> Unit,
    classSubjects: List<SubjectEntity>,
    activeSubject: SubjectEntity?,
    onSelectSubject: (SubjectEntity) -> Unit,
    subjectChapters: List<ChapterEntity>,
    activeChapter: ChapterEntity?,
    onSelectChapter: (ChapterEntity) -> Unit,
    allQuestions: List<QuestionEntity>,
    viewModel: PaperMakerViewModel,
    onPushToMainApp: () -> Unit
) {
    var selectedType by remember { mutableStateOf(QuestionType.SHORT) }
    var replaceExisting by remember { mutableStateOf(false) }
    var bulkText by remember { mutableStateOf("") }

    val chapterQuestions = remember(allQuestions, activeChapter, selectedType) {
        if (activeChapter == null) emptyList()
        else allQuestions.filter { it.chapterId == activeChapter.id && it.type == selectedType.code }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AdminClassAndBookSelectorCard(
                selectedClass = selectedClass,
                onSelectClass = onSelectClass,
                classSubjects = classSubjects,
                activeSubject = activeSubject,
                onSelectSubject = onSelectSubject
            )
        }

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
                        text = "3. Select Chapter:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        subjectChapters.forEach { ch ->
                            val isSelected = activeChapter?.id == ch.id
                            FilterChip(
                                selected = isSelected,
                                onClick = { onSelectChapter(ch) },
                                label = { Text(ch.titleEn, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0D9488),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "4. Select Question Type:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuestionType.entries.forEach { qType ->
                            FilterChip(
                                selected = selectedType == qType,
                                onClick = { selectedType = qType },
                                label = { Text(qType.code, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0B2447),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = bulkText,
                        onValueChange = { bulkText = it },
                        label = {
                            Text(
                                if (selectedType == QuestionType.MCQ) {
                                    "Paste MCQs (Urdu Only, English Only, or Both -> Auto-Translates)"
                                } else {
                                    "Paste ${selectedType.titleEn} (Urdu Only, English Only, or Both -> Auto-Translates)"
                                }
                            )
                        },
                        supportingText = {
                            Text(
                                text = "آپ صرف اردو میڈیم پیسٹ کریں تو انگلش آٹو بن جائے گی، اور اگر صرف انگلش میڈیم پیسٹ کریں تو اردو میڈیم آٹو بن جائے گا! بائی لینگویج میں دونوں خود بخود شو ہوں گے۔",
                                fontSize = 11.sp,
                                color = Color(0xFF0D9488)
                            )
                        },
                        minLines = 5,
                        maxLines = 10,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Replace existing ${selectedType.code}s in this chapter",
                            fontSize = 12.sp
                        )
                        Switch(
                            checked = replaceExisting,
                            onCheckedChange = { replaceExisting = it }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (activeChapter != null) {
                                    viewModel.bulkAddOrReplaceQuestions(
                                        chapter = activeChapter,
                                        type = selectedType,
                                        rawText = bulkText,
                                        replaceExisting = replaceExisting
                                    )
                                    bulkText = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save & Auto-Translate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = onPushToMainApp,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("Upload to Main App", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        itemsIndexed(chapterQuestions, key = { _, q -> q.id }) { idx, q ->
            val enText = q.resolvedQuestionEn()
            val urText = q.resolvedQuestionUr()
            Card(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (enText.isNotBlank()) {
                            Text(
                                text = "${idx + 1}. $enText",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFF0B2447)
                            )
                        }
                        if (urText.isNotBlank() && urText != enText) {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = "\u200F${idx + 1}۔ $urText",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF0D9488),
                                    textAlign = TextAlign.Right,
                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.deleteSingleQuestion(q.id) }) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Question",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminAdMobConfigTab(
    viewModel: PaperMakerViewModel,
    onPushToMainApp: () -> Unit
) {
    val context = LocalContext.current
    val adState by AdManager.state.collectAsState()

    var adsEnabled by remember(adState.adsEnabled) { mutableStateOf(adState.adsEnabled) }
    var useTestAds by remember(adState.useTestAds) { mutableStateOf(adState.useTestAds) }
    var appId by remember(adState.appId) { mutableStateOf(adState.appId) }
    var bannerId by remember(adState.bannerAdUnitId) { mutableStateOf(adState.bannerAdUnitId) }
    var interstitialId by remember(adState.interstitialAdUnitId) { mutableStateOf(adState.interstitialAdUnitId) }
    var rewardedId by remember(adState.rewardedAdUnitId) { mutableStateOf(adState.rewardedAdUnitId) }

    fun saveImmediately(
        newAdsEnabled: Boolean = adsEnabled,
        newUseTestAds: Boolean = useTestAds,
        newAppId: String = appId,
        newBannerId: String = bannerId,
        newInterstitialId: String = interstitialId,
        newRewardedId: String = rewardedId
    ) {
        AdManager.updateAdConfig(
            context = context,
            adsEnabled = newAdsEnabled,
            useTestAds = newUseTestAds,
            appId = newAppId,
            bannerId = newBannerId,
            interstitialId = newInterstitialId,
            rewardedId = newRewardedId
        )
    }

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
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AdsClick, contentDescription = null, tint = Color(0xFFF59E0B))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Google AdMob Manager (Instant Auto-Save)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // SWITCH 1: Master Switch to Turn All Google Ads ON or OFF
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (adsEnabled) "Google Ads in App: ON" else "Google Ads in App: OFF (Disabled)",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (adsEnabled) Color(0xFF10B981) else MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "Turn OFF to completely disable all Banner, Interstitial & Rewarded Ads in the app.",
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = adsEnabled,
                            onCheckedChange = { newValue ->
                                adsEnabled = newValue
                                saveImmediately(newAdsEnabled = newValue)
                                viewModel.showStatus(
                                    if (newValue) "Google Ads turned ON & saved!" else "Google Ads turned OFF & saved!"
                                )
                            }
                        )
                    }

                    // SWITCH 2: Test Ads vs Live AdMob IDs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (useTestAds) "Google Test Ads Mode: ON" else "Live AdMob IDs Mode: ON (Test Ads OFF)",
                                fontWeight = FontWeight.Bold,
                                color = if (useTestAds) Color(0xFFF59E0B) else Color(0xFF10B981)
                            )
                            Text(
                                text = "Turn OFF this switch to use your own real Google AdMob IDs below. Saved automatically on toggle.",
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = useTestAds,
                            onCheckedChange = { newValue ->
                                useTestAds = newValue
                                saveImmediately(newUseTestAds = newValue)
                                viewModel.showStatus(
                                    if (newValue) "Test Ads Mode ON & saved!" else "Test Ads Mode OFF (Live IDs Active) & saved!"
                                )
                            }
                        )
                    }

                    OutlinedTextField(
                        value = appId,
                        onValueChange = {
                            appId = it
                            saveImmediately(newAppId = it)
                        },
                        label = { Text("AdMob App ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = bannerId,
                        onValueChange = {
                            bannerId = it
                            saveImmediately(newBannerId = it)
                        },
                        label = { Text("Banner Ad Unit ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = interstitialId,
                        onValueChange = {
                            interstitialId = it
                            saveImmediately(newInterstitialId = it)
                        },
                        label = { Text("Interstitial Ad Unit ID") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = rewardedId,
                        onValueChange = {
                            rewardedId = it
                            saveImmediately(newRewardedId = it)
                        },
                        label = { Text("Rewarded Ad Unit ID (Book & Chapter Unlocks)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            saveImmediately()
                            onPushToMainApp()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Push AdMob Config to GitHub", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminClassAndBookSelectorCard(
    selectedClass: String,
    onSelectClass: (String) -> Unit,
    classSubjects: List<SubjectEntity>,
    activeSubject: SubjectEntity?,
    onSelectSubject: (SubjectEntity) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("1. Select Class:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = selectedClass == "9",
                    onClick = { onSelectClass("9") },
                    label = { Text("Class 9th", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0B2447),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedClass == "10",
                    onClick = { onSelectClass("10") },
                    label = { Text("Class 10th", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0B2447),
                        selectedLabelColor = Color.White
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Text("2. Select Subject / Book:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                classSubjects.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { subj ->
                            FilterChip(
                                selected = activeSubject?.id == subj.id,
                                onClick = { onSelectSubject(subj) },
                                label = { Text(subj.nameEn, fontSize = 11.sp, maxLines = 1) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0D9488),
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
