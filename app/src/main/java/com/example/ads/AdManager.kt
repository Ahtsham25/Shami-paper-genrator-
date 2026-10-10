package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdsClick
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {

    private const val TAG = "ShamiAdManager"
    private const val PREFS_NAME = "shami_admob_prefs"
    private const val GOOGLE_TEST_PUBLISHER_PREFIX = "ca-app-pub-3940256099942544"

    const val GOOGLE_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val GOOGLE_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val GOOGLE_TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    const val GOOGLE_TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    val CODE_APP_ID: String = BuildConfig.ADMOB_APP_ID.ifBlank { GOOGLE_TEST_APP_ID }
    val CODE_BANNER_AD_UNIT_ID: String = BuildConfig.ADMOB_BANNER_ID.ifBlank { GOOGLE_TEST_BANNER_ID }
    val CODE_INTERSTITIAL_AD_UNIT_ID: String = BuildConfig.ADMOB_INTERSTITIAL_ID.ifBlank { GOOGLE_TEST_INTERSTITIAL_ID }
    val CODE_REWARDED_AD_UNIT_ID: String = BuildConfig.ADMOB_REWARDED_ID.ifBlank { GOOGLE_TEST_REWARDED_ID }

    private val mainHandler: Handler by lazy { Handler(Looper.getMainLooper()) }

    private fun sanitizeUnitId(candidate: String, fallback: String): String {
        val trimmed = candidate.trim()
        return if (trimmed.startsWith("ca-app-pub-") && trimmed.contains("/")) {
            trimmed
        } else {
            fallback
        }
    }

    data class AdConfigurationState(
        val adsEnabled: Boolean = true,
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
            get() = if (useTestAds) {
                GOOGLE_TEST_BANNER_ID
            } else {
                sanitizeUnitId(bannerAdUnitId, GOOGLE_TEST_BANNER_ID)
            }

        val activeInterstitialId: String
            get() = if (useTestAds) {
                GOOGLE_TEST_INTERSTITIAL_ID
            } else {
                sanitizeUnitId(interstitialAdUnitId, GOOGLE_TEST_INTERSTITIAL_ID)
            }

        val activeRewardedId: String
            get() = if (useTestAds) {
                GOOGLE_TEST_REWARDED_ID
            } else {
                sanitizeUnitId(rewardedAdUnitId, GOOGLE_TEST_REWARDED_ID)
            }
    }

    private val _state = MutableStateFlow(AdConfigurationState())
    val state: StateFlow<AdConfigurationState> = _state.asStateFlow()

    private var rewardedAd: RewardedAd? = null
    private var interstitialAd: InterstitialAd? = null
    private var isLoadingRewarded = false
    private var isLoadingInterstitial = false
    @Volatile
    private var isSdkInitStarted = false

    private fun runOnMainThreadSafe(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            try {
                block()
            } catch (t: Throwable) {
                Log.w(TAG, "AdManager main-thread action error: ${t.message}")
            }
        } else {
            mainHandler.post {
                try {
                    block()
                } catch (t: Throwable) {
                    Log.w(TAG, "AdManager posted main-thread action error: ${t.message}")
                }
            }
        }
    }

    fun isGoogleTestUnitId(adUnitId: String): Boolean {
        return adUnitId.trim().startsWith(GOOGLE_TEST_PUBLISHER_PREFIX) || adUnitId.isBlank()
    }

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
            fp.contains("robolectric") ||
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

    fun shouldUseGoogleMobileAdsSdk(): Boolean {
        return _state.value.adsEnabled && !isRunningOnEmulator()
    }

    fun initialize(context: Context) {
        try {
            val appContext = context.applicationContext
            val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            val adsEnabled = prefs.getBoolean("ads_enabled", true)

            val codeHasLiveIds = !isGoogleTestUnitId(CODE_REWARDED_AD_UNIT_ID) ||
                !isGoogleTestUnitId(CODE_BANNER_AD_UNIT_ID) ||
                !isGoogleTestUnitId(CODE_INTERSTITIAL_AD_UNIT_ID)

            val savedBanner = prefs.getString("banner_id", null)
            val savedInterstitial = prefs.getString("interstitial_id", null)
            val savedRewarded = prefs.getString("rewarded_id", null)
            val savedAppId = prefs.getString("app_id", null)

            val effectiveBanner = if (!savedBanner.isNullOrBlank()) {
                savedBanner
            } else {
                CODE_BANNER_AD_UNIT_ID
            }

            val effectiveInterstitial = if (!savedInterstitial.isNullOrBlank()) {
                savedInterstitial
            } else {
                CODE_INTERSTITIAL_AD_UNIT_ID
            }

            val effectiveRewarded = if (!savedRewarded.isNullOrBlank()) {
                savedRewarded
            } else {
                CODE_REWARDED_AD_UNIT_ID
            }

            val anyLiveIdConfigured = !isGoogleTestUnitId(effectiveRewarded) ||
                !isGoogleTestUnitId(effectiveBanner) ||
                !isGoogleTestUnitId(effectiveInterstitial)

            val useTestAds = if (prefs.contains("use_test_ads")) {
                prefs.getBoolean("use_test_ads", !anyLiveIdConfigured)
            } else if (codeHasLiveIds) {
                false
            } else {
                !anyLiveIdConfigured
            }

            _state.value = _state.value.copy(
                adsEnabled = adsEnabled,
                useTestAds = useTestAds,
                appId = savedAppId ?: CODE_APP_ID,
                bannerAdUnitId = effectiveBanner,
                interstitialAdUnitId = effectiveInterstitial,
                rewardedAdUnitId = effectiveRewarded,
                lastAdStatusMessage = when {
                    !adsEnabled -> "Google Ads Disabled (OFF)"
                    useTestAds -> "Google Test Ads Active"
                    else -> "Live AdMob Ads Active"
                }
            )

            if (!adsEnabled) {
                rewardedAd = null
                interstitialAd = null
                return
            }

            if (shouldUseGoogleMobileAdsSdk()) {
                initializeSdkAndPreload(appContext)
            } else {
                _state.value = _state.value.copy(
                    isSdkInitialized = true,
                    isRewardedLoaded = true,
                    isInterstitialLoaded = true
                )
            }
        } catch (t: Throwable) {
            Log.w(TAG, "AdManager initialize warning: ${t.message}")
        }
    }

    private fun initializeSdkAndPreload(appContext: Context) {
        runOnMainThreadSafe {
            if (_state.value.isSdkInitialized) {
                preloadRewardedAd(appContext)
                preloadInterstitialAd(appContext)
                return@runOnMainThreadSafe
            }
            if (isSdkInitStarted) return@runOnMainThreadSafe
            isSdkInitStarted = true
            try {
                MobileAds.initialize(appContext) {
                    runOnMainThreadSafe {
                        _state.value = _state.value.copy(
                            isSdkInitialized = true,
                            lastAdStatusMessage = if (_state.value.useTestAds) {
                                "Google Test Ads Ready"
                            } else {
                                "Live AdMob Production Mode"
                            }
                        )
                        preloadRewardedAd(appContext)
                        preloadInterstitialAd(appContext)
                    }
                }
            } catch (t: Throwable) {
                isSdkInitStarted = false
                Log.w(TAG, "MobileAds initialization warning: ${t.message}")
            }
        }
    }

    fun updateAdConfig(
        context: Context,
        adsEnabled: Boolean = _state.value.adsEnabled,
        useTestAds: Boolean = _state.value.useTestAds,
        appId: String = _state.value.appId,
        bannerId: String = _state.value.bannerAdUnitId,
        interstitialId: String = _state.value.interstitialAdUnitId,
        rewardedId: String = _state.value.rewardedAdUnitId,
        configTimestamp: Long = System.currentTimeMillis()
    ) {
        val cleanBanner = sanitizeUnitId(bannerId, GOOGLE_TEST_BANNER_ID)
        val cleanInterstitial = sanitizeUnitId(interstitialId, GOOGLE_TEST_INTERSTITIAL_ID)
        val cleanRewarded = sanitizeUnitId(rewardedId, GOOGLE_TEST_REWARDED_ID)
        val cleanAppId = appId.trim().ifBlank { GOOGLE_TEST_APP_ID }

        try {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean("ads_enabled", adsEnabled)
                .putBoolean("use_test_ads", useTestAds)
                .putString("app_id", cleanAppId)
                .putString("banner_id", cleanBanner)
                .putString("interstitial_id", cleanInterstitial)
                .putString("rewarded_id", cleanRewarded)
                .putLong("ad_config_updated_at", configTimestamp)
                .apply()
        } catch (t: Throwable) {
            Log.w(TAG, "Failed saving ad prefs: ${t.message}")
        }

        runOnMainThreadSafe {
            val previousActiveBanner = _state.value.activeBannerId
            val previousActiveInterstitial = _state.value.activeInterstitialId
            val previousActiveRewarded = _state.value.activeRewardedId

            _state.value = _state.value.copy(
                adsEnabled = adsEnabled,
                useTestAds = useTestAds,
                appId = cleanAppId,
                bannerAdUnitId = cleanBanner,
                interstitialAdUnitId = cleanInterstitial,
                rewardedAdUnitId = cleanRewarded,
                isRewardedLoaded = if (!adsEnabled) false else if (isRunningOnEmulator()) true else _state.value.isRewardedLoaded,
                isInterstitialLoaded = if (!adsEnabled) false else if (isRunningOnEmulator()) true else _state.value.isInterstitialLoaded,
                lastAdStatusMessage = when {
                    !adsEnabled -> "Google Ads Disabled (OFF)"
                    useTestAds -> "Google Test Ads Active"
                    else -> "Live AdMob IDs Active (Test Ads OFF)"
                }
            )

            if (!adsEnabled) {
                rewardedAd = null
                interstitialAd = null
                isLoadingRewarded = false
                isLoadingInterstitial = false
                return@runOnMainThreadSafe
            }

            if (_state.value.activeRewardedId != previousActiveRewarded) {
                rewardedAd = null
                isLoadingRewarded = false
            }
            if (_state.value.activeInterstitialId != previousActiveInterstitial) {
                interstitialAd = null
                isLoadingInterstitial = false
            }

            if (shouldUseGoogleMobileAdsSdk()) {
                initializeSdkAndPreload(context.applicationContext)
            }
        }
    }

    fun applyRemoteConfigIfPresent(context: Context, remote: RemoteAdConfig?) {
        if (remote == null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val localUpdated = prefs.getLong("ad_config_updated_at", 0L)
            val hasLocalOverride = localUpdated > 0L || prefs.contains("ads_enabled") || prefs.contains("use_test_ads")
            // Do not let an un-timestamped or older remote GitHub config overwrite local Admin panel switches!
            if (hasLocalOverride && (remote.updatedAt <= 0L || remote.updatedAt <= localUpdated)) {
                return
            }
            updateAdConfig(
                context = context,
                adsEnabled = remote.adsEnabled,
                useTestAds = remote.useTestAds,
                appId = remote.appId,
                bannerId = remote.bannerAdUnitId,
                interstitialId = remote.interstitialAdUnitId,
                rewardedId = remote.rewardedAdUnitId,
                configTimestamp = if (remote.updatedAt > 0L) remote.updatedAt else System.currentTimeMillis()
            )
        } catch (t: Throwable) {
            Log.w(TAG, "applyRemoteConfigIfPresent warning: ${t.message}")
        }
    }

    fun preloadRewardedAd(context: Context) {
        if (!shouldUseGoogleMobileAdsSdk()) return
        runOnMainThreadSafe {
            if (!shouldUseGoogleMobileAdsSdk()) return@runOnMainThreadSafe
            if (isLoadingRewarded || rewardedAd != null) return@runOnMainThreadSafe
            val unitId = _state.value.activeRewardedId
            if (unitId.isBlank()) return@runOnMainThreadSafe

            isLoadingRewarded = true
            try {
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
            } catch (t: Throwable) {
                isLoadingRewarded = false
                Log.w(TAG, "preloadRewardedAd warning: ${t.message}")
            }
        }
    }

    fun preloadInterstitialAd(context: Context) {
        if (!shouldUseGoogleMobileAdsSdk()) return
        runOnMainThreadSafe {
            if (!shouldUseGoogleMobileAdsSdk()) return@runOnMainThreadSafe
            if (isLoadingInterstitial || interstitialAd != null) return@runOnMainThreadSafe
            val unitId = _state.value.activeInterstitialId
            if (unitId.isBlank()) return@runOnMainThreadSafe

            isLoadingInterstitial = true
            try {
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
            } catch (t: Throwable) {
                isLoadingInterstitial = false
                Log.w(TAG, "preloadInterstitialAd warning: ${t.message}")
            }
        }
    }

    /**
     * Directly opens the full-screen Google AdMob Ad on tap without showing any intermediate dialog!
     * If Google Ads are turned OFF in Admin, unlocks immediately.
     */
    fun triggerRewardedUnlock(
        activity: Activity?,
        context: Context,
        onUnlocked: () -> Unit
    ) {
        if (!_state.value.adsEnabled) {
            onUnlocked()
            return
        }

        if (isRunningOnEmulator() || activity == null || activity.isFinishing) {
            onUnlocked()
            return
        }

        runOnMainThreadSafe {
            val currentRewarded = rewardedAd
            if (currentRewarded != null) {
                try {
                    var earned = false
                    currentRewarded.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            _state.value = _state.value.copy(isRewardedLoaded = false)
                            preloadRewardedAd(context)
                            if (earned) {
                                onUnlocked()
                            }
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            rewardedAd = null
                            _state.value = _state.value.copy(isRewardedLoaded = false)
                            preloadRewardedAd(context)
                            onUnlocked()
                        }
                    }
                    currentRewarded.show(activity) {
                        earned = true
                    }
                    return@runOnMainThreadSafe
                } catch (t: Throwable) {
                    rewardedAd = null
                    onUnlocked()
                    return@runOnMainThreadSafe
                }
            }

            // If RewardedAd is still loading, show preloaded InterstitialAd immediately if ready
            val currentInterstitial = interstitialAd
            if (currentInterstitial != null) {
                try {
                    currentInterstitial.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            _state.value = _state.value.copy(isInterstitialLoaded = false)
                            preloadInterstitialAd(context)
                            preloadRewardedAd(context)
                            onUnlocked()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                            _state.value = _state.value.copy(isInterstitialLoaded = false)
                            preloadInterstitialAd(context)
                            onUnlocked()
                        }
                    }
                    currentInterstitial.show(activity)
                    return@runOnMainThreadSafe
                } catch (t: Throwable) {
                    interstitialAd = null
                    onUnlocked()
                    return@runOnMainThreadSafe
                }
            }

            // Load and immediately display the Google RewardedAd full-screen activity (no intermediate popup dialog)
            try {
                Toast.makeText(context, "Loading Google Ad...", Toast.LENGTH_SHORT).show()
                val unitId = _state.value.activeRewardedId
                val adRequest = AdRequest.Builder().build()
                RewardedAd.load(
                    context.applicationContext,
                    unitId,
                    adRequest,
                    object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) {
                            try {
                                var earned = false
                                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        rewardedAd = null
                                        preloadRewardedAd(context)
                                        if (earned) {
                                            onUnlocked()
                                        }
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        rewardedAd = null
                                        preloadRewardedAd(context)
                                        onUnlocked()
                                    }
                                }
                                if (!activity.isFinishing) {
                                    ad.show(activity) {
                                        earned = true
                                    }
                                } else {
                                    onUnlocked()
                                }
                            } catch (t: Throwable) {
                                onUnlocked()
                            }
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            rewardedAd = null
                            preloadRewardedAd(context)
                            onUnlocked()
                        }
                    }
                )
            } catch (t: Throwable) {
                onUnlocked()
            }
        }
    }

    fun showInterstitialIfLoaded(activity: Activity?, context: Context, onComplete: () -> Unit) {
        if (!_state.value.adsEnabled || !shouldUseGoogleMobileAdsSdk()) {
            onComplete()
            return
        }

        runOnMainThreadSafe {
            val current = interstitialAd
            if (current != null && activity != null && !activity.isFinishing) {
                try {
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
                } catch (t: Throwable) {
                    interstitialAd = null
                    preloadInterstitialAd(context)
                    onComplete()
                }
            } else {
                preloadInterstitialAd(context)
                onComplete()
            }
        }
    }
}

