package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ListItem(
                    headlineContent = { Text("Privacy Policy") },
                    leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bijlibillchecker.pk/privacy.html"))
                        context.startActivity(intent)
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("Share App") },
                    leadingContent = { Icon(Icons.Default.Share, contentDescription = null) },
                    modifier = Modifier.clickable {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Download Bijli Bill & Utility Checker: https://bijlibillchecker.pk")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share App"))
                    }
                )
            }
            item {
                ListItem(
                    headlineContent = { Text("App Version") },
                    supportingContent = { Text("1.0.0 (Build 1)") },
                    leadingContent = { Icon(Icons.Default.Info, contentDescription = null) }
                )
            }
        }
    }
}
