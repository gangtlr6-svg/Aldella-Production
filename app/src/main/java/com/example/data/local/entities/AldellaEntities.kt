package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "attendance_records")
data class AttendanceEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: String = "",
    val fullName: String = "",
    val department: String = "Production",
    val shiftType: String = "Day", // Day / Night
    val attendanceDate: String = "", // DD-MM-YYYY
    val timeIn1: String = "",
    val timeOut1: String = "",
    val timeIn2: String = "",
    val timeOut2: String = "",
    val timeIn3: String = "",
    val timeOut3: String = "",
    val statusRemarks: String = "",
    val manualOverrideReason: String = "",
    val totalWorkingHours: String = "00:00:00",
    val recordedBy: String = "Aldella Admin",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "complaint_downtime_records")
data class ComplaintDowntimeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String, // "DOWNTIME" or "COMPLAINT"
    val areaOrMachine: String,
    val details: String,
    val status: String = "OPEN", // "OPEN" or "RESOLVED"
    val startTimeFormatted: String,
    val endTimeFormatted: String = "Awaiting Admin resolution",
    val totalHours: String = "00:00:00",
    val submittedBy: String = "MG",
    val updatedBy: String = "",
    val isViewedByAdmin: Boolean = false,
    val attachmentsCount: Int = 0,
    val attachmentType: String = "NONE", // "IMAGE", "VIDEO", "DOCUMENT", "FILE", "NONE"
    val attachmentName: String = "",
    val attachmentUri: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "process_sheets")
data class ProcessSheetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val areaKey: String, // e.g. "Defrosting", "Line 1", etc.
    val processKey: String, // e.g. "RF Process", "Tumbler 1 Process"
    val productName: String = "",
    val productCode: String = "",
    val brand: String = "",
    val chickenProduct: String = "",
    val batchNo: String = "",
    val date: String = "",
    val startTime: String = "--",
    val endTime: String = "--",
    val operationalTimer: String = "00:00:00",
    val status: String = "INPUT READY", // INPUT READY, RUNNING, COMPLETED
    val totalKgQty: Double = 0.0,
    val totalHours: String = "00:00:00",
    val inputKg: Double = 0.0,
    val outputKg: Double = 0.0,
    val productWasteKg: Double = 0.0,
    val waterWasteLitres: Double = 0.0,
    val temperatureC: Double = 0.0,
    val wasteType: String = "Product / Waste",
    val remarks: String = "",
    val copy1Attached: Boolean = false,
    val copy2Attached: Boolean = false,
    val recordedBy: String = "Aldella Admin",
    val batchSequence: Int = 1,
    val isSubmitted: Boolean = false,
    val submissionSaudiTime: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val senderName: String,
    val senderEmail: String,
    val senderRole: String,
    val messageText: String,
    val timestampFormatted: String,
    val attachmentType: String = "NONE", // NONE, PHOTO, DOCUMENT, VOICE
    val isSoftDeleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "admin_audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val actionType: String,
    val category: String,
    val description: String,
    val timestampFormatted: String,
    val performedBy: String = "Aldella Admin",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_accounts")
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val email: String,
    val name: String,
    val role: String, // ADMIN, SUPERVISOR, OPERATOR
    val password: String = "123456",
    val isApproved: Boolean = true,
    val isDeleted: Boolean = false,
    val failedAttempts: Int = 0,
    val lockoutUntil: Long = 0L,
    val lastActiveFormatted: String = "Fri Sep 18 2026",
    val isOnline: Boolean = true,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "custom_template_fields")
data class CustomTemplateFieldEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fieldName: String,
    val fieldType: String = "Text",
    val targetArea: String = "ALL",
    val processKey: String = "ALL",
    val isColumnVisible: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
