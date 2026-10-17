package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.data.repository.ContentRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(repository: ContentRepository, onBack: () -> Unit) {
    val context = LocalContext.current
    val tariffs = repository.tariffSlabs.map { it.tariffName }.distinct()
    var selectedTariff by remember { mutableStateOf(tariffs.firstOrNull() ?: "") }
    var unitsStr by remember { mutableStateOf("150") }

    var calculationResult by remember { mutableStateOf<CalculationResult?>(null) }

    LaunchedEffect(Unit) {
        repository.incrementStat("calculator_used")
    }

    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Electricity Bill Calculator") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (calculationResult != null) {
                        IconButton(onClick = {
                            val res = calculationResult!!
                            val shareText = "Bill Estimate for ${res.tariffName} (${res.units} units):\nTotal: Rs. ${res.total}\n(Estimate only - Bijli Bill Checker)"
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Share Bill Estimate"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Tariff", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        tariffs.forEach { tariff ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { selectedTariff = tariff },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = selectedTariff == tariff, onClick = { selectedTariff = tariff })
                                Text(tariff, style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = unitsStr,
                            onValueChange = { unitsStr = it },
                            label = { Text("Units Consumed") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val units = unitsStr.toIntOrNull() ?: 0
                                val slabs = repository.tariffSlabs.filter { it.tariffName == selectedTariff }
                                val charges = repository.tariffCharges.find { it.tariffName == selectedTariff }

                                calculationResult = computeBill(selectedTariff, units, slabs, charges)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Calculate Bill")
                        }
                    }
                }
            }

            if (calculationResult != null) {
                val res = calculationResult!!
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Calculation Breakdown", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Tariff: ${res.tariffName}")
                            Text("Units: ${res.units}")
                            Text("Energy Charge: Rs. ${res.energy}")
                            Text("Fixed Charge: Rs. ${res.fixedCharge}")
                            Text("Taxes (GST + Other): Rs. ${res.tax}")
                            Text("Other Flat: Rs. ${res.otherFlat}")
                            Divider(modifier = Modifier.padding(vertical = 12.dp))
                            Text("Total Estimated Bill: Rs. ${res.total}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Disclaimer: Estimate only. Actual bill may differ. Not affiliated with any government body or DISCO.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

data class CalculationResult(
    val tariffName: String,
    val units: Int,
    val energy: Double,
    val fixedCharge: Double,
    val tax: Double,
    val otherFlat: Double,
    val total: Double,
    val mode: String
)

fun computeBill(tariffName: String, units: Int, slabs: List<com.example.data.model.TariffSlab>, charges: com.example.data.model.TariffCharge?): CalculationResult {
    var energy = 0.0
    if (slabs.isEmpty()) return CalculationResult(tariffName, units, 0.0, 0.0, 0.0, 0.0, 0.0, "none")

    val calcMode = slabs[0].calcMode

    if (calcMode == "whole_rate") {
        val matching = slabs.find { units in it.fromUnit..it.toUnit } ?: slabs.last()
        energy = units * matching.ratePerUnit
    } else {
        val sorted = slabs.sortedBy { it.fromUnit }
        for (slab in sorted) {
            if (units < slab.fromUnit) continue
            val upper = if (slab.toUnit == 0) 999999 else slab.toUnit
            val unitsInSlab = maxOf(0, minOf(units, upper) - slab.fromUnit + 1)
            energy += unitsInSlab * slab.ratePerUnit
        }
    }

    val fixedCharge = charges?.fixedCharge ?: 0.0
    val gstPercent = charges?.gstPercent ?: 0.0
    val otherPercent = charges?.otherPercent ?: 0.0
    val otherFlat = charges?.otherFlat ?: 0.0

    val subtotal = energy + fixedCharge
    val tax = subtotal * ((gstPercent + otherPercent) / 100.0)
    val total = subtotal + tax + otherFlat

    return CalculationResult(
        tariffName = tariffName,
        units = units,
        energy = Math.round(energy * 100.0) / 100.0,
        fixedCharge = Math.round(fixedCharge * 100.0) / 100.0,
        tax = Math.round(tax * 100.0) / 100.0,
        otherFlat = Math.round(otherFlat * 100.0) / 100.0,
        total = Math.round(total * 100.0) / 100.0,
        mode = calcMode
    )
}
