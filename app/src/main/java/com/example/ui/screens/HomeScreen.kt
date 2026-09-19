package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.AldellaViewModel
import com.example.ui.components.AldellaHeader
import com.example.ui.components.DailyReportExportDialog
import com.example.ui.components.OnlineUsersDialog
import com.example.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: AldellaViewModel,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()
    val allProcessSheets by viewModel.allProcessSheets.collectAsState()
    val allComplaints by viewModel.allComplaintsAndDowntime.collectAsState()
    val activeUsers by viewModel.activeUserAccounts.collectAsState()
    val unreadCount by viewModel.unreadComplaintsCount.collectAsState()

    val runningCount = allProcessSheets.count { it.status == "RUNNING" }
    val openIssuesCount = allComplaints.count { it.status == "OPEN" }

    var showDailyReportDialog by remember { mutableStateOf(false) }
    var showOnlineUsersDialog by remember { mutableStateOf(false) }

    if (showDailyReportDialog) {
        DailyReportExportDialog(
            viewModel = viewModel,
            onDismiss = { showDailyReportDialog = false }
        )
    }

    if (showOnlineUsersDialog) {
        OnlineUsersDialog(
            viewModel = viewModel,
            onDismiss = { showOnlineUsersDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AldellaNavyDark)
    ) {
        AldellaHeader(viewModel = viewModel)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Session Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_session_card"),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentUser.email,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = AldellaTextDark
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = AldellaBlueLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = currentUser.role,
                                    color = AldellaBlueBadgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Factory operations workspace",
                            fontSize = 12.sp,
                            color = AldellaTextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AldellaBlueLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "User",
                            tint = AldellaBluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Admin Exclusive Live Overview Card (Admin Only)
            if (currentUser.role == "ADMIN") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_exclusive_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = AldellaBluePrimary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "ADMIN EXCLUSIVE",
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Live all-process overview",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AldellaTextDark
                                )
                            }

                            if (openIssuesCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(AldellaRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$openIssuesCount",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick Stats Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBox(
                                label = "Active",
                                value = "$runningCount",
                                color = AldellaGreen,
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = "Down",
                                value = "${allComplaints.count { it.type == "DOWNTIME" && it.status == "OPEN" }}",
                                color = if (openIssuesCount > 0) AldellaRed else AldellaTextMuted,
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                label = "Online users",
                                value = "${activeUsers.size.coerceAtLeast(1)}",
                                color = AldellaBluePrimary,
                                modifier = Modifier.weight(1f).testTag("online_users_stat_box"),
                                onClick = { showOnlineUsersDialog = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Active now: ${currentUser.name}",
                            fontSize = 12.sp,
                            color = AldellaTextMuted,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { onNavigate("live_overview") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_live_view_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Open live view", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }

                            OutlinedButton(
                                onClick = { showDailyReportDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("daily_export_button"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AldellaBluePrimary)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Daily export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Production Operator (Visible to Operator, Supervisor, Admin)
            MenuCard(
                title = "Production Operator",
                subtitle = "Process sheets for all factory lines",
                icon = Icons.Default.PrecisionManufacturing,
                iconBg = Color(0xFFDCFCE7),
                iconTint = Color(0xFF16A34A),
                testTag = "menu_production_operator",
                onClick = { onNavigate("production_operator") }
            )

            // Complaint (Visible to Operator, Supervisor, Admin)
            MenuCard(
                title = "Complaint",
                subtitle = "Confidential issues and Machine Down Time",
                icon = Icons.Default.WarningAmber,
                iconBg = Color(0xFFFEE2E2),
                iconTint = AldellaRed,
                badge = if (openIssuesCount > 0) "$openIssuesCount" else null,
                testTag = "menu_complaint",
                onClick = { onNavigate("complaint") }
            )

            // Team Chat (Visible to Operator, Supervisor, Admin)
            MenuCard(
                title = "Team Chat",
                subtitle = "Text, photos, documents, and voice messages",
                icon = Icons.Default.ChatBubbleOutline,
                iconBg = Color(0xFFF3E8FF),
                iconTint = Color(0xFF9333EA),
                testTag = "menu_team_chat",
                onClick = { onNavigate("team_chat") }
            )

            // Attendance (Visible to Supervisor & Admin; Hidden for Operator)
            if (currentUser.role == "ADMIN" || currentUser.role == "SUPERVISOR") {
                MenuCard(
                    title = "Attendance",
                    subtitle = "Member attendance and check-in records",
                    icon = Icons.Default.AssignmentTurnedIn,
                    iconBg = Color(0xFFE0F2FE),
                    iconTint = Color(0xFF0284C7),
                    testTag = "menu_attendance",
                    onClick = { onNavigate("attendance") }
                )
            }

            // Admin Control Panel (Visible to Admin Only)
            if (currentUser.role == "ADMIN") {
                MenuCard(
                    title = "Admin Control Panel",
                    subtitle = "Online users, bell alerts, approvals, and audits",
                    icon = Icons.Default.AdminPanelSettings,
                    iconBg = Color(0xFFFEF3C7),
                    iconTint = Color(0xFFD97706),
                    badge = if (unreadCount > 0) "$unreadCount" else null,
                    testTag = "menu_admin_panel",
                    onClick = { onNavigate("admin_panel") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatBox(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = if (onClick != null) {
            modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { onClick() }
        } else modifier,
        color = AldellaBlueLight.copy(alpha = 0.5f),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    color = AldellaTextMuted,
                    fontWeight = FontWeight.Medium
                )
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = "Touch to view",
                        tint = AldellaBluePrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    badge: String? = null,
    testTag: String,
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
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AldellaTextDark
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = AldellaRed,
                            shape = CircleShape
                        ) {
                            Text(
                                text = badge,
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = AldellaTextMuted
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
