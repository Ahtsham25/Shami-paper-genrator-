package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ads.AdManager
import com.example.ads.AdMobBannerBar
import com.example.data.PaperHeaderConfig
import com.example.data.PaperLanguage
import com.example.ui.PaperMakerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperPreviewScreen(
    viewModel: PaperMakerViewModel
) {
    BackHandler { viewModel.navigateBack() }

    val context = LocalContext.current
    val activity = context as? Activity
    val header by viewModel.paperHeader.collectAsState()
    val mcqs by viewModel.selectedMcqs.collectAsState()
    val shorts by viewModel.selectedShortQuestions.collectAsState()
    val longs by viewModel.selectedLongQuestions.collectAsState()
    val lang = PaperLanguage.fromCode(header.languageMode)

    val mcqMarks = mcqs.sumOf { it.marks }
    val shortMarks = shorts.sumOf { it.marks }
    val longMarks = longs.sumOf { it.marks }
    val totalMarks = mcqMarks + shortMarks + longMarks

    val fontScaleMultiplier = when (header.fontSizeScale.uppercase()) {
        "SMALL" -> 0.90f
        "LARGE" -> 1.15f
        else -> 1.0f
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Paper Preview & Editor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "${header.subjectNameEn} • Total Marks: $totalMarks • Tap header fields to edit",
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
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0B2447),
                titleContentColor = Color.White
            )
        )

        // ====================================================================
        // TOP TOOLBAR:
        // 1. Two Paper Versions (Version 1: Table Style | Version 2: Classic Style)
        // 2. Paper Size & Text Scale (A4 | Legal | S / M / L)
        // 3. Paper Language (English | Urdu | Bilingual)
        // ====================================================================
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1: Two Paper Versions + Page Size Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = header.paperVersion == 1,
                        onClick = { viewModel.setPaperVersion(1) },
                        label = { Text("Version 1: Table Style", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0B2447),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("paper_version_1_chip")
                    )
                    FilterChip(
                        selected = header.paperVersion == 2,
                        onClick = { viewModel.setPaperVersion(2) },
                        label = { Text("Version 2: Classic", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0B2447),
                            selectedLabelColor = Color.White
                        ),
                        modifier = Modifier.testTag("paper_version_2_chip")
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    FilterChip(
                        selected = header.paperSize == "A4",
                        onClick = {
                            viewModel.setPaperSize(if (header.paperSize == "A4") "LEGAL" else "A4")
                        },
                        label = { Text("Size: ${header.paperSize}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0D9488),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = true,
                        onClick = {
                            val nextScale = when (header.fontSizeScale) {
                                "SMALL" -> "MEDIUM"
                                "MEDIUM" -> "LARGE"
                                else -> "SMALL"
                            }
                            viewModel.setFontSizeScale(nextScale)
                        },
                        label = { Text("Font: ${header.fontSizeScale.first()}", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0D9488),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Row 2: Paper Language (English / Urdu / Bilingual)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaperLanguage.entries.forEach { mode ->
                        val selected = (mode == lang)
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setPaperLanguage(mode) },
                            label = {
                                Text(
                                    text = mode.labelEn,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0B2447),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ====================================================================
        // PRINTABLE EXAM PAPER SHEET WITH DIRECT INLINE EDITABLE HEADER
        // ====================================================================
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, Color(0xFF0B2447), RoundedCornerShape(12.dp))
                        .testTag("printable_paper_sheet"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        // 1. DIRECTLY EDITABLE PAPER HEADER (Version 1 Table vs Version 2 Classic)
                        if (header.paperVersion == 1) {
                            EditableTablePaperHeader(
                                header = header,
                                totalMarks = totalMarks,
                                onUpdateHeader = { viewModel.updatePaperHeader(it) }
                            )
                        } else {
                            EditableClassicPaperHeader(
                                header = header,
                                totalMarks = totalMarks,
                                onUpdateHeader = { viewModel.updatePaperHeader(it) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        var sectionNum = 1

                        // 2. QUESTION NO. 1: MCQs
                        if (mcqs.isNotEmpty()) {
                            PaperSectionHeaderStrip(
                                questionNum = sectionNum++,
                                titleEn = "Choose the correct option.",
                                titleUr = "درست جواب کا انتخاب کریں۔",
                                marksLabel = "(${mcqs.size} x 1 = $mcqMarks)",
                                language = lang
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            mcqs.forEachIndexed { idx, q ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (header.paperVersion == 1) {
                                                Modifier
                                                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                                    .padding(8.dp)
                                            } else {
                                                Modifier.padding(vertical = 5.dp)
                                            }
                                        )
                                ) {
                                    if (lang == PaperLanguage.ENGLISH || lang == PaperLanguage.BILINGUAL) {
                                        Text(
                                            text = "${idx + 1}. ${q.questionEn}",
                                            color = Color(0xFF0F172A),
                                            fontSize = (13f * fontScaleMultiplier).sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (lang == PaperLanguage.URDU || lang == PaperLanguage.BILINGUAL) {
                                        Text(
                                            text = if (lang == PaperLanguage.URDU) "${idx + 1}۔ ${q.questionUr}" else q.questionUr,
                                            color = Color(0xFF0F172A),
                                            fontSize = (13f * fontScaleMultiplier).sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    if (header.paperVersion == 1) {
                                        // Table Grid Options Row for Version 1
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                                .background(Color(0xFFF8FAFC))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            val optA = when (lang) {
                                                PaperLanguage.ENGLISH -> "(A) ${q.optionAEn}"
                                                PaperLanguage.URDU -> "(الف) ${q.optionAUr}"
                                                PaperLanguage.BILINGUAL -> "(A) ${q.optionAEn}/${q.optionAUr}"
                                            }
                                            val optB = when (lang) {
                                                PaperLanguage.ENGLISH -> "(B) ${q.optionBEn}"
                                                PaperLanguage.URDU -> "(ب) ${q.optionBUr}"
                                                PaperLanguage.BILINGUAL -> "(B) ${q.optionBEn}/${q.optionBUr}"
                                            }
                                            val optC = when (lang) {
                                                PaperLanguage.ENGLISH -> "(C) ${q.optionCEn}"
                                                PaperLanguage.URDU -> "(ج) ${q.optionCUr}"
                                                PaperLanguage.BILINGUAL -> "(C) ${q.optionCEn}/${q.optionCUr}"
                                            }
                                            val optD = when (lang) {
                                                PaperLanguage.ENGLISH -> "(D) ${q.optionDEn}"
                                                PaperLanguage.URDU -> "(د) ${q.optionDUr}"
                                                PaperLanguage.BILINGUAL -> "(D) ${q.optionDEn}/${q.optionDUr}"
                                            }
                                            Text(optA, fontSize = (11f * fontScaleMultiplier).sp, color = Color(0xFF334155), modifier = Modifier.weight(1f))
                                            Text(optB, fontSize = (11f * fontScaleMultiplier).sp, color = Color(0xFF334155), modifier = Modifier.weight(1f))
                                            Text(optC, fontSize = (11f * fontScaleMultiplier).sp, color = Color(0xFF334155), modifier = Modifier.weight(1f))
                                            Text(optD, fontSize = (11f * fontScaleMultiplier).sp, color = Color(0xFF334155), modifier = Modifier.weight(1f))
                                        }
                                    } else {
                                        val optText = when (lang) {
                                            PaperLanguage.ENGLISH ->
                                                "(A) ${q.optionAEn}    (B) ${q.optionBEn}    (C) ${q.optionCEn}    (D) ${q.optionDEn}"
                                            PaperLanguage.URDU ->
                                                "(الف) ${q.optionAUr}    (ب) ${q.optionBUr}    (ج) ${q.optionCUr}    (د) ${q.optionDUr}"
                                            PaperLanguage.BILINGUAL ->
                                                "(A) ${q.optionAEn} / ${q.optionAUr}   (B) ${q.optionBEn} / ${q.optionBUr}   (C) ${q.optionCEn} / ${q.optionCUr}   (D) ${q.optionDEn} / ${q.optionDUr}"
                                        }
                                        Text(
                                            text = optText,
                                            color = Color(0xFF334155),
                                            fontSize = (12f * fontScaleMultiplier).sp,
                                            textAlign = if (lang == PaperLanguage.URDU) TextAlign.End else TextAlign.Start,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(top = 4.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 3. QUESTION NO. 2: SHORT QUESTIONS
                        if (shorts.isNotEmpty()) {
                            PaperSectionHeaderStrip(
                                questionNum = sectionNum++,
                                titleEn = "Write short answers to the following questions.",
                                titleUr = "درج ذیل مختصر سوالات کے جوابات تحریر کریں۔",
                                marksLabel = "(Marks: $shortMarks)",
                                language = lang
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            shorts.forEachIndexed { idx, q ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    if (lang == PaperLanguage.ENGLISH || lang == PaperLanguage.BILINGUAL) {
                                        Text(
                                            text = "(${idx + 1}) ${q.questionEn}   [${q.marks}]",
                                            color = Color(0xFF0F172A),
                                            fontSize = (13f * fontScaleMultiplier).sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    if (lang == PaperLanguage.URDU || lang == PaperLanguage.BILINGUAL) {
                                        Text(
                                            text = "(${idx + 1}) ${q.questionUr}   [${q.marks}]",
                                            color = Color(0xFF0F172A),
                                            fontSize = (13f * fontScaleMultiplier).sp,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.End,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 4. QUESTION NO. 3: LONG QUESTIONS
                        if (longs.isNotEmpty()) {
                            PaperSectionHeaderStrip(
                                questionNum = sectionNum,
                                titleEn = "Answer the following detailed / long questions.",
                                titleUr = "درج ذیل تفصیلی سوالات کے جوابات تحریر کریں۔",
                                marksLabel = "(Marks: $longMarks)",
                                language = lang
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            longs.forEachIndexed { idx, q ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    if (lang == PaperLanguage.ENGLISH || lang == PaperLanguage.BILINGUAL) {
                                        Text(
                                            text = "Q.${idx + 1}: ${q.questionEn}   (${q.marks} Marks)",
                                            color = Color(0xFF0F172A),
                                            fontSize = (13f * fontScaleMultiplier).sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (lang == PaperLanguage.URDU || lang == PaperLanguage.BILINGUAL) {
                                        Text(
                                            text = "(${idx + 1}) ${q.questionUr}   (${q.marks} نمبر)",
                                            color = Color(0xFF0F172A),
                                            fontSize = (13f * fontScaleMultiplier).sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.End,
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

        // Bottom Save & Download PDF Action Bar
        Surface(
            tonalElevation = 8.dp,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.savePaperAndOptionallyExportPdf(exportPdf = false)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_paper_only_button")
                    ) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save Paper", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            AdManager.showInterstitialIfLoaded(activity, context) {
                                viewModel.savePaperAndOptionallyExportPdf(
                                    exportPdf = true,
                                    sharePdfAfterExport = false
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("download_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.savePaperAndOptionallyExportPdf(
                                exportPdf = true,
                                sharePdfAfterExport = true
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        modifier = Modifier.testTag("share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", modifier = Modifier.size(18.dp))
                    }
                }
                AdMobBannerBar()
            }
        }
    }
}

/**
 * VERSION 1: Directly Editable Table Style Paper Header
 * Every field (Institution Name, Student Name, Roll No, Subject, Time Allowed) can be tapped
 * and edited directly inside the table on the paper!
 */
@Composable
private fun EditableTablePaperHeader(
    header: PaperHeaderConfig,
    totalMarks: Int,
    onUpdateHeader: (PaperHeaderConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.8.dp, Color(0xFF0B2447), RoundedCornerShape(6.dp))
    ) {
        // Table Row 1: Editable Institution / Academy Name
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF1F5F9))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                BasicTextField(
                    value = header.institutionName,
                    onValueChange = { onUpdateHeader(header.copy(institutionName = it)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0B2447),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("inline_edit_institution_name")
                )
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Editable Institution Name",
                    tint = Color(0xFF0D9488),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        HorizontalDivider(thickness = 1.2.dp, color = Color(0xFF0B2447))

        // Table Row 2: Editable Student Name | Editable Roll No
        Row(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Name: ",
                    color = Color(0xFF0F172A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                BasicTextField(
                    value = header.studentNameValue,
                    onValueChange = { onUpdateHeader(header.copy(studentNameValue = it)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    decorationBox = { inner ->
                        if (header.studentNameValue.isEmpty()) {
                            Text("______________", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .width(1.2.dp)
                    .height(32.dp)
                    .background(Color(0xFF0B2447))
            )

            Row(
                modifier = Modifier
                    .weight(0.85f)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Roll No: ",
                    color = Color(0xFF0F172A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                BasicTextField(
                    value = header.rollNumberValue,
                    onValueChange = { onUpdateHeader(header.copy(rollNumberValue = it)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    decorationBox = { inner ->
                        if (header.rollNumberValue.isEmpty()) {
                            Text("_________", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        HorizontalDivider(thickness = 1.2.dp, color = Color(0xFF0B2447))

        // Table Row 3: Editable Subject | Editable Time Allowed | Auto Total Marks
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1.1f)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Book: ",
                    color = Color(0xFF0F172A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                BasicTextField(
                    value = header.subjectNameEn,
                    onValueChange = { onUpdateHeader(header.copy(subjectNameEn = it)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0D9488),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .width(1.2.dp)
                    .height(32.dp)
                    .background(Color(0xFF0B2447))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Time: ",
                    color = Color(0xFF0F172A),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                BasicTextField(
                    value = header.timeAllowed,
                    onValueChange = { onUpdateHeader(header.copy(timeAllowed = it)) },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .width(1.2.dp)
                    .height(32.dp)
                    .background(Color(0xFF0B2447))
            )

            Text(
                text = "Marks: $totalMarks",
                color = Color(0xFF0B2447),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
            )
        }
    }
}

/**
 * VERSION 2: Directly Editable Classic Style Paper Header
 */
@Composable
private fun EditableClassicPaperHeader(
    header: PaperHeaderConfig,
    totalMarks: Int,
    onUpdateHeader: (PaperHeaderConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.5.dp, Color(0xFF0B2447), RoundedCornerShape(8.dp))
            .background(Color(0xFFF8FAFC))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            BasicTextField(
                value = header.institutionName,
                onValueChange = { onUpdateHeader(header.copy(institutionName = it)) },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color(0xFF0B2447),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Editable Institution Name",
                tint = Color(0xFF0D9488),
                modifier = Modifier.size(15.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${header.classLabel} — Subject: ",
                color = Color(0xFF0D9488),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            BasicTextField(
                value = header.subjectNameEn,
                onValueChange = { onUpdateHeader(header.copy(subjectNameEn = it)) },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color(0xFF0D9488),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            color = Color(0xFFCBD5E1)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text("Student Name: ", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = header.studentNameValue,
                    onValueChange = { onUpdateHeader(header.copy(studentNameValue = it)) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 11.sp),
                    decorationBox = { inner ->
                        if (header.studentNameValue.isEmpty()) {
                            Text("______________", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        inner()
                    }
                )
            }
            Row(modifier = Modifier.weight(0.8f), verticalAlignment = Alignment.CenterVertically) {
                Text("Roll No: ", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = header.rollNumberValue,
                    onValueChange = { onUpdateHeader(header.copy(rollNumberValue = it)) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 11.sp),
                    decorationBox = { inner ->
                        if (header.rollNumberValue.isEmpty()) {
                            Text("_________", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        inner()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Text("Time Allowed: ", color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                BasicTextField(
                    value = header.timeAllowed,
                    onValueChange = { onUpdateHeader(header.copy(timeAllowed = it)) },
                    singleLine = true,
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
            }
            Text(
                text = "Total Marks: $totalMarks",
                color = Color(0xFF0B2447),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun PaperSectionHeaderStrip(
    questionNum: Int,
    titleEn: String,
    titleUr: String,
    marksLabel: String,
    language: PaperLanguage
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B2447), RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (language) {
                PaperLanguage.ENGLISH -> "Q.$questionNum: $titleEn"
                PaperLanguage.URDU -> "سوال نمبر $questionNum: $titleUr"
                PaperLanguage.BILINGUAL -> "Q.$questionNum: $titleEn | سوال نمبر $questionNum: $titleUr"
            },
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = marksLabel,
            color = Color(0xFFFFD700),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
