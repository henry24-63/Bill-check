package com.example.data.ads

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.data.repository.ContentRepository
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

enum class AdActionType {
    BUTTON_CLICK,
    BILL_SEARCH,
    CALCULATION_RESULT,
    PORTAL_OPEN,
    NAVIGATION
}

enum class AdProviderMode {
    SMART_AI,
    ADMOB,
    ADSTERRA,
    BOTH
}

object SmartAdManager {
    private const val TAG = "SmartAdManager"

    // Default Google official test ad unit IDs
    const val DEFAULT_ADMOB_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val DEFAULT_ADMOB_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"

    private var isInitialized = false
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false

    // AI heuristic metrics
    private var totalUserClicks = 0
    private var lastAdShownTimestamp: Long = 0L
    private var aiDecisionCounter = 0

    // State for Adsterra interactive pop-up dialog
    var adsterraPopupUrl by mutableStateOf<String?>(null)
    var showAdsterraDialog by mutableStateOf(false)
    private var onAdsterraDismissed: (() -> Unit)? = null

    fun initialize(context: Context) {
        if (isInitialized) return
        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: ${status.adapterStatusMap}")
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing MobileAds", e)
        }
    }

    /**
     * Preloads an AdMob interstitial ad ahead of time for instantaneous showing.
     */
    fun preloadInterstitial(context: Context, repository: ContentRepository) {
        if (interstitialAd != null || isInterstitialLoading) return

        val adUnitId = getAdMobInterstitialId(repository)
        isInterstitialLoading = true

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "AdMob Interstitial loaded successfully")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    isInterstitialLoading = false
                    Log.e(TAG, "AdMob Interstitial failed to load: ${error.message}")
                }
            }
        )
    }

    /**
     * Core AI Engine: Analyzes action context, user activity, and admin configurations
     * to automatically decide if and what ad to display (AdMob Interstitial vs Adsterra Pop-up vs Banner).
     */
    fun triggerSmartAd(
        activity: Activity,
        repository: ContentRepository,
        actionType: AdActionType,
        onProceed: () -> Unit
    ) {
        // Check Master toggle
        val adsMasterEnabled = repository.siteSettings.find { 
            it.key.equals("ads_enabled", ignoreCase = true) 
        }?.value?.toBooleanStrictOrNull() ?: repository.adConfig?.adsEnabled ?: true

        if (!adsMasterEnabled) {
            onProceed()
            return
        }

        totalUserClicks++
        aiDecisionCounter++

        val providerMode = getProviderMode(repository)
        val cooldownSeconds = getCooldownSeconds(repository)
        val currentTime = System.currentTimeMillis()
        val elapsedSeconds = (currentTime - lastAdShownTimestamp) / 1000

        // AI Rule 1: High-value actions (Bill search, Calculation, Portal open) increase ad intent
        val isHighIntent = actionType == AdActionType.BILL_SEARCH || 
                           actionType == AdActionType.CALCULATION_RESULT || 
                           actionType == AdActionType.PORTAL_OPEN

        // AI Rule 2: Frequency check (every 2-3 clicks or high-intent action)
        val triggerThreshold = if (isHighIntent) 1 else getClickFrequency(repository)
        val shouldTrigger = (totalUserClicks % triggerThreshold == 0) && (elapsedSeconds >= cooldownSeconds)

        if (!shouldTrigger) {
            onProceed()
            return
        }

        // AI Decision: Choose network (AdMob vs Adsterra)
        val chosenNetwork = when (providerMode) {
            AdProviderMode.ADMOB -> "admob"
            AdProviderMode.ADSTERRA -> "adsterra"
            AdProviderMode.SMART_AI, AdProviderMode.BOTH -> {
                // AI alternates or chooses based on availability
                val adsterraLink = getAdsterraDirectLink(repository)
                if (interstitialAd != null && (aiDecisionCounter % 2 == 0 || adsterraLink.isEmpty())) {
                    "admob"
                } else if (adsterraLink.isNotEmpty()) {
                    "adsterra"
                } else if (interstitialAd != null) {
                    "admob"
                } else {
                    "none"
                }
            }
        }

        when (chosenNetwork) {
            "admob" -> {
                if (interstitialAd != null) {
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            lastAdShownTimestamp = System.currentTimeMillis()
                            preloadInterstitial(activity, repository)
                            onProceed()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null
                            preloadInterstitial(activity, repository)
                            onProceed()
                        }

                        override fun onAdShowedFullScreenContent() {
                            lastAdShownTimestamp = System.currentTimeMillis()
                        }
                    }
                    interstitialAd?.show(activity)
                } else {
                    // Preload for next time and continue
                    preloadInterstitial(activity, repository)
                    onProceed()
                }
            }

            "adsterra" -> {
                val directLink = getAdsterraDirectLink(repository)
                if (directLink.isNotEmpty()) {
                    lastAdShownTimestamp = System.currentTimeMillis()
                    adsterraPopupUrl = directLink
                    onAdsterraDismissed = onProceed
                    showAdsterraDialog = true
                } else {
                    onProceed()
                }
            }

            else -> {
                preloadInterstitial(activity, repository)
                onProceed()
            }
        }
    }

    fun dismissAdsterraPopup() {
        showAdsterraDialog = false
        adsterraPopupUrl = null
        val callback = onAdsterraDismissed
        onAdsterraDismissed = null
        callback?.invoke()
    }

    // Helper functions to retrieve admin panel settings with fallback defaults
    fun getProviderMode(repository: ContentRepository): AdProviderMode {
        val modeStr = repository.cachedAppConfig?.adMode ?: repository.siteSettings.find { 
            it.key.equals("ad_provider_mode", ignoreCase = true) 
        }?.value?.trim()?.lowercase() ?: "manual"

        return when (modeStr) {
            "admob" -> AdProviderMode.ADMOB
            "adsterra" -> AdProviderMode.ADSTERRA
            else -> AdProviderMode.SMART_AI
        }
    }

    fun getAdMobBannerId(repository: ContentRepository): String {
        return repository.cachedAppConfig?.admobBannerId ?: repository.siteSettings.find { 
            it.key.equals("admob_banner_id", ignoreCase = true) 
        }?.value?.trim()?.ifEmpty { null } ?: DEFAULT_ADMOB_BANNER_ID
    }

    fun getAdMobInterstitialId(repository: ContentRepository): String {
        return repository.cachedAppConfig?.admobInterstitialId ?: repository.siteSettings.find { 
            it.key.equals("admob_interstitial_id", ignoreCase = true) 
        }?.value?.trim()?.ifEmpty { null } ?: DEFAULT_ADMOB_INTERSTITIAL_ID
    }

    fun getAdsterraDirectLink(repository: ContentRepository): String {
        return repository.cachedAppConfig?.adsterraUrl ?: repository.siteSettings.find { 
            it.key.equals("adsterra_direct_link", ignoreCase = true) ||
            it.key.equals("adsterra_link", ignoreCase = true) ||
            it.key.equals("adsterra_smartlink", ignoreCase = true)
        }?.value?.trim() ?: ""
    }

    fun getAdsterraBannerUrl(repository: ContentRepository): String {
        return repository.siteSettings.find { 
            it.key.equals("adsterra_banner_url", ignoreCase = true) 
        }?.value?.trim() ?: ""
    }

    private fun getClickFrequency(repository: ContentRepository): Int {
        val count = repository.siteSettings.find { 
            it.key.equals("ai_button_click_frequency", ignoreCase = true) 
        }?.value?.toIntOrNull()
        return count ?: 2
    }

    private fun getCooldownSeconds(repository: ContentRepository): Long {
        val sec = repository.siteSettings.find { 
            it.key.equals("ai_interstitial_cooldown_sec", ignoreCase = true) 
        }?.value?.toLongOrNull()
        return sec ?: 25L
    }
}

