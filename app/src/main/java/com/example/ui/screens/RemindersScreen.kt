package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.model.ReminderModel
import com.example.services.ReminderManager
import com.example.data.repository.ContentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(repository: ContentRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val reminderManager = remember { ReminderManager(context) }
    var reminders by remember { mutableStateOf(reminderManager.getReminders()) }
    var showAddDialog by remember { mutableStateOf(false) }

    var titleInput by remember { mutableStateOf("") }
    var dayInput by remember { mutableStateOf("10") }
    var hourInput by remember { mutableStateOf("9") }

    LaunchedEffect(Unit) {
        repository.incrementStat("reminder_created")
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill & Utility Reminders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Reminder")
            }
        }
    ) { padding ->
        if (reminders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No reminders set yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reminders) { reminder ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                                Text("Day ${reminder.dayOfMonth} of month at ${reminder.hour}:00", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = reminder.enabled,
                                onCheckedChange = { enabled ->
                                    reminderManager.toggleReminder(reminder.id, enabled)
                                    reminders = reminderManager.getReminders()
                                }
                            )
                            IconButton(onClick = {
                                reminderManager.deleteReminder(reminder.id)
                                reminders = reminderManager.getReminders()
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Reminder") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = titleInput,
                            onValueChange = { titleInput = it },
                            label = { Text("Reminder Title (e.g., LESCO Bill Due)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = dayInput,
                            onValueChange = { dayInput = it },
                            label = { Text("Day of Month (1-28)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = hourInput,
                            onValueChange = { hourInput = it },
                            label = { Text("Hour (0-23)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val day = dayInput.toIntOrNull() ?: 10
                        val hr = hourInput.toIntOrNull() ?: 9
                        if (titleInput.isNotBlank()) {
                            val newReminder = ReminderModel(
                                title = titleInput,
                                type = "bill",
                                dayOfMonth = day.coerceIn(1, 28),
                                hour = hr.coerceIn(0, 23),
                                minute = 0
                            )
                            reminderManager.addReminder(newReminder)
                            reminders = reminderManager.getReminders()
                            titleInput = ""
                            showAddDialog = false
                        }
                    }) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
