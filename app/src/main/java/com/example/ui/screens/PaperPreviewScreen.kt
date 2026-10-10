package com.example.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ads.AdManager
import com.example.ads.AdMobBannerBar
import com.example.data.PaperHeaderConfig
import com.example.data.PaperLanguage
import com.example.data.SavedPaperPayload
import com.example.pdf.ExamPdfGenerator
import com.example.ui.PaperMakerViewModel
import kotlin.math.roundToInt

import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import com.example.ui.BottomNavTab
import com.example.ui.DownloadedPdfInfo

val STANDARD_FONT_SIZE_TABLE = listOf(8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24)

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
    val lastDownloadedPdf by viewModel.lastDownloadedPdf.collectAsState()

    val mcqMarks = mcqs.sumOf { it.marks }
    val shortMarks = shorts.sumOf { it.marks }
    val longMarks = longs.sumOf { it.marks }
    val totalMarks = mcqMarks + shortMarks + longMarks

    var showFullScreenPreviewDialog by remember { mutableStateOf(false) }

    if (showFullScreenPreviewDialog) {
        FullScreenPaperPreviewDialog(
            viewModel = viewModel,
            onDismiss = { showFullScreenPreviewDialog = false }
        )
    }

    lastDownloadedPdf?.let { pdfInfo ->
        DownloadedPdfLocationDialog(
            pdfInfo = pdfInfo,
            onOpenPdfNow = {
                ExamPdfGenerator.shareOrOpenPdf(context, pdfInfo.file, share = false)
            },
            onPrintPdfNow = {
                activity?.let { ExamPdfGenerator.printExamPdf(it, pdfInfo.file) }
            },
            onSharePdfNow = {
                ExamPdfGenerator.shareOrOpenPdf(context, pdfInfo.file, share = true)
            },
            onOpenSavedDownloadsTab = {
                viewModel.clearLastDownloadedPdf()
                viewModel.selectBottomTab(BottomNavTab.SAVED_DOWNLOADS)
            },
            onDismiss = {
                viewModel.clearLastDownloadedPdf()
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Full Paper Preview & Editor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "${header.subjectNameEn} • ${header.paperSize} • Font: ${header.fontSizePt}pt • Marks: $totalMarks",
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
                    onClick = { showFullScreenPreviewDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D9488),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .testTag("full_screen_preview_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Fullscreen,
                        contentDescription = "Full Screen Preview",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Full View", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFF0B2447),
                titleContentColor = Color.White
            )
        )

        // Main scrollable workspace containing:
        // 1. Top Paper Size (A4 / Legal), Version (1 / 2), and Font Size Table (8..24pt) + Slider
        // 2. True Full-Page Sheet Preview (A4 210x297mm vs Legal 8.5x14in + Side-by-Side Comparison)
        // 3. Directly Editable Paper Sheet (Inline Header & Live Font Scaling)
        PaperPreviewWorkspaceContent(
            viewModel = viewModel,
            onOpenFullScreen = { showFullScreenPreviewDialog = true },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )

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
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.savePaperAndOptionallyExportPdf(exportPdf = false)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("save_paper_only_button")
                    ) {
                        Icon(Icons.Default.BookmarkAdded, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("download_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    if (activity != null) {
                        OutlinedButton(
                            onClick = {
                                viewModel.printCurrentPaper(activity)
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(0.85f)
                                .testTag("print_paper_button")
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Print", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.savePaperAndOptionallyExportPdf(
                                exportPdf = true,
                                sharePdfAfterExport = true
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("share_pdf_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share PDF", modifier = Modifier.size(16.dp))
                    }
                }
                AdMobBannerBar()
            }
        }
    }
}

