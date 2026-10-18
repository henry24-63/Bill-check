package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.ads.AdsterraPopupDialog
import com.example.data.ads.SmartAdManager
import com.example.data.ads.SmartBannerAdView
import com.example.data.model.AppConfig
import com.example.data.repository.ContentRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {
    private lateinit var repository: ContentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        repository = ContentRepository(applicationContext)

        // Initialize Smart AI Ad Engine and preload interstitial ad
        SmartAdManager.initialize(applicationContext)
        SmartAdManager.preloadInterstitial(this, repository)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                // Smart AI decides whether to display banner on this screen
                val shouldShowBanner = currentRoute != "splash" && 
                                       currentRoute?.startsWith("maintenance") != true

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    bottomBar = {
                        if (shouldShowBanner && SmartAdManager.currentPlacement == com.example.data.ads.AdPlacement.BOTTOM_BAR) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SmartBannerAdView(repository = repository)
                                }
                            }
                        }
                    }
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = padding.calculateBottomPadding())
                    ) {
                        NavHost(navController = navController, startDestination = "splash") {
                            composable("splash") {
                                SplashScreen(
                                    repository = repository,
                                    onNavigateHome = { 
                                        navController.navigate("home") { 
                                            popUpTo("splash") { inclusive = true } 
                                        } 
                                    },
                                    onNavigateMaintenance = { msg ->
                                        val encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8.toString())
                                        navController.navigate("maintenance/$encoded") { 
                                            popUpTo("splash") { inclusive = true } 
                                        }
                                    },
                                    onNavigateUpdate = { config, isForce ->
                                        // Show update dialog
                                    }
                                )
                            }
                            composable("maintenance/{msg}") { backStackEntry ->
                                val msg = backStackEntry.arguments?.getString("msg") ?: "Under maintenance"
                                MaintenanceScreen(message = msg, onRetry = {
                                    navController.navigate("splash") { 
                                        popUpTo("maintenance/{msg}") { inclusive = true } 
                                    }
                                })
                            }
                            composable("home") {
                                HomeScreen(
                                    repository = repository, 
                                    onNavigate = { route -> navController.navigate(route) }
                                )
                            }
                            composable("bill_check") {
                                BillCheckScreen(
                                    repository = repository,
                                    onOpenBill = { url, name ->
                                        repository.currentSelectedUrl = url
                                        repository.currentSelectedTitle = name
                                        navController.navigate("webview")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("direct_bill") {
                                BillCheckScreen(
                                    repository = repository,
                                    onOpenBill = { url, name ->
                                        repository.currentSelectedUrl = url
                                        repository.currentSelectedTitle = name
                                        navController.navigate("webview")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("bills") {
                                BillListScreen(
                                    repository = repository,
                                    onSelectBill = { url, name ->
                                        repository.currentSelectedUrl = url
                                        repository.currentSelectedTitle = name
                                        navController.navigate("webview")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("webview") {
                                WebViewScreen(
                                    url = repository.currentSelectedUrl,
                                    title = repository.currentSelectedTitle.ifEmpty { "Bill Viewer" },
                                    repository = repository,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("webview/{url}/{name}") { backStackEntry ->
                                val url = backStackEntry.arguments?.getString("url") ?: repository.currentSelectedUrl
                                val name = backStackEntry.arguments?.getString("name") ?: repository.currentSelectedTitle
                                WebViewScreen(
                                    url = url,
                                    title = name.ifEmpty { "Bill Viewer" },
                                    repository = repository,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("calculator") { 
                                CalculatorScreen(repository = repository, onBack = { navController.popBackStack() }) 
                            }
                            composable("appliances") { 
                                ApplianceCalculatorScreen(repository = repository, onBack = { navController.popBackStack() }) 
                            }
                            composable("helplines") { 
                                HelplinesScreen(repository = repository, onBack = { navController.popBackStack() }) 
                            }
                            composable("codes") { 
                                PackageCodesScreen(repository = repository, onBack = { navController.popBackStack() }) 
                            }
                            composable("reminders") { 
                                RemindersScreen(repository = repository, onBack = { navController.popBackStack() }) 
                            }
                            composable("tips") { 
                                TipsScreen(repository = repository, onBack = { navController.popBackStack() }) 
                            }
                            composable("feedback") { 
                                FeedbackScreen(onBack = { navController.popBackStack() }) 
                            }
                            composable("settings") { 
                                SettingsScreen(
                                    repository = repository,
                                    onNavigate = { route -> navController.navigate(route) },
                                    onBack = { navController.popBackStack() }
                                ) 
                            }
                            composable("privacy_policy") {
                                PrivacyPolicyScreen(
                                    repository = repository,
                                    onOpenWebView = { url, name ->
                                        repository.currentSelectedUrl = url
                                        repository.currentSelectedTitle = name
                                        navController.navigate("webview")
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }

                        // Adsterra Interactive Smart Dialog
                        AdsterraPopupDialog()
                    }
                }
            }
        }
    }
}
