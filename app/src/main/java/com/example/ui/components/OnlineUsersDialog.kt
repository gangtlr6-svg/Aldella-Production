package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.data.local.entities.UserAccountEntity
import com.example.ui.AldellaViewModel
import com.example.ui.theme.*

@Composable
fun OnlineUsersDialog(
    viewModel: AldellaViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val currentTimestamp by viewModel.currentTimestamp.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeUsers by viewModel.activeUserAccounts.collectAsState()

    // Ensure current user is visible in list
    val allDisplayUsers = remember(activeUsers, currentUser) {
        val list = activeUsers.toMutableList()
        if (list.none { it.email.equals(currentUser.email, ignoreCase = true) }) {
            list.add(
                0,
                UserAccountEntity(
                    email = currentUser.email,
                    name = currentUser.name,
                    role = currentUser.role,
                    isOnline = true,
                    lastActiveFormatted = "Active now in workspace",
                    lastActiveTimestamp = System.currentTimeMillis()
                )
            )
        }
        list
    }

    val onlineCount = allDisplayUsers.count { it.isOnline }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(AldellaGreen, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Online Factory Users",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = AldellaTextDark
                        )
                        Text(
                            text = "$onlineCount Active • Live Factory Session",
                            fontSize = 11.sp,
                            color = AldellaGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AldellaBlueLight
                ) {
                    Text(
                        text = "LIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AldellaBluePrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
            ) {
                // Automatic Time Banner
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = AldellaBluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Exact Saudi Arabia Time: $currentTimestamp",
                            fontSize = 11.sp,
                            color = AldellaNavyDark,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    allDisplayUsers.forEach { user ->
                        val isSelf = user.email.equals(currentUser.email, ignoreCase = true)
                        val roleColor = when (user.role.uppercase()) {
                            "ADMIN" -> Color(0xFFD97706)
                            "SUPERVISOR" -> AldellaBluePrimary
                            else -> AldellaGreen
                        }

                        val station = when (user.role.uppercase()) {
                            "ADMIN" -> "Command Center & Master Controls"
                            "SUPERVISOR" -> "Floor Oversight & Quality Inspection"
                            else -> "Line 1 & Defrosting Station"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelf) Color(0xFFEFF6FF) else Color(0xFFF8FAFC)
                            ),
                            border = CardDefaults.outlinedCardBorder()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(roleColor.copy(alpha = 0.15f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = user.name.take(1).uppercase(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = roleColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = user.name,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = AldellaTextDark
                                                )
                                                if (isSelf) {
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Surface(
                                                        color = AldellaBluePrimary,
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "YOU",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = user.email,
                                                fontSize = 11.sp,
                                                color = AldellaTextMuted
                                            )
                                        }
                                    }

                                    Surface(
                                        color = roleColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = user.role,
                                            color = roleColor,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(if (user.isOnline) AldellaGreen else Color(0xFF94A3B8), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (user.isOnline) "Active Now" else "Inactive",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = if (user.isOnline) AldellaGreen else Color(0xFF64748B)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "• ${station}",
                                            fontSize = 11.sp,
                                            color = AldellaTextMuted
                                        )
                                    }

                                    if (!isSelf) {
                                        TextButton(
                                            onClick = {
                                                viewModel.switchUser(user.email, user.role, user.name)
                                                Toast.makeText(context, "Switched to ${user.name} (${user.role})", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Switch", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = Color.White
    )
}
