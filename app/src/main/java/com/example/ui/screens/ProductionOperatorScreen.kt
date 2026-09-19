package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AldellaViewModel
import com.example.ui.ProcessAreaConfig
import com.example.ui.components.AldellaHeader
import com.example.ui.theme.*

@Composable
fun ProductionOperatorScreen(
    viewModel: AldellaViewModel,
    onSelectArea: (String) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val processAreas by viewModel.processAreas.collectAsState()
    val allProcessSheets by viewModel.allProcessSheets.collectAsState()

    var showAddAreaDialog by remember { mutableStateOf(false) }
    var newAreaNameInput by remember { mutableStateOf("") }
    var areaToRename by remember { mutableStateOf<ProcessAreaConfig?>(null) }
    var renameAreaInput by remember { mutableStateOf("") }

    // Dialog: Add Area
    if (showAddAreaDialog) {
        AlertDialog(
            onDismissRequest = { showAddAreaDialog = false },
            title = { Text("Add New Process Area", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newAreaNameInput,
                    onValueChange = { newAreaNameInput = it },
                    label = { Text("Area Name") },
                    placeholder = { Text("e.g. Line 3 Freezing") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("op_add_area_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAreaNameInput.isNotBlank()) {
                            viewModel.addProcessArea(newAreaNameInput)
                            Toast.makeText(context, "Area \"$newAreaNameInput\" added!", Toast.LENGTH_SHORT).show()
                            newAreaNameInput = ""
                            showAddAreaDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("op_confirm_add_area_btn")
                ) {
                    Text("Add Area")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAreaDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Rename Area
    areaToRename?.let { target ->
        AlertDialog(
            onDismissRequest = { areaToRename = null },
            title = { Text("Rename Process Area", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameAreaInput,
                    onValueChange = { renameAreaInput = it },
                    label = { Text("New Area Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("op_rename_area_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameAreaInput.isNotBlank()) {
                            viewModel.renameProcessArea(target.name, renameAreaInput)
                            Toast.makeText(context, "Area renamed to \"$renameAreaInput\"", Toast.LENGTH_SHORT).show()
                            areaToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("op_confirm_rename_area_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { areaToRename = null }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AldellaNavyDark)
    ) {
        AldellaHeader(
            viewModel = viewModel,
            title = "Production Lines & Areas",
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
                Text(
                    text = "Factory production line management",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )

                if (currentUser.role == "ADMIN") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            newAreaNameInput = ""
                            showAddAreaDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("op_screen_add_area_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Area", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            processAreas.forEachIndexed { index, area ->
                val count = allProcessSheets.count { it.areaKey == area.name }
                val running = allProcessSheets.any { it.areaKey == area.name && it.status == "RUNNING" }

                AreaCard(
                    title = area.name,
                    processesCount = area.processes.size,
                    activeRunning = running,
                    totalSheetsLogged = count,
                    icon = getIconForArea(area.name),
                    iconBg = getBgForArea(index),
                    iconTint = getTintForArea(index),
                    testTag = "area_card_${area.name.replace(" ", "_")}",
                    isAdmin = currentUser.role == "ADMIN",
                    onRename = {
                        renameAreaInput = area.name
                        areaToRename = area
                    },
                    onDelete = {
                        viewModel.removeProcessArea(area.name)
                        Toast.makeText(context, "Area \"${area.name}\" removed!", Toast.LENGTH_SHORT).show()
                    },
                    onClick = { onSelectArea(area.name) }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AreaCard(
    title: String,
    processesCount: Int,
    activeRunning: Boolean,
    totalSheetsLogged: Int,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    testTag: String,
    isAdmin: Boolean = false,
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {},
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(iconBg, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AldellaTextDark
                    )
                    if (activeRunning) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = AldellaGreenLight,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "RUNNING",
                                color = AldellaGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$processesCount process${if (processesCount > 1) "es" else ""} • $totalSheetsLogged logged",
                    fontSize = 11.sp,
                    color = AldellaTextMuted
                )
            }

            if (isAdmin) {
                IconButton(
                    onClick = onRename,
                    modifier = Modifier.size(32.dp).testTag("card_rename_${title.replace(" ", "_")}")
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Rename", tint = AldellaBluePrimary, modifier = Modifier.size(16.dp))
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp).testTag("card_delete_${title.replace(" ", "_")}")
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = AldellaRed, modifier = Modifier.size(16.dp))
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun getIconForArea(area: String): ImageVector {
    return when {
        area.contains("RF", true) -> Icons.Default.Sensors
        area.contains("Defrost", true) -> Icons.Default.AcUnit
        area.contains("Xray", true) -> Icons.Default.FilterCenterFocus
        area.contains("DSI", true) -> Icons.Default.ContentCut
        area.contains("Injection", true) -> Icons.Default.WaterDrop
        area.contains("L1", true) || area.contains("Line 1", true) -> Icons.Default.PrecisionManufacturing
        area.contains("L2", true) || area.contains("Line 2", true) -> Icons.Default.PrecisionManufacturing
        area.contains("L3", true) || area.contains("Line 3", true) -> Icons.Default.PrecisionManufacturing
        area.contains("Packing", true) -> Icons.Default.Inventory2
        else -> Icons.Default.PrecisionManufacturing
    }
}

private fun getBgForArea(index: Int): Color {
    val bgs = listOf(
        Color(0xFFE0F2FE), Color(0xFFFEE2E2), Color(0xFFE0E7FF), Color(0xFFDCFCE7),
        Color(0xFFFEF3C7), Color(0xFFF3E8FF), Color(0xFFCCFBF1), Color(0xFFFFEDD5),
        Color(0xFFF1F5F9), Color(0xFFE2E8F0)
    )
    return bgs[index % bgs.size]
}

private fun getTintForArea(index: Int): Color {
    val tints = listOf(
        Color(0xFF0284C7), Color(0xFFDC2626), Color(0xFF4F46E5), Color(0xFF16A34A),
        Color(0xFFD97706), Color(0xFF9333EA), Color(0xFF0D9488), Color(0xFFEA580C),
        Color(0xFF475569), Color(0xFF334155)
    )
    return tints[index % tints.size]
}
