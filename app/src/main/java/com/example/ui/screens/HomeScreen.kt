package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.example.data.ads.AdActionType
import com.example.data.ads.SmartAdManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillLink
import com.example.data.model.PortalRequirementHelper
import com.example.data.model.RecentSearch
import com.example.data.remote.SupabaseApi
import com.example.data.repository.ContentRepository
import com.squareup.moshi.Types

data class ServiceItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repository: ContentRepository, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("recent_bills_prefs", Context.MODE_PRIVATE)
    val moshi = SupabaseApi.moshi
    val listType = Types.newParameterizedType(List::class.java, RecentSearch::class.java)
    val recentAdapter = moshi.adapter<List<RecentSearch>>(listType)

    var showBillPopup by remember { mutableStateOf(false) }
    var selectedLink by remember(repository.billLinks) {
        mutableStateOf(repository.billLinks.firstOrNull())
    }

    // Portal-specific dynamic requirement (label, placeholder, max-length, hint)
    val requirement = remember(selectedLink) {
        PortalRequirementHelper.getRequirement(selectedLink)
    }

    var quickRefNumber by remember { 
        val saved = repository.getLastRefNumber()
        mutableStateOf(saved.ifEmpty { requirement.sampleValue }) 
    }
    var isAreaDropdownExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load recent searches from local storage
    var recentSearches by remember {
        mutableStateOf(
            try {
                val json = prefs.getString("history", "[]") ?: "[]"
                recentAdapter.fromJson(json) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        )
    }

    fun saveRecentSearch(discoName: String, ref: String, targetUrl: String) {
        try {
            val current = recentSearches.toMutableList()
            current.removeAll { it.refNumber == ref }
            current.add(0, RecentSearch(discoName, ref, targetUrl))
            if (current.size > 6) current.removeAt(current.size - 1)
            recentSearches = current
            prefs.edit().putString("history", recentAdapter.toJson(current)).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Clean, distinct non-duplicate service cards
    val services = listOf(
        ServiceItem(
            title = "Check Bill",
            subtitle = "Auto-fill reference & search",
            icon = Icons.Default.Bolt,
            onClick = { showBillPopup = true }
        ),
        ServiceItem(
            title = "All DISCO Portals",
            subtitle = "GEPCO, LESCO, FESCO...",
            icon = Icons.Default.Language,
            onClick = { onNavigate("bills") }
        ),
        ServiceItem(
            title = "Bill Calculator",
            subtitle = "Units to Rs calculation",
            icon = Icons.Default.Calculate,
            onClick = { onNavigate("calculator") }
        ),
        ServiceItem(
            title = "Appliance Calc",
            subtitle = "Watts & monthly usage",
            icon = Icons.Default.ElectricMeter,
            onClick = { onNavigate("appliances") }
        ),
        ServiceItem(
            title = "Helplines",
            subtitle = "Emergency & DISCOs",
            icon = Icons.Default.Phone,
            onClick = { onNavigate("helplines") }
        ),
        ServiceItem(
            title = "Package Codes",
            subtitle = "Jazz, Zong, Telenor, Ufone",
            icon = Icons.Default.PhoneAndroid,
            onClick = { onNavigate("codes") }
        ),
        ServiceItem(
            title = "Reminders",
            subtitle = "Due date alerts & tracker",
            icon = Icons.Default.Alarm,
            onClick = { onNavigate("reminders") }
        ),
        ServiceItem(
            title = "Energy Tips",
            subtitle = "Save electricity & money",
            icon = Icons.Default.Lightbulb,
            onClick = { onNavigate("tips") }
        ),
        ServiceItem(
            title = "Feedback",
            subtitle = "Help, contact & suggestions",
            icon = Icons.Default.Feedback,
            onClick = { onNavigate("feedback") }
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(
                            "Bijli Bill & Utility Checker", 
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Pakistan Electricity & Utility Portals", 
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live announcement banner from admin
            val announcement = repository.announcements.firstOrNull()
            if (announcement != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(announcement.titleEn, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(announcement.bodyEn, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            // PRIMARY HERO BANNER: Clean, uncluttered card that opens popup on tap
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hero_bill_check_card")
                        .clickable { showBillPopup = true },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(46.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Bolt, 
                                        contentDescription = null, 
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Check Electricity Bill", 
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Auto-fill reference & search instant portal", 
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick access button to open popup
                        Button(
                            onClick = { showBillPopup = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_bill_popup_button"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "⚡ Check Bill Now (Pop-up)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Saved reference shortcut indicator if available
                        val lastRef = repository.getLastRefNumber()
                        if (lastRef.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.History, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Saved Ref: $lastRef",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // DISTINCT SERVICES GRID (No duplicate options)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Services & Utilities", 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigate("bills") }) {
                        Text("View All Portals")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))

                val chunkedServices = services.chunked(2)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    chunkedServices.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { item ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { 
                                            val activity = context as? Activity
                                            if (activity != null) {
                                                SmartAdManager.triggerSmartAd(activity, repository, AdActionType.BUTTON_CLICK) {
                                                    item.onClick()
                                                }
                                            } else {
                                                item.onClick()
                                            }
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            modifier = Modifier.size(40.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    item.icon, 
                                                    contentDescription = item.title, 
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                item.title, 
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                item.subtitle, 
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowItems.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // INTERACTIVE BILL CHECK POP-UP (ModalBottomSheet)
    if (showBillPopup) {
        ModalBottomSheet(
            onDismissRequest = { showBillPopup = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .navigationBarsPadding()
            ) {
                // Header of the Pop-up
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "⚡ Check Bill Portal",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Adapts to ${selectedLink?.nameEn ?: "Selected Board"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = { showBillPopup = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Select Electricity / Utility Board Dropdown
                ExposedDropdownMenuBox(
                    expanded = isAreaDropdownExpanded,
                    onExpandedChange = { isAreaDropdownExpanded = !isAreaDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedLink?.nameEn ?: "Select Utility Board / Portal",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Select Portal / Company") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAreaDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = isAreaDropdownExpanded,
                        onDismissRequest = { isAreaDropdownExpanded = false }
                    ) {
                        repository.billLinks.forEach { link ->
                            val linkReq = PortalRequirementHelper.getRequirement(link)
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                link.nameEn,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                "Req: ${linkReq.shortLabel} (${linkReq.maxLength} digits)",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (selectedLink?.id == link.id) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    val oldReq = requirement
                                    selectedLink = link
                                    isAreaDropdownExpanded = false
                                    val newReq = PortalRequirementHelper.getRequirement(link)
                                    // When user selects a new portal, update box with that portal's requirement format
                                    if (quickRefNumber.isEmpty() || quickRefNumber == oldReq.sampleValue) {
                                        quickRefNumber = newReq.sampleValue
                                        repository.saveLastRefNumber(newReq.sampleValue)
                                    }
                                    errorMessage = null
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PORTAL REQUIREMENT GUIDE BADGE
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
                                Icon(
                                    Icons.Default.Info, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Format Requirement:",
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

                        // One-tap chip to type/fill the sample format
                        SuggestionChip(
                            onClick = {
                                quickRefNumber = requirement.sampleValue
                                repository.saveLastRefNumber(quickRefNumber)
                                errorMessage = null
                            },
                            label = { 
                                Text("Use Sample ✍️", style = MaterialTheme.typography.labelSmall) 
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. DYNAMIC INPUT BOX (Typed / Formatted based on Portal Requirement)
                OutlinedTextField(
                    value = quickRefNumber,
                    onValueChange = { input ->
                        quickRefNumber = input.filter { it.isDigit() }.take(requirement.maxLength)
                        repository.saveLastRefNumber(quickRefNumber)
                        errorMessage = null
                    },
                    label = { Text(requirement.label) },
                    placeholder = { Text(requirement.placeholder) },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (quickRefNumber.isNotEmpty()) {
                                IconButton(onClick = {
                                    quickRefNumber = ""
                                    repository.saveLastRefNumber("")
                                }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = {
                                try {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                    val pastedDigits = clip.filter { it.isDigit() }.take(requirement.maxLength)
                                    if (pastedDigits.isNotEmpty()) {
                                        quickRefNumber = pastedDigits
                                        repository.saveLastRefNumber(pastedDigits)
                                        errorMessage = null
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
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
                                "${quickRefNumber.length}/${requirement.maxLength}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = if (quickRefNumber.length == requirement.maxLength) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    singleLine = true,
                    isError = errorMessage != null,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Recent searches chips inside popup
                if (recentSearches.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Recent Searches:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(recentSearches) { recent ->
                            SuggestionChip(
                                onClick = {
                                    quickRefNumber = recent.refNumber
                                    repository.saveLastRefNumber(recent.refNumber)
                                    val matched = repository.billLinks.find { it.nameEn == recent.disco }
                                    if (matched != null) selectedLink = matched
                                    errorMessage = null
                                },
                                label = { Text("${recent.disco.split(" ")[0]}: ${recent.refNumber}") }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Primary Submit Button: Auto-fill & Open
                Button(
                    onClick = {
                        val targetLink = selectedLink ?: repository.billLinks.firstOrNull()
                        if (targetLink == null) {
                            errorMessage = "Please select a portal or electricity board."
                            return@Button
                        }
                        val cleanRef = quickRefNumber.trim()
                        if (cleanRef.isNotEmpty() && cleanRef.length < requirement.minLength) {
                            errorMessage = "Enter valid ${requirement.minLength}-${requirement.maxLength} digits for ${targetLink.nameEn}."
                            return@Button
                        }

                        val targetUrl = repository.resolveBillUrl(targetLink.url, cleanRef)
                        repository.saveLastRefNumber(cleanRef)
                        if (cleanRef.isNotEmpty()) {
                            saveRecentSearch(targetLink.nameEn, cleanRef, targetUrl)
                        }

                        val proceedToWebview = {
                            repository.currentSelectedUrl = targetUrl
                            repository.currentSelectedTitle = targetLink.nameEn
                            showBillPopup = false
                            onNavigate("webview")
                        }

                        val activity = context as? Activity
                        if (activity != null) {
                            SmartAdManager.triggerSmartAd(activity, repository, AdActionType.BILL_SEARCH) {
                                proceedToWebview()
                            }
                        } else {
                            proceedToWebview()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 13.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (quickRefNumber.isNotEmpty()) "Search Bill ⚡ (Auto-Fill & Open)" else "Open Official Portal 🌐",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Button: Open Portal Directly
                OutlinedButton(
                    onClick = {
                        val targetLink = selectedLink ?: repository.billLinks.firstOrNull()
                        if (targetLink != null) {
                            repository.currentSelectedUrl = targetLink.url
                            repository.currentSelectedTitle = targetLink.nameEn
                            showBillPopup = false
                            onNavigate("webview")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Official Website Directly")
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
