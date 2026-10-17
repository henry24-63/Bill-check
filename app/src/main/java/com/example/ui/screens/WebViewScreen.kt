package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.repository.ContentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(url: String, title: String, repository: ContentRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    var webViewRef: WebView? = null
    var canGoBack by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.incrementStat("bill_check_open")
    }

    BackHandler {
        if (webViewRef != null && webViewRef!!.canGoBack()) {
            webViewRef!!.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Disclaimer strip
            Box(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Disclaimer: Estimate only. Actual bill may differ. Not affiliated with any government body or DISCO.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                canGoBack = view?.canGoBack() == true
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
        }
    }
}
