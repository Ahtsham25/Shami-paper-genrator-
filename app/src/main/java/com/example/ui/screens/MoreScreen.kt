package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ads.AdMobBannerBar
import com.example.ui.PaperMakerViewModel

internal fun verifySecretAdminPin(input: String): Boolean {
    val expected = intArrayOf(51, 55, 55, 53).map { it.toChar() }.joinToString("")
    return input.trim() == expected
}

@Composable
fun MoreScreen(
    viewModel: PaperMakerViewModel
) {
    val context = LocalContext.current
    val header by viewModel.paperHeader.collectAsState()

    var instName by remember(header.institutionName) { mutableStateOf(header.institutionName) }
    var defaultTime by remember(header.timeAllowed) { mutableStateOf(header.timeAllowed) }
    var secretTapCount by remember { mutableIntStateOf(0) }
    var showPinDialog by remember { mutableStateOf(false) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                enteredPin = ""
                pinError = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF0B2447)
                )
            },
            title = {
                Text(
                    text = "Admin Access",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it
                            pinError = false
                        },
                        label = { Text("Enter Password") },
                        singleLine = true,
                        isError = pinError,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text(
                            text = "Incorrect password.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (verifySecretAdminPin(enteredPin)) {
                            showPinDialog = false
                            enteredPin = ""
                            pinError = false
                            viewModel.openOwnerAdminApp()
                        } else {
                            pinError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B2447))
                ) {
                    Text("Unlock")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        enteredPin = ""
                        pinError = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Shami Academy Profile Header
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0B2447))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_shami_logo_1791434850280),
                            contentDescription = "Paper Maker by Shami Academy Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .clickable {
                                    secretTapCount++
                                    if (secretTapCount >= 5) {
                                        secretTapCount = 0
                                        enteredPin = ""
                                        pinError = false
                                        showPinDialog = true
                                    }
                                }
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "Paper Maker by Shami Academy",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "Version 1.0 • Class 9th & 10th Exam Suite",
                                color = Color(0xFFFFD700),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 2. Default Institution Header Settings
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = null,
                                tint = Color(0xFF0D9488)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Default Institution Header",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = instName,
                            onValueChange = { instName = it },
                            label = { Text("Institution / Academy / School Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = defaultTime,
                            onValueChange = { defaultTime = it },
                            label = { Text("Default Paper Time Allowed") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                viewModel.updatePaperHeader(
                                    header.copy(
                                        institutionName = instName,
                                        timeAllowed = defaultTime
                                    )
                                )
                                viewModel.showStatus("Default Header saved!")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488)),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Save Default Header")
                        }
                    }
                }
            }

            // 3. More Options (Privacy Policy, Share, Reset Locks, About)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        MoreMenuRow(
                            icon = Icons.Default.Policy,
                            title = "Privacy Policy",
                            subtitle = "Data protection, local storage & Google AdMob terms",
                            onClick = { viewModel.openPrivacyPolicy() },
                            testTag = "more_privacy_policy_item"
                        )
                        HorizontalDivider()
                        MoreMenuRow(
                            icon = Icons.Default.RestartAlt,
                            title = "Reset Unlocked Books & Chapters",
                            subtitle = "Re-lock items to default state",
                            onClick = { viewModel.resetAllRewardedAdLocks() }
                        )
                        HorizontalDivider()
                        MoreMenuRow(
                            icon = Icons.Default.Share,
                            title = "Share Paper Maker by Shami Academy",
                            subtitle = "Share this app with teachers, schools & academies",
                            onClick = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "Check out Paper Maker by Shami Academy — Smart Exam Paper Generator for Class 9th & 10th!"
                                    )
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Share App"))
                            }
                        )
                        HorizontalDivider()
                        MoreMenuRow(
                            icon = Icons.Default.Info,
                            title = "About Shami Academy",
                            subtitle = "Dedicated to empowering teachers & academies with instant exam creation",
                            onClick = {
                                viewModel.showStatus("Paper Maker by Shami Academy v1.0")
                            }
                        )
                    }
                }
            }
        }

        AdMobBannerBar()
    }
}

@Composable
private fun MoreMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .let { if (testTag.isNotBlank()) it.testTag(testTag) else it }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0B2447).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF0B2447)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    viewModel: PaperMakerViewModel
) {
    BackHandler { viewModel.navigateBack() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = "Privacy Policy",
                    fontWeight = FontWeight.Bold
                )
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
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF0D9488),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Paper Maker by Shami Academy\nPrivacy Policy & Terms",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "1. Overview & Educational Purpose\nPaper Maker by Shami Academy is an educational exam paper generation tool designed for teachers, schools, and academies to create Class 9th and Class 10th question papers in English, Urdu, and Bilingual formats.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "2. Data Storage & User Content\nAll exam papers, institution headers, student names, roll numbers, and custom selections created by you are stored locally on your device.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "3. Google AdMob & Rewarded Advertisements\nThis application uses the official Google Mobile Ads SDK (AdMob) to display Banner, Interstitial, and Rewarded advertisements. Watching a Rewarded Ad unlocks additional books and chapters beyond the free first book and first chapter. Google may use advertising identifiers in accordance with Google's Privacy & Terms.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "4. Permissions Used\n• INTERNET & ACCESS_NETWORK_STATE: Used exclusively to load Google AdMob advertisements and sync syllabus question banks from cloud updates.\n• No camera, microphone, contacts, or location permissions are requested or collected.",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Text(
                            text = "5. Contact & Support\nFor syllabus updates, queries, or feedback regarding Paper Maker by Shami Academy, please contact Shami Academy Administration.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
