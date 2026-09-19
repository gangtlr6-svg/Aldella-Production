package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ProcessSheetEntity
import com.example.ui.AldellaViewModel
import com.example.ui.components.AldellaHeader
import com.example.ui.components.BatchFullContentDialog
import com.example.ui.theme.*

@Composable
fun ProcessUpdatesScreen(
    areaName: String,
    processName: String,
    viewModel: AldellaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allProcessSheets by viewModel.allProcessSheets.collectAsState()
    val sheets = allProcessSheets.filter { it.areaKey == areaName && it.processKey == processName }
    var selectedBatchForFullView by remember { mutableStateOf<ProcessSheetEntity?>(null) }

    selectedBatchForFullView?.let { sheet ->
        BatchFullContentDialog(
            sheet = sheet,
            batchOrdinal = viewModel.getBatchOrdinal(sheet.batchSequence),
            isAdmin = currentUser.role == "ADMIN",
            onDelete = if (currentUser.role == "ADMIN") {
                {
                    viewModel.deleteProcessSheet(sheet)
                    Toast.makeText(context, "Batch #${sheet.id} deleted by Admin", Toast.LENGTH_SHORT).show()
                    selectedBatchForFullView = null
                }
            } else null,
            onDismiss = { selectedBatchForFullView = null }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AldellaNavyDark)
    ) {
        AldellaHeader(
            viewModel = viewModel,
            title = "$processName Updates",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Protected Updates Log",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "$areaName • ${sheets.size} recorded batch${if (sheets.size != 1) "es" else ""}",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }

                Button(
                    onClick = { viewModel.exportCsvReport(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export", fontSize = 12.sp)
                }
            }

            if (sheets.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No saved updates for $processName yet.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AldellaTextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Fill in the Live Process Sheet and tap 'Update / Save' to preserve batches here.",
                            fontSize = 12.sp,
                            color = AldellaTextMuted
                        )
                    }
                }
            } else {
                sheets.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_card_${item.batchNo.replace(" ", "_")}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${item.batchNo} • ${item.productName}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AldellaTextDark
                                    )
                                    Text(
                                        text = "Code: ${item.productCode} • Date: ${item.date}",
                                        fontSize = 12.sp,
                                        color = AldellaTextMuted
                                    )
                                }

                                Surface(
                                    color = if (item.status == "RUNNING") AldellaGreenLight else AldellaBlueLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = item.status,
                                        color = if (item.status == "RUNNING") AldellaGreen else AldellaBlueBadgeText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Total Qty: ${item.totalKgQty} kg",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = AldellaTextDark
                                )
                                Text(
                                    text = "Hours: ${item.totalHours}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AldellaBluePrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Waste: ${item.productWasteKg} kg (${item.waterWasteLitres}L water)",
                                    fontSize = 11.sp,
                                    color = AldellaTextMuted
                                )
                                Text(
                                    text = "Temp: ${item.temperatureC} °C",
                                    fontSize = 11.sp,
                                    color = AldellaTextMuted
                                )
                            }

                            if (item.remarks.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Remarks: ${item.remarks}",
                                    fontSize = 11.sp,
                                    color = AldellaTextDark
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Recorded by: ${item.recordedBy} • Start: ${item.startTime} • End: ${item.endTime}",
                                fontSize = 11.sp,
                                color = AldellaTextMuted
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { selectedBatchForFullView = item },
                                colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open & View All Content", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
