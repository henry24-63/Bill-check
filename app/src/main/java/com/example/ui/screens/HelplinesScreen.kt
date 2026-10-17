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
fun HelplinesScreen(repository: ContentRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val helplines = repository.helplines
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        repository.incrementStat("helpline_call")
    }

    BackHandler { onBack() }

    val filtered = helplines.filter {
        it.orgNameEn.contains(searchQuery, true) || it.category.contains(searchQuery, true) || it.number.contains(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Helplines Directory") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search helplines...") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { helpline ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${helpline.number}"))
                            context.startActivity(intent)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(helpline.orgNameEn, style = MaterialTheme.typography.titleMedium)
                                if (!helpline.orgNameUr.isNullOrEmpty()) {
                                    Text(helpline.orgNameUr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(helpline.number, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${helpline.number}"))
                                context.startActivity(intent)
                            }) {
                                Icon(Icons.Default.Phone, contentDescription = "Call", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
