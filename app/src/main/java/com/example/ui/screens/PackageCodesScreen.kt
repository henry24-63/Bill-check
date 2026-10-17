package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.repository.ContentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PackageCodesScreen(repository: ContentRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val codes = repository.packageCodes
    var selectedTelco by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        repository.incrementStat("code_dial")
    }

    BackHandler { onBack() }

    val telcos = listOf("All") + codes.map { it.telco }.distinct()
    val filtered = codes.filter { selectedTelco == "All" || it.telco == selectedTelco }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("USSD Package Codes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = telcos.indexOf(selectedTelco), modifier = Modifier.fillMaxWidth()) {
                telcos.forEach { telco ->
                    Tab(
                        selected = selectedTelco == telco,
                        onClick = { selectedTelco = telco },
                        text = { Text(telco) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { code ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val encodedUssd = code.ussdCode.replace("#", "%23")
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encodedUssd"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(code.titleEn, style = MaterialTheme.typography.titleMedium)
                                Text(code.ussdCode, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                                if (!code.description.isNullOrEmpty()) {
                                    Text(code.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = {
                                val encodedUssd = code.ussdCode.replace("#", "%23")
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$encodedUssd"))
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Phone, contentDescription = "Dial", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
