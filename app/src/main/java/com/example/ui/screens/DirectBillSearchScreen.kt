package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.BillLink
import com.example.data.remote.SupabaseApi
import com.example.data.repository.ContentRepository
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Types

@JsonClass(generateAdapter = true)
data class RecentSearch(
    val disco: String = "", 
    val refNumber: String = "", 
    val targetUrl: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DirectBillSearchScreen(
    repository: ContentRepository, 
    onOpenBill: (String, String) -> Unit, 
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("recent_bills_prefs", Context.MODE_PRIVATE)
    
    // Use Moshi with KotlinJsonAdapterFactory from SupabaseApi to prevent reflective serialization crashes
    val moshi = SupabaseApi.moshi
    val listType = Types.newParameterizedType(List::class.java, RecentSearch::class.java)
    val adapter = moshi.adapter<List<RecentSearch>>(listType)

    // Pull active links directly from Admin Panel / ContentRepository
    val availableLinks = remember(repository.billLinks) {
        if (repository.billLinks.isNotEmpty()) {
            repository.billLinks.filter { it.isActive }
        } else {
            listOf(
                BillLink(1, "GEPCO (Gujranwala)", "گیپکو گوجرانوالہ", "Electricity", "https://bill.pitc.com.pk/gepcobill", null, 1, true),
                BillLink(2, "LESCO (Lahore)", "لیسکو لاہور", "Electricity", "https://bill.pitc.com.pk/lescobill", null, 2, true),
                BillLink(3, "FESCO (Faisalabad)", "فیسکو فیصل آباد", "Electricity", "https://bill.pitc.com.pk/fescobill", null, 3, true),
                BillLink(4, "IESCO (Islamabad)", "آئیسکو اسلام آباد", "Electricity", "https://bill.pitc.com.pk/iescobill", null, 4, true),
                BillLink(5, "MEPCO (Multan)", "میپکو ملتان", "Electricity", "https://bill.pitc.com.pk/mepcobill", null, 5, true),
                BillLink(6, "PESCO (Peshawar)", "پیسکو پشاور", "Electricity", "https://bill.pitc.com.pk/pescobill", null, 6, true),
                BillLink(7, "HESCO (Hyderabad)", "حیسکو حیدرآباد", "Electricity", "https://bill.pitc.com.pk/hescobill", null, 7, true),
                BillLink(8, "QESCO (Quetta)", "کیسکو کوئٹہ", "Electricity", "https://bill.pitc.com.pk/qescobill", null, 8, true),
                BillLink(9, "SEPCO (Sukkur)", "سیپکو سکھر", "Electricity", "https://bill.pitc.com.pk/sepcobill", null, 9, true),
                BillLink(10, "TESCO (Tribal)", "ٹیسکو قبائلی علاقہ", "Electricity", "https://bill.pitc.com.pk/tescobill", null, 10, true)
            )
        }
    }

    var selectedLink by remember { mutableStateOf(availableLinks.firstOrNull() ?: BillLink(1, "Electricity Portal", null, "Electricity", "https://bill.pitc.com.pk/gepcobill", null, 1, true)) }
    var refNumber by remember { mutableStateOf(repository.getLastRefNumber()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var recentSearches by remember {
        mutableStateOf(
            try {
                val json = prefs.getString("history", "[]") ?: "[]"
                adapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    BackHandler { onBack() }

    fun saveRecent(name: String, ref: String, finalUrl: String) {
        try {
            val current = recentSearches.toMutableList()
            current.removeAll { it.refNumber == ref && it.disco == name }
            current.add(0, RecentSearch(name, ref, finalUrl))
            if (current.size > 10) current.removeAt(current.size - 1)
            recentSearches = current
            prefs.edit().putString("history", adapter.toJson(current)).apply()
            repository.saveLastRefNumber(ref)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Instant Bill Search") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Select Board / Link (Admin Configured)", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))

                        availableLinks.forEach { link ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedLink = link }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedLink.id == link.id && selectedLink.url == link.url,
                                    onClick = { selectedLink = link }
                                )
                                Column(modifier = Modifier.padding(start = 4.dp)) {
                                    Text(link.nameEn, style = MaterialTheme.typography.bodyLarge)
                                    if (!link.nameUr.isNullOrEmpty()) {
                                        Text(link.nameUr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = refNumber,
                            onValueChange = { 
                                refNumber = it.filter { ch -> ch.isDigit() }
                                errorMessage = null 
                            },
                            label = { Text("Reference Number / Consumer ID (Optional)") },
                            placeholder = { Text("e.g., 14-digit reference number") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (errorMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val cleanRef = refNumber.trim()
                                val finalUrl = repository.resolveBillUrl(selectedLink.url, cleanRef)
                                if (cleanRef.isNotEmpty()) {
                                    saveRecent(selectedLink.nameEn, cleanRef, finalUrl)
                                }
                                onOpenBill(finalUrl, selectedLink.nameEn)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (refNumber.isNotBlank()) "Search Bill Instantly" else "Open Official Portal")
                        }
                    }
                }
            }

            if (recentSearches.isNotEmpty()) {
                item {
                    Text("Recent Searches (Saved Locally)", style = MaterialTheme.typography.titleMedium)
                }
                items(recentSearches) { recent ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val finalUrl = if (recent.targetUrl.isNotEmpty()) {
                                    recent.targetUrl
                                } else {
                                    val matched = availableLinks.find { it.nameEn == recent.disco } ?: availableLinks[0]
                                    repository.resolveBillUrl(matched.url, recent.refNumber)
                                }
                                onOpenBill(finalUrl, recent.disco)
                            }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(recent.disco, style = MaterialTheme.typography.titleSmall)
                                    Text("Ref: ${recent.refNumber}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("Open ⚡", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