@Composable
private fun DownloadedPdfLocationDialog(
    pdfInfo: DownloadedPdfInfo,
    onOpenPdfNow: () -> Unit,
    onPrintPdfNow: () -> Unit,
    onSharePdfNow: () -> Unit,
    onOpenSavedDownloadsTab: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF16A34A),
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "PDF Saved to Mobile!",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = Color(0xFF0B2447)
                )
                Text(
                    text = "پیپر آپ کے موبائل میں ڈاؤن لوڈ ہو گیا ہے",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF16A34A)
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDF4),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "1. موبائل فائل مینیجر (Phone File Manager):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF166534)
                        )
                        Text(
                            text = "📁 ${pdfInfo.publicFolderDisplay}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFF0B2447)
                        )
                        Text(
                            text = "File: ${pdfInfo.fileName}",
                            fontSize = 11.sp,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEFF6FF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(10.dp))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "2. ایپ کے اندر (Inside App):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF1E40AF)
                        )
                        Text(
                            text = "ہوم اسکرین پر نیچے موجود 'Saved & Downloads' ٹیب میں بھی یہ پیپر ہمیشہ موجود رہے گا۔",
                            fontSize = 11.sp,
                            color = Color(0xFF1E3A8A)
                        )
                    }
                }

                Button(
                    onClick = onOpenPdfNow,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_open_pdf_now_button")
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open PDF Now (پیپر ابھی کھولیں)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrintPdfNow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Print / Save", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onSharePdfNow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                OutlinedButton(
                    onClick = onOpenSavedDownloadsTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Go to Saved & Downloads Tab", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A))
            ) {
                Text("OK / ٹھیک ہے", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Reusable Full Paper Preview + Controls Workspace used both in:
 * 1. PaperPreviewScreen
 * 2. QuestionPickerAndBuilderScreen ("Full Preview" tab) so the user can see the
 *    full A4 & Legal preview even before going to Print & Download!
 */
@Composable
fun PaperPreviewWorkspaceContent(
    viewModel: PaperMakerViewModel,
    onOpenFullScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val header by viewModel.paperHeader.collectAsState()
    val mcqs by viewModel.selectedMcqs.collectAsState()
    val shorts by viewModel.selectedShortQuestions.collectAsState()
    val longs by viewModel.selectedLongQuestions.collectAsState()
    val lang = PaperLanguage.fromCode(header.languageMode)

    val mcqMarks = mcqs.sumOf { it.marks }
    val shortMarks = shorts.sumOf { it.marks }
    val longMarks = longs.sumOf { it.marks }
    val totalMarks = mcqMarks + shortMarks + longMarks
    val fontScaleMultiplier = header.effectiveFontScale()

    var compareA4AndLegalSideBySide by remember { mutableStateOf(false) }
    var fitWholePageInFrame by remember { mutableStateOf(true) }

    val currentPayload = remember(header, mcqs, shorts, longs) {
        SavedPaperPayload(
            header = header,
            mcqs = mcqs,
            shortQuestions = shorts,
            longQuestions = longs
        )
    }

    val renderedPreview = remember(currentPayload) {
        ExamPdfGenerator.renderPaperPagesToBitmaps(currentPayload, scaleFactor = 1.6f)
    }

    val a4ComparePreview = remember(currentPayload, compareA4AndLegalSideBySide) {
        if (compareA4AndLegalSideBySide) {
            ExamPdfGenerator.renderPaperPagesToBitmaps(
                currentPayload.copy(header = header.copy(paperSize = "A4")),
                scaleFactor = 1.35f
            )
        } else null
    }

    val legalComparePreview = remember(currentPayload, compareA4AndLegalSideBySide) {
        if (compareA4AndLegalSideBySide) {
            ExamPdfGenerator.renderPaperPagesToBitmaps(
                currentPayload.copy(header = header.copy(paperSize = "LEGAL")),
                scaleFactor = 1.35f
            )
        } else null
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ====================================================================
        // 1. TOP CONTROL TABLES:
        //    - Paper Size Table (A4 vs Legal + Compare A4 & Legal)
        //    - Paper Version Table (Version 1: Table vs Version 2: Classic)
        //    - Font Size Table (8 | 9 | 10 | 11 | 12 | 14 | 16 | 18 | 20 | 22 | 24) + Slider
        //    - Paper Language Medium (English | Urdu | Bilingual)
        // ====================================================================
        item {
            PaperFormattingControlTablesCard(
                header = header,
                lang = lang,
                compareA4AndLegal = compareA4AndLegalSideBySide,
                onToggleCompareA4AndLegal = { compareA4AndLegalSideBySide = !compareA4AndLegalSideBySide },
                onSelectPaperSize = { size ->
                    viewModel.setPaperSize(size)
                },
                onSelectPaperVersion = { ver ->
                    viewModel.setPaperVersion(ver)
                },
                onSelectFontSizePt = { pt ->
                    viewModel.setFontSizePt(pt)
                },
                onSelectLanguage = { mode ->
                    viewModel.setPaperLanguage(mode)
                }
            )
        }

        // ====================================================================
        // 2. TRUE FULL-PAGE SHEET PREVIEW (A4 210×297mm & LEGAL 8.5×14in)
        //    Shows the complete page(s) with exact A4 vs Legal proportions,
        //    page fill %, and side-by-side A4 vs Legal comparison!
        // ====================================================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_page_preview_container"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Header bar of the Full Page Sheet Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (compareA4AndLegalSideBySide) {
                                    "Full Page Comparison: A4 Size vs Legal Size"
                                } else if (header.paperSize.equals("LEGAL", ignoreCase = true)) {
                                    "Full Paper Preview — Legal Size (8.5 × 14 in / 216 × 356 mm)"
                                } else {
                                    "Full Paper Preview — A4 Size (8.27 × 11.69 in / 210 × 297 mm)"
                                },
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Font Size: ${header.fontSizePt} pt  •  Total Pages: ${renderedPreview.totalPages}  •  Page 1 Fill: ${renderedPreview.firstPageFillPercent}%",
                                color = Color(0xFFFFD700),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = compareA4AndLegalSideBySide,
                                onClick = { compareA4AndLegalSideBySide = !compareA4AndLegalSideBySide },
                                label = {
                                    Text(
                                        text = if (compareA4AndLegalSideBySide) "Single View" else "A4 vs Legal",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Compare,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0D9488),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF334155),
                                    labelColor = Color.White
                                ),
                                modifier = Modifier.testTag("compare_a4_legal_button")
                            )

                            IconButton(
                                onClick = onOpenFullScreen,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF0D9488))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Expand Full Preview",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Page Fill Progress Bar showing how much of the A4 or Legal sheet is used
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (header.paperSize == "LEGAL") {
                                    "Legal Sheet Height Used (1008 pt — 20% Taller than A4)"
                                } else {
                                    "A4 Sheet Height Used (842 pt Standard Height)"
                                },
                                color = Color(0xFFCBD5E1),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${renderedPreview.firstPageFillPercent}% of Page 1 (${renderedPreview.totalPages} Page${if (renderedPreview.totalPages > 1) "s" else ""})",
                                color = Color(0xFF2DD4BF),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { renderedPreview.firstPageFillPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (renderedPreview.firstPageFillPercent > 92) Color(0xFFF59E0B) else Color(0xFF10B981),
                            trackColor = Color(0xFF334155)
                        )
                    }

                    if (compareA4AndLegalSideBySide && a4ComparePreview != null && legalComparePreview != null) {
                        // SIDE-BY-SIDE A4 vs LEGAL COMPARISON VIEW
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            // Left Column: A4 Sheet (595 x 842)
                            val isA4Selected = header.paperSize.equals("A4", ignoreCase = true)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isA4Selected) Color(0xFF0D9488).copy(alpha = 0.25f) else Color(0xFF0F172A))
                                    .border(
                                        width = if (isA4Selected) 2.dp else 1.dp,
                                        color = if (isA4Selected) Color(0xFF2DD4BF) else Color(0xFF475569),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setPaperSize("A4") }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "A4 Size (210×297mm)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${a4ComparePreview.totalPages} Page(s) • ${a4ComparePreview.firstPageFillPercent}% Fill",
                                    color = Color(0xFFFFD700),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                a4ComparePreview.pageBitmaps.forEachIndexed { idx, bmp ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(595f / 842f)
                                            .shadow(4.dp, RoundedCornerShape(4.dp))
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp))
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "A4 Page ${idx + 1}",
                                            contentScale = ContentScale.FillBounds,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "A4 Page ${idx + 1}",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 9.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }

                            // Right Column: Legal Sheet (612 x 1008 — Taller Aspect Ratio!)
                            val isLegalSelected = header.paperSize.equals("LEGAL", ignoreCase = true)
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isLegalSelected) Color(0xFF0D9488).copy(alpha = 0.25f) else Color(0xFF0F172A))
                                    .border(
                                        width = if (isLegalSelected) 2.dp else 1.dp,
                                        color = if (isLegalSelected) Color(0xFF2DD4BF) else Color(0xFF475569),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setPaperSize("LEGAL") }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Legal Size (8.5×14in)",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "${legalComparePreview.totalPages} Page(s) • ${legalComparePreview.firstPageFillPercent}% Fill",
                                    color = Color(0xFFFFD700),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                legalComparePreview.pageBitmaps.forEachIndexed { idx, bmp ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(612f / 1008f)
                                            .shadow(4.dp, RoundedCornerShape(4.dp))
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFF94A3B8), RoundedCornerShape(4.dp))
                                    ) {
                                        Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Legal Page ${idx + 1}",
                                            contentScale = ContentScale.FillBounds,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Legal Page ${idx + 1} (20% Taller)",
                                        color = Color(0xFFCBD5E1),
                                        fontSize = 9.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    } else {
                        // SINGLE ACTIVE SIZE FULL-PAGE SHEET PREVIEW (A4 or Legal)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing Complete ${header.paperSize} Sheet(s) — Tap page to zoom full screen",
                                color = Color(0xFFE2E8F0),
                                fontSize = 11.sp
                            )
                            FilterChip(
                                selected = fitWholePageInFrame,
                                onClick = { fitWholePageInFrame = !fitWholePageInFrame },
                                label = {
                                    Text(
                                        text = if (fitWholePageInFrame) "Fit Whole Page" else "Full Width",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0D9488),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF334155),
                                    labelColor = Color.White
                                )
                            )
                        }

                        val pageAspectRatio = renderedPreview.pageWidthPt.toFloat() / renderedPreview.pageHeightPt.toFloat()

                        renderedPreview.pageBitmaps.forEachIndexed { pageIndex, bmp ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .then(
                                            if (fitWholePageInFrame) {
                                                Modifier
                                                    .heightIn(max = 460.dp)
                                                    .aspectRatio(pageAspectRatio)
                                            } else {
                                                Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(pageAspectRatio)
                                            }
                                        )
                                        .shadow(8.dp, RoundedCornerShape(6.dp))
                                        .background(Color.White, RoundedCornerShape(6.dp))
                                        .border(2.dp, Color(0xFF2DD4BF), RoundedCornerShape(6.dp))
                                        .clickable { onOpenFullScreen() }
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "${header.paperSize} Full Page Preview ${pageIndex + 1}",
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${header.paperSize} Sheet — Page ${pageIndex + 1} of ${renderedPreview.totalPages} (${if (renderedPreview.isLegal) "8.5 × 14 in" else "210 × 297 mm"} • Font ${header.fontSizePt}pt)",
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // ====================================================================
        // 3. INTERACTIVE INLINE EDITABLE PAPER SHEET
        //    Tap Institution Name, Student Name, Roll No, Subject, or Time
        //    directly inside the paper header to edit!
        // ====================================================================
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Interactive Editable Sheet (${header.paperSize} • Font ${header.fontSizePt}pt)",
                            color = Color(0xFF0D9488),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Tap any header box below to edit text",
                            color = Color(0xFF64748B),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

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
                            val cleanEn = q.resolvedQuestionEn()
                            val cleanUr = q.resolvedQuestionUr()
                            val optAEn = q.resolvedOptionAEn()
                            val optBEn = q.resolvedOptionBEn()
                            val optCEn = q.resolvedOptionCEn()
                            val optDEn = q.resolvedOptionDEn()
                            val optAUr = q.resolvedOptionAUr()
                            val optBUr = q.resolvedOptionBUr()
                            val optCUr = q.resolvedOptionCUr()
                            val optDUr = q.resolvedOptionDUr()

                            val itemDir = if (lang == PaperLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr
                            CompositionLocalProvider(LocalLayoutDirection provides itemDir) {
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
                                    when (lang) {
                                        PaperLanguage.ENGLISH -> {
                                            Text(
                                                text = "${idx + 1}. $cleanEn",
                                                color = Color(0xFF0F172A),
                                                fontSize = (13f * fontScaleMultiplier).sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Left,
                                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        PaperLanguage.URDU -> {
                                            Text(
                                                text = "\u200F${idx + 1}۔ ${cleanUr.ifBlank { q.questionUr }}",
                                                color = Color(0xFF0F172A),
                                                fontSize = (13f * fontScaleMultiplier).sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Right,
                                                style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        PaperLanguage.BILINGUAL -> {
                                            if (cleanEn.isNotBlank()) {
                                                Text(
                                                    text = "${idx + 1}. $cleanEn",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (13f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Left,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            if (cleanUr.isNotBlank()) {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                    Text(
                                                        text = "\u200F${idx + 1}۔ $cleanUr",
                                                        color = Color(0xFF0F172A),
                                                        fontSize = (13f * fontScaleMultiplier).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Right,
                                                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    val optA = when (lang) {
                                        PaperLanguage.ENGLISH -> "(A) $optAEn"
                                        PaperLanguage.URDU -> "\u200F(الف) ${optAUr.ifBlank { q.optionAUr }}"
                                        PaperLanguage.BILINGUAL -> "(A) $optAEn / $optAUr"
                                    }
                                    val optB = when (lang) {
                                        PaperLanguage.ENGLISH -> "(B) $optBEn"
                                        PaperLanguage.URDU -> "\u200F(ب) ${optBUr.ifBlank { q.optionBUr }}"
                                        PaperLanguage.BILINGUAL -> "(B) $optBEn / $optBUr"
                                    }
                                    val optC = when (lang) {
                                        PaperLanguage.ENGLISH -> "(C) $optCEn"
                                        PaperLanguage.URDU -> "\u200F(ج) ${optCUr.ifBlank { q.optionCUr }}"
                                        PaperLanguage.BILINGUAL -> "(C) $optCEn / $optCUr"
                                    }
                                    val optD = when (lang) {
                                        PaperLanguage.ENGLISH -> "(D) $optDEn"
                                        PaperLanguage.URDU -> "\u200F(د) ${optDUr.ifBlank { q.optionDUr }}"
                                        PaperLanguage.BILINGUAL -> "(D) $optDEn / $optDUr"
                                    }

                                    val optAlign = if (lang == PaperLanguage.URDU) TextAlign.Right else TextAlign.Left
                                    val optDir = if (lang == PaperLanguage.URDU) TextDirection.Rtl else TextDirection.Ltr

                                    if (header.paperVersion == 1) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(4.dp))
                                                .background(Color(0xFFF8FAFC))
                                                .padding(6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(optA, fontSize = (11f * fontScaleMultiplier).sp, color = Color(0xFF334155), textAlign = optAlign, style = LocalTextStyle.current.copy(textDirection = optDir), modifier = Modifier.weight(1f))
                                            Text(optB, fontSize = (11f * fontScaleMultiplier).sp, color = Color(0xFF334155), textAlign = optAlign, style = LocalTextStyle.current.copy(textDirection = optDir), modifier = Modifier.weight(1f))
                                            Text(optC, fontSize = (11f * fontScaleMultiplier).sp, color = Color(0xFF334155), textAlign = optAlign, style = LocalTextStyle.current.copy(textDirection = optDir), modifier = Modifier.weight(1f))
                                            Text(optD, fontSize = (11f * fontScaleMultiplier).sp, color = Color(0xFF334155), textAlign = optAlign, style = LocalTextStyle.current.copy(textDirection = optDir), modifier = Modifier.weight(1f))
                                        }
                                    } else {
                                        val optText = "$optA    $optB    $optC    $optD"
                                        Text(
                                            text = optText,
                                            color = Color(0xFF334155),
                                            fontSize = (12f * fontScaleMultiplier).sp,
                                            textAlign = optAlign,
                                            style = LocalTextStyle.current.copy(textDirection = optDir),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        HorizontalDivider(color = Color(0xFFE2E8F0), modifier = Modifier.padding(top = 4.dp))
                                    }
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
                            marksLabel = if (lang == PaperLanguage.URDU) "(کل نمبر: $shortMarks)" else "(Marks: $shortMarks)",
                            language = lang
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        shorts.forEachIndexed { idx, q ->
                            val cleanEn = q.resolvedQuestionEn()
                            val cleanUr = q.resolvedQuestionUr()

                            val itemDir = if (lang == PaperLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr
                            CompositionLocalProvider(LocalLayoutDirection provides itemDir) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    when (lang) {
                                        PaperLanguage.ENGLISH -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "(${idx + 1}) $cleanEn",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (13f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Left,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "[${q.marks}]",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (12f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        PaperLanguage.URDU -> {
                                            // True Right-to-Left Short Question: Question on Right (1۔ ...), Marks on Left ([2])
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "\u200F${idx + 1}۔ ${cleanUr.ifBlank { q.questionUr }}",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (13f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    textAlign = TextAlign.Right,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "\u200F[${q.marks}]",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (12f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Left,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                                                )
                                            }
                                        }
                                        PaperLanguage.BILINGUAL -> {
                                            if (cleanEn.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = "(${idx + 1}) $cleanEn",
                                                        color = Color(0xFF0F172A),
                                                        fontSize = (13f * fontScaleMultiplier).sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        textAlign = TextAlign.Left,
                                                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "[${q.marks}]",
                                                        color = Color(0xFF0F172A),
                                                        fontSize = (12f * fontScaleMultiplier).sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            if (cleanUr.isNotBlank()) {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                    Text(
                                                        text = "\u200F${idx + 1}۔ $cleanUr",
                                                        color = Color(0xFF0F172A),
                                                        fontSize = (13f * fontScaleMultiplier).sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        textAlign = TextAlign.Right,
                                                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }
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
                            marksLabel = if (lang == PaperLanguage.URDU) "(کل نمبر: $longMarks)" else "(Marks: $longMarks)",
                            language = lang
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        longs.forEachIndexed { idx, q ->
                            val cleanEn = q.resolvedQuestionEn()
                            val cleanUr = q.resolvedQuestionUr()

                            val itemDir = if (lang == PaperLanguage.URDU) LayoutDirection.Rtl else LayoutDirection.Ltr
                            CompositionLocalProvider(LocalLayoutDirection provides itemDir) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 5.dp)
                                ) {
                                    when (lang) {
                                        PaperLanguage.ENGLISH -> {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "Q.${idx + 1}: $cleanEn",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (13f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Left,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "(${q.marks} Marks)",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (12f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        PaperLanguage.URDU -> {
                                            // True Right-to-Left Long Question: Question on Right, Marks on Left
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.Top
                                            ) {
                                                Text(
                                                    text = "\u200Fسوال نمبر ${idx + 1}: ${cleanUr.ifBlank { q.questionUr }}",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (13f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Right,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "\u200F(${q.marks} نمبر)",
                                                    color = Color(0xFF0F172A),
                                                    fontSize = (12f * fontScaleMultiplier).sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Left,
                                                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl)
                                                )
                                            }
                                        }
                                        PaperLanguage.BILINGUAL -> {
                                            if (cleanEn.isNotBlank()) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    Text(
                                                        text = "Q.${idx + 1}: $cleanEn",
                                                        color = Color(0xFF0F172A),
                                                        fontSize = (13f * fontScaleMultiplier).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Left,
                                                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr),
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "(${q.marks} Marks)",
                                                        color = Color(0xFF0F172A),
                                                        fontSize = (12f * fontScaleMultiplier).sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            if (cleanUr.isNotBlank()) {
                                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                                    Text(
                                                        text = "\u200Fسوال نمبر ${idx + 1}: $cleanUr   (${q.marks} نمبر)",
                                                        color = Color(0xFF0F172A),
                                                        fontSize = (13f * fontScaleMultiplier).sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Right,
                                                        style = LocalTextStyle.current.copy(textDirection = TextDirection.Rtl),
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
                }
            }
        }
    }
}

/**
 * Top Control Tables Card:
 * 1. Paper Version (Version 1: Table | Version 2: Classic) & Page Size Table (A4 | Legal | Compare)
 * 2. Font Size Table with all exact font size numbers (8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24)
 *    PLUS a smooth slider and -/+ buttons
 * 3. Paper Language Medium (English | Urdu | Bilingual)
 */
@Composable
private fun PaperFormattingControlTablesCard(
    header: PaperHeaderConfig,
    lang: PaperLanguage,
    compareA4AndLegal: Boolean,
    onToggleCompareA4AndLegal: () -> Unit,
    onSelectPaperSize: (String) -> Unit,
    onSelectPaperVersion: (Int) -> Unit,
    onSelectFontSizePt: (Int) -> Unit,
    onSelectLanguage: (PaperLanguage) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ================================================================
            // TABLE 1: PAGE SIZE TABLE (A4 vs LEGAL) + PAPER VERSION (1 vs 2)
            // ================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Size Bordered Table (A4 | Legal)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color(0xFF0B2447), RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    val isA4 = header.paperSize.equals("A4", ignoreCase = true) && !compareA4AndLegal
                    val isLegal = header.paperSize.equals("LEGAL", ignoreCase = true) && !compareA4AndLegal

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isA4) Color(0xFF0D9488) else Color.White)
                            .clickable {
                                if (compareA4AndLegal) onToggleCompareA4AndLegal()
                                onSelectPaperSize("A4")
                            }
                            .padding(vertical = 8.dp)
                            .testTag("paper_size_a4_cell"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "A4 Size",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isA4) Color.White else Color(0xFF0B2447)
                            )
                            Text(
                                text = "210×297 mm",
                                fontSize = 9.sp,
                                color = if (isA4) Color(0xFFFFD700) else Color(0xFF64748B)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color(0xFF0B2447))
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (isLegal) Color(0xFF0D9488) else Color.White)
                            .clickable {
                                if (compareA4AndLegal) onToggleCompareA4AndLegal()
                                onSelectPaperSize("LEGAL")
                            }
                            .padding(vertical = 8.dp)
                            .testTag("paper_size_legal_cell"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Legal Size",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLegal) Color.White else Color(0xFF0B2447)
                            )
                            Text(
                                text = "8.5×14 in",
                                fontSize = 9.sp,
                                color = if (isLegal) Color(0xFFFFD700) else Color(0xFF64748B)
                            )
                        }
                    }
                }

                // Paper Version Bordered Table (Version 1: Table | Version 2: Classic)
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.5.dp, Color(0xFF0B2447), RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    val v1 = header.paperVersion == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (v1) Color(0xFF0B2447) else Color.White)
                            .clickable { onSelectPaperVersion(1) }
                            .padding(vertical = 8.dp)
                            .testTag("paper_version_1_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Version 1",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (v1) Color.White else Color(0xFF0B2447)
                            )
                            Text(
                                text = "Table Style",
                                fontSize = 9.sp,
                                color = if (v1) Color(0xFFFFD700) else Color(0xFF64748B)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Color(0xFF0B2447))
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (!v1) Color(0xFF0B2447) else Color.White)
                            .clickable { onSelectPaperVersion(2) }
                            .padding(vertical = 8.dp)
                            .testTag("paper_version_2_chip"),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Version 2",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (!v1) Color.White else Color(0xFF0B2447)
                            )
                            Text(
                                text = "Classic",
                                fontSize = 9.sp,
                                color = if (!v1) Color(0xFFFFD700) else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // ================================================================
            // TABLE 2: FONT SIZE NUMBER TABLE (8 | 9 | 10 | 11 | 12 | 14 | 16 | 18 | 20 | 22 | 24)
            //          + SMOOTH SLIDER / SCROLL BAR
            // ================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.5.dp, Color(0xFF0B2447), RoundedCornerShape(10.dp))
                    .background(Color.White)
                    .padding(8.dp)
                    .testTag("font_size_table")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Font Size Table (Tap Size or Scroll Slider):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0B2447)
                    )
                    Surface(
                        color = Color(0xFF0D9488),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Active Font: ${header.fontSizePt} pt",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bordered Grid/Table of Exact Font Size Numbers: 8, 9, 10, 11, 12, 14, 16, 18, 20, 22, 24
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF0B2447), RoundedCornerShape(6.dp))
                        .background(Color(0xFFF8FAFC)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    STANDARD_FONT_SIZE_TABLE.forEachIndexed { index, pt ->
                        val isSelected = header.fontSizePt == pt
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (isSelected) Color(0xFF0B2447) else Color.Transparent
                                )
                                .clickable { onSelectFontSizePt(pt) }
                                .padding(vertical = 7.dp)
                                .testTag("font_size_cell_$pt"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$pt",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) Color(0xFFFFD700) else Color(0xFF0F172A)
                            )
                        }
                        if (index < STANDARD_FONT_SIZE_TABLE.lastIndex) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(28.dp)
                                    .background(Color(0xFFCBD5E1))
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Smooth Font Size Slider + Minus / Plus Stepper Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onSelectFontSizePt((header.fontSizePt - 1).coerceAtLeast(8)) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease Font Size",
                            tint = Color(0xFF0B2447),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "8pt",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )

                    Slider(
                        value = header.fontSizePt.toFloat(),
                        onValueChange = { newVal ->
                            onSelectFontSizePt(newVal.roundToInt())
                        },
                        valueRange = 8f..24f,
                        steps = 15,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF0D9488),
                            activeTrackColor = Color(0xFF0B2447),
                            inactiveTrackColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(26.dp)
                            .testTag("font_size_slider")
                    )

                    Text(
                        text = "24pt",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )

                    IconButton(
                        onClick = { onSelectFontSizePt((header.fontSizePt + 1).coerceAtMost(24)) },
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase Font Size",
                            tint = Color(0xFF0B2447),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // ================================================================
            // TABLE 3: PAPER LANGUAGE MEDIUM (ENGLISH | URDU | BILINGUAL)
            // ================================================================
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperLanguage.entries.forEach { mode ->
                    val selected = (mode == lang)
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectLanguage(mode) },
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
}

/**
 * Full-Screen Zoomable Paper Preview Dialog so the user can inspect the entire
 * A4 (210×297mm) or Legal (8.5×14in) paper sheet edge-to-edge, compare A4 vs Legal,
 * and change font sizes (8..24pt) in real time!
 */
@Composable
private fun FullScreenPaperPreviewDialog(
    viewModel: PaperMakerViewModel,
    onDismiss: () -> Unit
) {
    val header by viewModel.paperHeader.collectAsState()
    val mcqs by viewModel.selectedMcqs.collectAsState()
    val shorts by viewModel.selectedShortQuestions.collectAsState()
    val longs by viewModel.selectedLongQuestions.collectAsState()

    var zoomFactor by remember { mutableFloatStateOf(1.0f) }
    var compareSideBySide by remember { mutableStateOf(false) }

    val payload = remember(header, mcqs, shorts, longs) {
        SavedPaperPayload(
            header = header,
            mcqs = mcqs,
            shortQuestions = shorts,
            longQuestions = longs
        )
    }

    val previewResult = remember(payload) {
        ExamPdfGenerator.renderPaperPagesToBitmaps(payload, scaleFactor = 2.0f)
    }

    val a4Preview = remember(payload, compareSideBySide) {
        if (compareSideBySide) {
            ExamPdfGenerator.renderPaperPagesToBitmaps(
                payload.copy(header = header.copy(paperSize = "A4")),
                scaleFactor = 1.5f
            )
        } else null
    }
    val legalPreview = remember(payload, compareSideBySide) {
        if (compareSideBySide) {
            ExamPdfGenerator.renderPaperPagesToBitmaps(
                payload.copy(header = header.copy(paperSize = "LEGAL")),
                scaleFactor = 1.5f
            )
        } else null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar in Full Screen Preview
                Surface(
                    color = Color(0xFF0B2447),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Full Paper Preview (${header.paperSize} • Font ${header.fontSizePt}pt)",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (header.paperSize == "LEGAL") {
                                        "Legal Sheet (8.5 × 14 in) • ${previewResult.totalPages} Page(s) • Page 1 Fill: ${previewResult.firstPageFillPercent}%"
                                    } else {
                                        "A4 Sheet (210 × 297 mm) • ${previewResult.totalPages} Page(s) • Page 1 Fill: ${previewResult.firstPageFillPercent}%"
                                    },
                                    color = Color(0xFFFFD700),
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { zoomFactor = (zoomFactor - 0.2f).coerceAtLeast(0.6f) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = "${(zoomFactor * 100).roundToInt()}%",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { zoomFactor = (zoomFactor + 0.2f).coerceAtMost(2.2f) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                ) {
                                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom In", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(Color(0xFFEF4444), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Full Preview", tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick A4 / Legal / Compare + Font Size Table right inside Full-Screen Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChip(
                                selected = header.paperSize == "A4" && !compareSideBySide,
                                onClick = {
                                    compareSideBySide = false
                                    viewModel.setPaperSize("A4")
                                },
                                label = { Text("A4 (210×297mm)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0D9488),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = header.paperSize == "LEGAL" && !compareSideBySide,
                                onClick = {
                                    compareSideBySide = false
                                    viewModel.setPaperSize("LEGAL")
                                },
                                label = { Text("Legal (8.5×14in)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0D9488),
                                    selectedLabelColor = Color.White,
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                            FilterChip(
                                selected = compareSideBySide,
                                onClick = { compareSideBySide = !compareSideBySide },
                                label = { Text("Compare Both", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFFFFD700),
                                    selectedLabelColor = Color(0xFF0B2447),
                                    containerColor = Color(0xFF1E293B),
                                    labelColor = Color.White
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Font Size Table Row inside Full Screen
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF475569), RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E293B)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Font:",
                                color = Color(0xFFFFD700),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                            STANDARD_FONT_SIZE_TABLE.forEach { pt ->
                                val selected = header.fontSizePt == pt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (selected) Color(0xFF0D9488) else Color.Transparent)
                                        .clickable { viewModel.setFontSizePt(pt) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$pt",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // Full Page Sheet Canvas Viewport
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (compareSideBySide && a4Preview != null && legalPreview != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "A4 Size (210×297mm) • ${a4Preview.firstPageFillPercent}% Page 1 Fill",
                                    color = Color(0xFFFFD700),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                a4Preview.pageBitmaps.forEachIndexed { idx, bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "A4 Page ${idx + 1}",
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier
                                            .width((170 * zoomFactor).dp)
                                            .height((240 * zoomFactor).dp)
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .border(2.dp, Color(0xFF2DD4BF), RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Legal Size (8.5×14in) • ${legalPreview.firstPageFillPercent}% Page 1 Fill",
                                    color = Color(0xFF2DD4BF),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                legalPreview.pageBitmaps.forEachIndexed { idx, bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Legal Page ${idx + 1}",
                                        contentScale = ContentScale.FillBounds,
                                        modifier = Modifier
                                            .width((170 * zoomFactor).dp)
                                            .height((280 * zoomFactor).dp)
                                            .background(Color.White, RoundedCornerShape(4.dp))
                                            .border(2.dp, Color(0xFFFFD700), RoundedCornerShape(4.dp))
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    } else {
                        val baseWidthDp = 330f * zoomFactor
                        val baseHeightDp = if (previewResult.isLegal) {
                            baseWidthDp * (1008f / 612f)
                        } else {
                            baseWidthDp * (842f / 595f)
                        }
                        previewResult.pageBitmaps.forEachIndexed { index, bmp ->
                            Text(
                                text = "${header.paperSize} Sheet — Page ${index + 1} of ${previewResult.totalPages}",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Full Page ${index + 1}",
                                contentScale = ContentScale.FillBounds,
                                modifier = Modifier
                                    .width(baseWidthDp.dp)
                                    .height(baseHeightDp.dp)
                                    .shadow(10.dp, RoundedCornerShape(6.dp))
                                    .background(Color.White, RoundedCornerShape(6.dp))
                                    .border(2.dp, Color.White, RoundedCornerShape(6.dp))
                            )
                        }
                    }
                }
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
    val isUrdu = header.languageMode.equals(PaperLanguage.URDU.code, ignoreCase = true)
    val dir = if (isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides dir) {
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
                        text = if (isUrdu) "نام طالب علم: " else "Name: ",
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
                        text = if (isUrdu) "رول نمبر: " else "Roll No: ",
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
                        text = if (isUrdu) "مضمون: " else "Book: ",
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    BasicTextField(
                        value = if (isUrdu) header.subjectNameUr.ifBlank { header.subjectNameEn } else header.subjectNameEn,
                        onValueChange = {
                            if (isUrdu) onUpdateHeader(header.copy(subjectNameUr = it))
                            else onUpdateHeader(header.copy(subjectNameEn = it))
                        },
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
                        text = if (isUrdu) "وقت: " else "Time: ",
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
                    text = if (isUrdu) "کل نمبر: $totalMarks" else "Marks: $totalMarks",
                    color = Color(0xFF0B2447),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                )
            }
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
    val isUrdu = header.languageMode.equals(PaperLanguage.URDU.code, ignoreCase = true)
    val dir = if (isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides dir) {
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
                    text = if (isUrdu) "${header.classLabel} — مضمون: " else "${header.classLabel} — Subject: ",
                    color = Color(0xFF0D9488),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                BasicTextField(
                    value = if (isUrdu) header.subjectNameUr.ifBlank { header.subjectNameEn } else header.subjectNameEn,
                    onValueChange = {
                        if (isUrdu) onUpdateHeader(header.copy(subjectNameUr = it))
                        else onUpdateHeader(header.copy(subjectNameEn = it))
                    },
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
                    Text(
                        text = if (isUrdu) "نام طالب علم: " else "Student Name: ",
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    Text(
                        text = if (isUrdu) "رول نمبر: " else "Roll No: ",
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
                    Text(
                        text = if (isUrdu) "وقت: " else "Time Allowed: ",
                        color = Color(0xFF0F172A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    BasicTextField(
                        value = header.timeAllowed,
                        onValueChange = { onUpdateHeader(header.copy(timeAllowed = it)) },
                        singleLine = true,
                        textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
                Text(
                    text = if (isUrdu) "کل نمبر: $totalMarks" else "Total Marks: $totalMarks",
                    color = Color(0xFF0B2447),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
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
    val isUrdu = (language == PaperLanguage.URDU)
    val stripDirection = if (isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr
    CompositionLocalProvider(LocalLayoutDirection provides stripDirection) {
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
                    PaperLanguage.URDU -> "\u200Fسوال نمبر $questionNum: $titleUr"
                    PaperLanguage.BILINGUAL -> "Q.$questionNum: $titleEn | سوال نمبر $questionNum: $titleUr"
                },
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = if (isUrdu) TextAlign.Right else TextAlign.Left,
                style = LocalTextStyle.current.copy(textDirection = if (isUrdu) TextDirection.Rtl else TextDirection.Ltr),
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isUrdu) "\u200F$marksLabel" else marksLabel,
                color = Color(0xFFFFD700),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                style = LocalTextStyle.current.copy(textDirection = if (isUrdu) TextDirection.Rtl else TextDirection.Ltr)
            )
        }
    }
}