@Composable
fun AdMobBannerBar(
    modifier: Modifier = Modifier
) {
    val adState by AdManager.state.collectAsState()
    if (!adState.adsEnabled) return

    val useSdkBanner = !AdManager.isRunningOnEmulator() && adState.isSdkInitialized
    val activeBannerUnitId = adState.activeBannerId
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (useSdkBanner) {
            // Key by activeBannerUnitId so a new AdView is cleanly created if the unit ID changes,
            // preventing BaseAdView.setAdUnitId IllegalStateException ("The ad unit ID can only be set once on AdView").
            key(activeBannerUnitId) {
                val bannerHolder = remember(activeBannerUnitId) {
                    var createdAdView: AdView? = null
                    val container = FrameLayout(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    try {
                        val adView = AdView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            setAdSize(AdSize.BANNER)
                            adUnitId = activeBannerUnitId
                            adListener = object : AdListener() {}
                        }
                        container.addView(adView)
                        adView.loadAd(AdRequest.Builder().build())
                        createdAdView = adView
                    } catch (t: Throwable) {
                        Log.w("ShamiAdManager", "Banner AdView creation/load skipped: ${t.message}")
                    }
                    Pair(container, createdAdView)
                }

                DisposableEffect(bannerHolder) {
                    onDispose {
                        try {
                            bannerHolder.second?.destroy()
                        } catch (_: Throwable) {
                        }
                    }
                }

                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { bannerHolder.first }
                )
            }
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
                        text = if (adState.useTestAds) "Google AdMob Banner (Test Ad)" else "Google AdMob Banner (Live Mode)",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = if (adState.useTestAds) "TEST AD" else "LIVE AD",
                    color = Color(0xFF10B981),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
