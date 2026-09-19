package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintDowntimeScreen(
    viewModel: AldellaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allComplaints by viewModel.allComplaintsAndDowntime.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    // Machine Downtime inputs
    val machineOptions = listOf("RF", "Tumbler 1", "Tumbler 2", "Xray", "Injection", "DSI", "Line 1", "Line 2", "Line 3")
    var selectedMachine by remember { mutableStateOf(machineOptions.first()) }
    var downtimeDetails by remember { mutableStateOf("") }
    var downtimeAttachmentType by remember { mutableStateOf("NONE") }
    var downtimeAttachmentName by remember { mutableStateOf("") }

    // Complaint inputs
    val problemAreas = listOf(
        "General", "Breaded", "Tumbler 1", "Tumbler 2", "Marination", "Cutting",
        "RF", "Xray", "Injection", "DSI", "Cooking", "Sample", "Spiral",
        "Secondary Packing", "L1", "L2", "L3"
    )
    var selectedProblemArea by remember { mutableStateOf("Breaded") }
    var complaintDetails by remember { mutableStateOf("") }
    var complaintAttachmentType by remember { mutableStateOf("NONE") }
    var complaintAttachmentName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AldellaNavyDark)
    ) {
        AldellaHeader(
            viewModel = viewModel,
            title = "Complaint & Downtime",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notice Banner
            Surface(
                color = AldellaNavySurface,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Confidential",
                        tint = AldellaBlueBadgeText,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Complaints and Machine Down Time records are sent confidentially to the Admin.",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }

            // Card 1: Machine Down Time
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("machine_downtime_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Machine Down Time",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AldellaTextDark
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "KSA Riyadh timestamps are recorded automatically.",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Machine / line:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AldellaTextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        machineOptions.forEach { machine ->
                            FilterChip(
                                selected = selectedMachine == machine,
                                onClick = { selectedMachine = machine },
                                label = { Text(machine, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = downtimeDetails,
                        onValueChange = { downtimeDetails = it },
                        label = { Text("Downtime reason / details (Optional)") },
                        placeholder = { Text("e.g. Belt slip, emergency stop, motor overheat") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Attach media / files for Admin & Supervisor:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AldellaTextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = downtimeAttachmentType == "IMAGE",
                            onClick = {
                                if (downtimeAttachmentType == "IMAGE") {
                                    downtimeAttachmentType = "NONE"
                                    downtimeAttachmentName = ""
                                } else {
                                    downtimeAttachmentType = "IMAGE"
                                    downtimeAttachmentName = "${selectedMachine}_sensor_defect.jpg"
                                    Toast.makeText(context, "Image attached: $downtimeAttachmentName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Image", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = downtimeAttachmentType == "VIDEO",
                            onClick = {
                                if (downtimeAttachmentType == "VIDEO") {
                                    downtimeAttachmentType = "NONE"
                                    downtimeAttachmentName = ""
                                } else {
                                    downtimeAttachmentType = "VIDEO"
                                    downtimeAttachmentName = "${selectedMachine}_stoppage_recording.mp4"
                                    Toast.makeText(context, "Video attached: $downtimeAttachmentName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Video", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = downtimeAttachmentType == "DOCUMENT",
                            onClick = {
                                if (downtimeAttachmentType == "DOCUMENT") {
                                    downtimeAttachmentType = "NONE"
                                    downtimeAttachmentName = ""
                                } else {
                                    downtimeAttachmentType = "DOCUMENT"
                                    downtimeAttachmentName = "${selectedMachine}_incident_report.pdf"
                                    Toast.makeText(context, "Document attached: $downtimeAttachmentName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Document", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = downtimeAttachmentType == "FILE",
                            onClick = {
                                if (downtimeAttachmentType == "FILE") {
                                    downtimeAttachmentType = "NONE"
                                    downtimeAttachmentName = ""
                                } else {
                                    downtimeAttachmentType = "FILE"
                                    downtimeAttachmentName = "${selectedMachine}_line_telemetry.csv"
                                    Toast.makeText(context, "Data file attached: $downtimeAttachmentName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Log File", fontSize = 11.sp) }
                        )
                    }

                    if (downtimeAttachmentName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = AldellaBlueLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Attached: $downtimeAttachmentName ($downtimeAttachmentType)",
                                    fontSize = 11.sp,
                                    color = AldellaBluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = {
                                        downtimeAttachmentType = "NONE"
                                        downtimeAttachmentName = ""
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = AldellaRed, modifier = Modifier.size(14.dp))
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
                            onClick = {
                                viewModel.reportMachineProblem(
                                    machine = selectedMachine,
                                    details = downtimeDetails,
                                    attachmentType = downtimeAttachmentType,
                                    attachmentName = downtimeAttachmentName
                                )
                                Toast.makeText(context, "Downtime recorded on $selectedMachine (Sent to Admin Notification Bell)", Toast.LENGTH_SHORT).show()
                                downtimeDetails = ""
                                downtimeAttachmentType = "NONE"
                                downtimeAttachmentName = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("machine_problem_btn")
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Machine problem", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.setMachineReady(selectedMachine)
                                Toast.makeText(context, "$selectedMachine certified Ready / All OK", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).testTag("machine_ready_btn")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Machine ready / All OK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Card 2: Complaint Submission
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("complaint_submission_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Complaint submission",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AldellaTextDark
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "Submit starts the complaint timer immediately. Admin will see the start time, resolution end time, and total hours in the notification feed.",
                        fontSize = 12.sp,
                        color = AldellaTextMuted
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Problem area:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AldellaTextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        problemAreas.forEach { area ->
                            FilterChip(
                                selected = selectedProblemArea == area,
                                onClick = { selectedProblemArea = area },
                                label = { Text(area, fontSize = 12.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = complaintDetails,
                        onValueChange = { complaintDetails = it },
                        label = { Text("Complaint details") },
                        placeholder = { Text("Describe the issue...") },
                        modifier = Modifier.fillMaxWidth().height(100.dp).testTag("complaint_details_input"),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Attach media / files for Admin & Supervisor:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AldellaTextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = complaintAttachmentType == "IMAGE",
                            onClick = {
                                if (complaintAttachmentType == "IMAGE") {
                                    complaintAttachmentType = "NONE"
                                    complaintAttachmentName = ""
                                } else {
                                    complaintAttachmentType = "IMAGE"
                                    complaintAttachmentName = "${selectedProblemArea}_photo_evidence.jpg"
                                    Toast.makeText(context, "Image attached: $complaintAttachmentName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Image", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = complaintAttachmentType == "VIDEO",
                            onClick = {
                                if (complaintAttachmentType == "VIDEO") {
                                    complaintAttachmentType = "NONE"
                                    complaintAttachmentName = ""
                                } else {
                                    complaintAttachmentType = "VIDEO"
                                    complaintAttachmentName = "${selectedProblemArea}_issue_clip.mp4"
                                    Toast.makeText(context, "Video attached: $complaintAttachmentName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Video", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = complaintAttachmentType == "DOCUMENT",
                            onClick = {
                                if (complaintAttachmentType == "DOCUMENT") {
                                    complaintAttachmentType = "NONE"
                                    complaintAttachmentName = ""
                                } else {
                                    complaintAttachmentType = "DOCUMENT"
                                    complaintAttachmentName = "${selectedProblemArea}_statement.pdf"
                                    Toast.makeText(context, "Document attached: $complaintAttachmentName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("Document", fontSize = 11.sp) }
                        )

                        FilterChip(
                            selected = complaintAttachmentType == "FILE",
                            onClick = {
                                if (complaintAttachmentType == "FILE") {
                                    complaintAttachmentType = "NONE"
                                    complaintAttachmentName = ""
                                } else {
                                    complaintAttachmentType = "FILE"
                                    complaintAttachmentName = "${selectedProblemArea}_export.xlsx"
                                    Toast.makeText(context, "File attached: $complaintAttachmentName", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(14.dp)) },
                            label = { Text("File / Sheet", fontSize = 11.sp) }
                        )
                    }

                    if (complaintAttachmentName.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            color = AldellaBlueLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Attached: $complaintAttachmentName ($complaintAttachmentType)",
                                    fontSize = 11.sp,
                                    color = AldellaBluePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = {
                                        complaintAttachmentType = "NONE"
                                        complaintAttachmentName = ""
                                    },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = AldellaRed, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (complaintDetails.isBlank()) {
                                Toast.makeText(context, "Please enter complaint details", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.submitComplaint(
                                problemArea = selectedProblemArea,
                                details = complaintDetails,
                                attachmentType = complaintAttachmentType,
                                attachmentName = complaintAttachmentName
                            )
                            Toast.makeText(context, "Complaint sent confidentially to Admin (Alert Bell notified)", Toast.LENGTH_SHORT).show()
                            complaintDetails = ""
                            complaintAttachmentType = "NONE"
                            complaintAttachmentName = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("submit_complaint_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Submit complaint", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Card 3: Admin-only Downtime & Complaint Log
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_downtime_log_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = AldellaBluePrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin-only downtime & complaint log",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = AldellaTextDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (allComplaints.isEmpty()) {
                        Text(
                            text = "No downtime or complaint records.",
                            fontSize = 12.sp,
                            color = AldellaTextMuted
                        )
                    } else {
                        allComplaints.forEach { item ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AldellaBlueLight.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = if (item.type == "DOWNTIME") AldellaAmberLight else AldellaRedLight,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = "${item.type} • ${item.areaOrMachine}",
                                            color = if (item.type == "DOWNTIME") AldellaAmber else AldellaRed,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }

                                    Surface(
                                        color = if (item.status == "OPEN") AldellaRed else AldellaGreen,
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = item.status,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = item.details, fontSize = 12.sp, color = AldellaTextDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Start: ${item.startTimeFormatted} • By ${item.submittedBy}",
                                        fontSize = 11.sp,
                                        color = AldellaTextMuted
                                    )
                                    if (!item.isViewedByAdmin) {
                                        Surface(color = AldellaRedLight, shape = RoundedCornerShape(4.dp)) {
                                            Text(
                                                text = "NEW (Bell Alert)",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = AldellaRed,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Media Attachment View & Download Area for Admin & Supervisor
                                if (item.attachmentName.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        color = Color.White,
                                        shape = RoundedCornerShape(8.dp),
                                        border = CardDefaults.outlinedCardBorder(),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    when (item.attachmentType.uppercase()) {
                                                        "VIDEO" -> Icons.Default.Videocam
                                                        "IMAGE", "PHOTO" -> Icons.Default.Image
                                                        "DOCUMENT", "PDF" -> Icons.Default.PictureAsPdf
                                                        else -> Icons.Default.InsertDriveFile
                                                    },
                                                    contentDescription = null,
                                                    tint = AldellaBluePrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        text = item.attachmentName,
                                                        fontWeight = FontWeight.SemiBold,
                                                        fontSize = 11.sp,
                                                        color = AldellaTextDark
                                                    )
                                                    Text(
                                                        text = "Media Type: ${item.attachmentType}",
                                                        fontSize = 10.sp,
                                                        color = AldellaTextMuted
                                                    )
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    viewModel.openOrDownloadAttachment(context, item.attachmentName, item.attachmentType)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("View / Download", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (!item.isViewedByAdmin) {
                                        TextButton(onClick = { viewModel.markComplaintViewed(item.id) }) {
                                            Text("Mark Read", color = AldellaBluePrimary, fontSize = 11.sp)
                                        }
                                    }
                                    if (item.status == "OPEN") {
                                        TextButton(onClick = { viewModel.resolveComplaintOrDowntime(item) }) {
                                            Text("Resolve", color = AldellaGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    TextButton(onClick = { viewModel.deleteComplaintOrDowntime(item) }) {
                                        Text("Delete", color = AldellaRed, fontSize = 12.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
