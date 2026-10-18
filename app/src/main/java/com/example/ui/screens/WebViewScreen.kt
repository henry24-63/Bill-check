package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.repository.ContentRepository
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewScreen(
    url: String, 
    title: String, 
    repository: ContentRepository, 
    autoFillRef: String = repository.currentRefNumber.ifEmpty { repository.getLastRefNumber() },
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var webViewRef: WebView? = null
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf(0) }
    var hasAutoSubmitted by remember { mutableStateOf(false) }
    var showAutoFillBadge by remember { mutableStateOf(false) }

    // Resolve and sanitize URL
    val effectiveUrl = remember(url) {
        val raw = if (url.isNotBlank()) url else repository.currentSelectedUrl
        var decoded = try {
            URLDecoder.decode(raw, StandardCharsets.UTF_8.toString())
        } catch (e: Exception) {
            raw
        }
        decoded = decoded.trim()
        if (decoded.isNotEmpty() && !decoded.startsWith("http://") && !decoded.startsWith("https://")) {
            decoded = "https://$decoded"
        }
        decoded
    }

    val displayTitle = remember(title) {
        if (title.isNotBlank()) title else repository.currentSelectedTitle.ifEmpty { "Bill Viewer" }
    }

    // Extract domain for subtle security badge
    val hostDomain = remember(effectiveUrl) {
        try {
            Uri.parse(effectiveUrl).host ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    LaunchedEffect(effectiveUrl) {
        if (effectiveUrl.isNotEmpty()) {
            repository.incrementStat("bill_check_open")
        }
    }

    BackHandler {
        if (webViewRef != null && webViewRef!!.canGoBack()) {
            webViewRef!!.goBack()
        } else {
            onBack()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                shadowElevation = 2.dp
            ) {
                Column {
                    TopAppBar(
                        windowInsets = WindowInsets.statusBars,
                        title = {
                            Column {
                                Text(
                                    text = displayTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                if (hostDomain.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 1.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Secure",
                                            modifier = Modifier.size(11.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = hostDomain,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        actions = {
                            // Quick Auto-Fill action button
                            if (autoFillRef.isNotBlank()) {
                                IconButton(onClick = {
                                    webViewRef?.let { wv ->
                                        wv.evaluateJavascript(buildAutoFillScript(autoFillRef)) { res ->
                                            Toast.makeText(context, "Auto-filled: $autoFillRef", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }) {
                                    Icon(Icons.Default.Bolt, contentDescription = "Auto-Fill Reference", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            IconButton(onClick = {
                                hasError = false
                                isLoading = true
                                hasAutoSubmitted = false
                                webViewRef?.reload()
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Reload")
                            }

                            IconButton(onClick = {
                                val currentUrl = webViewRef?.url ?: effectiveUrl
                                if (currentUrl.isNotEmpty()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("URL", currentUrl))
                                    Toast.makeText(context, "Link copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Link")
                            }

                            IconButton(onClick = {
                                val currentUrl = webViewRef?.url ?: effectiveUrl
                                if (currentUrl.isNotEmpty()) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open in browser")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Thin loading progress bar seamlessly integrated under header
                    AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(ComposeColor.White)
        ) {
            if (hasError) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("⚠️", style = MaterialTheme.typography.displayMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Page Failed to Load", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (errorMessage.isNotEmpty()) errorMessage else "Connection error. The utility portal might be busy or offline.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = {
                            hasError = false
                            isLoading = true
                            hasAutoSubmitted = false
                            webViewRef?.reload()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Retry")
                        }
                        OutlinedButton(onClick = {
                            val currentUrl = webViewRef?.url ?: effectiveUrl
                            if (currentUrl.isNotEmpty()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }) {
                            Text("Open in Browser")
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
                                javaScriptCanOpenWindowsAutomatically = true
                                setSupportMultipleWindows(false)
                                allowFileAccess = true
                                allowContentAccess = true
                                userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                            }
                            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val target = request?.url?.toString() ?: return false
                                    if (target.startsWith("http://") || target.startsWith("https://")) {
                                        return false // Keep inside WebView
                                    }
                                    return try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                        true
                                    } catch (e: Exception) {
                                        false
                                    }
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, finishUrl: String?) {
                                    super.onPageFinished(view, finishUrl)
                                    isLoading = false

                                    // Auto-fill and auto-submit reference number on portal forms
                                    val ref = autoFillRef.trim()
                                    if (ref.isNotEmpty() && !hasAutoSubmitted) {
                                        val script = buildAutoFillScript(ref)
                                        view?.evaluateJavascript(script) { res ->
                                            if (res != null && !res.contains("not_found") && !res.contains("error")) {
                                                hasAutoSubmitted = true
                                                showAutoFillBadge = true
                                            }
                                        }

                                        // Delayed retry in case DOM rendered asynchronously
                                        view?.postDelayed({
                                            if (!hasAutoSubmitted) {
                                                view.evaluateJavascript(script) { res ->
                                                    if (res != null && !res.contains("not_found") && !res.contains("error")) {
                                                        hasAutoSubmitted = true
                                                        showAutoFillBadge = true
                                                    }
                                                }
                                            }
                                        }, 700)
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    super.onReceivedError(view, request, error)
                                    if (request?.isForMainFrame == true) {
                                        hasError = true
                                        errorMessage = error?.description?.toString() ?: "Failed to connect to page."
                                        isLoading = false
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: SslError?
                                ) {
                                    // Proceed through government portal SSL chains
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

                            if (effectiveUrl.isNotEmpty()) {
                                loadUrl(effectiveUrl)
                            }
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                    }
                )

                // Floating auto-fill confirmation badge
                AnimatedVisibility(
                    visible = showAutoFillBadge,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 6.dp,
                        shadowElevation = 8.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Check, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Auto-filled Ref: $autoFillRef ⚡",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Robust JavaScript snippet to locate reference number input on utility portals,
 * populate value with native events, and trigger the search button.
 */
private fun buildAutoFillScript(referenceNumber: String): String {
    val cleanRef = referenceNumber.replace("\\", "\\\\").replace("'", "\\'").trim()
    return """
        (function() {
            try {
                var ref = '$cleanRef';
                if (!ref || ref.length < 3) return 'empty_ref';

                function fillField(el, val) {
                    el.focus();
                    el.value = val;
                    try {
                        var proto = Object.getPrototypeOf(el);
                        var setter = Object.getOwnPropertyDescriptor(proto, 'value');
                        if (setter && setter.set) {
                            setter.set.call(el, val);
                        }
                    } catch(e) {}
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                    el.dispatchEvent(new Event('blur', { bubbles: true }));
                }

                function triggerSearch(form) {
                    setTimeout(function() {
                        var btn = null;
                        var btnSelectors = [
                            'button[type="submit"]',
                            'input[type="submit"]',
                            'input[value*="search" i]',
                            'input[value*="view" i]',
                            'input[value*="submit" i]',
                            'button[id*="search" i]',
                            'button[id*="submit" i]',
                            'button[id*="view" i]',
                            'button[name*="search" i]',
                            'button[name*="submit" i]',
                            'input[name*="search" i]',
                            '.btn-search',
                            'a.btn'
                        ];

                        if (form) {
                            for (var b = 0; b < btnSelectors.length; b++) {
                                var candidate = form.querySelector(btnSelectors[b]);
                                if (candidate && candidate.offsetParent !== null) {
                                    btn = candidate;
                                    break;
                                }
                            }
                        }

                        if (!btn) {
                            for (var c = 0; c < btnSelectors.length; c++) {
                                var candidate = document.querySelector(btnSelectors[c]);
                                if (candidate && candidate.offsetParent !== null) {
                                    btn = candidate;
                                    break;
                                }
                            }
                        }

                        if (btn) {
                            btn.click();
                        } else if (form) {
                            form.submit();
                        }
                    }, 300);
                }

                // 1. Check known Pakistani DISCO / PITC reference field names and IDs
                var prioritySelectors = [
                    'input[name*="ref_no" i]',
                    'input[name*="refno" i]',
                    'input[id*="ref_no" i]',
                    'input[id*="refno" i]',
                    'input[name*="reference" i]',
                    'input[id*="reference" i]',
                    'input[name*="consumer" i]',
                    'input[id*="consumer" i]',
                    'input[name*="txtRef" i]',
                    'input[id*="txtRef" i]',
                    'input[name="ref"]',
                    'input[id="ref"]',
                    'input[name="search"]',
                    'input[id="search"]',
                    'input[name*="account" i]',
                    'input[placeholder*="reference" i]',
                    'input[placeholder*="consumer" i]',
                    'input[placeholder*="14" i]'
                ];

                var targetInput = null;
                for (var i = 0; i < prioritySelectors.length; i++) {
                    var el = document.querySelector(prioritySelectors[i]);
                    if (el && el.offsetParent !== null && !el.disabled && el.type !== 'hidden') {
                        targetInput = el;
                        break;
                    }
                }

                // 2. Segmented Inputs check (Batch, Sub-div, Ref, RU)
                var batchEl = document.querySelector('input[name*="batch" i], input[id*="batch" i]');
                var subDivEl = document.querySelector('input[name*="sub" i], input[id*="sub" i]');
                var refEl = document.querySelector('input[name*="ref" i], input[id*="ref" i]');
                var ruEl = document.querySelector('input[name*="ru" i], input[id*="ru" i]');

                if (ref.length === 14 && (batchEl || subDivEl) && refEl) {
                    if (batchEl) fillField(batchEl, ref.substring(0, 2));
                    if (subDivEl) fillField(subDivEl, ref.substring(2, 6));
                    if (refEl) fillField(refEl, ref.substring(6, 13));
                    if (ruEl) fillField(ruEl, ref.substring(13, 14));
                    triggerSearch(batchEl ? batchEl.form : (subDivEl ? subDivEl.form : null));
                    return 'segmented_filled';
                }

                // 3. Fallback to only visible text input on page
                if (!targetInput) {
                    var visibleInputs = Array.prototype.slice.call(
                        document.querySelectorAll('input[type="text"], input[type="number"], input[type="tel"], input:not([type])')
                    ).filter(function(el) {
                        return el.offsetParent !== null && !el.disabled && el.type !== 'hidden' && el.name !== 'captcha';
                    });
                    if (visibleInputs.length === 1) {
                        targetInput = visibleInputs[0];
                    }
                }

                if (targetInput) {
                    fillField(targetInput, ref);
                    triggerSearch(targetInput.form);
                    return 'single_filled';
                }

                return 'not_found';
            } catch(e) {
                return 'error: ' + e.message;
            }
        })();
    """.trimIndent()
}
