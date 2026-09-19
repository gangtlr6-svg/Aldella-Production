package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.data.local.entities.ChatMessageEntity
import com.example.ui.AldellaViewModel
import com.example.ui.components.AldellaHeader
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamChatScreen(
    viewModel: AldellaViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val chatMessages by viewModel.activeChatMessages.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var messageInput by remember { mutableStateOf("") }
    var selectedAttachmentType by remember { mutableStateOf("NONE") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AldellaNavyDark)
    ) {
        AldellaHeader(
            viewModel = viewModel,
            title = "Aldella Team Chat",
            onBackClick = onBack
        )

        // Subtitle Banner
        Surface(
            color = AldellaNavySurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Shared operational room. Admin and Supervisor can remove messages from the room; sender, timestamp, and deletion audit history remain protected centrally.",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(chatMessages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    canDelete = currentUser.role == "ADMIN" || currentUser.role == "SUPERVISOR",
                    onDelete = { viewModel.deleteChatMessage(message.id) }
                )
            }
        }

        // Input Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(12.dp)
                .testTag("chat_input_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (selectedAttachmentType != "NONE") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AldellaBlueLight,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Attachment: $selectedAttachmentType attached",
                                color = AldellaBlueBadgeText,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        IconButton(onClick = { selectedAttachmentType = "NONE" }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    placeholder = { Text("Type an operational message...") },
                    modifier = Modifier.fillMaxWidth().testTag("chat_message_input"),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = {
                                selectedAttachmentType = "PHOTO"
                                Toast.makeText(context, "Photo attached", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = "Photo", tint = AldellaBluePrimary)
                        }

                        IconButton(
                            onClick = {
                                selectedAttachmentType = "DOCUMENT"
                                Toast.makeText(context, "Document attached", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = "Document", tint = AldellaBluePrimary)
                        }

                        IconButton(
                            onClick = {
                                selectedAttachmentType = "VOICE"
                                Toast.makeText(context, "Voice note recorded (0:12)", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = AldellaRed)
                        }
                    }

                    Button(
                        onClick = {
                            if (messageInput.isBlank() && selectedAttachmentType == "NONE") return@Button
                            viewModel.sendChatMessage(
                                text = if (messageInput.isBlank()) "[$selectedAttachmentType file transmission]" else messageInput,
                                attachmentType = selectedAttachmentType
                            )
                            messageInput = ""
                            selectedAttachmentType = "NONE"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AldellaBluePrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("send_chat_btn")
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Send", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessageEntity,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = AldellaCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (message.senderRole == "ADMIN") AldellaBlueLight else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = message.senderName.take(1),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (message.senderRole == "ADMIN") AldellaBluePrimary else AldellaTextDark
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message.senderName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AldellaTextDark
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        color = if (message.senderRole == "ADMIN") AldellaBluePrimary else Color(0xFF64748B),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = message.senderRole,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.timestampFormatted,
                        fontSize = 10.sp,
                        color = AldellaTextMuted
                    )
                    if (canDelete) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp).padding(start = 4.dp)) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete",
                                tint = AldellaRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (message.attachmentType == "VOICE") {
                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PlayCircleFilled, contentDescription = "Play", tint = AldellaBluePrimary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Voice Memo (0:12)", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else if (message.attachmentType != "NONE") {
                Surface(
                    color = Color(0xFFF1F5F9),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (message.attachmentType == "PHOTO") Icons.Default.Image else Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = AldellaBluePrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(message.attachmentType, fontSize = 11.sp, color = AldellaTextDark)
                    }
                }
            }

            Text(
                text = message.messageText,
                fontSize = 13.sp,
                color = AldellaTextDark
            )
        }
    }
}
