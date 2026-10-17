package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BillLink
import com.example.data.repository.ContentRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillListScreen(
    repository: ContentRepository, 
    onSelectBill: (String, String) -> Unit, 
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var links by remember { mutableStateOf(repository.billLinks) }
    var searchQuery by remember { mutableStateOf("") }
    var quickRef by remember { mutableStateOf(repository.getLastRefNumber()) }

    fun refreshLinks() {
        scope.launch {
            isLoading = true
            links = repository.fetchBillLinks()
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshLinks()
    }

    val filteredLinks = remember(links, searchQuery) {
        if (searchQuery.isBlank()) {
            links
        } else {
            links.filter {
                it.nameEn.contains(searchQuery, ignoreCase = true) ||
                (it.nameUr ?: "").contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.url.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Electricity & Utility Bills") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { refreshLinks() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh links")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    // Quick Reference & Search Bar
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Links Configured from Admin Panel", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = quickRef,
                                onValueChange = { 
                                    quickRef = it.filter { ch -> ch.isDigit() }
                                    repository.saveLastRefNumber(quickRef)
                                },
                                label = { Text("Reference Number (Optional)") },
                                placeholder = { Text("Enter 14-digit ref if available") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Filter by board name (e.g. GEPCO, LESCO...)") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

                if (filteredLinks.isEmpty() && !isLoading) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("⚡", style = MaterialTheme.typography.displayMedium)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("No Bill Links Found", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Add or enable bill links in the Admin Panel under 'Bill Links'.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = { refreshLinks() }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Check Again")
                            }
                        }
                    }
                } else {
                    items(filteredLinks) { link ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val finalUrl = if (quickRef.isNotBlank()) {
                                        repository.resolveBillUrl(link.url, quickRef)
                                    } else {
                                        link.url
                                    }
                                    repository.currentSelectedUrl = finalUrl
                                    repository.currentSelectedTitle = link.nameEn
                                    onSelectBill(finalUrl, link.nameEn)
                                },
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!link.logoUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = link.logoUrl,
                                        contentDescription = link.nameEn,
                                        modifier = Modifier.size(44.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.ElectricBolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(link.nameEn, style = MaterialTheme.typography.titleMedium)
                                    if (!link.nameUr.isNullOrEmpty()) {
                                        Text(
                                            link.nameUr,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        link.url,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val finalUrl = if (quickRef.isNotBlank()) {
                                            repository.resolveBillUrl(link.url, quickRef)
                                        } else {
                                            link.url
                                        }
                                        repository.currentSelectedUrl = finalUrl
                                        repository.currentSelectedTitle = link.nameEn
                                        onSelectBill(finalUrl, link.nameEn)
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Open")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            if (isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
            }
        }
    }
}
