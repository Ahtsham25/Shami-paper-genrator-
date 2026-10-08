package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.BuildConfig
import com.example.data.RemoteAdConfig
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {

    private const val TAG = "ShamiAdManager"
    private const val GOOGLE_TEST_PUBLISHER_PREFIX = "ca-app-pub-3940256099942544"

    const val GOOGLE_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val GOOGLE_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val GOOGLE_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val GOOGLE_TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    // Replace these in .env or right here in code with your real AdMob IDs to automatically
    // disable Test Ads and run 100% Live AdMob Ads on physical devices!
    val CODE_APP_ID: String = BuildConfig.ADMOB_APP_ID.ifBlank { GOOGLE_TEST_APP_ID }
    val CODE_BANNER_AD_UNIT_ID: String = BuildConfig.ADMOB_BANNER_ID.ifBlank { GOOGLE_TEST_BANNER_ID }
    val CODE_INTERSTITIAL_AD_UNIT_ID: String = BuildConfig.ADMOB_INTERSTITIAL_ID.ifBlank { GOOGLE_TEST_INTERSTITIAL_ID }
    val CODE_REWARDED_AD_UNIT_ID: String = BuildConfig.ADMOB_REWARDED_ID.ifBlank { GOOGLE_TEST_REWARDED_ID }

    data class AdConfigurationState(
        val useTestAds: Boolean = true,
        val appId: String = CODE_APP_ID,
        val bannerAdUnitId: String = CODE_BANNER_AD_UNIT_ID,
        val interstitialAdUnitId: String = CODE_INTERSTITIAL_AD_UNIT_ID,
        val rewardedAdUnitId: String = CODE_REWARDED_AD_UNIT_ID,
        val isSdkInitialized: Boolean = false,
        val isRewardedLoaded: Boolean = false,
        val isInterstitialLoaded: Boolean = false,
        val lastAdStatusMessage: String = "Google Mobile Ads Ready"
    ) {
        val activeBannerId: String
            get() = if (useTestAds) GOOGLE_TEST_BANNER_ID else bannerAdUnitId.trim()

        val activeInterstitialId: String
            get() = if (useTestAds) GOOGLE_TEST_INTERSTITIAL_ID else interstitialAdUnitId.trim()

        val activeRewardedId: String
            get() = if (useTestAds) GOOGLE_TEST_REWARDED_ID else rewardedAdUnitId.trim()
    }

    private val _state = MutableStateFlow(AdConfigurationState())
    val state: StateFlow<AdConfigurationState> = _state.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingRewarded = false
    private var isLoadingInterstitial = false

    fun isGoogleTestUnitId(adUnitId: String): Boolean {
        return adUnitId.trim().startsWith(GOOGLE_TEST_PUBLISHER_PREFIX) || adUnitId.isBlank()
    }

    /**
     * Detects whether the app is running inside a virtual/streaming emulator environment.
     * On emulators or when useTestAds == true, we use lightweight native Compose test ads
     * instead of spinning up offscreen WebViews/AdServices that log MESA/JS/AdServices errors.
     */
    fun isRunningOnEmulator(): Boolean {
        val fp = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val device = Build.DEVICE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        return fp.startsWith("generic") ||
            fp.startsWith("unknown") ||
            model.contains("google_sdk") ||
            model.contains("emulator") ||
            model.contains("android sdk built for") ||
            model.contains("sdk_gphone") ||
            manufacturer.contains("genymotion") ||
            (brand.startsWith("generic") && device.startsWith("generic")) ||
            product.contains("sdk") ||
            product.contains("emulator") ||
            product.contains("simulator") ||
            hardware.contains("goldfish") ||
            hardware.contains("ranchu") ||
            hardware.contains("cuttlefish")
    }

    fun shouldUseLiveGoogleSdk(): Boolean {
        return !_state.value.useTestAds && !isRunningOnEmulator()
    }

    fun initialize(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("shami_admob_prefs", Context.MODE_PRIVATE)

        val codeHasLiveIds = !isGoogleTestUnitId(CODE_REWARDED_AD_UNIT_ID) ||
            !isGoogleTestUnitId(CODE_BANNER_AD_UNIT_ID) ||
            !isGoogleTestUnitId(CODE_INTERSTITIAL_AD_UNIT_ID)

        val savedBanner = prefs.getString("banner_id", null)
        val savedInterstitial = prefs.getString("interstitial_id", null)
        val savedRewarded = prefs.getString("rewarded_id", null)
        val savedAppId = prefs.getString("app_id", null)

        val effectiveBanner = if (!isGoogleTestUnitId(CODE_BANNER_AD_UNIT_ID)) {
            CODE_BANNER_AD_UNIT_ID
        } else {
            savedBanner ?: CODE_BANNER_AD_UNIT_ID
        }

        val effectiveInterstitial = if (!isGoogleTestUnitId(CODE_INTERSTITIAL_AD_UNIT_ID)) {
            CODE_INTERSTITIAL_AD_UNIT_ID
        } else {
            savedInterstitial ?: CODE_INTERSTITIAL_AD_UNIT_ID
        }

        val effectiveRewarded = if (!isGoogleTestUnitId(CODE_REWARDED_AD_UNIT_ID)) {
            CODE_REWARDED_AD_UNIT_ID
        } else {
            savedRewarded ?: CODE_REWARDED_AD_UNIT_ID
        }

        val anyLiveIdConfigured = !isGoogleTestUnitId(effectiveRewarded) ||
            !isGoogleTestUnitId(effectiveBanner) ||
            !isGoogleTestUnitId(effectiveInterstitial)

        val useTestAds = if (codeHasLiveIds) {
            false
        } else if (prefs.contains("use_test_ads")) {
            prefs.getBoolean("use_test_ads", !anyLiveIdConfigured)
        } else {
            !anyLiveIdConfigured
        }

        _state.value = _state.value.copy(
            useTestAds = useTestAds,
            appId = savedAppId ?: CODE_APP_ID,
            bannerAdUnitId = effectiveBanner,
            interstitialAdUnitId = effectiveInterstitial,
            rewardedAdUnitId = effectiveRewarded
        )

        if (shouldUseLiveGoogleSdk()) {
            initializeLiveSdkIfNeeded(appContext)
        } else {
            _state.value = _state.value.copy(
                isSdkInitialized = true,
                isRewardedLoaded = true,
                isInterstitialLoaded = true,
                lastAdStatusMessage = "Google Test Ads Mode Ready"
            )
        }
    }

    private fun initializeLiveSdkIfNeeded(appContext: Context) {
        try {
            MobileAds.initialize(appContext) {
                _state.value = _state.value.copy(
                    isSdkInitialized = true,
                    lastAdStatusMessage = "Live AdMob Production Mode"
                )
                preloadRewardedAd(appContext)
                preloadInterstitialAd(appContext)
            }
        } catch (e: Exception) {
            Log.w(TAG, "MobileAds initialization warning: ${e.message}")
        }
    }

    fun updateAdConfig(
        context: Context,
        useTestAds: Boolean,
        appId: String,
        bannerId: String,
        interstitialId: String,
        rewardedId: String
    ) {
        val cleanBanner = bannerId.trim().ifBlank { GOOGLE_TEST_BANNER_ID }
        val cleanInterstitial = interstitialId.trim().ifBlank { GOOGLE_TEST_INTERSTITIAL_ID }
        val cleanRewarded = rewardedId.trim().ifBlank { GOOGLE_TEST_REWARDED_ID }
        val cleanAppId = appId.trim().ifBlank { GOOGLE_TEST_APP_ID }

        context.applicationContext
            .getSharedPreferences("shami_admob_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("use_test_ads", useTestAds)
            .putString("app_id", cleanAppId)
            .putString("banner_id", cleanBanner)
            .putString("interstitial_id", cleanInterstitial)
            .putString("rewarded_id", cleanRewarded)
            .apply()

        rewardedAd = null
        interstitialAd = null
        isLoadingRewarded = false
        isLoadingInterstitial = false

        _state.value = _state.value.copy(
            useTestAds = useTestAds,
            appId = cleanAppId,
            bannerAdUnitId = cleanBanner,
            interstitialAdUnitId = cleanInterstitial,
            rewardedAdUnitId = cleanRewarded,
            isRewardedLoaded = useTestAds,
            isInterstitialLoaded = useTestAds,
            lastAdStatusMessage = if (useTestAds) {
                "Google Test Ads Active"
            } else {
                "Live AdMob IDs Active (Test Ads OFF)"
            }
        )

        if (shouldUseLiveGoogleSdk()) {
            initializeLiveSdkIfNeeded(context.applicationContext)
        }
    }

    fun applyRemoteConfigIfPresent(context: Context, remote: RemoteAdConfig?) {
        if (remote == null) return
        updateAdConfig(
            context = context,
            useTestAds = remote.useTestAds,
            appId = remote.appId,
            bannerId = remote.bannerAdUnitId,
            interstitialId = remote.interstitialAdUnitId,
            rewardedId = remote.rewardedAdUnitId
        )
    }

    fun preloadRewardedAd(context: Context) {
        if (!shouldUseLiveGoogleSdk()) return
        if (isLoadingRewarded || rewardedAd != null) return
        val unitId = _state.value.activeRewardedId
        if (unitId.isBlank()) return

        isLoadingRewarded = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            unitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoadingRewarded = false
                    _state.value = _state.value.copy(
                        isRewardedLoaded = true,
                        lastAdStatusMessage = "Rewarded Ad Ready"
                    )
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    isLoadingRewarded = false
                    _state.value = _state.value.copy(
                        isRewardedLoaded = false,
                        lastAdStatusMessage = "Rewarded Load: ${loadAdError.message}"
                    )
                }
            }
        )
    }

    fun preloadInterstitialAd(context: Context) {
        if (!shouldUseLiveGoogleSdk()) return
        if (isLoadingInterstitial || interstitialAd != null) return
        val unitId = _state.value.activeInterstitialId
        if (unitId.isBlank()) return

        isLoadingInterstitial = true
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context.applicationContext,
            unitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoadingInterstitial = false
                    _state.value = _state.value.copy(isInterstitialLoaded = true)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    interstitialAd = null
                    isLoadingInterstitial = false
                    _state.value = _state.value.copy(isInterstitialLoaded = false)
                }
            }
        )
    }

    fun showRewardedAd(
        activity: Activity?,
        context: Context,
        onRewardEarned: () -> Unit,
        onAdFailedInLiveMode: (String) -> Unit,
        onNeedTestFallbackInTestMode: () -> Unit
    ) {
        // In Test Ads mode or on an Emulator, immediately run the native Test Rewarded Ad countdown
        // without creating offscreen WebViews that trigger MESA/JS/AdServices errors.
        if (!shouldUseLiveGoogleSdk()) {
            onNeedTestFallbackInTestMode()
            return
        }

        val currentAd = rewardedAd
        if (currentAd != null && activity != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    _state.value = _state.value.copy(isRewardedLoaded = false)
                    preloadRewardedAd(context)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    _state.value = _state.value.copy(isRewardedLoaded = false)
                    preloadRewardedAd(context)
                    onAdFailedInLiveMode("Ad failed to display: ${adError.message}")
                }
            }
            currentAd.show(activity) {
                onRewardEarned()
            }
            return
        }

        val unitId = _state.value.activeRewardedId
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context.applicationContext,
            unitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    if (activity != null) {
                        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd = null
                                preloadRewardedAd(context)
                            }
                        }
                        ad.show(activity) {
                            onRewardEarned()
                        }
                    } else {
                        onAdFailedInLiveMode("Unable to display ad right now.")
                    }
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    rewardedAd = null
                    _state.value = _state.value.copy(
                        isRewardedLoaded = false,
                        lastAdStatusMessage = "Rewarded Ad Error (${loadAdError.code})"
                    )
                    onAdFailedInLiveMode(
                        "Could not load Google Rewarded Ad right now (${loadAdError.message}). Please try again."
                    )
                }
            }
        )
    }

    fun showInterstitialIfLoaded(activity: Activity?, context: Context, onComplete: () -> Unit) {
        if (!shouldUseLiveGoogleSdk()) {
            onComplete()
            return
        }

        val current = interstitialAd
        if (current != null && activity != null) {
            current.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    _state.value = _state.value.copy(isInterstitialLoaded = false)
                    preloadInterstitialAd(context)
                    onComplete()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    interstitialAd = null
                    _state.value = _state.value.copy(isInterstitialLoaded = false)
                    preloadInterstitialAd(context)
                    onComplete()
                }
            }
            current.show(activity)
        } else {
            preloadInterstitialAd(context)
            onComplete()
        }
    }
}

