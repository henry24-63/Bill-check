package com.example.ui.screens

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillLink
import com.example.data.model.PortalRequirementHelper
import com.example.data.repository.ContentRepository

// Constant URL Template with {REF} placeholder
const val BILL_URL_TEMPLATE = "https://bill.pitc.com.pk/gepcobill?ref={REF}"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillCheckScreen(
    repository: ContentRepository, 
    onOpenBill: (String, String) -> Unit, 
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("bill_check_prefs", Context.MODE_PRIVATE)

    // Pull active links saved in the Admin Panel
    val adminLinks = remember(repository.billLinks) {
        if (repository.billLinks.isNotEmpty()) {
            repository.billLinks.filter { it.isActive }
        } else {
            emptyList()
        }
    }

    var selectedLink by remember(adminLinks) {
        mutableStateOf(adminLinks.firstOrNull())
    }

    val requirement = remember(selectedLink) {
        PortalRequirementHelper.getRequirement(selectedLink)
    }

    var refNumber by remember { 
        val saved = prefs.getString("last_ref", "") ?: repository.getLastRefNumber()
        mutableStateOf(saved.ifEmpty { requirement.sampleValue }) 
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Check", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Official Bill Portal Search", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "The input box adapts to the selected portal's requirement.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Area Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isDropdownExpanded,
                            onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedLink?.nameEn ?: "Select Portal / Company",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Select Portal / Company") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )

                            ExposedDropdownMenu(
                                expanded = isDropdownExpanded,
                                onDismissRequest = { isDropdownExpanded = false }
                            ) {
                                adminLinks.forEach { link ->
                                    val linkReq = PortalRequirementHelper.getRequirement(link)
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(link.nameEn, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                                Text(
                                                    "Requirement: ${linkReq.shortLabel} (${linkReq.maxLength} digits)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            val oldReq = requirement
                                            selectedLink = link
                                            isDropdownExpanded = false
                                            val newReq = PortalRequirementHelper.getRequirement(link)
                                            if (refNumber.isEmpty() || refNumber == oldReq.sampleValue) {
                                                refNumber = newReq.sampleValue
                                            }
                                            errorMessage = null
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Requirement Info Banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Requirement for ${selectedLink?.nameEn ?: "Portal"}:",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "${requirement.label} (${requirement.maxLength} Digits)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        requirement.hintLocation,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                SuggestionChip(
                                    onClick = {
                                        refNumber = requirement.sampleValue
                                        errorMessage = null
                                    },
                                    label = { Text("Use Sample ✍️", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reference Number Input
                        OutlinedTextField(
                            value = refNumber,
                            onValueChange = { 
                                refNumber = it.filter { char -> char.isDigit() }.take(requirement.maxLength)
                                errorMessage = null
                            },
                            label = { Text(requirement.label) },
                            placeholder = { Text(requirement.placeholder) },
                            trailingIcon = {
                                if (refNumber.isNotEmpty()) {
                                    IconButton(onClick = { refNumber = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            supportingText = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        if (errorMessage != null) errorMessage!! else requirement.hintLocation,
                                        color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${refNumber.length}/${requirement.maxLength}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (refNumber.length == requirement.maxLength) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            singleLine = true,
                            isError = errorMessage != null,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Button(
                            onClick = {
                                if (refNumber.length < requirement.minLength) {
                                    errorMessage = "Please enter valid ${requirement.minLength}-${requirement.maxLength} digits."
                                    return@Button
                                }
                                prefs.edit().putString("last_ref", refNumber).apply()
                                repository.saveLastRefNumber(refNumber)

                                val targetTemplate = selectedLink?.url ?: BILL_URL_TEMPLATE
                                val finalUrl = repository.resolveBillUrl(targetTemplate, refNumber)
                                val portalName = selectedLink?.nameEn ?: "Bill Viewer"

                                onOpenBill(finalUrl, portalName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (refNumber.isNotEmpty()) "View Bill ⚡ (Auto-Fill & Open)" else "Open Portal",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