/**
 * Native Smart Banner Composable that switches between AdMob and Adsterra
 * dynamically or embeds seamlessly at the bottom of the screen.
 */
@Composable
fun SmartBannerAdView(repository: ContentRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adsEnabled = repository.siteSettings.find { 
        it.key.equals("ads_enabled", ignoreCase = true) 
    }?.value?.toBooleanStrictOrNull() ?: repository.adConfig?.adsEnabled ?: true

    val bannerEnabled = repository.siteSettings.find { 
        it.key.equals("banner_enabled", ignoreCase = true) 
    }?.value?.toBooleanStrictOrNull() ?: repository.adConfig?.bannerEnabled ?: true

    if (!adsEnabled || !bannerEnabled) return

    val providerMode = SmartAdManager.getProviderMode(repository)
    val adsterraBannerUrl = SmartAdManager.getAdsterraBannerUrl(repository)
    val admobBannerId = SmartAdManager.getAdMobBannerId(repository)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (providerMode == AdProviderMode.ADSTERRA && adsterraBannerUrl.isNotEmpty()) {
                // Adsterra Banner View
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(adsterraBannerUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Sponsored Advertisement", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("Tap to explore special promotions & utilities", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            } else {
                // AdMob Native Banner
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { ctx ->
                        AdView(ctx).apply {
                            setAdSize(AdSize.BANNER)
                            adUnitId = admobBannerId
                            loadAd(AdRequest.Builder().build())
                        }
                    }
                )
            }
        }
    }
}

/**
 * Interactive Adsterra Pop-up / Smartlink Dialog with skip countdown
 */
@Composable
fun AdsterraPopupDialog() {
    val context = LocalContext.current
    if (SmartAdManager.showAdsterraDialog && !SmartAdManager.adsterraPopupUrl.isNullOrEmpty()) {
        val targetUrl = SmartAdManager.adsterraPopupUrl!!
        var countdownSeconds by remember { mutableStateOf(3) }

        LaunchedEffect(Unit) {
            while (countdownSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                countdownSeconds--
            }
        }

        Dialog(onDismissRequest = {
            if (countdownSeconds <= 0) {
                SmartAdManager.dismissAdsterraPopup()
            }
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                "Sponsored Ad ⚡",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        if (countdownSeconds <= 0) {
                            IconButton(onClick = { SmartAdManager.dismissAdsterraPopup() }) {
                                Icon(Icons.Default.Close, contentDescription = "Close Ad")
                            }
                        } else {
                            Text(
                                "Skip in ${countdownSeconds}s",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.size(70.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        "Special Sponsored Offer",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Check out this exclusive featured sponsor partner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                            SmartAdManager.dismissAdsterraPopup()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Sponsored Link ↗", fontWeight = FontWeight.Bold)
                    }

                    if (countdownSeconds <= 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { SmartAdManager.dismissAdsterraPopup() }) {
                            Text("Continue to App")
                        }
                    }
                }
            }
        }
    }
}
