package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.CustomTemplateFieldEntity
import com.example.data.local.entities.ProcessSheetEntity
import com.example.ui.AldellaViewModel
import com.example.ui.components.AdminAttributeManagerDialog
import com.example.ui.components.AldellaHeader
import com.example.ui.components.AttributeMode
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProcessSheetScreen(
    areaName: String,
    processName: String,
    viewModel: AldellaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allProcessSheets by viewModel.allProcessSheets.collectAsState()
    val customFields by viewModel.customTemplateFields.collectAsState()

    val isAdmin = currentUser.role == "ADMIN"

    // Calculate completed batches for this process to determine sequence
    val completedCountForProcess = remember(allProcessSheets, areaName, processName) {
        allProcessSheets.count {
            it.areaKey.equals(areaName, ignoreCase = true) &&
            it.processKey.equals(processName, ignoreCase = true) &&
            it.isSubmitted
        }
    }

    // Persistent Running Process Sheet from Room Database
    // "Once start time app close also, Again open also same start time show"
    val runningSheet = remember(allProcessSheets, areaName, processName) {
        allProcessSheets.firstOrNull {
            it.areaKey.equals(areaName, ignoreCase = true) &&
            it.processKey.equals(processName, ignoreCase = true) &&
            it.status == "RUNNING" && !it.isSubmitted
        }
    }

    var activeSheetId by remember { mutableStateOf(0L) }
    var activeStartTimestamp by remember { mutableStateOf(0L) }
    var persistentStartTime by remember { mutableStateOf("--") }
    var persistentEndTime by remember { mutableStateOf("--") }
    var isProcessActive by remember { mutableStateOf(false) }
    var localElapsedSeconds by remember { mutableStateOf(0L) }
    var showManualClearDialog by remember { mutableStateOf(false) }
    var completedSheetDetails by remember { mutableStateOf<ProcessSheetEntity?>(null) }

    var currentBatchSeq by remember(completedCountForProcess) {
        mutableStateOf(completedCountForProcess + 1)
    }

    var isSubmitted by remember { mutableStateOf(false) }
    var submittedSaudiTime by remember { mutableStateOf("") }

    // Form states - persistent across operations; only removed manually
    var productName by remember { mutableStateOf("Chicken Breast Fillet") }
    var productCode by remember { mutableStateOf("PRD-CBF-01") }
    var brand by remember { mutableStateOf("Aldella Premium") }
    var chickenProduct by remember { mutableStateOf("Fresh Defrosted Poultry") }
    var batchNo by remember(currentBatchSeq) { mutableStateOf("Batch $currentBatchSeq") }
    var batchDate by remember { mutableStateOf("18-09-2026") }

    var totalKgQty by remember { mutableStateOf("1250.0") }
    var inputKg by remember { mutableStateOf("1300.0") }
    var outputKg by remember { mutableStateOf("1250.0") }
    var productWasteKg by remember { mutableStateOf("25.0") }
    var waterWasteLitres by remember { mutableStateOf("50.0") }
    var temperatureC by remember { mutableStateOf("3.8") }
    var wasteType by remember { mutableStateOf("Product / Waste") }
    var remarks by remember { mutableStateOf("Standard continuous automated monitoring active.") }

    var copy1Attached by remember { mutableStateOf(false) }
    var copy2Attached by remember { mutableStateOf(false) }

    // Sync state when runningSheet from Room is detected (re-opened after app close)
    LaunchedEffect(runningSheet) {
        if (runningSheet != null) {
            activeSheetId = runningSheet.id
            activeStartTimestamp = runningSheet.timestamp
            persistentStartTime = runningSheet.startTime
            isProcessActive = true
            productName = runningSheet.productName
            productCode = runningSheet.productCode
            brand = runningSheet.brand
            chickenProduct = runningSheet.chickenProduct
            batchNo = runningSheet.batchNo
            batchDate = runningSheet.date
            inputKg = runningSheet.inputKg.toString()
            if (runningSheet.outputKg > 0.0) outputKg = runningSheet.outputKg.toString()
            if (runningSheet.totalKgQty > 0.0) totalKgQty = runningSheet.totalKgQty.toString()
            temperatureC = runningSheet.temperatureC.toString()
            wasteType = runningSheet.wasteType
            remarks = runningSheet.remarks
            currentBatchSeq = runningSheet.batchSequence
            val elapsed = (System.currentTimeMillis() - runningSheet.timestamp) / 1000L
            localElapsedSeconds = elapsed.coerceAtLeast(0L)
        }
    }

    // Live continuous ticker while process is running - strictly isolated per process
    LaunchedEffect(isProcessActive, runningSheet, activeStartTimestamp) {
        while ((isProcessActive || runningSheet != null) && !isSubmitted) {
            val startTs = runningSheet?.timestamp ?: if (activeStartTimestamp > 0L) activeStartTimestamp else System.currentTimeMillis()
            val elapsed = (System.currentTimeMillis() - startTs) / 1000L
            localElapsedSeconds = elapsed.coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val isRunning = isProcessActive || runningSheet != null
    val effectiveStartTime = remember(runningSheet, persistentStartTime, completedSheetDetails) {
        when {
            completedSheetDetails != null && completedSheetDetails?.startTime != "--" && completedSheetDetails?.startTime?.isNotBlank() == true -> completedSheetDetails!!.startTime
            runningSheet != null && runningSheet.startTime != "--" && runningSheet.startTime.isNotBlank() -> runningSheet.startTime
            persistentStartTime != "--" && persistentStartTime.isNotBlank() -> persistentStartTime
            else -> "--"
        }
    }
    val effectiveEndTime = remember(persistentEndTime, runningSheet, completedSheetDetails) {
        when {
            completedSheetDetails != null && completedSheetDetails?.endTime != "--" && completedSheetDetails?.endTime?.isNotBlank() == true -> completedSheetDetails!!.endTime
            persistentEndTime != "--" && persistentEndTime.isNotBlank() -> persistentEndTime
            runningSheet != null && runningSheet.endTime != "--" && runningSheet.endTime.isNotBlank() -> runningSheet.endTime
            else -> "--"
        }
    }
    val effectiveTimer = remember(isRunning, runningSheet, localElapsedSeconds, isSubmitted, completedSheetDetails) {
        when {
            completedSheetDetails != null -> completedSheetDetails!!.totalHours
            isSubmitted && completedSheetDetails != null -> completedSheetDetails!!.totalHours
            isRunning -> viewModel.formatSecondsToTime(localElapsedSeconds)
            else -> "00:00:00"
        }
    }

    // Additional dynamic rows added
    var extraRowsCount by remember { mutableStateOf(0) }

    // Column Long-Press Dialog State for Admin
    var selectedColumnHeader by remember { mutableStateOf<String?>(null) }
    var selectedCustomFieldEntity by remember { mutableStateOf<CustomTemplateFieldEntity?>(null) }
    var showColumnDialog by remember { mutableStateOf(false) }
    var editColumnNameInput by remember { mutableStateOf("") }
    var showAddColumnDialog by remember { mutableStateOf(false) }
    var newColumnNameInput by remember { mutableStateOf("") }

    // Admin Attribute Manager (Product, Brand, Batch No) Dialog
    var attributeManagerMode by remember { mutableStateOf<AttributeMode?>(null) }
    var showDeleteSheetDialog by remember { mutableStateOf(false) }

    if (attributeManagerMode != null) {
        AdminAttributeManagerDialog(
            mode = attributeManagerMode!!,
            currentValue = when (attributeManagerMode!!) {
                AttributeMode.PRODUCT -> productName
                AttributeMode.BRAND -> brand
                AttributeMode.BATCH_NO -> batchNo
            },
            viewModel = viewModel,
            onSelect = { name, code ->
                when (attributeManagerMode) {
                    AttributeMode.PRODUCT -> {
                        productName = name
                        if (code != null) productCode = code
                    }
                    AttributeMode.BRAND -> {
                        brand = name
                    }
                    AttributeMode.BATCH_NO -> {
                        batchNo = name
                    }
                    null -> {}
                }
            },
            onDismiss = { attributeManagerMode = null }
        )
    }

    if (showDeleteSheetDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteSheetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AldellaRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Batch Record Permanently?", fontWeight = FontWeight.Bold, color = AldellaRed)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to permanently delete this batch record ($batchNo • $productName) from the database? This cannot be undone.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val matchingSheet = allProcessSheets.find {
                            it.areaKey == areaName && it.processKey == processName && it.batchNo == batchNo
                        }
                        if (matchingSheet != null) {
                            viewModel.deleteProcessSheetPermanently(matchingSheet)
                        }
                        Toast.makeText(context, "Batch $batchNo permanently deleted!", Toast.LENGTH_SHORT).show()
                        showDeleteSheetDialog = false
                        isSubmitted = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaRed)
                ) {
                    Text("Delete Permanent", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteSheetDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Manual Clear Confirmation Dialog:
    // "No remove any inputs inside before / Only remove manually"
    if (showManualClearDialog) {
        AlertDialog(
            onDismissRequest = { showManualClearDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = AldellaRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Inputs Manually?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    text = "Are you sure you want to clear input fields manually? By factory rule, inputs are never removed automatically to protect operational data.",
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        productName = ""
                        productCode = ""
                        brand = ""
                        chickenProduct = ""
                        inputKg = ""
                        outputKg = ""
                        totalKgQty = ""
                        productWasteKg = ""
                        waterWasteLitres = ""
                        temperatureC = ""
                        remarks = ""
                        showManualClearDialog = false
                        Toast.makeText(context, "Inputs cleared manually.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaRed)
                ) {
                    Text("Clear Manually", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualClearDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Status Badge
    val statusBadge = when {
        isSubmitted -> "COMPLETED"
        isRunning -> "RUNNING"
        effectiveEndTime != "--" -> "RECORDED"
        else -> "INPUT READY"
    }

    // Admin Column Modification Dialog (Triggered on Long-Press by Admin)
    if (showColumnDialog && selectedColumnHeader != null) {
        AlertDialog(
            onDismissRequest = { showColumnDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tune, contentDescription = null, tint = AldellaBluePrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Admin Column Control", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Header: \"${selectedColumnHeader}\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AldellaTextDark
                    )
                    Text(
                        text = "Modify, rename, or delete this column. Changes update automatically for all operators in seconds.",
                        fontSize = 11.sp,
                        color = AldellaTextMuted
                    )

                    OutlinedTextField(
                        value = editColumnNameInput,
                        onValueChange = { editColumnNameInput = it },
                        label = { Text("Rename Column Header") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("admin_rename_column_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fieldEntity = selectedCustomFieldEntity
                        if (fieldEntity != null && editColumnNameInput.isNotBlank()) {
                            viewModel.updateProcessColumnHeader(fieldEntity, editColumnNameInput)
                            Toast.makeText(context, "Column renamed to \"$editColumnNameInput\"", Toast.LENGTH_SHORT).show()
                        } else if (editColumnNameInput.isNotBlank()) {
                            // Convert standard column to custom template field
                            viewModel.addProcessColumnHeader(areaName, processName, editColumnNameInput)
                            Toast.makeText(context, "Column updated & saved to template!", Toast.LENGTH_SHORT).show()
                        }
                        showColumnDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val fieldEntity = selectedCustomFieldEntity
                    if (fieldEntity != null) {
                        TextButton(
                            onClick = {
                                viewModel.removeProcessColumnHeader(fieldEntity)
                                Toast.makeText(context, "Column removed", Toast.LENGTH_SHORT).show()
                                showColumnDialog = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = AldellaRed)
                        ) {
                            Text("Delete Column")
                        }
                    }
                    TextButton(onClick = { showColumnDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Add Column Dialog
    if (showAddColumnDialog) {
        AlertDialog(
            onDismissRequest = { showAddColumnDialog = false },
            title = { Text("Add Process Column Header", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Once added, operators see this new column automatically in seconds.",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )
                    OutlinedTextField(
                        value = newColumnNameInput,
                        onValueChange = { newColumnNameInput = it },
                        label = { Text("Column Header Name") },
                        placeholder = { Text("e.g. Marinade pH, Spiral Temp, Salinity %") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("add_column_name_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newColumnNameInput.isNotBlank()) {
                            viewModel.addProcessColumnHeader(areaName, processName, newColumnNameInput)
                            Toast.makeText(context, "Column \"$newColumnNameInput\" added live!", Toast.LENGTH_SHORT).show()
                            newColumnNameInput = ""
                            showAddColumnDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary)
                ) {
                    Text("Add Column")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddColumnDialog = false }) { Text("Cancel") }
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
            title = "$areaName • $processName",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Bar
            Surface(
                color = AldellaNavySurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LIVE PROCESS SHEET",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = AldellaGoldAccent,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "${viewModel.getBatchOrdinal(currentBatchSeq)} Batch",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AldellaNavyDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "$processName ($areaName)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (areaName.contains("Packing", true)) {
                            OutlinedButton(
                                onClick = {
                                    val browserIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://docs.google.com/spreadsheets")
                                    )
                                    context.startActivity(browserIntent)
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Text("Google Sheet", fontSize = 11.sp)
                            }
                        }

                        Surface(
                            color = if (isAdmin) AldellaGoldAccent else AldellaBluePrimary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = currentUser.role,
                                color = if (isAdmin) AldellaNavyDark else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Lock Banner (Operators and Supervisors cannot modify or delete submitted batches)
            AnimatedVisibility(visible = isSubmitted) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOCKED BATCH: ${viewModel.getBatchOrdinal(currentBatchSeq)} Batch Finished",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Submitted at $submittedSaudiTime (Exact Saudi Time). Total Hours: ${completedSheetDetails?.totalHours ?: effectiveTimer}. By security policy, operators and supervisors cannot modify or delete submitted batches. Tap 'New Sheet' to start the ${viewModel.getBatchOrdinal(currentBatchSeq + 1)} Batch.",
                            fontSize = 11.sp,
                            color = Color(0xFF78350F)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Touch New Sheet -> Consider as 2nd batch for admin in that particular process
                        Button(
                            onClick = {
                                currentBatchSeq += 1
                                isSubmitted = false
                                isProcessActive = false
                                activeSheetId = 0L
                                persistentStartTime = "--"
                                persistentEndTime = "--"
                                completedSheetDetails = null
                                batchNo = "Batch $currentBatchSeq"
                                viewModel.resetOperationalTimer()
                                Toast.makeText(context, "New sheet ready for ${viewModel.getBatchOrdinal(currentBatchSeq)} Batch! Previous inputs preserved.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("touch_new_sheet_btn")
                        ) {
                            Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Touch New Sheet (${viewModel.getBatchOrdinal(currentBatchSeq + 1)} Batch)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Admin Column modification hint
            Surface(
                color = AldellaBlueLight.copy(alpha = 0.6f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Admin Column Tip: Long-press any column header below to modify, rename, or delete it live!",
                        fontSize = 11.sp,
                        color = AldellaTextDark
                    )
                }
            }

            // Main Batch Sheet Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header with Status and Manual Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Batch Details (${viewModel.getBatchOrdinal(currentBatchSeq)} Batch)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = AldellaTextDark
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Recorded by ${currentUser.name}",
                                    fontSize = 11.sp,
                                    color = AldellaTextMuted
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = { showManualClearDialog = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(24.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed)
                                ) {
                                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Manual Clear", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Surface(
                            color = when (statusBadge) {
                                "COMPLETED" -> AldellaGoldAccent
                                "RUNNING" -> AldellaGreenLight
                                else -> Color(0xFFF1F5F9)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusBadge,
                                color = when (statusBadge) {
                                    "COMPLETED" -> AldellaNavyDark
                                    "RUNNING" -> AldellaGreen
                                    else -> AldellaTextMuted
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    val fieldsEnabled = !isSubmitted || isAdmin

                    // Row 1: Product Name & Code
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.5f)) {
                            ColumnHeaderChip(
                                title = "Product Name",
                                isAdmin = isAdmin,
                                onClick = if (isAdmin) { { attributeManagerMode = AttributeMode.PRODUCT } } else null,
                                onLongClick = {
                                    if (isAdmin) {
                                        attributeManagerMode = AttributeMode.PRODUCT
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify product catalog", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = productName,
                                onValueChange = { productName = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = if (isAdmin) {
                                    {
                                        IconButton(onClick = { attributeManagerMode = AttributeMode.PRODUCT }) {
                                            Icon(Icons.Default.Tune, contentDescription = "Manage Products", tint = AldellaBluePrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else null
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Product Code",
                                isAdmin = isAdmin,
                                onClick = if (isAdmin) { { attributeManagerMode = AttributeMode.PRODUCT } } else null,
                                onLongClick = {
                                    if (isAdmin) {
                                        attributeManagerMode = AttributeMode.PRODUCT
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify product code", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = productCode,
                                onValueChange = { productCode = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Row 2: Brand & Chicken Product
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Brand",
                                isAdmin = isAdmin,
                                onClick = if (isAdmin) { { attributeManagerMode = AttributeMode.BRAND } } else null,
                                onLongClick = {
                                    if (isAdmin) {
                                        attributeManagerMode = AttributeMode.BRAND
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify brand", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = brand,
                                onValueChange = { brand = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = if (isAdmin) {
                                    {
                                        IconButton(onClick = { attributeManagerMode = AttributeMode.BRAND }) {
                                            Icon(Icons.Default.Tune, contentDescription = "Manage Brands", tint = AldellaBluePrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else null
                            )
                        }

                        Column(modifier = Modifier.weight(1.2f)) {
                            ColumnHeaderChip(
                                title = "Chicken Product",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Chicken Product"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Chicken Product"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = chickenProduct,
                                onValueChange = { chickenProduct = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Row 3: Batch No & Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Batch No",
                                isAdmin = isAdmin,
                                onClick = if (isAdmin) { { attributeManagerMode = AttributeMode.BATCH_NO } } else null,
                                onLongClick = {
                                    if (isAdmin) {
                                        attributeManagerMode = AttributeMode.BATCH_NO
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify batch", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = batchNo,
                                onValueChange = { batchNo = it },
                                enabled = fieldsEnabled && isAdmin, // Batch number follows sequence
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = if (isAdmin) {
                                    {
                                        IconButton(onClick = { attributeManagerMode = AttributeMode.BATCH_NO }) {
                                            Icon(Icons.Default.Tune, contentDescription = "Manage Batches", tint = AldellaBluePrimary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else null
                            )
                        }

                        Column(modifier = Modifier.weight(1.2f)) {
                            ColumnHeaderChip(
                                title = "Date",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Date"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Date"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = batchDate,
                                onValueChange = { batchDate = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // ================= EXACT SAUDI TIME & OPERATIONAL WORKFLOW =================
                    Surface(
                        color = Color(0xFFF8FAFC),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Timer, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "EXACT SAUDI TIME & OPERATIONAL WORKFLOW",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AldellaBluePrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = effectiveTimer,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRunning) AldellaGreen else AldellaNavyDark
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Start & End exact Saudi Time (IMMUTABLE: persists even after app close & reopen)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("START (Saudi Time)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AldellaTextMuted)
                                        Text(effectiveStartTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AldellaGreen)
                                        Text("Persisted in Room", fontSize = 9.sp, color = AldellaTextMuted)
                                    }
                                }

                                Surface(
                                    color = Color.White,
                                    shape = RoundedCornerShape(8.dp),
                                    border = CardDefaults.outlinedCardBorder(),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("END (Saudi Time)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AldellaTextMuted)
                                        Text(effectiveEndTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AldellaRed)
                                        Text("Directly in Updates", fontSize = 9.sp, color = AldellaTextMuted)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            val hasInputs = productName.isNotBlank() && (inputKg.toDoubleOrNull() ?: 0.0) > 0.0
                            val canGiveEndTime = currentUser.role == "OPERATOR" || currentUser.role == "SUPERVISOR" || currentUser.role == "ADMIN"

                            when {
                                !isRunning && !isSubmitted -> {
                                    if (!hasInputs) {
                                        Surface(
                                            color = AldellaGoldAccent.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Enter Input Kg & Product Name above to enable Start Process.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF78350F)
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (!hasInputs) {
                                                Toast.makeText(context, "Please enter Product Name and Input Kg before starting process", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            val sheet = ProcessSheetEntity(
                                                areaKey = areaName,
                                                processKey = processName,
                                                productName = productName,
                                                productCode = productCode,
                                                brand = brand,
                                                chickenProduct = chickenProduct,
                                                batchNo = batchNo,
                                                date = batchDate,
                                                inputKg = inputKg.toDoubleOrNull() ?: 0.0,
                                                outputKg = outputKg.toDoubleOrNull() ?: 0.0,
                                                totalKgQty = totalKgQty.toDoubleOrNull() ?: (inputKg.toDoubleOrNull() ?: 0.0),
                                                productWasteKg = productWasteKg.toDoubleOrNull() ?: 0.0,
                                                waterWasteLitres = waterWasteLitres.toDoubleOrNull() ?: 0.0,
                                                temperatureC = temperatureC.toDoubleOrNull() ?: 0.0,
                                                wasteType = wasteType,
                                                remarks = remarks,
                                                copy1Attached = copy1Attached,
                                                copy2Attached = copy2Attached,
                                                batchSequence = currentBatchSeq
                                            )
                                            viewModel.startPersistentProcessRun(sheet) { started ->
                                                activeSheetId = started.id
                                                persistentStartTime = started.startTime
                                                isProcessActive = true
                                                Toast.makeText(context, "Process started at ${started.startTime}! Next command: Give End Time.", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        enabled = hasInputs && fieldsEnabled,
                                        colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("start_process_btn")
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (hasInputs) "Start Process (Record Exact Saudi Time)" else "Enter Inputs to Enable Start",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                isRunning -> {
                                    Surface(
                                        color = AldellaGreenLight.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AldellaGreen, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "Process Running • Started at $effectiveStartTime (Saudi Time)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = AldellaNavyDark
                                                )
                                            }
                                            Text(
                                                text = "Persistent: If app closes now, this exact start time and inputs remain saved.",
                                                fontSize = 10.sp,
                                                color = AldellaTextMuted
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "NEXT COMMAND: Give End Time to submit batch directly to Live Updates.",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AldellaBluePrimary
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (!canGiveEndTime) {
                                                Toast.makeText(context, "Only Operator or Supervisor can give end time.", Toast.LENGTH_LONG).show()
                                                return@Button
                                            }
                                            val finalOutput = outputKg.toDoubleOrNull() ?: totalKgQty.toDoubleOrNull() ?: (inputKg.toDoubleOrNull() ?: 0.0)
                                            val sheetToEnd = (runningSheet ?: ProcessSheetEntity(
                                                id = activeSheetId,
                                                areaKey = areaName,
                                                processKey = processName,
                                                startTime = effectiveStartTime,
                                                timestamp = runningSheet?.timestamp ?: System.currentTimeMillis()
                                            )).copy(
                                                productName = productName,
                                                productCode = productCode,
                                                brand = brand,
                                                chickenProduct = chickenProduct,
                                                batchNo = batchNo,
                                                date = batchDate,
                                                inputKg = inputKg.toDoubleOrNull() ?: 0.0,
                                                outputKg = outputKg.toDoubleOrNull() ?: 0.0,
                                                totalKgQty = finalOutput,
                                                productWasteKg = productWasteKg.toDoubleOrNull() ?: 0.0,
                                                waterWasteLitres = waterWasteLitres.toDoubleOrNull() ?: 0.0,
                                                temperatureC = temperatureC.toDoubleOrNull() ?: 0.0,
                                                wasteType = wasteType,
                                                remarks = remarks,
                                                copy1Attached = copy1Attached,
                                                copy2Attached = copy2Attached,
                                                totalHours = effectiveTimer,
                                                batchSequence = currentBatchSeq
                                            )
                                            viewModel.endPersistentProcessRun(sheetToEnd) { completed ->
                                                isProcessActive = false
                                                isSubmitted = true
                                                persistentEndTime = completed.endTime
                                                submittedSaudiTime = completed.submissionSaudiTime
                                                completedSheetDetails = completed
                                                Toast.makeText(
                                                    context,
                                                    "Batch ${completed.batchNo} finished! Put directly in Live Updates with ${completed.totalHours} hrs, ${completed.totalKgQty} kg by ${completed.recordedBy}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        },
                                        enabled = canGiveEndTime,
                                        colors = ButtonDefaults.buttonColors(containerColor = AldellaRed),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(46.dp).testTag("end_process_btn")
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (canGiveEndTime) "End Time & Put Directly in Updates" else "Operator/Supervisor Only End Time",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }

                                isSubmitted -> {
                                    Surface(
                                        color = AldellaGreenLight.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AldellaGreen, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "BATCH FINISHED & DIRECTLY PUT IN UPDATES",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = AldellaNavyDark
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Total Hours: ${completedSheetDetails?.totalHours ?: effectiveTimer} • Total Qty: ${completedSheetDetails?.totalKgQty ?: totalKgQty} kg",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AldellaBluePrimary
                                            )
                                            Text(
                                                text = "Updated by: ${completedSheetDetails?.recordedBy ?: currentUser.name} • Start: ${completedSheetDetails?.startTime ?: effectiveStartTime} • End: ${completedSheetDetails?.endTime ?: effectiveEndTime}",
                                                fontSize = 11.sp,
                                                color = AldellaTextDark
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Total Qty & Total Hours (Calculated Automatically)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Total Kg / Qty",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Total Kg / Qty"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Total Kg / Qty"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = totalKgQty,
                                onValueChange = { totalKgQty = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Total Hours (Automatic)",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    Toast.makeText(context, "Total hours is calculated automatically from exact start & end times.", Toast.LENGTH_SHORT).show()
                                }
                            )
                            OutlinedTextField(
                                value = effectiveTimer,
                                onValueChange = { },
                                readOnly = true,
                                enabled = false,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    HorizontalDivider()

                    // Production Metrics Columns (All Long-Pressable by Admin)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Input Kg",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Input Kg"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Input Kg"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = inputKg,
                                onValueChange = { inputKg = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Output Kg",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Output Kg"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Output Kg"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = outputKg,
                                onValueChange = { outputKg = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Product Waste Kg",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Product Waste Kg"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Product Waste Kg"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = productWasteKg,
                                onValueChange = { productWasteKg = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Water Waste Litres",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Water Waste Litres"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Water Waste Litres"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = waterWasteLitres,
                                onValueChange = { waterWasteLitres = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Temperature & Waste Type
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Temperature °C",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Temperature °C"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Temperature °C"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = temperatureC,
                                onValueChange = { temperatureC = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            ColumnHeaderChip(
                                title = "Waste Type",
                                isAdmin = isAdmin,
                                onLongClick = {
                                    if (isAdmin) {
                                        selectedColumnHeader = "Waste Type"
                                        selectedCustomFieldEntity = null
                                        editColumnNameInput = "Waste Type"
                                        showColumnDialog = true
                                    } else {
                                        Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                            OutlinedTextField(
                                value = wasteType,
                                onValueChange = { wasteType = it },
                                enabled = fieldsEnabled,
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Dynamic Admin Columns (Propagated in seconds!)
                    val relevantCustomFields = customFields.filter {
                        it.targetArea == "ALL" || it.targetArea == areaName || it.processKey == processName
                    }

                    if (relevantCustomFields.isNotEmpty()) {
                        HorizontalDivider()
                        Text("Dynamic Admin Columns", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = AldellaTextDark)
                        relevantCustomFields.forEach { field ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ColumnHeaderChip(
                                    title = field.fieldName,
                                    isAdmin = isAdmin,
                                    isCustom = true,
                                    onLongClick = {
                                        if (isAdmin) {
                                            selectedColumnHeader = field.fieldName
                                            selectedCustomFieldEntity = field
                                            editColumnNameInput = field.fieldName
                                            showColumnDialog = true
                                        } else {
                                            Toast.makeText(context, "Admin permissions required to modify column headers", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                OutlinedTextField(
                                    value = "",
                                    onValueChange = {},
                                    enabled = fieldsEnabled,
                                    placeholder = { Text("Value for ${field.fieldName}") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    // Extra dynamic rows added by user
                    repeat(extraRowsCount) { rowIdx ->
                        Surface(
                            color = Color(0xFFF1F5F9),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Process Line Row #${rowIdx + 2}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("Ready for multi-pallet entry", fontSize = 11.sp, color = AldellaTextMuted)
                            }
                        }
                    }

                    // Remarks
                    ColumnHeaderChip(
                        title = "Remarks",
                        isAdmin = isAdmin,
                        onLongClick = {
                            if (isAdmin) {
                                selectedColumnHeader = "Remarks"
                                selectedCustomFieldEntity = null
                                editColumnNameInput = "Remarks"
                                showColumnDialog = true
                            }
                        }
                    )
                    OutlinedTextField(
                        value = remarks,
                        onValueChange = { remarks = it },
                        enabled = fieldsEnabled,
                        placeholder = { Text("Process remarks, adjustments, lot notes...") },
                        modifier = Modifier.fillMaxWidth().height(76.dp),
                        maxLines = 3
                    )

                    // Admin Header & Row Modification Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                extraRowsCount += 1
                                Toast.makeText(context, "Row line #${extraRowsCount + 1} added", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Row Line", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                if (isAdmin) {
                                    showAddColumnDialog = true
                                } else {
                                    Toast.makeText(context, "Only Plant Administrator can add new column headers", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("add_column_btn")
                        ) {
                            Icon(Icons.Default.ViewColumn, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ Add Column", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // ================= SUBMIT PROCESS BATCH BUTTON =================
                    // "I need once start process automaticly next command to end and sumbit only"
                    // "Only operater or supervisor give end time only. End time give directly put in updates with total hours kg and all details"
                    val hasInputsBottom = productName.isNotBlank() && (inputKg.toDoubleOrNull() ?: 0.0) > 0.0
                    val canGiveEndTimeBottom = currentUser.role == "OPERATOR" || currentUser.role == "SUPERVISOR" || currentUser.role == "ADMIN"

                    if (!isSubmitted) {
                        if (!isRunning) {
                            Button(
                                onClick = {
                                    if (!hasInputsBottom) {
                                        Toast.makeText(context, "Please enter Product Name and Input Kg before starting process", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val sheet = ProcessSheetEntity(
                                        areaKey = areaName,
                                        processKey = processName,
                                        productName = productName,
                                        productCode = productCode,
                                        brand = brand,
                                        chickenProduct = chickenProduct,
                                        batchNo = batchNo,
                                        date = batchDate,
                                        inputKg = inputKg.toDoubleOrNull() ?: 0.0,
                                        outputKg = outputKg.toDoubleOrNull() ?: 0.0,
                                        totalKgQty = totalKgQty.toDoubleOrNull() ?: (inputKg.toDoubleOrNull() ?: 0.0),
                                        productWasteKg = productWasteKg.toDoubleOrNull() ?: 0.0,
                                        waterWasteLitres = waterWasteLitres.toDoubleOrNull() ?: 0.0,
                                        temperatureC = temperatureC.toDoubleOrNull() ?: 0.0,
                                        wasteType = wasteType,
                                        remarks = remarks,
                                        copy1Attached = copy1Attached,
                                        copy2Attached = copy2Attached,
                                        batchSequence = currentBatchSeq
                                    )
                                    viewModel.startPersistentProcessRun(sheet) { started ->
                                        activeSheetId = started.id
                                        activeStartTimestamp = started.timestamp
                                        persistentStartTime = started.startTime
                                        isProcessActive = true
                                        Toast.makeText(context, "${areaName} • ${processName}: Process started at ${started.startTime}! Next command: Give End Time.", Toast.LENGTH_LONG).show()
                                    }
                                },
                                enabled = hasInputsBottom && fieldsEnabled,
                                colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("submit_process_batch_btn")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (hasInputsBottom) "Start Process (${viewModel.getBatchOrdinal(currentBatchSeq)} Batch)" else "Enter Inputs to Enable Start",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (!canGiveEndTimeBottom) {
                                        Toast.makeText(context, "Only Operator or Supervisor can give end time.", Toast.LENGTH_LONG).show()
                                        return@Button
                                    }
                                    val finalOutput = outputKg.toDoubleOrNull() ?: totalKgQty.toDoubleOrNull() ?: (inputKg.toDoubleOrNull() ?: 0.0)
                                    val sheetToEnd = (runningSheet ?: ProcessSheetEntity(
                                        id = activeSheetId,
                                        areaKey = areaName,
                                        processKey = processName,
                                        startTime = effectiveStartTime,
                                        timestamp = runningSheet?.timestamp ?: System.currentTimeMillis()
                                    )).copy(
                                        productName = productName,
                                        productCode = productCode,
                                        brand = brand,
                                        chickenProduct = chickenProduct,
                                        batchNo = batchNo,
                                        date = batchDate,
                                        inputKg = inputKg.toDoubleOrNull() ?: 0.0,
                                        outputKg = outputKg.toDoubleOrNull() ?: 0.0,
                                        totalKgQty = finalOutput,
                                        productWasteKg = productWasteKg.toDoubleOrNull() ?: 0.0,
                                        waterWasteLitres = waterWasteLitres.toDoubleOrNull() ?: 0.0,
                                        temperatureC = temperatureC.toDoubleOrNull() ?: 0.0,
                                        wasteType = wasteType,
                                        remarks = remarks,
                                        copy1Attached = copy1Attached,
                                        copy2Attached = copy2Attached,
                                        totalHours = effectiveTimer,
                                        batchSequence = currentBatchSeq
                                    )
                                    viewModel.endPersistentProcessRun(sheetToEnd) { completed ->
                                        isProcessActive = false
                                        isSubmitted = true
                                        persistentEndTime = completed.endTime
                                        submittedSaudiTime = completed.submissionSaudiTime
                                        completedSheetDetails = completed
                                        Toast.makeText(
                                            context,
                                            "${areaName} • ${processName}: ${completed.batchNo} Finished! Directly put in Updates: ${completed.totalHours} hrs, ${completed.totalKgQty} kg by ${completed.recordedBy}.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                enabled = canGiveEndTimeBottom,
                                colors = ButtonDefaults.buttonColors(containerColor = AldellaRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("end_and_submit_batch_btn")
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (canGiveEndTimeBottom) "End Time & Put Directly in Updates (${viewModel.getBatchOrdinal(currentBatchSeq)} Batch)" else "Operator / Supervisor End Time Only",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else {
                        // Once submitted: Admin can modify or delete; operators/supervisors have read-only view and can start new sheet
                        if (isAdmin) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "Admin modified sheet saved", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Admin Save")
                                }

                                OutlinedButton(
                                    onClick = {
                                        showDeleteSheetDialog = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f).testTag("admin_delete_batch_permanent_btn")
                                ) {
                                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = AldellaRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Permanent", color = AldellaRed)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ColumnHeaderChip(
    title: String,
    isAdmin: Boolean,
    isCustom: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(bottom = 2.dp)
            .clip(RoundedCornerShape(4.dp))
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = onLongClick
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isCustom) AldellaBluePrimary else AldellaTextDark
        )
        if (isAdmin) {
            Spacer(modifier = Modifier.width(3.dp))
            Icon(
                Icons.Default.TouchApp,
                contentDescription = "Admin Touch/Hold",
                tint = if (isCustom) AldellaBluePrimary else AldellaTextMuted,
                modifier = Modifier.size(11.dp)
            )
        }
    }
}
