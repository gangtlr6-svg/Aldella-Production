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
import com.example.ui.AldellaViewModel
import com.example.ui.components.AldellaHeader
import com.example.ui.theme.*

@Composable
fun AreaProcessesScreen(
    areaName: String,
    viewModel: AldellaViewModel,
    onOpenSheet: (String) -> Unit,
    onOpenUpdates: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val processAreas by viewModel.processAreas.collectAsState()
    val allProcessSheets by viewModel.allProcessSheets.collectAsState()

    val areaConfig = processAreas.firstOrNull { it.name.equals(areaName, ignoreCase = true) }
    val processes = areaConfig?.processes ?: listOf("Standard Process")

    var showAddProcessDialog by remember { mutableStateOf(false) }
    var newProcessNameInput by remember { mutableStateOf("") }

    var processToRename by remember { mutableStateOf<String?>(null) }
    var renameProcessInput by remember { mutableStateOf("") }

    // Dialog: Add Process
    if (showAddProcessDialog) {
        AlertDialog(
            onDismissRequest = { showAddProcessDialog = false },
            title = { Text("Add Process to $areaName", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newProcessNameInput,
                    onValueChange = { newProcessNameInput = it },
                    label = { Text("Process Name") },
                    placeholder = { Text("e.g. Marination QC Process") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_process_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProcessNameInput.isNotBlank()) {
                            viewModel.addProcessToArea(areaName, newProcessNameInput)
                            Toast.makeText(context, "Process added to $areaName", Toast.LENGTH_SHORT).show()
                            newProcessNameInput = ""
                            showAddProcessDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_add_process_btn")
                ) {
                    Text("Add Process")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddProcessDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Dialog: Rename Process
    processToRename?.let { oldName ->
        AlertDialog(
            onDismissRequest = { processToRename = null },
            title = { Text("Rename Process", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameProcessInput,
                    onValueChange = { renameProcessInput = it },
                    label = { Text("New Process Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_process_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameProcessInput.isNotBlank()) {
                            viewModel.renameProcessInArea(areaName, oldName, renameProcessInput)
                            Toast.makeText(context, "Process renamed to \"$renameProcessInput\"", Toast.LENGTH_SHORT).show()
                            processToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_rename_process_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { processToRename = null }) { Text("Cancel") }
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
            title = areaName,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Open a live Process sheet. Completed records move to protected Update Lists.",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                if (currentUser.role == "ADMIN") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            newProcessNameInput = ""
                            showAddProcessDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("area_add_process_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Process", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            processes.forEach { processName ->
                val sheets = allProcessSheets.filter { it.areaKey == areaName && it.processKey == processName }
                val isRunning = sheets.any { it.status == "RUNNING" }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("process_card_${processName.replace(" ", "_")}"),
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
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(AldellaBlueLight, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = AldellaBluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = processName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AldellaTextDark
                                    )
                                    Text(
                                        text = "Live protected production sheet",
                                        fontSize = 11.sp,
                                        color = AldellaTextMuted
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isRunning) {
                                    Surface(
                                        color = AldellaGreenLight,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "RUNNING",
                                            color = AldellaGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                if (currentUser.role == "ADMIN") {
                                    IconButton(
                                        onClick = {
                                            renameProcessInput = processName
                                            processToRename = processName
                                        },
                                        modifier = Modifier.size(28.dp).testTag("rename_proc_${processName.replace(" ", "_")}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = AldellaBluePrimary, modifier = Modifier.size(15.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.removeProcessFromArea(areaName, processName)
                                            Toast.makeText(context, "Process \"$processName\" removed", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp).testTag("delete_proc_${processName.replace(" ", "_")}")
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AldellaRed, modifier = Modifier.size(15.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onOpenSheet(processName) },
                                colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_process_${processName.replace(" ", "_")}")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Process", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { onOpenUpdates(processName) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaBluePrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_updates_${processName.replace(" ", "_")}")
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Updates (${sheets.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
