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
import com.example.data.local.entities.ComplaintDowntimeEntity
import com.example.data.local.entities.ProcessSheetEntity
import com.example.ui.AldellaViewModel
import com.example.ui.components.AldellaHeader
import com.example.ui.components.BatchFullContentDialog
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveOverviewScreen(
    viewModel: AldellaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allProcessSheets by viewModel.allProcessSheets.collectAsState()
    val allComplaints by viewModel.allComplaintsAndDowntime.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()

    var filterQuery by remember { mutableStateOf("") }
    var fromDate by remember { mutableStateOf("") }
    var toDate by remember { mutableStateOf("") }

    var sheetToRename by remember { mutableStateOf<ProcessSheetEntity?>(null) }
    var renameBatchInput by remember { mutableStateOf("") }
    var renameProductInput by remember { mutableStateOf("") }
    var selectedBatchForFullView by remember { mutableStateOf<ProcessSheetEntity?>(null) }

    // Dialog to view all content of a batch (temp, batch code, product name, operator, total kg, total hours)
    selectedBatchForFullView?.let { sheet ->
        BatchFullContentDialog(
            sheet = sheet,
            batchOrdinal = viewModel.getBatchOrdinal(sheet.batchSequence),
            isAdmin = currentUser.role == "ADMIN",
            onRename = if (currentUser.role == "ADMIN") {
                {
                    renameBatchInput = sheet.batchNo
                    renameProductInput = sheet.productName
                    sheetToRename = sheet
                    selectedBatchForFullView = null
                }
            } else null,
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

    // Rename Batch Dialog
    sheetToRename?.let { sheet ->
        AlertDialog(
            onDismissRequest = { sheetToRename = null },
            title = { Text("Rename Batch & Sheet", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("${sheet.areaKey} • ${sheet.processKey}", fontSize = 12.sp, color = AldellaTextMuted)
                    OutlinedTextField(
                        value = renameBatchInput,
                        onValueChange = { renameBatchInput = it },
                        label = { Text("Batch Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("live_rename_batch_input")
                    )
                    OutlinedTextField(
                        value = renameProductInput,
                        onValueChange = { renameProductInput = it },
                        label = { Text("Product Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameBatchInput.isNotBlank()) {
                            viewModel.renameProcessSheet(sheet, renameBatchInput, renameProductInput)
                            Toast.makeText(context, "Batch renamed to $renameBatchInput", Toast.LENGTH_SHORT).show()
                            sheetToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    modifier = Modifier.testTag("confirm_live_rename_btn")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { sheetToRename = null }) { Text("Cancel") }
            }
        )
    }

    val filteredSheets = remember(allProcessSheets, filterQuery) {
        if (filterQuery.isBlank()) allProcessSheets
        else allProcessSheets.filter {
            it.areaKey.contains(filterQuery, ignoreCase = true) ||
            it.processKey.contains(filterQuery, ignoreCase = true) ||
            it.batchNo.contains(filterQuery, ignoreCase = true)
        }
    }

    val runningSheets = filteredSheets.filter { it.status == "RUNNING" }
    val completedSheets = filteredSheets.filter { it.status != "RUNNING" }
    val activeComplaints = allComplaints.filter { it.status == "OPEN" }

    var activeOverviewTab by remember { mutableStateOf("PROCESS") } // "PROCESS" or "UPDATE"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AldellaNavyDark)
    ) {
        AldellaHeader(
            viewModel = viewModel,
            title = "Live All-Process Overview",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Current task activity and updates across all connected production processes.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )

            // Primary Overview Switcher: [ PROCESS ] and [ UPDATE ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { activeOverviewTab = "PROCESS" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeOverviewTab == "PROCESS") AldellaBluePrimary else AldellaNavySurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("overview_process_tab_btn")
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Process", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Button(
                    onClick = { activeOverviewTab = "UPDATE" },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (activeOverviewTab == "UPDATE") AldellaBluePrimary else AldellaNavySurface
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(46.dp).testTag("overview_update_tab_btn")
                ) {
                    Icon(Icons.Default.Update, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Update", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Filter Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filter_live_batches_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Filter live batches",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = AldellaTextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type RF, Xray, DSI, Injection, L1, L2, or L3 to view matching areas/processes.",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = filterQuery,
                        onValueChange = { filterQuery = it },
                        label = { Text("Process / area") },
                        placeholder = { Text("e.g. RF, Defrosting, Line 1") },
                        modifier = Modifier.fillMaxWidth().testTag("filter_process_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AldellaBluePrimary,
                            unfocusedBorderColor = AldellaBorder
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = fromDate,
                            onValueChange = { fromDate = it },
                            label = { Text("From date") },
                            placeholder = { Text("DD-MM-YYYY") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = toDate,
                            onValueChange = { toDate = it },
                            label = { Text("To date") },
                            placeholder = { Text("DD-MM-YYYY") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.exportCsvReport(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("export_csv_btn")
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                Toast.makeText(context, "PDF generated & saved to downloads", Toast.LENGTH_SHORT).show()
                                viewModel.exportDailyActivities(context)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("export_pdf_btn")
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export PDF", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = {
                                filterQuery = ""
                                fromDate = ""
                                toDate = ""
                            }
                        ) {
                            Text("Clear", color = AldellaRed, fontSize = 12.sp)
                        }
                    }
                }
            }

            // Database Connected Status Banner
            Surface(
                color = AldellaNavySurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(AldellaGreen, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Central database connected • ${allProcessSheets.size} production, ${allComplaints.count { it.type == "DOWNTIME" }} downtime, ${allComplaints.count { it.type == "COMPLAINT" }} complaint records",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }

            // Active Complaints Section
            if (activeComplaints.isNotEmpty()) {
                Text(
                    text = "Active complaints (${activeComplaints.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                activeComplaints.forEach { complaint ->
                    ComplaintCard(
                        item = complaint,
                        onResolve = { viewModel.resolveComplaintOrDowntime(complaint) },
                        onDelete = { viewModel.deleteComplaintOrDowntime(complaint) }
                    )
                }
            }

            // Tab Content: PROCESS vs UPDATE
            if (activeOverviewTab == "UPDATE") {
                // Finished Batches Updates View
                Text(
                    text = "Finished Batch Updates (${completedSheets.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Time-to-time updates of finished batches. 1st Batch, 2nd Batch tracking with exact Saudi start/end times & automatic total hours.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                if (completedSheets.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
                    ) {
                        Text(
                            text = "No finished batches submitted yet. Once an operator or supervisor submits a finished process sheet, it will appear here as 1st Batch, 2nd Batch, etc.",
                            modifier = Modifier.padding(16.dp),
                            color = AldellaTextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    completedSheets.forEach { sheet ->
                        BatchUpdateCard(
                            sheet = sheet,
                            isAdmin = currentUser.role == "ADMIN",
                            batchOrdinal = viewModel.getBatchOrdinal(sheet.batchSequence),
                            onViewAllContent = { selectedBatchForFullView = sheet },
                            onRename = {
                                renameBatchInput = sheet.batchNo
                                renameProductInput = sheet.productName
                                sheetToRename = sheet
                            },
                            onDelete = {
                                viewModel.deleteProcessSheet(sheet)
                                Toast.makeText(context, "Batch #${sheet.id} deleted by Admin", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            } else {
                // Live Running Process Sheets
                Text(
                    text = "Live Process Sheets (${runningSheets.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (runningSheets.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
                    ) {
                        Text(
                            text = "No active running process sheets at this moment.",
                            modifier = Modifier.padding(16.dp),
                            color = AldellaTextMuted,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    runningSheets.forEach { sheet ->
                        LiveSheetCard(
                            sheet = sheet,
                            isAdmin = currentUser.role == "ADMIN",
                            onViewAllContent = { selectedBatchForFullView = sheet },
                            onRename = {
                                renameBatchInput = sheet.batchNo
                                renameProductInput = sheet.productName
                                sheetToRename = sheet
                            },
                            onDelete = {
                                viewModel.deleteProcessSheet(sheet)
                                Toast.makeText(context, "Sheet #${sheet.id} (${sheet.batchNo}) deleted", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }

            // Member Attendance Sessions
            Text(
                text = "Recent Member Attendance (${allAttendance.size})",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            if (allAttendance.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
                ) {
                    Text(
                        text = "No member attendance recorded yet.",
                        modifier = Modifier.padding(16.dp),
                        color = AldellaTextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                allAttendance.take(5).forEach { att ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = att.fullName.ifBlank { "Member ${att.memberId}" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${att.department} • Shift: ${att.shiftType} • ${att.attendanceDate}",
                                    fontSize = 12.sp,
                                    color = AldellaTextMuted
                                )
                            }
                            Surface(
                                color = AldellaGreenLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = att.totalWorkingHours,
                                    color = AldellaGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
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
private fun ComplaintCard(
    item: ComplaintDowntimeEntity,
    onResolve: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = AldellaRedLight,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = item.areaOrMachine,
                            color = AldellaRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Submitted by ${item.submittedBy}",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )
                }

                Surface(
                    color = if (item.status == "OPEN") AldellaRed else AldellaGreen,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = item.status,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.details,
                fontSize = 13.sp,
                color = AldellaTextDark
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Started: ${item.startTimeFormatted}",
                fontSize = 11.sp,
                color = AldellaTextMuted
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (item.status == "OPEN") {
                    Button(
                        onClick = onResolve,
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Resolve", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                OutlinedButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Delete", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun LiveSheetCard(
    sheet: ProcessSheetEntity,
    isAdmin: Boolean = false,
    onViewAllContent: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${sheet.batchNo} • ${sheet.processKey} • ${sheet.recordedBy}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = AldellaTextDark
                    )
                    Text(
                        text = "${sheet.areaKey} • Product: ${sheet.productName.ifBlank { "Standard poultry" }}",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )
                }

                Surface(
                    color = if (sheet.status == "RUNNING") AldellaGreenLight else AldellaBlueLight,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = sheet.status,
                        color = if (sheet.status == "RUNNING") AldellaGreen else AldellaBlueBadgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick metrics row: Temp, Running Hours, Total Kg
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
                        Text("${sheet.temperatureC}°C", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E3A8A))
                    }
                }
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("HOURS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        Text(sheet.operationalTimer, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                    }
                }
                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL KG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AldellaGreen)
                        Text("${sheet.totalKgQty} kg", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF14532D))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Started at: ${sheet.startTime} (Saudi Time)",
                    fontSize = 11.sp,
                    color = AldellaTextMuted
                )
                Text(
                    text = "Operator: ${sheet.recordedBy}",
                    fontSize = 11.sp,
                    color = AldellaTextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onViewAllContent,
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

            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onRename,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AldellaBlueLight,
                            contentColor = AldellaBluePrimary
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rename", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun BatchUpdateCard(
    sheet: ProcessSheetEntity,
    isAdmin: Boolean = false,
    batchOrdinal: String,
    onViewAllContent: () -> Unit = {},
    onRename: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "$batchOrdinal Batch",
                            color = AldellaBluePrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = AldellaGreenLight,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "FINISHED",
                            color = AldellaGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "🔒 No Modify / Delete by Staff",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AldellaTextMuted,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${sheet.processKey} Finished • Area: ${sheet.areaKey}",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = AldellaTextDark
            )
            Text(
                text = "Batch Name: ${sheet.batchNo} • Product: ${sheet.productName.ifBlank { "Standard Poultry" }}",
                fontSize = 13.sp,
                color = AldellaTextDark
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Highlighted Metrics Cards: Temp, Total Hours, Total Kg
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = Color(0xFFEFF6FF),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TEMPERATURE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AldellaBluePrimary)
                        Text("${sheet.temperatureC}°C", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1E3A8A))
                    }
                }

                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL HOURS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                        Text(sheet.totalHours.ifBlank { sheet.operationalTimer.ifBlank { "0h 0m" } }, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF92400E))
                    }
                }

                Surface(
                    color = Color(0xFFF0FDF4),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("TOTAL KG", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AldellaGreen)
                        Text("${sheet.totalKgQty} kg", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF14532D))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                color = Color(0xFFF8FAFC),
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Exact Saudi Start Time:",
                            fontSize = 12.sp,
                            color = AldellaTextMuted
                        )
                        Text(
                            text = sheet.startTime.ifBlank { "N/A" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AldellaTextDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Exact Saudi End Time:",
                            fontSize = 12.sp,
                            color = AldellaTextMuted
                        )
                        Text(
                            text = sheet.endTime.ifBlank { "N/A" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AldellaTextDark
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Operator / Updated by: ${sheet.recordedBy}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AldellaTextDark
                )

                Text(
                    text = sheet.submissionSaudiTime.ifBlank { sheet.date },
                    fontSize = 11.sp,
                    color = AldellaTextMuted
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent button to open and view all content as requested
            Button(
                onClick = onViewAllContent,
                colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open & View All Content", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (isAdmin) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = onRename,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AldellaBlueLight,
                            contentColor = AldellaBluePrimary
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rename Batch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = onDelete,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Batch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
