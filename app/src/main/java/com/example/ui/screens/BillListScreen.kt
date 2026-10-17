package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BillLink
import com.example.data.repository.ContentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillListScreen(repository: ContentRepository, onSelectBill: (String, String) -> Unit, onBack: () -> Unit) {
    val billLinks = repository.billLinks

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Electricity & Utility Bills") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(billLinks) { link ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        onSelectBill(link.url, link.nameEn)
                    },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!link.logoUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = link.logoUrl,
                                contentDescription = link.nameEn,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(link.nameEn, style = MaterialTheme.typography.titleMedium)
                            if (!link.nameUr.isNullOrEmpty()) {
                                Text(link.nameUr, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(link.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
