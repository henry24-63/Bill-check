package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(
                            repository = repository,
                            onNavigateHome = { navController.navigate("home") { popUpTo("splash") { inclusive = true } } },
                            onNavigateMaintenance = { msg ->
                                val encoded = URLEncoder.encode(msg, StandardCharsets.UTF_8.toString())
                                navController.navigate("maintenance/$encoded") { popUpTo("splash") { inclusive = true } }
                            },
                            onNavigateUpdate = { config, isForce ->
                                // Show update dialog
                            }
                        )
                    }
                    composable("maintenance/{msg}") { backStackEntry ->
                        val msg = backStackEntry.arguments?.getString("msg") ?: "Under maintenance"
                        MaintenanceScreen(message = msg, onRetry = {
                            navController.navigate("splash") { popUpTo("maintenance/{msg}") { inclusive = true } }
                        })
                    }
                    composable("home") {
                        HomeScreen(repository = repository, onNavigate = { route ->
                            navController.navigate(route)
                        })
                    }
                    composable("bill_check") {
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
                    composable("direct_bill") {
                        DirectBillSearchScreen(
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
                    composable("calculator") { CalculatorScreen(repository = repository, onBack = { navController.popBackStack() }) }
                    composable("appliances") { ApplianceCalculatorScreen(repository = repository, onBack = { navController.popBackStack() }) }
                    composable("helplines") { HelplinesScreen(repository = repository, onBack = { navController.popBackStack() }) }
                    composable("codes") { PackageCodesScreen(repository = repository, onBack = { navController.popBackStack() }) }
                    composable("reminders") { RemindersScreen(repository = repository, onBack = { navController.popBackStack() }) }
                    composable("tips") { TipsScreen(repository = repository, onBack = { navController.popBackStack() }) }
                    composable("feedback") { FeedbackScreen(onBack = { navController.popBackStack() }) }
                    composable("settings") { SettingsScreen(onBack = { navController.popBackStack() }) }
                }
            }
        }
    }
}
