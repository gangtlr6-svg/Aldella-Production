package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.ProcessSheetEntity
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun BatchFullContentDialog(
    sheet: ProcessSheetEntity,
    batchOrdinal: String = "",
    isAdmin: Boolean = false,
    onRename: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val displayOrdinal = if (batchOrdinal.isNotBlank()) batchOrdinal else "${sheet.batchSequence}th"
    val isFinished = sheet.status == "COMPLETED" || sheet.isSubmitted

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AldellaCardBg,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("batch_full_content_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Header Banner
                Surface(
                    color = AldellaNavyDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AldellaBluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.PrecisionManufacturing,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = Color(0xFF1E3A8A),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = sheet.areaKey,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = if (isFinished) AldellaGreenLight else Color(0xFFFEF3C7),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (isFinished) "FINISHED" else "RUNNING",
                                            color = if (isFinished) AldellaGreen else Color(0xFFB45309),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${sheet.processKey} ($displayOrdinal Batch)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_full_content_btn")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                // Scrollable Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Tamper-proof Saudi Time Record Notice
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = AldellaGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Locked & Tamper-Proof Factory Record",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AldellaGreen
                                )
                                Text(
                                    text = "Recorded in Central DB with exact Saudi Arabia timestamps. Only Admin has oversight.",
                                    fontSize = 11.sp,
                                    color = AldellaTextDark
                                )
                            }
                        }
                    }

                    // 4 Core Highlight Metric Cards (Temp, Total Hours, Total Kg, Operator)
                    Text(
                        text = "CORE PRODUCTION METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AldellaTextMuted,
                        letterSpacing = 0.8.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Temperature Card (Highly requested: "for example temp")
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("TEMPERATURE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AldellaBluePrimary)
                                    Icon(Icons.Default.Thermostat, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${sheet.temperatureC} °C",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF1E3A8A)
                                )
                                Text("Process sensor", fontSize = 10.sp, color = AldellaTextMuted)
                            }
                        }

                        // 2. Total Hours Card (Highly requested: "total hours")
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEF3C7),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("TOTAL HOURS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                    Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sheet.totalHours.ifBlank { sheet.operationalTimer.ifBlank { "00:00:00" } },
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF92400E)
                                )
                                Text("Calculated duration", fontSize = 10.sp, color = AldellaTextMuted)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 3. Total Kg Card (Highly requested: "total kg")
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("TOTAL KG", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AldellaGreen)
                                    Icon(Icons.Default.Scale, contentDescription = null, tint = AldellaGreen, modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${sheet.totalKgQty} kg",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF14532D)
                                )
                                Text("Yield output", fontSize = 10.sp, color = AldellaTextMuted)
                            }
                        }

                        // 4. Operator Name Card (Highly requested: "operater name")
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF5F3FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFDDD6FE))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("OPERATOR", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6D28D9))
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF6D28D9), modifier = Modifier.size(16.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = sheet.recordedBy.ifBlank { "Production Staff" },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4C1D95),
                                    maxLines = 1
                                )
                                Text("Submitted by", fontSize = 10.sp, color = AldellaTextMuted)
                            }
                        }
                    }

                    // Product & Batch Identification Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "PRODUCT & BATCH SPECIFICATIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AldellaTextMuted,
                                letterSpacing = 0.8.sp
                            )

                            DetailRow("Product Name:", sheet.productName.ifBlank { "Standard Poultry" }, isBold = true)
                            DetailRow("Product Code:", sheet.productCode.ifBlank { "N/A" })
                            DetailRow("Batch Number / Code:", sheet.batchNo.ifBlank { "Batch 1" }, isBold = true)
                            DetailRow("Brand:", sheet.brand.ifBlank { "Aldella Premium" })
                            DetailRow("Chicken Product:", sheet.chickenProduct.ifBlank { "Fresh Defrosted Poultry" })
                            DetailRow("Production Date:", sheet.date.ifBlank { "N/A" })
                            DetailRow("Shift Batch Sequence:", "$displayOrdinal Batch in Area")
                        }
                    }

                    // Mass Balance & Waste Detail Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "QUANTITIES & WASTE BALANCE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AldellaTextMuted,
                                letterSpacing = 0.8.sp
                            )

                            DetailRow("Input Weight:", "${sheet.inputKg} kg")
                            DetailRow("Output Weight:", "${sheet.outputKg} kg")
                            DetailRow("Total Quantity Yield:", "${sheet.totalKgQty} kg", isBold = true)
                            DetailRow("Product Waste:", "${sheet.productWasteKg} kg")
                            DetailRow("Water Waste:", "${sheet.waterWasteLitres} Litres")
                            DetailRow("Waste Classification:", sheet.wasteType)
                        }
                    }

                    // Operational Timestamps (Saudi Arabia Time)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SAUDI ARABIA TIME RECORD (ASIA/RIYADH)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AldellaTextMuted,
                                letterSpacing = 0.8.sp
                            )

                            DetailRow("Exact Start Time:", sheet.startTime.ifBlank { "--" }, isBold = true)
                            DetailRow("Exact End Time:", sheet.endTime.ifBlank { "--" }, isBold = true)
                            DetailRow("Total Operational Time:", sheet.totalHours.ifBlank { sheet.operationalTimer }, isBold = true)
                            DetailRow("Submission Timestamp:", sheet.submissionSaudiTime.ifBlank { "${sheet.date} ${sheet.endTime}" })
                            DetailRow("Database Reference ID:", "Record #${sheet.id}")
                        }
                    }

                    // Remarks & Physical Verification
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "VERIFICATION & SUPERVISION REMARKS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AldellaTextMuted,
                                letterSpacing = 0.8.sp
                            )

                            Text(
                                text = sheet.remarks.ifBlank { "No special operational notes recorded." },
                                fontSize = 13.sp,
                                color = AldellaTextDark
                            )

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (sheet.copy1Attached) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (sheet.copy1Attached) AldellaGreen else AldellaTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy 1 Attached", fontSize = 12.sp, color = AldellaTextDark)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (sheet.copy2Attached) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = null,
                                        tint = if (sheet.copy2Attached) AldellaGreen else AldellaTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy 2 Attached", fontSize = 12.sp, color = AldellaTextDark)
                                }
                            }
                        }
                    }
                }

                // Footer Action Bar
                Surface(
                    color = Color(0xFFF8FAFC),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isAdmin && onRename != null && onDelete != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = onRename,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Rename", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = onDelete,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 12.sp)
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Close View", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AldellaTextMuted
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Medium,
            color = AldellaTextDark
        )
    }
}