@Composable
fun AdMobBannerBar(
    modifier: Modifier = Modifier
) {
    val adState by AdManager.state.collectAsState()
    val useLiveSdk = remember(adState.useTestAds) { AdManager.shouldUseLiveGoogleSdk() }
    val activeBannerUnitId = adState.activeBannerId

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (useLiveSdk) {
            AndroidView(
                modifier = Modifier.fillMaxWidth(),
                factory = { ctx ->
                    AdView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        setAdSize(AdSize.BANNER)
                        adUnitId = activeBannerUnitId
                        adListener = object : AdListener() {}
                        loadAd(AdRequest.Builder().build())
                    }
                },
                update = { adView ->
                    if (adView.adUnitId != activeBannerUnitId) {
                        adView.adUnitId = activeBannerUnitId
                        adView.loadAd(AdRequest.Builder().build())
                    }
                }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0B2447))
                    .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AdsClick,
                        contentDescription = "AdMob Banner",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Google AdMob Banner (Test Ad)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "TEST AD",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Automatically starts and plays the Google Rewarded Ad as soon as a locked Book or Chapter is tapped!
 * No confirmation button is needed — the Rewarded Ad triggers immediately on tap.
 */
@Composable
fun AutoPlayingRewardedAdOverlay(
    itemTitle: String,
    onDismiss: () -> Unit,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity

    var isLoadingSdkAd by remember { mutableStateOf(true) }
    var inEmulatorTestCountdown by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableIntStateOf(3) }
    var liveAdError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(itemTitle) {
        isLoadingSdkAd = true
        liveAdError = null
        AdManager.showRewardedAd(
            activity = activity,
            context = context,
            onRewardEarned = {
                isLoadingSdkAd = false
                onUnlocked()
            },
            onAdFailedInLiveMode = { errMsg ->
                isLoadingSdkAd = false
                liveAdError = errMsg
            },
            onNeedTestFallbackInTestMode = {
                isLoadingSdkAd = false
                inEmulatorTestCountdown = true
            }
        )
    }

    LaunchedEffect(inEmulatorTestCountdown) {
        if (inEmulatorTestCountdown) {
            secondsLeft = 3
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
            onUnlocked()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isLoadingSdkAd && !inEmulatorTestCountdown) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.PlayCircleFilled,
                contentDescription = "Rewarded Ad",
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(42.dp)
            )
        },
        title = {
            Text(
                text = if (liveAdError != null) "Ad Unavailable" else "Playing Rewarded Ad...",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Unlocking: $itemTitle",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (isLoadingSdkAd) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
                        Text("Starting Google Rewarded Ad...", fontSize = 13.sp)
                    }
                }

                if (inEmulatorTestCountdown) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0B2447))
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Google Test Rewarded Ad ($secondsLeft s)...",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (3 - secondsLeft) / 3f },
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                if (liveAdError != null) {
                    Text(
                        text = liveAdError!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            if (liveAdError != null) {
                TextButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        }
    )
}
