package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entities.ComplaintDowntimeEntity
import com.example.data.local.entities.ProcessSheetEntity
import com.example.ui.AldellaViewModel
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AldellaHeader(
    viewModel: AldellaViewModel,
    title: String? = null,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val liveTime by viewModel.currentTimestamp.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val showBroadcast by viewModel.showBroadcastPopup.collectAsState()
    val activeBroadcast by viewModel.activeBroadcast.collectAsState()
    val unreadCount by viewModel.unreadComplaintsCount.collectAsState()
    val allComplaints by viewModel.allComplaintsAndDowntime.collectAsState()
    val allSheets by viewModel.allProcessSheets.collectAsState()

    var showUserSwitchDialog by remember { mutableStateOf(false) }
    var showBellDialog by remember { mutableStateOf(false) }
    var bellTab by remember { mutableIntStateOf(0) }
    var selectedComplaint by remember { mutableStateOf<ComplaintDowntimeEntity?>(null) }
    var selectedBatchSheet by remember { mutableStateOf<ProcessSheetEntity?>(null) }

    // Emergency Broadcast Popup
    if (showBroadcast && activeBroadcast != null) {
        AlertDialog(
            onDismissRequest = { /* Must acknowledge */ },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = AldellaRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "EMERGENCY ANNOUNCEMENT",
                    fontWeight = FontWeight.Bold,
                    color = AldellaRed
                )
            },
            text = {
                Column {
                    Text(
                        text = activeBroadcast ?: "",
                        fontSize = 15.sp,
                        color = AldellaTextDark
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Issued by Aldella Plant Admin. Please acknowledge receipt to proceed.",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.acknowledgeBroadcast() },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaRed)
                ) {
                    Text("Acknowledge", color = Color.White)
                }
            },
            containerColor = Color.White
        )
    }

    // Switch User Dialog
    if (showUserSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showUserSwitchDialog = false },
            title = {
                Text(
                    "Switch User Account",
                    fontWeight = FontWeight.Bold,
                    color = AldellaTextDark
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Current active: ${currentUser.email} (${currentUser.role})",
                        fontSize = 13.sp,
                        color = AldellaTextMuted
                    )
                    Divider()
                    AccountOptionRow(
                        name = "Aldella Admin",
                        email = "admin@aldella.com",
                        role = "ADMIN",
                        isSelected = currentUser.role == "ADMIN"
                    ) {
                        viewModel.switchUser("admin@aldella.com", "ADMIN", "Aldella Admin")
                        showUserSwitchDialog = false
                    }
                    AccountOptionRow(
                        name = "Production Supervisor",
                        email = "supervisor@aldella.com",
                        role = "SUPERVISOR",
                        isSelected = currentUser.role == "SUPERVISOR"
                    ) {
                        viewModel.switchUser("supervisor@aldella.com", "SUPERVISOR", "Production Supervisor")
                        showUserSwitchDialog = false
                    }
                    AccountOptionRow(
                        name = "Operator 1",
                        email = "operator1@aldella.com",
                        role = "OPERATOR",
                        isSelected = currentUser.role == "OPERATOR"
                    ) {
                        viewModel.switchUser("operator1@aldella.com", "OPERATOR", "Operator 1")
                        showUserSwitchDialog = false
                    }

                    HorizontalDivider()

                    Button(
                        onClick = {
                            viewModel.logout()
                            showUserSwitchDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().testTag("logout_session_btn")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Out / Lock Screen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUserSwitchDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = Color.White
        )
    }

    // Notification Bell Dialog (For Admin & Supervisor)
    if (showBellDialog) {
        val finishedBatches = allSheets.filter { it.isSubmitted || it.status == "COMPLETED" }
        val mediaRecords = allComplaints.filter { it.attachmentsCount > 0 || it.attachmentName.isNotBlank() }

        AlertDialog(
            onDismissRequest = { showBellDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = AldellaBluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Operation Alerts", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AldellaTextDark)
                    }
                    if (unreadCount > 0) {
                        Surface(color = AldellaRed, shape = RoundedCornerShape(12.dp)) {
                            Text(
                                text = "$unreadCount NEW",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 480.dp)
                ) {
                    TabRow(
                        selectedTabIndex = bellTab,
                        containerColor = Color(0xFFF1F5F9),
                        contentColor = AldellaBluePrimary
                    ) {
                        Tab(
                            selected = bellTab == 0,
                            onClick = { bellTab = 0 },
                            text = { Text("Complaints (${allComplaints.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = bellTab == 1,
                            onClick = { bellTab = 1 },
                            text = { Text("Batches (${finishedBatches.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = bellTab == 2,
                            onClick = { bellTab = 2 },
                            text = { Text("Media (${mediaRecords.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (bellTab) {
                        0 -> {
                            if (allComplaints.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No complaints or downtime records.", color = AldellaTextMuted, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(allComplaints) { item ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.markComplaintViewed(item.id)
                                                    selectedComplaint = item
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (!item.isViewedByAdmin) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "[${item.type}] ${item.areaOrMachine}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = if (item.type == "DOWNTIME") AldellaRed else AldellaBluePrimary
                                                    )
                                                    Surface(
                                                        color = if (item.status == "OPEN") AldellaRed else AldellaGreen,
                                                        shape = RoundedCornerShape(6.dp)
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

                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(item.details, fontSize = 12.sp, color = AldellaTextDark, maxLines = 2, overflow = TextOverflow.Ellipsis)

                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("By: ${item.submittedBy}", fontSize = 10.sp, color = AldellaTextMuted, fontWeight = FontWeight.Medium)
                                                    Text("Total: ${item.totalHours}", fontSize = 10.sp, color = AldellaBluePrimary, fontWeight = FontWeight.Bold)
                                                }

                                                if (item.attachmentName.isNotBlank()) {
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Surface(
                                                        color = Color(0xFFE2E8F0),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(12.dp), tint = AldellaBluePrimary)
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = "${item.attachmentName} (${item.attachmentType})",
                                                                fontSize = 10.sp,
                                                                color = AldellaBluePrimary,
                                                                fontWeight = FontWeight.SemiBold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (finishedBatches.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No finished batches yet.", color = AldellaTextMuted, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(finishedBatches) { sheet ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedBatchSheet = sheet },
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "${sheet.areaKey} • ${sheet.batchNo}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = AldellaBluePrimary
                                                    )
                                                    Surface(color = AldellaGreen, shape = RoundedCornerShape(6.dp)) {
                                                        Text("FINISHED", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Text("Product: ${sheet.productName} (${sheet.chickenProduct})", fontSize = 12.sp, color = AldellaTextDark)
                                                Spacer(modifier = Modifier.height(3.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text("Operator: ${sheet.recordedBy}", fontSize = 10.sp, color = AldellaTextMuted)
                                                    Text("Total: ${sheet.totalHours}", fontSize = 10.sp, color = AldellaBluePrimary, fontWeight = FontWeight.Bold)
                                                }
                                                Text("Tap to inspect exact operator inputs", fontSize = 10.sp, color = AldellaBlueBadgeText, fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (mediaRecords.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No operator media or document updates.", color = AldellaTextMuted, fontSize = 13.sp)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(mediaRecords) { record ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            when (record.attachmentType.uppercase()) {
                                                                "VIDEO" -> Icons.Default.Videocam
                                                                "IMAGE", "PHOTO" -> Icons.Default.Image
                                                                "DOCUMENT", "PDF" -> Icons.Default.PictureAsPdf
                                                                else -> Icons.Default.InsertDriveFile
                                                            },
                                                            contentDescription = null,
                                                            tint = AldellaBluePrimary,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(record.attachmentName, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AldellaTextDark)
                                                    }
                                                    Surface(color = AldellaBlueLight, shape = RoundedCornerShape(6.dp)) {
                                                        Text(record.attachmentType, color = AldellaBluePrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text("Line/Machine: ${record.areaOrMachine} • By: ${record.submittedBy}", fontSize = 11.sp, color = AldellaTextMuted)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.openOrDownloadAttachment(context, record.attachmentName, record.attachmentType)
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("View / Open / Download", fontSize = 10.sp)
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
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (unreadCount > 0) {
                        Button(
                            onClick = {
                                viewModel.markAllComplaintsViewed()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Mark all read", fontSize = 11.sp)
                        }
                    }
                    TextButton(onClick = { showBellDialog = false }) {
                        Text("Close")
                    }
                }
            },
            containerColor = Color.White
        )
    }

    // Detail Dialog: Finished Batch Full Operator Input Modal
    selectedBatchSheet?.let { sheet ->
        AlertDialog(
            onDismissRequest = { selectedBatchSheet = null },
            title = {
                Column {
                    Text("Batch Finished Details", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AldellaBluePrimary)
                    Text("Operator Submission Record", fontSize = 11.sp, color = AldellaTextMuted)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DetailRow("Factory Area:", sheet.areaKey)
                    DetailRow("Process Line:", sheet.processKey)
                    DetailRow("Batch Number:", "${sheet.batchSequence} • ${sheet.batchNo}")
                    DetailRow("Product Name:", sheet.productName)
                    DetailRow("Product Code:", sheet.productCode)
                    DetailRow("Chicken Spec:", sheet.chickenProduct)
                    DetailRow("Start Time:", sheet.startTime)
                    DetailRow("End Time:", sheet.endTime)
                    DetailRow("Total Hours:", sheet.totalHours)
                    DetailRow("Input Quantity:", "${sheet.inputKg} KG")
                    DetailRow("Output Yield:", "${sheet.outputKg} KG")
                    DetailRow("Waste Loss:", "${sheet.productWasteKg} KG")
                    DetailRow("Water Waste:", "${sheet.waterWasteLitres} L")
                    DetailRow("Core Temp:", "${sheet.temperatureC} °C")
                    DetailRow("Recorded By:", sheet.recordedBy)
                    DetailRow("Saudi Submission:", sheet.submissionSaudiTime)
                    if (sheet.remarks.isNotBlank()) {
                        DetailRow("Operator Remarks:", sheet.remarks)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { selectedBatchSheet = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Close")
                }
            },
            containerColor = Color.White
        )
    }

    // Detail Dialog: Complaint & Downtime Full Details with Attachments & Actions
    selectedComplaint?.let { complaint ->
        AlertDialog(
            onDismissRequest = { selectedComplaint = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("[${complaint.type}] ${complaint.areaOrMachine}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AldellaTextDark)
                    Surface(color = if (complaint.status == "OPEN") AldellaRed else AldellaGreen, shape = RoundedCornerShape(6.dp)) {
                        Text(complaint.status, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Description / Input:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AldellaTextDark)
                    Surface(color = Color(0xFFF1F5F9), shape = RoundedCornerShape(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(complaint.details, fontSize = 12.sp, modifier = Modifier.padding(8.dp), color = AldellaTextDark)
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    DetailRow("Start Time:", complaint.startTimeFormatted)
                    DetailRow("End Time:", complaint.endTimeFormatted)
                    DetailRow("Total Hours:", complaint.totalHours)
                    DetailRow("Submitted By:", complaint.submittedBy)
                    DetailRow("Updated By:", complaint.updatedBy.ifBlank { "Pending resolution" })

                    if (complaint.attachmentName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Uploaded File/Media:", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AldellaTextDark)
                        Button(
                            onClick = {
                                viewModel.openOrDownloadAttachment(context, complaint.attachmentName, complaint.attachmentType)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("View / Download ${complaint.attachmentName}", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (complaint.status == "OPEN" && (currentUser.role == "ADMIN" || currentUser.role == "SUPERVISOR")) {
                        Button(
                            onClick = {
                                viewModel.resolveComplaintOrDowntime(complaint)
                                selectedComplaint = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Mark Resolved", fontSize = 11.sp)
                        }
                    }
                    TextButton(onClick = { selectedComplaint = null }) {
                        Text("Close")
                    }
                }
            },
            containerColor = Color.White
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AldellaBlueHeader)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (onBackClick != null) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_aldella_logo_1789760741584),
                            contentDescription = "Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(6.dp))
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column {
                    Text(
                        text = title ?: "Aldella",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (title == null) "COMPANY PRODUCTION" else "FACTORY OPERATIONS",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        letterSpacing = 1.2.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (currentUser.role == "ADMIN" || currentUser.role == "SUPERVISOR") {
                    IconButton(
                        onClick = { showBellDialog = true },
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("notification_bell_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = AldellaRed,
                                        contentColor = Color.White
                                    ) {
                                        Text("$unreadCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = "Alerts Bell",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showUserSwitchDialog = true },
                    modifier = Modifier.size(40.dp).testTag("user_switch_btn")
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "User Switch",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
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
                text = "Welcome ${currentUser.name.split(" ").firstOrNull() ?: "User"}",
                color = Color.White.copy(alpha = 0.95f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = liveTime.ifBlank { "Syncing KSA time..." },
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = AldellaTextMuted,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.widthIn(min = 110.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            color = AldellaTextDark,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AccountOptionRow(
    name: String,
    email: String,
    role: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) AldellaBlueLight else Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Text(email, fontSize = 12.sp, color = AldellaTextMuted)
            }
            Surface(
                color = if (role == "ADMIN") AldellaBluePrimary else Color(0xFF64748B),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = role,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}
