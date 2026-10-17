package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.BillLink
import com.example.data.repository.ContentRepository
import kotlinx.coroutines.launch

// Step 3: Constant URL Template with {REF} placeholder
const val BILL_URL_TEMPLATE = "https://bill.pitc.com.pk/gepcobill?ref={REF}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillCheckScreen(repository: ContentRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = context.getSharedPreferences("bill_check_prefs", Context.MODE_PRIVATE)

    // Pull active links saved in the Admin Panel
    val adminLinks = remember(repository.billLinks) {
        if (repository.billLinks.isNotEmpty()) {
            repository.billLinks.filter { it.isActive }
        } else {
            emptyList()
        }
    }

    // Default to the first admin link if available, otherwise template
    var selectedLink by remember(adminLinks) {
        mutableStateOf(adminLinks.firstOrNull())
    }

    var refNumber by remember { mutableStateOf(prefs.getString("last_ref", "") ?: repository.getLastRefNumber()) }
    var finalUrl by remember { mutableStateOf<String?>(null) }
    var activeTitle by remember { mutableStateOf("Bill Check") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BackHandler {
        if (finalUrl != null) {
            finalUrl = null // Return to input view
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(if (finalUrl != null) activeTitle else "Bill Check", style = MaterialTheme.typography.titleMedium)
                        if (finalUrl != null) {
                            val host = try { Uri.parse(finalUrl).host ?: "" } catch (e: Exception) { "" }
                            if (host.isNotEmpty()) {
                                Text("🔒 $host", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (finalUrl != null) {
                            finalUrl = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (finalUrl != null) {
                        IconButton(onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(finalUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }) {
                            Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (finalUrl == null) {
            // Step 2: Input Screen
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Enter Reference Number", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Please enter your 14-digit electricity reference number",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // If admin has configured multiple links in the Admin Panel, show selection
                if (adminLinks.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    "Selected Portal (Configured in Admin):", 
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    selectedLink?.nameEn ?: "Default Bill Portal",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    selectedLink?.url ?: BILL_URL_TEMPLATE,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = refNumber,
                        onValueChange = { 
                            refNumber = it.filter { char -> char.isDigit() }.take(14)
                            errorMessage = null
                        },
                        label = { Text("14-Digit Reference Number") },
                        placeholder = { Text("e.g. 12345678901234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            errorMessage!!, 
                            color = MaterialTheme.colorScheme.error, 
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (refNumber.length != 14) {
                                errorMessage = "Reference number must be exactly 14 digits."
                                return@Button
                            }
                            // Save last used reference number locally
                            prefs.edit().putString("last_ref", refNumber).apply()
                            repository.saveLastRefNumber(refNumber)

                            // Resolve URL: priority to admin link URL if present, otherwise BILL_URL_TEMPLATE
                            val templateToUse = selectedLink?.url ?: BILL_URL_TEMPLATE
                            finalUrl = repository.resolveBillUrl(templateToUse, refNumber)
                            activeTitle = selectedLink?.nameEn ?: "Bill Check"

                            scope.launch {
                                repository.incrementStat("bill_check_open")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ElectricBolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("View Bill")
                    }

                    // Direct open without reference number (exact page from admin panel)
                    val directUrl = selectedLink?.url
                    if (!directUrl.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                finalUrl = directUrl
                                activeTitle = selectedLink?.nameEn ?: "Bill Portal"
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Open Official Website Directly")
                        }
                    }
                }

                // If there are other admin links, list them below for 1-tap switching
                if (adminLinks.size > 1) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All Available Utility Links (Admin)", style = MaterialTheme.typography.titleSmall)
                    }
                    items(adminLinks) { link ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedLink = link
                                    val template = link.url
                                    if (refNumber.length == 14) {
                                        finalUrl = repository.resolveBillUrl(template, refNumber)
                                        activeTitle = link.nameEn
                                    } else {
                                        // Open direct URL configured by admin
                                        finalUrl = template
                                        activeTitle = link.nameEn
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(link.nameEn, style = MaterialTheme.typography.bodyLarge)
                                    Text(link.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Open ⚡", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        } else {
            // Step 4 & 6: Fullscreen WebView with app top bar and back button
            BillCheckWebViewContainer(url = finalUrl!!) {
                finalUrl = null
            }
        }
    }
}

@Composable
fun BillCheckWebViewContainer(url: String, onBackToInput: () -> Unit) {
    val context = LocalContext.current
    var webViewRef: WebView? = null
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorDetails by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0) }

    BackHandler {
        if (webViewRef != null && webViewRef!!.canGoBack()) {
            webViewRef!!.goBack()
        } else {
            onBackToInput()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(ComposeColor.White)) {
        if (hasError) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚠️", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Failed to Load Bill", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    errorDetails, 
                    style = MaterialTheme.typography.bodyMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        hasError = false
                        isLoading = true
                        webViewRef?.reload()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                    OutlinedButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }) {
                        Text("Open in browser")
                    }
                }
            }
        } else {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(AndroidColor.WHITE)

                        // Cookie Management
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, pageUrl, favicon)
                                isLoading = true
                                android.util.Log.d("BillCheck", "Page started: $pageUrl")
                            }

                            override fun onPageFinished(view: WebView?, pageUrl: String?) {
                                super.onPageFinished(view, pageUrl)
                                isLoading = false
                                android.util.Log.d("BillCheck", "Page finished: $pageUrl")
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    val errorCode = error?.errorCode ?: 0
                                    val description = error?.description?.toString() ?: "Unknown error"
                                    errorDetails = "Error Code: $errorCode\nDescription: $description"
                                    android.util.Log.e("BillCheck", "Load error: Code $errorCode - $description")
                                    isLoading = false
                                }
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                handler?.proceed()
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                super.onProgressChanged(view, newProgress)
                                progress = newProgress
                                if (newProgress >= 80) {
                                    isLoading = false
                                }
                            }
                        }

                        loadUrl(url)
                        webViewRef = this
                    }
                },
                update = { webView ->
                    webViewRef = webView
                }
            )

            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}
