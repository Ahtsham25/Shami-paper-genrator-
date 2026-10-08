package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ads.AdMobBannerBar
import com.example.ads.AutoPlayingRewardedAdOverlay
import com.example.data.PaperLanguage
import com.example.data.SubjectEntity
import com.example.ui.PaperMakerViewModel

@Composable
fun HomeScreen(
    viewModel: PaperMakerViewModel
) {
    val subjects by viewModel.allSubjects.collectAsState()
    val chapters by viewModel.allChapters.collectAsState()
    val questions by viewModel.allQuestions.collectAsState()
    val selectedClass by viewModel.selectedClassLevel.collectAsState()
    val unlockedSubjectIds by viewModel.unlockedSubjectIds.collectAsState()
    val paperHeader by viewModel.paperHeader.collectAsState()

    val filteredSubjects = remember(subjects, selectedClass) {
        subjects.filter { it.classLevel == selectedClass }.sortedBy { it.orderIndex }
    }

    var subjectToUnlockViaAd by remember { mutableStateOf<SubjectEntity?>(null) }

    if (subjectToUnlockViaAd != null) {
        val targetSubj = subjectToUnlockViaAd!!
        AutoPlayingRewardedAdOverlay(
            itemTitle = "${targetSubj.nameEn} (Class ${targetSubj.classLevel}th)",
            onDismiss = { subjectToUnlockViaAd = null },
            onUnlocked = {
                viewModel.unlockSubjectViaRewardedAd(targetSubj.id)
                subjectToUnlockViaAd = null
                viewModel.openSubject(targetSubj)
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 156.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TOP BRAND HERO HEADER: "Paper Maker by Shami Academy" + Custom Logo
            item(span = { GridItemSpan(maxLineSpan) }) {
                ShamiBrandHeroCard(
                    onQuickSyncGitHub = {
                        val cfg = viewModel.gitHubService.getConfig()
                        viewModel.syncDatabaseFromGitHub(cfg)
                    }
                )
            }

            // 2. LANGUAGE MODE SELECTOR (English / Urdu / Bilingual)
            item(span = { GridItemSpan(maxLineSpan) }) {
                LanguageModeSelectorRow(
                    currentLanguage = PaperLanguage.fromCode(paperHeader.languageMode),
                    onSelectLanguage = { viewModel.setPaperLanguage(it) }
                )
            }

            // 3. CLASS 9th & CLASS 10th SELECTOR CARDS
            item(span = { GridItemSpan(maxLineSpan) }) {
                ClassSelectorBanner(
                    selectedClass = selectedClass,
                    onSelectClass = { viewModel.selectClassLevel(it) }
                )
            }

            // 4. SECTION TITLE FOR SUBJECT BOOKS
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = if (selectedClass == "9") {
                        "Class 9th Subjects"
                    } else {
                        "Class 10th Subjects"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // 5. SUBJECT CARDS GRID
            items(filteredSubjects, key = { it.id }) { subject ->
                val subjectChapters = chapters.filter { it.subjectId == subject.id }
                val subjectQuestions = questions.filter { it.subjectId == subject.id }
                val isBookUnlocked = subject.orderIndex == 0 ||
                    subject.isFreeByDefault ||
                    unlockedSubjectIds.contains(subject.id)

                SubjectBookCard(
                    subject = subject,
                    chapterCount = subjectChapters.size,
                    questionCount = subjectQuestions.size,
                    isBookUnlocked = isBookUnlocked,
                    onCardClick = {
                        if (isBookUnlocked) {
                            viewModel.openSubject(subject)
                        } else {
                            // Tapping a locked book immediately starts playing the Rewarded Ad!
                            subjectToUnlockViaAd = subject
                        }
                    }
                )
            }
        }

        AdMobBannerBar()
    }
}

@Composable
private fun ShamiBrandHeroCard(
    onQuickSyncGitHub: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shami_brand_hero_card"),
        shape = RoundedCornerShape(22.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0B2447),
                            Color(0xFF19376D),
                            Color(0xFF0D9488)
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(2.dp, Color(0xFFFFD700), CircleShape)
                            .background(Color(0xFF0B2447))
                            .padding(3.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_shami_logo_1791434850280),
                            contentDescription = "Paper Maker by Shami Academy Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Paper Maker by Shami Academy",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 24.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Smart Exam Paper Generator • Class 9th & 10th",
                            color = Color(0xFFFFD700),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Auto Header • Drag & Drop • English / Urdu / Bilingual",
                            color = Color(0xFFE2E8F0),
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onQuickSyncGitHub,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.14f))
                        .size(40.dp)
                        .testTag("hero_github_sync_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Sync Syllabus from GitHub",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun LanguageModeSelectorRow(
    currentLanguage: PaperLanguage,
    onSelectLanguage: (PaperLanguage) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = "Paper Medium:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperLanguage.entries.forEach { lang ->
                    val selected = (lang == currentLanguage)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectLanguage(lang) },
                        label = {
                            Text(
                                text = lang.labelEn,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0B2447),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("lang_chip_${lang.code}")
                    )
                }
            }
        }
    }
}

@Composable
private fun ClassSelectorBanner(
    selectedClass: String,
    onSelectClass: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ClassOptionCard(
            classNumber = "9",
            titleEn = "Class 9th",
            subtitle = "8 Books • Islamiat Included",
            isSelected = selectedClass == "9",
            onClick = { onSelectClass("9") },
            modifier = Modifier
                .weight(1f)
                .testTag("select_class_9_card")
        )
        ClassOptionCard(
            classNumber = "10",
            titleEn = "Class 10th",
            subtitle = "8 Books • Pak Studies Included",
            isSelected = selectedClass == "10",
            onClick = { onSelectClass("10") },
            modifier = Modifier
                .weight(1f)
                .testTag("select_class_10_card")
        )
    }
}

@Composable
private fun ClassOptionCard(
    classNumber: String,
    titleEn: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgBrush = if (isSelected) {
        Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF0F766E)))
    } else {
        Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(18.dp)
            ),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 2.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgBrush)
                .padding(14.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color.White.copy(alpha = 0.22f)
                                else Color(0xFF0B2447).copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.School,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFFFFD700) else Color(0xFF0D9488),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "${classNumber}th",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = titleEn,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = contentColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun SubjectBookCard(
    subject: SubjectEntity,
    chapterCount: Int,
    questionCount: Int,
    isBookUnlocked: Boolean,
    onCardClick: () -> Unit
) {
    val accentColor = remember(subject.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(subject.colorHex))
        } catch (e: Exception) {
            Color(0xFF0D9488)
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onCardClick)
            .testTag("subject_card_${subject.id}"),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.78f))
                        )
                    )
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconForSubjectKey(subject.iconKey),
                            contentDescription = subject.nameEn,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Only show a clean Lock icon if locked (no "Watch Ad" text!)
                    if (!isBookUnlocked) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0B2447).copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = subject.nameEn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$chapterCount Chapters",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$questionCount Qs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                }
            }
        }
    }
}

fun iconForSubjectKey(key: String): ImageVector {
    return when (key) {
        "science" -> Icons.Default.Science
        "bolt" -> Icons.Default.Bolt
        "menu_book" -> Icons.AutoMirrored.Filled.MenuBook
        "translate" -> Icons.Default.Translate
        "auto_stories" -> Icons.Default.AutoStories
        "mosque" -> Icons.Default.Mosque
        "flag" -> Icons.Default.Flag
        "computer" -> Icons.Default.Computer
        "biotech" -> Icons.Default.Biotech
        else -> Icons.Default.Description
    }
}
