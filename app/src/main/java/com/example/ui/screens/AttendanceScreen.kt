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
import com.example.data.local.entities.AttendanceEntity
import com.example.ui.AldellaViewModel
import com.example.ui.components.AldellaHeader
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    viewModel: AldellaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allAttendance by viewModel.allAttendance.collectAsState()
    val liveTime by viewModel.currentTimestamp.collectAsState()

    // Form inputs for current row
    var memberId by remember { mutableStateOf("ALD-108") }
    var fullName by remember { mutableStateOf("Mohammed Tariq") }
    var department by remember { mutableStateOf("Production - Defrosting") }
    var shiftType by remember { mutableStateOf("Day") }
    var attendanceDate by remember { mutableStateOf("18-09-2026") }

    var in1 by remember { mutableStateOf("07:00") }
    var out1 by remember { mutableStateOf("12:00") }
    var in2 by remember { mutableStateOf("12:45") }
    var out2 by remember { mutableStateOf("16:00") }
    var in3 by remember { mutableStateOf("") }
    var out3 by remember { mutableStateOf("") }

    var statusRemarks by remember { mutableStateOf("Present on shift. Standard break taken.") }
    var manualOverrideReason by remember { mutableStateOf("Regular verified check-in") }

    // Real-time calculated working hours
    val calculatedHours = remember(in1, out1, in2, out2, in3, out3) {
        viewModel.calculateAttendanceHours(in1, out1, in2, out2, in3, out3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AldellaNavyDark)
    ) {
        AldellaHeader(
            viewModel = viewModel,
            title = "Attendance",
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
                text = "13-column attendance schema. Total working hours are automatically calculated from up to three in/out sessions and synced to Admin.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Row Editor",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                Button(
                    onClick = {
                        memberId = "ALD-${(100..999).random()}"
                        fullName = ""
                        in1 = "07:00"
                        out1 = "12:00"
                        in2 = "12:45"
                        out2 = "16:00"
                        in3 = ""
                        out3 = ""
                        statusRemarks = ""
                        manualOverrideReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add attendance row", fontSize = 12.sp)
                }
            }

            // Member Attendance Entry Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("attendance_form_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AldellaBlueLight,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "MEMBER ROW • ADMIN AUDIT",
                                color = AldellaBlueBadgeText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = "Riyadh Synced",
                            fontSize = 11.sp,
                            color = AldellaTextMuted
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = memberId,
                            onValueChange = { memberId = it },
                            label = { Text("Member ID") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.weight(1.5f),
                            singleLine = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = department,
                            onValueChange = { department = it },
                            label = { Text("Department") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = attendanceDate,
                            onValueChange = { attendanceDate = it },
                            label = { Text("Date (DD-MM-YYYY)") },
                            modifier = Modifier.weight(1.2f),
                            singleLine = true
                        )
                    }

                    // Shift selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Shift: ", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = shiftType == "Day",
                            onClick = { shiftType = "Day" },
                            label = { Text("Day Shift") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FilterChip(
                            selected = shiftType == "Night",
                            onClick = { shiftType = "Night" },
                            label = { Text("Night Shift") }
                        )
                    }

                    Divider()

                    Text(
                        text = "Time Sessions (In / Out format HH:mm)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = AldellaTextDark
                    )

                    // Session 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = in1,
                            onValueChange = { in1 = it },
                            label = { Text("Time In 1") },
                            placeholder = { Text("07:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = out1,
                            onValueChange = { out1 = it },
                            label = { Text("Time Out 1") },
                            placeholder = { Text("12:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Session 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = in2,
                            onValueChange = { in2 = it },
                            label = { Text("Time In 2") },
                            placeholder = { Text("12:45") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = out2,
                            onValueChange = { out2 = it },
                            label = { Text("Time Out 2") },
                            placeholder = { Text("16:00") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Session 3
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = in3,
                            onValueChange = { in3 = it },
                            label = { Text("Time In 3 (Optional)") },
                            placeholder = { Text("16:30") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = out3,
                            onValueChange = { out3 = it },
                            label = { Text("Time Out 3 (Optional)") },
                            placeholder = { Text("19:30") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = statusRemarks,
                        onValueChange = { statusRemarks = it },
                        label = { Text("Status & Remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = manualOverrideReason,
                        onValueChange = { manualOverrideReason = it },
                        label = { Text("Action — Manual Override / Edit") },
                        placeholder = { Text("Reason / Admin instruction") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Total Working Hours Auto Calculation Box
                    Surface(
                        color = AldellaBlueLight,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL WORKING HOURS (AUTO)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AldellaBlueBadgeText
                                )
                                Text(
                                    text = "Calculated automatically from sessions",
                                    fontSize = 11.sp,
                                    color = AldellaTextMuted
                                )
                            }

                            Text(
                                text = calculatedHours,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = AldellaBlueBadgeText
                            )
                        }
                    }

                    // Save / Reset buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (fullName.isBlank() && memberId.isBlank()) {
                                    Toast.makeText(context, "Please enter Member ID or Name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val entity = AttendanceEntity(
                                    memberId = memberId,
                                    fullName = fullName,
                                    department = department,
                                    shiftType = shiftType,
                                    attendanceDate = attendanceDate,
                                    timeIn1 = in1,
                                    timeOut1 = out1,
                                    timeIn2 = in2,
                                    timeOut2 = out2,
                                    timeIn3 = in3,
                                    timeOut3 = out3,
                                    statusRemarks = statusRemarks,
                                    manualOverrideReason = manualOverrideReason,
                                    totalWorkingHours = calculatedHours
                                )
                                viewModel.saveAttendanceRecord(entity)
                                Toast.makeText(context, "Attendance record saved and synced!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.5f).testTag("save_attendance_btn")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Attendance Record", fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                in1 = ""
                                out1 = ""
                                in2 = ""
                                out2 = ""
                                in3 = ""
                                out3 = ""
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Clear Times", fontSize = 12.sp)
                        }
                    }
                }
            }

            // Saved Records List
            Text(
                text = "Saved Attendance Database (${allAttendance.size} records)",
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
                        text = "No saved attendance records yet. Save your first record above.",
                        modifier = Modifier.padding(16.dp),
                        color = AldellaTextMuted,
                        fontSize = 13.sp
                    )
                }
            } else {
                allAttendance.forEach { record ->
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
                                Column {
                                    Text(
                                        text = "${record.fullName} (${record.memberId})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = AldellaTextDark
                                    )
                                    Text(
                                        text = "${record.department} • Shift: ${record.shiftType} • ${record.attendanceDate}",
                                        fontSize = 12.sp,
                                        color = AldellaTextMuted
                                    )
                                }

                                Surface(
                                    color = AldellaGreenLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = record.totalWorkingHours,
                                        color = AldellaGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Sessions: [${record.timeIn1} - ${record.timeOut1}]" +
                                        (if (record.timeIn2.isNotBlank()) " | [${record.timeIn2} - ${record.timeOut2}]" else "") +
                                        (if (record.timeIn3.isNotBlank()) " | [${record.timeIn3} - ${record.timeOut3}]" else ""),
                                fontSize = 12.sp,
                                color = AldellaTextDark
                            )

                            if (record.statusRemarks.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Remarks: ${record.statusRemarks}",
                                    fontSize = 11.sp,
                                    color = AldellaTextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = { viewModel.deleteAttendanceRecord(record) }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = AldellaRed, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete Row", color = AldellaRed, fontSize = 12.sp)
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
