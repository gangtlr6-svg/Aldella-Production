package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.ComplaintDowntimeEntity
import com.example.data.local.entities.CustomTemplateFieldEntity
import com.example.data.local.entities.ProcessSheetEntity
import com.example.data.local.entities.UserAccountEntity
import com.example.ui.AldellaViewModel
import com.example.ui.ProcessAreaConfig
import com.example.ui.ReportFilterCriteria
import com.example.ui.components.AldellaHeader
import com.example.ui.components.BatchFullContentDialog
import com.example.ui.components.DailyReportExportDialog
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminControlPanelScreen(
    viewModel: AldellaViewModel,
    onNavigateToLiveOverview: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activeUsers by viewModel.activeUserAccounts.collectAsState()
    val deletedUsers by viewModel.deletedUserAccounts.collectAsState()
    val auditLogs by viewModel.allAuditLogs.collectAsState()
    val customFields by viewModel.customTemplateFields.collectAsState()
    val processAreas by viewModel.processAreas.collectAsState()
    val allProcessSheets by viewModel.allProcessSheets.collectAsState()
    val allComplaints by viewModel.allComplaintsAndDowntime.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val productCatalog by viewModel.productCatalog.collectAsState()
    val brandCatalog by viewModel.brandCatalog.collectAsState()
    val batchCatalog by viewModel.batchCatalog.collectAsState()

    // Firebase Firestore State Flows
    val isFirestoreAutoSync by viewModel.isFirestoreAutoSync.collectAsState()
    val firestoreSyncStatus by viewModel.firestoreSyncStatus.collectAsState()
    val isCloudBackupRunning by viewModel.isCloudBackupRunning.collectAsState()
    val isCloudRestoreRunning by viewModel.isCloudRestoreRunning.collectAsState()
    val firestoreLastSyncTime by viewModel.firestoreLastSyncTime.collectAsState()

    var showCloudRestoreConfirmDialog by remember { mutableStateOf(false) }
    var showFirestoreInfoDialog by remember { mutableStateOf(false) }

    var announcementText by remember { mutableStateOf("") }

    // Accordion Sections (Expandable for clean overview)
    var showAreasSection by remember { mutableStateOf(true) }
    var showCatalogSection by remember { mutableStateOf(true) }
    var showUsersSection by remember { mutableStateOf(false) }
    var showTemplateSection by remember { mutableStateOf(false) }
    var showSheetsSection by remember { mutableStateOf(false) }
    var showComplaintsSection by remember { mutableStateOf(false) }
    var showBackupSection by remember { mutableStateOf(false) }
    var showAuditSection by remember { mutableStateOf(false) }

    // Catalog Management Dialog States
    var catalogTab by remember { mutableIntStateOf(0) }
    var showAddCatalogItemDialog by remember { mutableStateOf(false) }
    var newCatalogItemName by remember { mutableStateOf("") }
    var newCatalogItemCode by remember { mutableStateOf("") }

    var catalogItemToRename by remember { mutableStateOf<String?>(null) }
    var renameCatalogItemName by remember { mutableStateOf("") }
    var renameCatalogItemCode by remember { mutableStateOf("") }

    var catalogItemToDeletePermanent by remember { mutableStateOf<String?>(null) }

    // Area Dialog States
    var showAddAreaDialog by remember { mutableStateOf(false) }
    var newAreaNameInput by remember { mutableStateOf("") }
    var areaToRename by remember { mutableStateOf<ProcessAreaConfig?>(null) }
    var renameAreaInput by remember { mutableStateOf("") }
    var areaForNewProcess by remember { mutableStateOf<ProcessAreaConfig?>(null) }
    var newProcessNameInput by remember { mutableStateOf("") }
    var processToRename by remember { mutableStateOf<Pair<String, String>?>(null) }
    var renameProcessInput by remember { mutableStateOf("") }
    var expandedAreaProcesses by remember { mutableStateOf<String?>(null) }

    // User Dialog States
    var showAddUserDialog by remember { mutableStateOf(false) }
    var newUserName by remember { mutableStateOf("") }
    var newUserEmail by remember { mutableStateOf("") }
    var newUserPassword by remember { mutableStateOf("") }
    var newUserRole by remember { mutableStateOf("OPERATOR") }
    var userToEdit by remember { mutableStateOf<UserAccountEntity?>(null) }
    var editUserName by remember { mutableStateOf("") }
    var editUserEmail by remember { mutableStateOf("") }
    var editUserRole by remember { mutableStateOf("OPERATOR") }

    // Admin Creation with 4 Account Limit
    var showCreateAdminDialog by remember { mutableStateOf(false) }
    var adminNameInput by remember { mutableStateOf("") }
    var adminEmailInput by remember { mutableStateOf("") }
    var adminPasswordInput by remember { mutableStateOf("") }

    // Template Field Dialog States
    var showAddFieldDialog by remember { mutableStateOf(false) }
    var newFieldNameInput by remember { mutableStateOf("") }
    var fieldToRename by remember { mutableStateOf<CustomTemplateFieldEntity?>(null) }
    var renameFieldNameInput by remember { mutableStateOf("") }

    // Sheet / Batch Dialog States
    var sheetToRename by remember { mutableStateOf<ProcessSheetEntity?>(null) }
    var renameSheetBatchInput by remember { mutableStateOf("") }
    var renameSheetProductInput by remember { mutableStateOf("") }

    // Complaint / Downtime Dialog States
    var complaintToEdit by remember { mutableStateOf<ComplaintDowntimeEntity?>(null) }
    var editComplaintMachineInput by remember { mutableStateOf("") }
    var editComplaintDetailsInput by remember { mutableStateOf("") }

    // Daily Report Dialog States
    var showDailyReportDialog by remember { mutableStateOf(false) }
    var reportFormatTab by remember { mutableIntStateOf(0) }

    // Database Restore Dialog States
    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreValidationMsg by remember { mutableStateOf<String?>(null) }
    var isRestoring by remember { mutableStateOf(false) }

    // Batch Updates Monitor & Inspection Dialog States
    var showUpdatesInspectionDialog by remember { mutableStateOf(false) }
    var selectedBatchForFullContent by remember { mutableStateOf<ProcessSheetEntity?>(null) }
    var updatesFilterArea by remember { mutableStateOf("All") }
    var updatesSearchQuery by remember { mutableStateOf("") }

    val completedProcessSheets = remember(allProcessSheets) {
        allProcessSheets.filter { it.isSubmitted || it.status == "COMPLETED" }
    }

    // ================== DIALOGS FOR ONE-TOUCH ACTIONS ==================

    // Detailed Batch View Dialog (Temp, Batch Code, Product Name, Operator, Total Kg, Total Hours)
    selectedBatchForFullContent?.let { sheet ->
        BatchFullContentDialog(
            sheet = sheet,
            batchOrdinal = viewModel.getBatchOrdinal(sheet.batchSequence),
            isAdmin = true,
            onRename = {
                sheetToRename = sheet
                renameSheetBatchInput = sheet.batchNo
                renameSheetProductInput = sheet.productName
                selectedBatchForFullContent = null
            },
            onDelete = {
                viewModel.deleteProcessSheet(sheet)
                Toast.makeText(context, "Batch #${sheet.id} deleted by Admin", Toast.LENGTH_SHORT).show()
                selectedBatchForFullContent = null
            },
            onDismiss = { selectedBatchForFullContent = null }
        )
    }

    // Admin Updates Inspection Dialog (Touch 'Update' in Admin Control Panel to see what's updated and open/view all content)
    if (showUpdatesInspectionDialog) {
        val filteredUpdates = remember(completedProcessSheets, updatesFilterArea, updatesSearchQuery) {
            completedProcessSheets.filter { sheet ->
                (updatesFilterArea == "All" || sheet.areaKey.equals(updatesFilterArea, ignoreCase = true)) &&
                (updatesSearchQuery.isBlank() ||
                 sheet.batchNo.contains(updatesSearchQuery, ignoreCase = true) ||
                 sheet.productName.contains(updatesSearchQuery, ignoreCase = true) ||
                 sheet.recordedBy.contains(updatesSearchQuery, ignoreCase = true) ||
                 sheet.processKey.contains(updatesSearchQuery, ignoreCase = true))
            }
        }

        AlertDialog(
            onDismissRequest = { showUpdatesInspectionDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Update, contentDescription = null, tint = AldellaGreen, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Admin Batch Updates Monitor",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AldellaTextDark
                            )
                        }
                        Text(
                            text = "Live completed batches across all areas. View temp, batch code, product name, operator, total kg, total hours.",
                            fontSize = 11.sp,
                            color = AldellaTextMuted
                        )
                    }

                    IconButton(onClick = { showUpdatesInspectionDialog = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = AldellaTextDark)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Search box
                    OutlinedTextField(
                        value = updatesSearchQuery,
                        onValueChange = { updatesSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search by batch, product, or operator...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (updatesSearchQuery.isNotEmpty()) {
                                IconButton(onClick = { updatesSearchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp)
                    )

                    // Area Filter Chips (All, Xray, L1, L2, L3, RF, DSI, etc.)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val areaList = listOf("All") + processAreas.map { it.name }
                        areaList.forEach { areaName ->
                            val isSelected = updatesFilterArea == areaName
                            FilterChip(
                                selected = isSelected,
                                onClick = { updatesFilterArea = areaName },
                                label = { Text(areaName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AldellaBluePrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    if (filteredUpdates.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(44.dp), tint = AldellaTextMuted)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No batch updates found.", color = AldellaTextMuted, fontSize = 13.sp)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredUpdates) { updateSheet ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    color = Color(0xFFEFF6FF),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "${updateSheet.areaKey} • ${viewModel.getBatchOrdinal(updateSheet.batchSequence)} Batch",
                                                        color = AldellaBluePrimary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = AldellaGreenLight,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = "FINISHED",
                                                        color = AldellaGreen,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            Text(
                                                text = updateSheet.submissionSaudiTime.ifBlank { updateSheet.date },
                                                fontSize = 11.sp,
                                                color = AldellaTextMuted
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "${updateSheet.processKey} Finished",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AldellaTextDark
                                        )
                                        Text(
                                            text = "Batch: ${updateSheet.batchNo} • Product: ${updateSheet.productName.ifBlank { "Poultry" }}",
                                            fontSize = 12.sp,
                                            color = AldellaTextDark
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 3 Key metrics: Temp, Hours, Total Kg
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Surface(
                                                color = Color(0xFFEFF6FF),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("TEMP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AldellaBluePrimary)
                                                    Text("${updateSheet.temperatureC}°C", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))
                                                }
                                            }
                                            Surface(
                                                color = Color(0xFFFEF3C7),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("TOTAL HRS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                    Text(updateSheet.totalHours.ifBlank { updateSheet.operationalTimer }, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E))
                                                }
                                            }
                                            Surface(
                                                color = Color(0xFFF0FDF4),
                                                shape = RoundedCornerShape(6.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text("TOTAL KG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AldellaGreen)
                                                    Text("${updateSheet.totalKgQty} kg", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14532D))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        Text(
                                            text = "Operator: ${updateSheet.recordedBy} • Start: ${updateSheet.startTime} • End: ${updateSheet.endTime}",
                                            fontSize = 11.sp,
                                            color = AldellaTextMuted
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Button(
                                            onClick = { selectedBatchForFullContent = updateSheet },
                                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(34.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Open & View All Content", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUpdatesInspectionDialog = false
                        onNavigateToLiveOverview()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen)
                ) {
                    Text("Go to Live Overview Tab")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdatesInspectionDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    // 1. Add Area Dialog
    if (showAddAreaDialog) {
        AlertDialog(
            onDismissRequest = { showAddAreaDialog = false },
            title = { Text("Add New Factory Process Area", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter area or production line name:", fontSize = 13.sp, color = AldellaTextMuted)
                    OutlinedTextField(
                        value = newAreaNameInput,
                        onValueChange = { newAreaNameInput = it },
                        placeholder = { Text("e.g. Spiral Freezer Line 4") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_area_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAreaNameInput.isNotBlank()) {
                            viewModel.addProcessArea(newAreaNameInput)
                            Toast.makeText(context, "Area \"$newAreaNameInput\" added successfully!", Toast.LENGTH_SHORT).show()
                            newAreaNameInput = ""
                            showAddAreaDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_add_area_btn")
                ) {
                    Text("Add Area")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAreaDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 2. Rename Area Dialog
    areaToRename?.let { area ->
        AlertDialog(
            onDismissRequest = { areaToRename = null },
            title = { Text("Rename Process Area", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Old Name: ${area.name}", fontSize = 13.sp, color = AldellaTextMuted)
                    OutlinedTextField(
                        value = renameAreaInput,
                        onValueChange = { renameAreaInput = it },
                        label = { Text("New Area Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_area_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameAreaInput.isNotBlank()) {
                            viewModel.renameProcessArea(area.name, renameAreaInput)
                            Toast.makeText(context, "Area renamed to \"$renameAreaInput\"", Toast.LENGTH_SHORT).show()
                            areaToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_rename_area_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { areaToRename = null }) { Text("Cancel") }
            }
        )
    }

    // 3. Add Process to Area Dialog
    areaForNewProcess?.let { area ->
        AlertDialog(
            onDismissRequest = { areaForNewProcess = null },
            title = { Text("Add Process to ${area.name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter sub-process workflow name:", fontSize = 13.sp, color = AldellaTextMuted)
                    OutlinedTextField(
                        value = newProcessNameInput,
                        onValueChange = { newProcessNameInput = it },
                        placeholder = { Text("e.g. Blast Chilling Process") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProcessNameInput.isNotBlank()) {
                            viewModel.addProcessToArea(area.name, newProcessNameInput)
                            Toast.makeText(context, "Process \"$newProcessNameInput\" added to ${area.name}!", Toast.LENGTH_SHORT).show()
                            newProcessNameInput = ""
                            areaForNewProcess = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary)
                ) {
                    Text("Add Process")
                }
            },
            dismissButton = {
                TextButton(onClick = { areaForNewProcess = null }) { Text("Cancel") }
            }
        )
    }

    // 4. Rename Process in Area Dialog
    processToRename?.let { (areaName, oldProcessName) ->
        AlertDialog(
            onDismissRequest = { processToRename = null },
            title = { Text("Rename Process in $areaName", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Current: $oldProcessName", fontSize = 13.sp, color = AldellaTextMuted)
                    OutlinedTextField(
                        value = renameProcessInput,
                        onValueChange = { renameProcessInput = it },
                        label = { Text("New Process Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameProcessInput.isNotBlank()) {
                            viewModel.renameProcessInArea(areaName, oldProcessName, renameProcessInput)
                            Toast.makeText(context, "Process renamed to \"$renameProcessInput\"", Toast.LENGTH_SHORT).show()
                            processToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { processToRename = null }) { Text("Cancel") }
            }
        )
    }

    // 5. Add User Dialog
    if (showAddUserDialog) {
        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            title = { Text("Add User / Staff Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newUserName,
                        onValueChange = { newUserName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_user_name_input")
                    )
                    OutlinedTextField(
                        value = newUserEmail,
                        onValueChange = { newUserEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_user_email_input")
                    )
                    Text("Select Role:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("OPERATOR", "SUPERVISOR", "ADMIN").forEach { role ->
                            FilterChip(
                                selected = newUserRole == role,
                                onClick = { newUserRole = role },
                                label = { Text(role, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newUserName.isNotBlank() && newUserEmail.isNotBlank()) {
                            viewModel.createUserAccount(newUserEmail, newUserName, newUserRole)
                            Toast.makeText(context, "User account registered!", Toast.LENGTH_SHORT).show()
                            newUserName = ""
                            newUserEmail = ""
                            showAddUserDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_add_user_btn")
                ) {
                    Text("Add User")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 5b. Create Admin Dialog (Strict 4 Account Limit)
    if (showCreateAdminDialog) {
        AlertDialog(
            onDismissRequest = { showCreateAdminDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AldellaBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create Admin Account (4 Limit)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Admin can create up to 4 admin accounts only limit. Once created, user can use directly to login with full admin power.",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )
                    OutlinedTextField(
                        value = adminNameInput,
                        onValueChange = { adminNameInput = it },
                        label = { Text("Admin Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_name_input")
                    )
                    OutlinedTextField(
                        value = adminEmailInput,
                        onValueChange = { adminEmailInput = it },
                        label = { Text("Admin Username / Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_email_input")
                    )
                    OutlinedTextField(
                        value = adminPasswordInput,
                        onValueChange = { adminPasswordInput = it },
                        label = { Text("Admin Password") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_password_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (adminNameInput.isNotBlank() && adminEmailInput.isNotBlank() && adminPasswordInput.isNotBlank()) {
                            viewModel.createAdminAccountWithLimit(
                                name = adminNameInput,
                                email = adminEmailInput,
                                pass = adminPasswordInput
                            ) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) {
                                    adminNameInput = ""
                                    adminEmailInput = ""
                                    adminPasswordInput = ""
                                    showCreateAdminDialog = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_create_admin_btn")
                ) {
                    Text("Create Admin")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAdminDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 6. Edit / Rename User Dialog
    userToEdit?.let { user ->
        AlertDialog(
            onDismissRequest = { userToEdit = null },
            title = { Text("Edit & Rename User", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editUserName,
                        onValueChange = { editUserName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_user_name_input")
                    )
                    OutlinedTextField(
                        value = editUserEmail,
                        onValueChange = { editUserEmail = it },
                        label = { Text("Email Address") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Update Role:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("OPERATOR", "SUPERVISOR", "ADMIN").forEach { role ->
                            FilterChip(
                                selected = editUserRole == role,
                                onClick = { editUserRole = role },
                                label = { Text(role, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editUserName.isNotBlank() && editUserEmail.isNotBlank()) {
                            viewModel.updateUserAccount(user.id, editUserEmail, editUserName, editUserRole)
                            Toast.makeText(context, "User profile updated!", Toast.LENGTH_SHORT).show()
                            userToEdit = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_edit_user_btn")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // 7. Add Custom Template Field Dialog
    if (showAddFieldDialog) {
        AlertDialog(
            onDismissRequest = { showAddFieldDialog = false },
            title = { Text("Add Custom Template Field", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFieldNameInput,
                    onValueChange = { newFieldNameInput = it },
                    label = { Text("Field / Column Header") },
                    placeholder = { Text("e.g. Marinade pH, Core Temp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("add_field_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFieldNameInput.isNotBlank()) {
                            viewModel.addCustomTemplateField(newFieldNameInput)
                            Toast.makeText(context, "Custom column added!", Toast.LENGTH_SHORT).show()
                            newFieldNameInput = ""
                            showAddFieldDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_add_field_btn")
                ) {
                    Text("Add Field")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFieldDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 8. Rename Custom Template Field Dialog
    fieldToRename?.let { field ->
        AlertDialog(
            onDismissRequest = { fieldToRename = null },
            title = { Text("Rename Template Field", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameFieldNameInput,
                    onValueChange = { renameFieldNameInput = it },
                    label = { Text("Field Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("rename_field_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameFieldNameInput.isNotBlank()) {
                            viewModel.renameCustomTemplateField(field, renameFieldNameInput)
                            Toast.makeText(context, "Field renamed to \"$renameFieldNameInput\"", Toast.LENGTH_SHORT).show()
                            fieldToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_rename_field_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { fieldToRename = null }) { Text("Cancel") }
            }
        )
    }

    // 9. Rename Process Sheet / Batch Dialog
    sheetToRename?.let { sheet ->
        AlertDialog(
            onDismissRequest = { sheetToRename = null },
            title = { Text("Rename Batch & Sheet Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${sheet.areaKey} • ${sheet.processKey}", fontSize = 12.sp, color = AldellaTextMuted)
                    OutlinedTextField(
                        value = renameSheetBatchInput,
                        onValueChange = { renameSheetBatchInput = it },
                        label = { Text("Batch Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("rename_sheet_batch_input")
                    )
                    OutlinedTextField(
                        value = renameSheetProductInput,
                        onValueChange = { renameSheetProductInput = it },
                        label = { Text("Product Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameSheetBatchInput.isNotBlank()) {
                            viewModel.renameProcessSheet(sheet, renameSheetBatchInput, renameSheetProductInput)
                            Toast.makeText(context, "Batch record updated!", Toast.LENGTH_SHORT).show()
                            sheetToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_rename_sheet_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { sheetToRename = null }) { Text("Cancel") }
            }
        )
    }

    // 10. Edit / Rename Complaint & Downtime Dialog
    complaintToEdit?.let { item ->
        AlertDialog(
            onDismissRequest = { complaintToEdit = null },
            title = { Text("Edit Issue / Downtime Record", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editComplaintMachineInput,
                        onValueChange = { editComplaintMachineInput = it },
                        label = { Text("Area / Machine Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editComplaintDetailsInput,
                        onValueChange = { editComplaintDetailsInput = it },
                        label = { Text("Problem Details") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.renameComplaintOrDowntime(item, editComplaintMachineInput, editComplaintDetailsInput)
                        Toast.makeText(context, "Record updated!", Toast.LENGTH_SHORT).show()
                        complaintToEdit = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary)
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { complaintToEdit = null }) { Text("Cancel") }
            }
        )
    }

    // 10. Enterprise 30-Year Production Report Dialog (PDF, Excel CSV, Docs with Multi-level Date & Full Data Filtering)
    if (showDailyReportDialog) {
        DailyReportExportDialog(
            viewModel = viewModel,
            onDismiss = { showDailyReportDialog = false }
        )
    }

    // 10B. Restore Database from JSON File Dialog
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { if (!isRestoring) showRestoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = AldellaBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Database from Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Paste the JSON content from an Aldella backup file or load the current schema to test restoration:",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                restoreJsonInput = viewModel.exportFullDatabaseBackupJson()
                                restoreValidationMsg = "Loaded current system database export into editor."
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Load Current Data", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                restoreJsonInput = ""
                                restoreValidationMsg = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Clear", fontSize = 11.sp)
                        }
                    }

                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = {
                            restoreJsonInput = it
                            restoreValidationMsg = null
                        },
                        label = { Text("Backup JSON Content") },
                        placeholder = { Text("{\n  \"application\": \"Aldella Factory Manager\",\n  \"processSheets\": [...]\n}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    )

                    restoreValidationMsg?.let { msg ->
                        Surface(
                            color = if (msg.contains("failed", ignoreCase = true) || msg.contains("invalid", ignoreCase = true)) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = msg,
                                fontSize = 11.sp,
                                color = if (msg.contains("failed", ignoreCase = true) || msg.contains("invalid", ignoreCase = true)) AldellaRed else AldellaGreen,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // Pre-validation inspection button
                    Button(
                        onClick = {
                            if (restoreJsonInput.isBlank()) {
                                restoreValidationMsg = "Please paste or enter valid JSON content."
                                return@Button
                            }
                            try {
                                val obj = JSONObject(restoreJsonInput)
                                val sheets = obj.optJSONArray("processSheets")?.length() ?: 0
                                val complaints = obj.optJSONArray("complaintsAndDowntime")?.length() ?: 0
                                val attendance = obj.optJSONArray("attendance")?.length() ?: 0
                                val app = obj.optString("application", "Unknown")
                                restoreValidationMsg = "Valid $app backup file found!\n• $sheets Process Sheets\n• $complaints Downtimes / Incidents\n• $attendance Attendance Records"
                            } catch (e: Exception) {
                                restoreValidationMsg = "Invalid JSON syntax: ${e.message}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify & Inspect Backup", fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (restoreJsonInput.isBlank()) {
                            restoreValidationMsg = "Please provide backup JSON content before restoring."
                            return@Button
                        }
                        isRestoring = true
                        coroutineScope.launch {
                            val (success, message) = viewModel.restoreDatabaseFromJson(restoreJsonInput)
                            isRestoring = false
                            if (success) {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                showRestoreDialog = false
                            } else {
                                restoreValidationMsg = message
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                    enabled = !isRestoring && restoreJsonInput.isNotBlank()
                ) {
                    if (isRestoring) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restoring...", fontSize = 12.sp)
                    } else {
                        Text("Confirm & Restore Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRestoreDialog = false },
                    enabled = !isRestoring
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White
        )
    }

    // 10C. Firebase Firestore Cloud Restore Confirmation Dialog
    if (showCloudRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isCloudRestoreRunning) showCloudRestoreConfirmDialog = false },
            icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AldellaBluePrimary) },
            title = { Text("Restore Data from Firestore?", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "This action will download all production process batches, user statuses/accounts, complaints & downtime incidents, and attendance records permanently stored in Firebase Firestore and integrate them into your local database.\n\nAre you sure you want to proceed?",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = AldellaTextDark
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCloudRestoreConfirmDialog = false
                        viewModel.restoreFromFirestoreNow(context) { _, _ -> }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary)
                ) {
                    Text("Yes, Restore from Firestore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudRestoreConfirmDialog = false }) {
                    Text("Cancel", fontSize = 12.sp)
                }
            },
            containerColor = Color.White
        )
    }

    // 10D. Firebase Firestore Schema and Cloud Info Dialog
    if (showFirestoreInfoDialog) {
        AlertDialog(
            onDismissRequest = { showFirestoreInfoDialog = false },
            icon = { Icon(Icons.Default.Storage, contentDescription = null, tint = AldellaBluePrimary) },
            title = { Text("Firebase Firestore Schema", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Aldella Factory Operations uses Firebase Firestore NoSQL cloud database for continuous disaster recovery and permanent enterprise storage across 4 primary collections:",
                        fontSize = 11.sp,
                        color = AldellaTextDark,
                        lineHeight = 15.sp
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("• production_processes: Batch codes, timer, operator, kg, waste, temp, Saudi timestamp.", fontSize = 10.sp, color = AldellaTextSecondary)
                    Text("• user_status: Staff email, name, role, approval status, and live presence heartbeat.", fontSize = 10.sp, color = AldellaTextSecondary)
                    Text("• complaints_downtime: Machine breakdowns, quality reports, attachments, and resolution hours.", fontSize = 10.sp, color = AldellaTextSecondary)
                    Text("• attendance_records: Shift sign-in/out timestamps, hours, member IDs, and supervisors.", fontSize = 10.sp, color = AldellaTextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Offline-first persistence ensures uninterrupted factory operations even during WiFi dropouts.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AldellaGreen)
                }
            },
            confirmButton = {
                Button(
                    onClick = { showFirestoreInfoDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary)
                ) {
                    Text("OK", fontSize = 12.sp)
                }
            },
            containerColor = Color.White
        )
    }

    // 11. Catalog Item Add Dialog
    if (showAddCatalogItemDialog) {
        val currentTitle = when (catalogTab) {
            0 -> "Product"
            1 -> "Brand"
            else -> "Batch Number"
        }
        AlertDialog(
            onDismissRequest = { showAddCatalogItemDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddCircle, contentDescription = null, tint = AldellaBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New $currentTitle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCatalogItemName,
                        onValueChange = { newCatalogItemName = it },
                        label = { Text("$currentTitle Name / Number") },
                        modifier = Modifier.fillMaxWidth().testTag("add_catalog_item_name_input"),
                        singleLine = true
                    )
                    if (catalogTab == 0) {
                        OutlinedTextField(
                            value = newCatalogItemCode,
                            onValueChange = { newCatalogItemCode = it },
                            label = { Text("Product Code (e.g. PRD-CBF-01)") },
                            modifier = Modifier.fillMaxWidth().testTag("add_catalog_item_code_input"),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedName = newCatalogItemName.trim()
                        if (trimmedName.isNotBlank()) {
                            when (catalogTab) {
                                0 -> viewModel.addProduct(trimmedName, newCatalogItemCode.trim())
                                1 -> viewModel.addBrand(trimmedName)
                                2 -> viewModel.addBatchNo(trimmedName)
                            }
                            Toast.makeText(context, "$currentTitle added to catalog", Toast.LENGTH_SHORT).show()
                            newCatalogItemName = ""
                            newCatalogItemCode = ""
                            showAddCatalogItemDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_add_catalog_item_btn")
                ) {
                    Text("Add $currentTitle")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCatalogItemDialog = false }) { Text("Cancel") }
            }
        )
    }

    // 12. Catalog Item Rename Dialog
    if (catalogItemToRename != null) {
        val currentTitle = when (catalogTab) {
            0 -> "Product"
            1 -> "Brand"
            else -> "Batch Number"
        }
        AlertDialog(
            onDismissRequest = { catalogItemToRename = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = AldellaBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Rename $currentTitle", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Original: \"$catalogItemToRename\"", fontSize = 12.sp, color = AldellaTextMuted)
                    OutlinedTextField(
                        value = renameCatalogItemName,
                        onValueChange = { renameCatalogItemName = it },
                        label = { Text("New $currentTitle Name") },
                        modifier = Modifier.fillMaxWidth().testTag("rename_catalog_item_name_input"),
                        singleLine = true
                    )
                    if (catalogTab == 0) {
                        OutlinedTextField(
                            value = renameCatalogItemCode,
                            onValueChange = { renameCatalogItemCode = it },
                            label = { Text("Product Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = renameCatalogItemName.trim()
                        if (trimmed.isNotBlank() && catalogItemToRename != null) {
                            when (catalogTab) {
                                0 -> viewModel.renameProduct(catalogItemToRename!!, trimmed, renameCatalogItemCode.trim())
                                1 -> viewModel.renameBrand(catalogItemToRename!!, trimmed)
                                2 -> viewModel.renameBatchNo(catalogItemToRename!!, trimmed)
                            }
                            Toast.makeText(context, "$currentTitle renamed successfully", Toast.LENGTH_SHORT).show()
                            catalogItemToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary)
                ) {
                    Text("Save Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { catalogItemToRename = null }) { Text("Cancel") }
            }
        )
    }

    // 13. Catalog Item Permanent Delete Dialog
    if (catalogItemToDeletePermanent != null) {
        val currentTitle = when (catalogTab) {
            0 -> "Product"
            1 -> "Brand"
            else -> "Batch Number"
        }
        AlertDialog(
            onDismissRequest = { catalogItemToDeletePermanent = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AldellaRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete $currentTitle Permanently?", fontWeight = FontWeight.Bold, color = AldellaRed)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete \"$catalogItemToDeletePermanent\" from the $currentTitle catalog? This action cannot be reversed.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catalogItemToDeletePermanent != null) {
                            when (catalogTab) {
                                0 -> viewModel.deleteProductPermanently(catalogItemToDeletePermanent!!)
                                1 -> viewModel.deleteBrandPermanently(catalogItemToDeletePermanent!!)
                                2 -> viewModel.deleteBatchNoPermanently(catalogItemToDeletePermanent!!)
                            }
                            Toast.makeText(context, "$currentTitle removed permanently", Toast.LENGTH_SHORT).show()
                            catalogItemToDeletePermanent = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaRed)
                ) {
                    Text("Delete Permanent", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { catalogItemToDeletePermanent = null }) { Text("Cancel") }
            }
        )
    }

    // ================== MAIN ADMIN PANEL UI ==================
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AldellaNavyDark)
    ) {
        AldellaHeader(
            viewModel = viewModel,
            title = "Admin Control Panel",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Prominent Banner Explaining Direct Touch Controls
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(AldellaGreenLight, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.TouchApp, contentDescription = null, tint = AldellaGreen, modifier = Modifier.size(22.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Admin Direct Touch Controls Active",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = AldellaTextDark
                        )
                        Text(
                            text = "Add, Rename, Delete & Remove factory items with instant 1-touch actions.",
                            fontSize = 11.sp,
                            color = AldellaTextMuted
                        )
                    }
                }
            }

            // Emergency Broadcast Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("emergency_broadcast_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Campaign, contentDescription = null, tint = AldellaRed, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Broadcast emergency announcement",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AldellaTextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sends an acknowledgement popup to all Operator & Supervisor tablets.",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = announcementText,
                        onValueChange = { announcementText = it },
                        placeholder = { Text("Enter emergency announcement...") },
                        modifier = Modifier.fillMaxWidth().testTag("broadcast_input"),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            if (announcementText.isBlank()) {
                                Toast.makeText(context, "Please enter an announcement", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.sendEmergencyBroadcast(announcementText)
                            Toast.makeText(context, "Emergency broadcast sent to all factory sessions!", Toast.LENGTH_LONG).show()
                            announcementText = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("send_broadcast_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Send Broadcast", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Quick Process & Update Overview Navigation
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Overview Monitor & Batch Updates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = AldellaTextDark
                        )
                        Surface(
                            color = AldellaGreenLight,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${completedProcessSheets.size} Updates",
                                color = AldellaGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Touch Process for running sheets, or Update to view what's updated and open/view all content (temp, batch code, product, operator, total kg, total hours).",
                        fontSize = 11.sp,
                        color = AldellaTextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onNavigateToLiveOverview,
                            modifier = Modifier.weight(1f).testTag("admin_nav_process_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Process", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showUpdatesInspectionDialog = true },
                            modifier = Modifier.weight(1f).testTag("admin_nav_update_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Update (${completedProcessSheets.size})", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Direct Preview of Finished Batches (e.g. Xray 1st batch, L1 1st batch finished)
                    if (completedProcessSheets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LATEST FINISHED BATCH UPDATES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AldellaTextMuted,
                                letterSpacing = 0.5.sp
                            )
                            TextButton(
                                onClick = { showUpdatesInspectionDialog = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("View All (${completedProcessSheets.size})", fontSize = 11.sp, color = AldellaBluePrimary, fontWeight = FontWeight.Bold)
                            }
                        }

                        completedProcessSheets.take(2).forEach { updateItem ->
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${updateItem.areaKey} • ${viewModel.getBatchOrdinal(updateItem.batchSequence)} Batch Finished",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = AldellaTextDark
                                        )
                                        Surface(
                                            color = AldellaGreenLight,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "COMPLETED",
                                                color = AldellaGreen,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "${updateItem.processKey} • Batch: ${updateItem.batchNo} • ${updateItem.productName.ifBlank { "Poultry" }}",
                                        fontSize = 12.sp,
                                        color = AldellaTextDark
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            color = Color(0xFFEFF6FF),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("TEMP", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AldellaBluePrimary)
                                                Text("${updateItem.temperatureC}°C", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                                            }
                                        }
                                        Surface(
                                            color = Color(0xFFFEF3C7),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("HOURS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                                Text(updateItem.totalHours.ifBlank { updateItem.operationalTimer }, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                                            }
                                        }
                                        Surface(
                                            color = Color(0xFFF0FDF4),
                                            shape = RoundedCornerShape(4.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(modifier = Modifier.padding(4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("TOTAL KG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AldellaGreen)
                                                Text("${updateItem.totalKgQty} kg", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Operator: ${updateItem.recordedBy}",
                                            fontSize = 11.sp,
                                            color = AldellaTextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Button(
                                            onClick = { selectedBatchForFullContent = updateItem },
                                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Open & View All Content", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Daily Activity Report Card (PDF • Excel • Docs & Download)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_daily_report_card"),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Summarize, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Daily Activity & Production Report",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AldellaTextDark
                            )
                        }
                        Surface(color = AldellaBlueLight, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "PDF • EXCEL • DOCS",
                                color = AldellaBluePrimary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Touch to export full shift production yields, batch totals, operator hours, and machine downtime directly to PDF, Excel / CSV, or Docs format with download options.",
                        fontSize = 11.sp,
                        color = AldellaTextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showDailyReportDialog = true },
                        modifier = Modifier.fillMaxWidth().testTag("admin_daily_report_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Daily Report (PDF • Excel • Docs)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ================= SECTION 1: FACTORY AREAS & PROCESSES (TOUCH TO ADD, RENAME, REMOVE) =================
            AdminExpandableCard(
                title = "Factory Areas & Layout (${processAreas.size})",
                subtitle = "Touch to Add • Rename • Remove areas & sub-processes",
                icon = Icons.Default.PrecisionManufacturing,
                isExpanded = showAreasSection,
                onToggle = { showAreasSection = !showAreasSection }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Factory Layout List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AldellaTextDark
                        )
                        Button(
                            onClick = {
                                newAreaNameInput = ""
                                showAddAreaDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_area_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Area", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    processAreas.forEach { area ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = area.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = AldellaTextDark
                                        )
                                        Text(
                                            text = "${area.processes.size} processes configured",
                                            fontSize = 11.sp,
                                            color = AldellaTextMuted
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // RENAME AREA BUTTON
                                        FilledTonalButton(
                                            onClick = {
                                                renameAreaInput = area.name
                                                areaToRename = area
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = AldellaBlueLight,
                                                contentColor = AldellaBluePrimary
                                            ),
                                            modifier = Modifier.height(32.dp).testTag("rename_area_${area.name.replace(" ", "_")}")
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Rename", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        // REMOVE AREA BUTTON
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.removeProcessArea(area.name)
                                                Toast.makeText(context, "Area \"${area.name}\" removed!", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                                            modifier = Modifier.height(32.dp).testTag("delete_area_${area.name.replace(" ", "_")}")
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Remove", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Toggle sub-processes
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            expandedAreaProcesses = if (expandedAreaProcesses == area.name) null else area.name
                                        },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (expandedAreaProcesses == area.name) "Hide sub-processes ▲" else "View / manage sub-processes ▼",
                                        fontSize = 11.sp,
                                        color = AldellaBluePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    TextButton(
                                        onClick = {
                                            newProcessNameInput = ""
                                            areaForNewProcess = area
                                        },
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(12.dp), tint = AldellaGreen)
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("+ Add Process", fontSize = 10.sp, color = AldellaGreen, fontWeight = FontWeight.Bold)
                                    }
                                }

                                AnimatedVisibility(visible = expandedAreaProcesses == area.name) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        area.processes.forEach { proc ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color.White, RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(proc, fontSize = 12.sp, color = AldellaTextDark)
                                                Row {
                                                    IconButton(
                                                        onClick = {
                                                            renameProcessInput = proc
                                                            processToRename = Pair(area.name, proc)
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(Icons.Default.Edit, contentDescription = "Rename Process", tint = AldellaBluePrimary, modifier = Modifier.size(13.dp))
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.removeProcessFromArea(area.name, proc)
                                                            Toast.makeText(context, "Process \"$proc\" removed", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete Process", tint = AldellaRed, modifier = Modifier.size(13.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= SECTION 1B: PRODUCT, BRAND & BATCH MASTER CATALOG =================
            AdminExpandableCard(
                title = "Product, Brand & Batch Master (${productCatalog.size} Prd • ${brandCatalog.size} Brd • ${batchCatalog.size} Batches)",
                subtitle = "Touch to Add • Rename • Remove • Permanently Delete",
                icon = Icons.Default.Inventory2,
                isExpanded = showCatalogSection,
                onToggle = { showCatalogSection = !showCatalogSection }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TabRow(
                        selectedTabIndex = catalogTab,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = AldellaBluePrimary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Tab(
                            selected = catalogTab == 0,
                            onClick = { catalogTab = 0 },
                            text = { Text("Products (${productCatalog.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = catalogTab == 1,
                            onClick = { catalogTab = 1 },
                            text = { Text("Brands (${brandCatalog.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = catalogTab == 2,
                            onClick = { catalogTab = 2 },
                            text = { Text("Batch No (${batchCatalog.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    val currentTabTitle = when (catalogTab) {
                        0 -> "Product"
                        1 -> "Brand"
                        else -> "Batch Number"
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$currentTabTitle Catalog List",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AldellaTextDark
                        )
                        Button(
                            onClick = {
                                newCatalogItemName = ""
                                newCatalogItemCode = ""
                                showAddCatalogItemDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_catalog_item_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add $currentTabTitle", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Content for currently selected tab
                    when (catalogTab) {
                        0 -> {
                            // Products list
                            if (productCatalog.isEmpty()) {
                                Text("No products configured. Touch Add Product above.", fontSize = 12.sp, color = AldellaTextMuted)
                            } else {
                                productCatalog.forEach { prd ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(prd.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AldellaTextDark)
                                            if (prd.code.isNotBlank()) {
                                                Text("Code: ${prd.code}", fontSize = 11.sp, color = AldellaBluePrimary)
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    catalogItemToRename = prd.name
                                                    renameCatalogItemName = prd.name
                                                    renameCatalogItemCode = prd.code
                                                },
                                                modifier = Modifier.size(32.dp).testTag("rename_product_${prd.name}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Rename Product", tint = AldellaBluePrimary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    catalogItemToDeletePermanent = prd.name
                                                },
                                                modifier = Modifier.size(32.dp).testTag("delete_product_${prd.name}")
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Product Permanent", tint = AldellaRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            // Brands list
                            if (brandCatalog.isEmpty()) {
                                Text("No brands configured. Touch Add Brand above.", fontSize = 12.sp, color = AldellaTextMuted)
                            } else {
                                brandCatalog.forEach { brd ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(brd.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AldellaTextDark, modifier = Modifier.weight(1f))
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    catalogItemToRename = brd.name
                                                    renameCatalogItemName = brd.name
                                                    renameCatalogItemCode = ""
                                                },
                                                modifier = Modifier.size(32.dp).testTag("rename_brand_${brd.name}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Rename Brand", tint = AldellaBluePrimary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    catalogItemToDeletePermanent = brd.name
                                                },
                                                modifier = Modifier.size(32.dp).testTag("delete_brand_${brd.name}")
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Brand Permanent", tint = AldellaRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            // Batch Numbers list
                            if (batchCatalog.isEmpty()) {
                                Text("No batch numbers configured. Touch Add Batch Number above.", fontSize = 12.sp, color = AldellaTextMuted)
                            } else {
                                batchCatalog.forEach { batch ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(batch.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AldellaTextDark, modifier = Modifier.weight(1f))
                                        Row {
                                            IconButton(
                                                onClick = {
                                                    catalogItemToRename = batch.name
                                                    renameCatalogItemName = batch.name
                                                    renameCatalogItemCode = ""
                                                },
                                                modifier = Modifier.size(32.dp).testTag("rename_batch_${batch.name}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Rename Batch", tint = AldellaBluePrimary, modifier = Modifier.size(16.dp))
                                            }
                                            IconButton(
                                                onClick = {
                                                    catalogItemToDeletePermanent = batch.name
                                                },
                                                modifier = Modifier.size(32.dp).testTag("delete_batch_${batch.name}")
                                            ) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = "Delete Batch Permanent", tint = AldellaRed, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= SECTION 2: USERS & STAFF (APPROVALS, 4 ADMIN LIMIT, RENAME/EDIT, DEACTIVATE) =================
            val pendingUsers: List<UserAccountEntity> = activeUsers.filter { !it.isApproved }
            val approvedUsers: List<UserAccountEntity> = activeUsers.filter { it.isApproved }
            val adminUsers = approvedUsers.filter { it.role == "ADMIN" }
            val adminCount = adminUsers.size

            AdminExpandableCard(
                title = "User Approval & Accounts (${approvedUsers.size})",
                subtitle = "Approvals (${pendingUsers.size}) • Admin Limit ($adminCount/4) • Add • Rename • Purge",
                icon = Icons.Default.Group,
                isExpanded = showUsersSection,
                onToggle = { showUsersSection = !showUsersSection }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                    // PENDING REGISTRATIONS FOR ADMIN APPROVAL
                    if (pendingUsers.isNotEmpty()) {
                        Surface(
                            color = Color(0xFFFEF3C7),
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.HourglassTop, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pending Registrations for Admin Approval (${pendingUsers.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF92400E)
                                    )
                                }
                                Text(
                                    text = "New users cannot log in until approved by plant administrator.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF78350F)
                                )

                                pendingUsers.forEach { pendingUser ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(pendingUser.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AldellaTextDark)
                                                Text("${pendingUser.email} • Role: ${pendingUser.role}", fontSize = 11.sp, color = AldellaTextMuted)
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Button(
                                                    onClick = {
                                                        viewModel.approveUserAccount(pendingUser)
                                                        Toast.makeText(context, "${pendingUser.name} approved! Can now log in.", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp).testTag("approve_user_${pendingUser.id}")
                                                ) {
                                                    Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.deleteUserAccount(pendingUser.id, pendingUser.email)
                                                        Toast.makeText(context, "Registration rejected", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(32.dp)
                                                ) {
                                                    Text("Reject", fontSize = 11.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ADMIN ACCOUNTS (STRICT 4 LIMIT)
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Admin Accounts ($adminCount / 4 Limit)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = AldellaNavyDark
                                    )
                                }
                                Text(
                                    text = if (adminCount >= 4) "Maximum 4 Admin accounts limit reached" else "Create direct-login admin accounts",
                                    fontSize = 11.sp,
                                    color = if (adminCount >= 4) AldellaRed else AldellaTextMuted
                                )
                            }

                            Button(
                                onClick = {
                                    if (adminCount < 4) {
                                        adminNameInput = ""
                                        adminEmailInput = ""
                                        adminPasswordInput = ""
                                        showCreateAdminDialog = true
                                    } else {
                                        Toast.makeText(context, "Limit reached: Maximum 4 Admin accounts allowed!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = adminCount < 4,
                                colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("create_admin_btn")
                            ) {
                                Icon(Icons.Default.AddModerator, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (adminCount < 4) "+ Admin" else "Limit (4)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Approved Staff Accounts (${approvedUsers.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AldellaTextDark
                        )
                        Button(
                            onClick = {
                                newUserName = ""
                                newUserEmail = ""
                                newUserRole = "OPERATOR"
                                showAddUserDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_user_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add User", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    approvedUsers.forEach { user ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AldellaTextDark)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = if (user.role == "ADMIN") Color(0xFFFEF3C7) else AldellaBlueLight,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = user.role,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (user.role == "ADMIN") Color(0xFFD97706) else AldellaBlueBadgeText,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(user.email, fontSize = 11.sp, color = AldellaTextMuted)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // RENAME / EDIT USER
                                    FilledTonalButton(
                                        onClick = {
                                            editUserName = user.name
                                            editUserEmail = user.email
                                            editUserRole = user.role
                                            userToEdit = user
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = AldellaBlueLight,
                                            contentColor = AldellaBluePrimary
                                        ),
                                        modifier = Modifier.height(30.dp).testTag("edit_user_${user.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Edit", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    // DEACTIVATE / DELETE USER
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.deleteUserAccount(user.id, user.email)
                                            Toast.makeText(context, "Account ${user.email} deactivated", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                                        modifier = Modifier.height(30.dp).testTag("deactivate_user_${user.id}")
                                    ) {
                                        Icon(Icons.Default.Block, contentDescription = "Deactivate", modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("Disable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    if (deletedUsers.isNotEmpty()) {
                        Divider()
                        Text(
                            text = "Deactivated Accounts (${deletedUsers.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AldellaRed
                        )

                        deletedUsers.forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFEF2F2), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                    Text("${user.email} • ${user.role}", fontSize = 10.sp, color = AldellaTextMuted)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    TextButton(
                                        onClick = {
                                            viewModel.restoreUserAccount(user.id, user.email)
                                            Toast.makeText(context, "Account restored", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Restore", color = AldellaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = {
                                            viewModel.permanentlyDeleteUserAccount(user)
                                            Toast.makeText(context, "Account permanently purged", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("Purge", color = AldellaRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= SECTION 3: TEMPLATE COLUMNS & FIELDS (TOUCH TO ADD, RENAME, DELETE) =================
            AdminExpandableCard(
                title = "Template Columns & Custom Fields (${customFields.size})",
                subtitle = "Touch to Add • Rename • Delete sheet headers",
                icon = Icons.Default.ViewKanban,
                isExpanded = showTemplateSection,
                onToggle = { showTemplateSection = !showTemplateSection }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Standard Locked Columns (Immutable):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = AldellaTextDark
                    )
                    Text(
                        text = "Product Name, Product Code, Batch No, Date, Start Time, End Time, Total Hours, Total Kg/Qty, Remarks",
                        fontSize = 11.sp,
                        color = AldellaTextMuted
                    )

                    Divider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Template Fields (${customFields.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = AldellaTextDark
                        )
                        Button(
                            onClick = {
                                newFieldNameInput = ""
                                showAddFieldDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("add_field_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Field", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (customFields.isEmpty()) {
                        Text(
                            text = "No custom fields added yet. Touch \"Add Field\" above to add dynamic columns like Marinade pH, Core Temp, etc.",
                            fontSize = 11.sp,
                            color = AldellaTextMuted
                        )
                    } else {
                        customFields.forEach { field ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(field.fieldName, fontWeight = FontWeight.Medium, fontSize = 13.sp, color = AldellaTextDark)

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            renameFieldNameInput = field.fieldName
                                            fieldToRename = field
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Rename", tint = AldellaBluePrimary, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteCustomTemplateField(field)
                                            Toast.makeText(context, "Field \"${field.fieldName}\" deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AldellaRed, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= SECTION 4: PRODUCTION SHEETS & BATCHES (TOUCH TO RENAME, DELETE) =================
            AdminExpandableCard(
                title = "Production Sheets & Batches (${allProcessSheets.size})",
                subtitle = "Touch to Rename batch • Delete sheet record",
                icon = Icons.Default.Description,
                isExpanded = showSheetsSection,
                onToggle = { showSheetsSection = !showSheetsSection }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (allProcessSheets.isEmpty()) {
                        Text(
                            text = "No production sheets logged in database.",
                            fontSize = 12.sp,
                            color = AldellaTextMuted
                        )
                    } else {
                        allProcessSheets.take(8).forEach { sheet ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${sheet.batchNo} • ${sheet.processKey}",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = AldellaTextDark
                                            )
                                            Text(
                                                text = "${sheet.areaKey} • ${sheet.productName} • ${sheet.totalKgQty} kg",
                                                fontSize = 11.sp,
                                                color = AldellaTextMuted
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // RENAME BATCH BUTTON
                                            FilledTonalButton(
                                                onClick = {
                                                    renameSheetBatchInput = sheet.batchNo
                                                    renameSheetProductInput = sheet.productName
                                                    sheetToRename = sheet
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = AldellaBlueLight,
                                                    contentColor = AldellaBluePrimary
                                                ),
                                                modifier = Modifier.height(28.dp).testTag("rename_sheet_${sheet.id}")
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Rename", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // DELETE SHEET BUTTON
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.deleteProcessSheet(sheet)
                                                    Toast.makeText(context, "Sheet #${sheet.id} (${sheet.batchNo}) deleted!", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                                                modifier = Modifier.height(28.dp).testTag("delete_sheet_${sheet.id}")
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Delete", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= SECTION 5: COMPLAINTS & DOWNTIME (TOUCH TO RESOLVE, EDIT, DELETE) =================
            AdminExpandableCard(
                title = "Complaints & Downtime Records (${allComplaints.size})",
                subtitle = "Touch to Resolve • Edit issue • Delete record",
                icon = Icons.Default.WarningAmber,
                isExpanded = showComplaintsSection,
                onToggle = { showComplaintsSection = !showComplaintsSection }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (allComplaints.isEmpty()) {
                        Text(
                            text = "No open machine problems or complaints.",
                            fontSize = 12.sp,
                            color = AldellaTextMuted
                        )
                    } else {
                        allComplaints.take(6).forEach { complaint ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                border = CardDefaults.outlinedCardBorder()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(complaint.areaOrMachine, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = if (complaint.status == "OPEN") Color(0xFFFEE2E2) else AldellaGreenLight,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = complaint.status,
                                                    color = if (complaint.status == "OPEN") AldellaRed else AldellaGreen,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(complaint.details, fontSize = 11.sp, color = AldellaTextDark, maxLines = 2)
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        if (complaint.status == "OPEN") {
                                            TextButton(
                                                onClick = {
                                                    viewModel.resolveComplaintOrDowntime(complaint)
                                                    Toast.makeText(context, "Issue marked resolved", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text("Resolve", color = AldellaGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                editComplaintMachineInput = complaint.areaOrMachine
                                                editComplaintDetailsInput = complaint.details
                                                complaintToEdit = complaint
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AldellaBluePrimary, modifier = Modifier.size(15.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                viewModel.deleteComplaintOrDowntime(complaint)
                                                Toast.makeText(context, "Issue record deleted", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AldellaRed, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ================= SECTION 6: BACKUP, RESTORE & 30-YEAR ARCHIVE =================
            AdminExpandableCard(
                title = "Backup, Restore & 30-Year Data Archive",
                subtitle = "JSON backup, database restore, 30-year master reports",
                icon = Icons.Default.CloudUpload,
                isExpanded = showBackupSection,
                onToggle = { showBackupSection = !showBackupSection }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Enterprise Longevity Status Banner
                    Surface(
                        color = Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(8.dp),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = AldellaGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("30-Year Archive Longevity Ready (2024–2056)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AldellaGreen)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Current Active Stored Data:\n• ${allProcessSheets.size} Production Batches\n• ${allComplaints.size} Downtime & Quality Incidents\n• ${allAttendance.size} Attendance Sign-in Records\n• ${activeUsers.size} Staff Accounts • ${productCatalog.size} Products • ${brandCatalog.size} Brands",
                                fontSize = 11.sp,
                                color = AldellaTextDark,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // DOWNLOAD FULL DATABASE BACKUP FILE (JSON)
                    Button(
                        onClick = {
                            viewModel.downloadFullBackupFile(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Download Full Backup File (JSON)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // RESTORE DATABASE BUTTON
                        OutlinedButton(
                            onClick = {
                                restoreJsonInput = ""
                                restoreValidationMsg = null
                                showRestoreDialog = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.SettingsBackupRestore, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Database", fontSize = 11.sp)
                        }

                        // MASTER 30-YEAR EXCEL ARCHIVE EXPORT
                        OutlinedButton(
                            onClick = {
                                viewModel.downloadReportDocument(context, "EXCEL", ReportFilterCriteria())
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = AldellaGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Full Master Excel", fontSize = 11.sp)
                        }
                    }

                    // FILTERED REPORT LAUNCHER
                    Button(
                        onClick = { showDailyReportDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open 30-Year Filter & Report Downloader", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ================= SECTION 7: AUDIT TRAIL LOG =================
            AdminExpandableCard(
                title = "Security Audit Trail (${auditLogs.size})",
                subtitle = "Automated log of all Add • Rename • Delete actions",
                icon = Icons.Default.Security,
                isExpanded = showAuditSection,
                onToggle = { showAuditSection = !showAuditSection }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (auditLogs.isEmpty()) {
                        Text("No audit logs recorded yet.", fontSize = 12.sp, color = AldellaTextMuted)
                    } else {
                        auditLogs.take(8).forEach { log ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(6.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.actionType, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AldellaBlueBadgeText)
                                    Text(log.timestampFormatted, fontSize = 10.sp, color = AldellaTextMuted)
                                }
                                Text(log.description, fontSize = 11.sp, color = AldellaTextDark)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdminNavCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(AldellaBlueLight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AldellaTextDark)
                Text(text = subtitle, fontSize = 11.sp, color = AldellaTextMuted)
            }
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun AdminExpandableCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(AldellaBlueLight, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AldellaTextDark)
                    Text(text = subtitle, fontSize = 11.sp, color = AldellaTextMuted)
                }
                Icon(
                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider()
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun CloudMetricChip(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.08f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count.toString(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
            Text(label, fontSize = 9.sp, color = AldellaTextSecondary, maxLines = 1)
        }
    }
}
