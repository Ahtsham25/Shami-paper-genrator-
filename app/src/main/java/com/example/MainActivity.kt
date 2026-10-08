package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ActiveScreen
import com.example.ui.BottomNavTab
import com.example.ui.PaperMakerViewModel
import com.example.ui.screens.AdminAppScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MoreScreen
import com.example.ui.screens.PaperPreviewScreen
import com.example.ui.screens.PrivacyPolicyScreen
import com.example.ui.screens.QuestionPickerAndBuilderScreen
import com.example.ui.screens.SavedDownloadsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SubjectChaptersScreen
import com.example.ui.theme.PaperMakerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PaperMakerTheme {
                PaperMakerAppRoot()
            }
        }
    }
}

@Composable
fun PaperMakerAppRoot(
    viewModel: PaperMakerViewModel = viewModel()
) {
    // If this APK was compiled as the Separate Small Admin App (-PadminApp=true),
    // launch directly into the standalone Admin App!
    if (BuildConfig.IS_ADMIN_APK) {
        val statusMessage by viewModel.statusMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(statusMessage) {
            val msg = statusMessage
            if (!msg.isNullOrBlank()) {
                snackbarHostState.showSnackbar(msg)
                viewModel.clearStatusMessage()
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AdminAppScreen(
                    viewModel = viewModel,
                    isStandaloneAdminApk = true
                )
            }
        }
        return
    }

    var showSplash by rememberSaveable { mutableStateOf(true) }

    if (showSplash) {
        SplashScreen(
            onSplashFinished = { showSplash = false }
        )
    } else {
        PaperMakerMainScaffold(viewModel = viewModel)
    }
}

@Composable
fun PaperMakerMainScaffold(
    viewModel: PaperMakerViewModel
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val activeScreen by viewModel.activeScreen.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(statusMessage) {
        val msg = statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (activeScreen == ActiveScreen.MAIN_TABS) {
                NavigationBar(
                    containerColor = Color(0xFF0B2447),
                    contentColor = Color.White
                ) {
                    val itemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF0B2447),
                        selectedTextColor = Color(0xFFFFD700),
                        indicatorColor = Color(0xFFFFD700),
                        unselectedIconColor = Color.White.copy(alpha = 0.72f),
                        unselectedTextColor = Color.White.copy(alpha = 0.72f)
                    )

                    NavigationBarItem(
                        selected = currentTab == BottomNavTab.HOME,
                        onClick = { viewModel.selectBottomTab(BottomNavTab.HOME) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == BottomNavTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                contentDescription = "Home"
                            )
                        },
                        label = {
                            Text(
                                text = "Home",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_tab_home")
                    )

                    NavigationBarItem(
                        selected = currentTab == BottomNavTab.SAVED_DOWNLOADS,
                        onClick = { viewModel.selectBottomTab(BottomNavTab.SAVED_DOWNLOADS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == BottomNavTab.SAVED_DOWNLOADS) Icons.Filled.FolderSpecial else Icons.Outlined.FolderSpecial,
                                contentDescription = "Downloads & Saved"
                            )
                        },
                        label = {
                            Text(
                                text = "Downloads & Saved",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_tab_saved")
                    )

                    NavigationBarItem(
                        selected = currentTab == BottomNavTab.MORE,
                        onClick = { viewModel.selectBottomTab(BottomNavTab.MORE) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == BottomNavTab.MORE) Icons.Filled.MoreHoriz else Icons.Outlined.MoreHoriz,
                                contentDescription = "More"
                            )
                        },
                        label = {
                            Text(
                                text = "More",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = itemColors,
                        modifier = Modifier.testTag("nav_tab_more")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeScreen) {
                ActiveScreen.MAIN_TABS -> {
                    when (currentTab) {
                        BottomNavTab.HOME -> HomeScreen(viewModel = viewModel)
                        BottomNavTab.SAVED_DOWNLOADS -> SavedDownloadsScreen(viewModel = viewModel)
                        BottomNavTab.MORE -> MoreScreen(viewModel = viewModel)
                    }
                }
                ActiveScreen.SUBJECT_CHAPTERS -> SubjectChaptersScreen(viewModel = viewModel)
                ActiveScreen.QUESTION_PICKER_AND_BUILDER -> QuestionPickerAndBuilderScreen(viewModel = viewModel)
                ActiveScreen.PAPER_PREVIEW -> PaperPreviewScreen(viewModel = viewModel)
                ActiveScreen.PRIVACY_POLICY -> PrivacyPolicyScreen(viewModel = viewModel)
                ActiveScreen.OWNER_ADMIN_APP -> AdminAppScreen(viewModel = viewModel, isStandaloneAdminApk = false)
            }
        }
    }
}
