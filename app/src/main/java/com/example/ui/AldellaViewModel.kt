package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.data.repository.AldellaRepository
import com.example.data.firebase.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class ReportFilterCriteria(
    val year: String = "ALL", // "ALL" or "2024".."2056" (Next 30 Years)
    val month: String = "ALL", // "ALL" or "01".."12"
    val day: String = "ALL", // "ALL" or "01".."31"
    val areaKey: String = "ALL", // "ALL" or specific Area
    val status: String = "ALL" // "ALL", "COMPLETED", "RUNNING", "OPEN", "RESOLVED"
)

data class UserSession(
    val email: String,
    val name: String,
    val role: String // "ADMIN", "SUPERVISOR", "OPERATOR"
)

data class ProcessAreaConfig(
    val name: String,
    val iconName: String,
    val processes: List<String>
)

data class ProductCatalogItem(
    val name: String,
    val code: String
)

data class BrandCatalogItem(
    val name: String
)

data class BatchCatalogItem(
    val name: String
)

class AldellaViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AldellaRepository
    private val prefs = application.getSharedPreferences("aldella_session_prefs", Context.MODE_PRIVATE)
    private var clockJob: Job? = null
    private var heartbeatJob: Job? = null
    private var timerJob: Job? = null

    // Live Riyadh time: dd-MM-yyyy HH:mm:ss
    private val _currentTimestamp = MutableStateFlow("")
    val currentTimestamp: StateFlow<String> = _currentTimestamp.asStateFlow()

    // Current logged-in user
    private val _currentUser = MutableStateFlow(
        UserSession("admin@aldella.com", "Aldella Admin", "ADMIN")
    )
    val currentUser: StateFlow<UserSession> = _currentUser.asStateFlow()

    // Auth status
    private val _isUserLoggedIn = MutableStateFlow(false)
    val isUserLoggedIn: StateFlow<Boolean> = _isUserLoggedIn.asStateFlow()

    // Emergency announcement
    private val _activeBroadcast = MutableStateFlow<String?>(null)
    val activeBroadcast: StateFlow<String?> = _activeBroadcast.asStateFlow()

    private val _showBroadcastPopup = MutableStateFlow(false)
    val showBroadcastPopup: StateFlow<Boolean> = _showBroadcastPopup.asStateFlow()

    // Active operational timer for process sheet
    private val _operationalTimerSeconds = MutableStateFlow(0L)
    val operationalTimerSeconds: StateFlow<Long> = _operationalTimerSeconds.asStateFlow()
    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()
    private val _timerStartTime = MutableStateFlow("--")
    val timerStartTime: StateFlow<String> = _timerStartTime.asStateFlow()
    private val _timerEndTime = MutableStateFlow("--")
    val timerEndTime: StateFlow<String> = _timerEndTime.asStateFlow()

    // Process Areas Configuration (modifiable by Admin)
    private val _processAreas = MutableStateFlow(
        listOf(
            ProcessAreaConfig("RF", "ic_defrost", listOf("RF Process", "Tumbler 1 Process", "Tumbler 2 Process", "Defrosting Process")),
            ProcessAreaConfig("DSI", "ic_dsi", listOf("Cutting Process", "Line 1 Marination Process", "DSI Inspection Process")),
            ProcessAreaConfig("Xray", "ic_xray", listOf("Xray Process", "Xray Inspection Process")),
            ProcessAreaConfig("L1", "ic_line", listOf("Breaded Process", "Cooking Process", "Sample Process", "Spiral Process", "L1 Packing Process")),
            ProcessAreaConfig("L2", "ic_line", listOf("Injection Process", "Marination Process", "Spiral Process", "L2 Packing Process")),
            ProcessAreaConfig("L3", "ic_line", listOf("Line 3 Process", "Freezing Process", "L3 Packing Process")),
            ProcessAreaConfig("Defrosting", "ic_defrost", listOf("RF Process", "Tumbler 1 Process", "Tumbler 2 Process")),
            ProcessAreaConfig("Line 1 Packing", "ic_packing", listOf("Machine Process", "Secondary Packing Process")),
            ProcessAreaConfig("Line 2 Packing", "ic_packing", listOf("Machine Process", "Secondary Packing Process")),
            ProcessAreaConfig("Line 3 Packing", "ic_packing", listOf("Machine Process", "Secondary Packing Process"))
        )
    )
    val processAreas: StateFlow<List<ProcessAreaConfig>> = _processAreas.asStateFlow()

    // Product Master Catalog (Editable by Admin: Add, Rename, Delete Permanent)
    private val _productCatalog = MutableStateFlow<List<ProductCatalogItem>>(
        listOf(
            ProductCatalogItem("Chicken Breast Fillet", "PRD-CBF-01"),
            ProductCatalogItem("Chicken Nuggets", "PRD-NUG-02"),
            ProductCatalogItem("Chicken Popcorn", "PRD-POP-03"),
            ProductCatalogItem("Marinated Wings", "PRD-WNG-04"),
            ProductCatalogItem("Breaded Tenders", "PRD-TND-05"),
            ProductCatalogItem("Fresh Defrosted Poultry", "PRD-FDP-06")
        )
    )
    val productCatalog: StateFlow<List<ProductCatalogItem>> = _productCatalog.asStateFlow()

    // Brand Master Catalog (Editable by Admin: Add, Rename, Delete Permanent)
    private val _brandCatalog = MutableStateFlow<List<BrandCatalogItem>>(
        listOf(
            BrandCatalogItem("Aldella Premium"),
            BrandCatalogItem("Aldella Gold"),
            BrandCatalogItem("Al-Watania Farm"),
            BrandCatalogItem("Chef's Choice"),
            BrandCatalogItem("Private Label Standard")
        )
    )
    val brandCatalog: StateFlow<List<BrandCatalogItem>> = _brandCatalog.asStateFlow()

    // Batch Identifiers & Sequences (Editable by Admin: Add, Rename, Delete Permanent)
    private val _batchCatalog = MutableStateFlow<List<BatchCatalogItem>>(
        listOf(
            BatchCatalogItem("Batch 1"),
            BatchCatalogItem("Batch 2"),
            BatchCatalogItem("Batch 3"),
            BatchCatalogItem("Batch 4"),
            BatchCatalogItem("Batch A-101"),
            BatchCatalogItem("Batch B-202")
        )
    )
    val batchCatalog: StateFlow<List<BatchCatalogItem>> = _batchCatalog.asStateFlow()

    // Live Database Flows
    val allAttendance: StateFlow<List<AttendanceEntity>>
    val allComplaintsAndDowntime: StateFlow<List<ComplaintDowntimeEntity>>
    val unreadComplaintsCount: StateFlow<Int>
    val allProcessSheets: StateFlow<List<ProcessSheetEntity>>
    val activeChatMessages: StateFlow<List<ChatMessageEntity>>
    val allAuditLogs: StateFlow<List<AuditLogEntity>>
    val activeUserAccounts: StateFlow<List<UserAccountEntity>>
    val deletedUserAccounts: StateFlow<List<UserAccountEntity>>
    val customTemplateFields: StateFlow<List<CustomTemplateFieldEntity>>

    // Firebase Firestore Permanent Cloud Storage & Sync State
    private val _isFirestoreAutoSync = MutableStateFlow(FirestoreManager.isAutoSyncEnabled(application))
    val isFirestoreAutoSync: StateFlow<Boolean> = _isFirestoreAutoSync.asStateFlow()

    private val _firestoreSyncStatus = MutableStateFlow("Permanent Cloud Storage Ready")
    val firestoreSyncStatus: StateFlow<String> = _firestoreSyncStatus.asStateFlow()

    private val _isCloudBackupRunning = MutableStateFlow(false)
    val isCloudBackupRunning: StateFlow<Boolean> = _isCloudBackupRunning.asStateFlow()

    private val _isCloudRestoreRunning = MutableStateFlow(false)
    val isCloudRestoreRunning: StateFlow<Boolean> = _isCloudRestoreRunning.asStateFlow()

    private val _firestoreLastSyncTime = MutableStateFlow(FirestoreManager.getLastSyncTime(application))
    val firestoreLastSyncTime: StateFlow<String> = _firestoreLastSyncTime.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = AldellaRepository(database.aldellaDao())

        allAttendance = repository.allAttendance.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allComplaintsAndDowntime = repository.allComplaintsAndDowntime.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        unreadComplaintsCount = allComplaintsAndDowntime.map { list ->
            list.count { !it.isViewedByAdmin && it.status == "OPEN" }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        allProcessSheets = repository.allProcessSheets.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        activeChatMessages = repository.activeChatMessages.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        allAuditLogs = repository.allAuditLogs.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        activeUserAccounts = repository.activeUserAccounts.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        deletedUserAccounts = repository.deletedUserAccounts.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )
        customTemplateFields = repository.customTemplateFields.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
        )

        // Session Restoration: No automatic logout on minimize/close/clear cache
        val savedLoggedIn = prefs.getBoolean("is_logged_in", true)
        val savedEmail = prefs.getString("user_email", "admin@aldella.com") ?: "admin@aldella.com"
        val savedName = prefs.getString("user_name", "Aldella Admin") ?: "Aldella Admin"
        val savedRole = prefs.getString("user_role", "ADMIN") ?: "ADMIN"

        _currentUser.value = UserSession(savedEmail, savedName, savedRole)
        _isUserLoggedIn.value = savedLoggedIn

        startClock()
        startHeartbeat()

        // Initialize Firebase Firestore for permanent cloud storage
        viewModelScope.launch(Dispatchers.IO) {
            try {
                FirestoreManager.getFirestore(application)
            } catch (e: Exception) {
                // Handled gracefully in offline mode
            }
        }
    }

    private fun startClock() {
        clockJob?.cancel()
        clockJob = viewModelScope.launch {
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
            while (true) {
                _currentTimestamp.value = sdf.format(Date())
                delay(1000)
            }
        }
    }

    private fun startHeartbeat() {
        heartbeatJob?.cancel()
        heartbeatJob = viewModelScope.launch {
            while (true) {
                if (_isUserLoggedIn.value) {
                    updateUserOnlineHeartbeat(_currentUser.value.email, isOnline = true)
                }
                delay(30_000)
            }
        }
    }

    fun updateUserOnlineHeartbeat(email: String, isOnline: Boolean) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val formatted = if (isOnline) "Active now" else "Inactive since ${_currentTimestamp.value}"
            repository.updateUserOnlineStatus(email, isOnline, now, formatted)
            if (_isFirestoreAutoSync.value) {
                val user = activeUserAccounts.value.firstOrNull { it.email.equals(email, ignoreCase = true) }
                if (user != null) {
                    val updated = user.copy(isOnline = isOnline, lastActiveTimestamp = now, lastActiveFormatted = formatted)
                    FirestoreManager.syncUserStatus(getApplication(), updated)
                }
            }
        }
    }

    fun formatRelativeActiveTime(lastActiveTimestamp: Long, isOnline: Boolean): Pair<String, Boolean> {
        val diff = (System.currentTimeMillis() - lastActiveTimestamp).coerceAtLeast(0)
        if (isOnline && diff < 3 * 60 * 1000) {
            return Pair("Active now", true)
        }
        val minutes = diff / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        val text = when {
            minutes < 1 -> "Active just now"
            minutes < 60 -> "Inactive ${minutes}m ago"
            hours < 24 -> "Inactive ${hours}h ago"
            else -> "Inactive ${days}d ago"
        }
        return Pair(text, false)
    }

    fun switchUser(email: String, role: String, name: String) {
        val oldEmail = _currentUser.value.email
        updateUserOnlineHeartbeat(oldEmail, isOnline = false)
        _currentUser.value = UserSession(email, name, role)
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_role", role)
            .apply()
        updateUserOnlineHeartbeat(email, isOnline = true)
    }

    // Emergency Broadcast
    fun sendEmergencyBroadcast(message: String) {
        if (message.isBlank()) return
        _activeBroadcast.value = message
        _showBroadcastPopup.value = true
        viewModelScope.launch {
            repository.logAudit(
                AuditLogEntity(
                    actionType = "BROADCAST_EMERGENCY",
                    category = "broadcast",
                    description = "Emergency broadcast issued: \"$message\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun acknowledgeBroadcast() {
        _showBroadcastPopup.value = false
    }

    // Operational Timer Controls
    fun startOperationalTimer() {
        if (_isTimerRunning.value) return
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
        if (_timerStartTime.value == "--") {
            _timerStartTime.value = sdf.format(Date())
        }
        _isTimerRunning.value = true
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_isTimerRunning.value) {
                delay(1000)
                _operationalTimerSeconds.value += 1
            }
        }
    }

    fun endOperationalTimer() {
        if (!_isTimerRunning.value && _operationalTimerSeconds.value == 0L) return
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
        _timerEndTime.value = sdf.format(Date())
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resetOperationalTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _operationalTimerSeconds.value = 0L
        _timerStartTime.value = "--"
        _timerEndTime.value = "--"
    }

    fun formatSecondsToTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, secs)
    }

    // Attendance Hours Calculator
    fun calculateAttendanceHours(
        in1: String, out1: String,
        in2: String, out2: String,
        in3: String, out3: String
    ): String {
        fun diffMinutes(tin: String, tout: String): Long {
            if (tin.isBlank() || tout.isBlank()) return 0
            return try {
                val partsIn = tin.trim().split(":")
                val partsOut = tout.trim().split(":")
                val minIn = partsIn[0].toInt() * 60 + partsIn[1].toInt()
                val minOut = partsOut[0].toInt() * 60 + partsOut[1].toInt()
                if (minOut >= minIn) (minOut - minIn).toLong() else (1440 - minIn + minOut).toLong()
            } catch (e: Exception) {
                0
            }
        }

        val totalMins = diffMinutes(in1, out1) + diffMinutes(in2, out2) + diffMinutes(in3, out3)
        val hrs = totalMins / 60
        val mins = totalMins % 60
        return String.format(Locale.ENGLISH, "%02d:%02d:00", hrs, mins)
    }

    fun saveAttendanceRecord(record: AttendanceEntity) {
        viewModelScope.launch {
            repository.saveAttendance(record)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.saveAttendance(getApplication(), record)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "SAVE_ATTENDANCE",
                    category = "attendance",
                    description = "Attendance logged for ${record.fullName} (${record.memberId})",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun deleteAttendanceRecord(record: AttendanceEntity) {
        viewModelScope.launch {
            repository.deleteAttendance(record)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.deleteAttendance(getApplication(), record.id)
            }
        }
    }

    // Complaints & Downtime Actions
    fun reportMachineProblem(
        machine: String,
        details: String,
        attachmentType: String = "NONE",
        attachmentName: String = "",
        attachmentUri: String = ""
    ) {
        viewModelScope.launch {
            val role = _currentUser.value.role
            val name = _currentUser.value.name
            val record = ComplaintDowntimeEntity(
                type = "DOWNTIME",
                areaOrMachine = machine,
                details = details.ifBlank { "Machine breakdown reported on $machine" },
                status = "OPEN",
                startTimeFormatted = _currentTimestamp.value,
                endTimeFormatted = "Ongoing",
                totalHours = "00:00:00",
                submittedBy = "$name ($role)",
                updatedBy = "$name ($role)",
                isViewedByAdmin = role == "ADMIN",
                attachmentsCount = if (attachmentType != "NONE") 1 else 0,
                attachmentType = attachmentType,
                attachmentName = attachmentName,
                attachmentUri = attachmentUri
            )
            repository.saveComplaintOrDowntime(record)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.saveComplaint(getApplication(), record)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "REPORT_DOWNTIME",
                    category = "downtime",
                    description = "Downtime alert on $machine reported by $name ($role)" +
                            if (attachmentType != "NONE") " [Attachment: $attachmentName ($attachmentType)]" else "",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun setMachineReady(machine: String) {
        viewModelScope.launch {
            val role = _currentUser.value.role
            val name = _currentUser.value.name
            val record = ComplaintDowntimeEntity(
                type = "DOWNTIME",
                areaOrMachine = machine,
                details = "Machine ready / All OK certified by $name ($role)",
                status = "RESOLVED",
                startTimeFormatted = _currentTimestamp.value,
                endTimeFormatted = _currentTimestamp.value,
                totalHours = "00:00:00",
                submittedBy = "$name ($role)",
                updatedBy = "$name ($role)",
                isViewedByAdmin = true
            )
            repository.saveComplaintOrDowntime(record)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.saveComplaint(getApplication(), record)
            }
        }
    }

    fun submitComplaint(
        problemArea: String,
        details: String,
        attachmentType: String = "NONE",
        attachmentName: String = "",
        attachmentUri: String = ""
    ) {
        if (details.isBlank()) return
        viewModelScope.launch {
            val role = _currentUser.value.role
            val name = _currentUser.value.name
            val record = ComplaintDowntimeEntity(
                type = "COMPLAINT",
                areaOrMachine = problemArea,
                details = details,
                status = "OPEN",
                startTimeFormatted = _currentTimestamp.value,
                endTimeFormatted = "Awaiting Admin resolution",
                totalHours = "00:00:00",
                submittedBy = "$name ($role)",
                updatedBy = "$name ($role)",
                isViewedByAdmin = role == "ADMIN",
                attachmentsCount = if (attachmentType != "NONE") 1 else 0,
                attachmentType = attachmentType,
                attachmentName = attachmentName,
                attachmentUri = attachmentUri
            )
            repository.saveComplaintOrDowntime(record)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.saveComplaint(getApplication(), record)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "SUBMIT_COMPLAINT",
                    category = "complaint",
                    description = "Complaint submitted for $problemArea by $name ($role): \"$details\"" +
                            if (attachmentType != "NONE") " [Attachment: $attachmentName ($attachmentType)]" else "",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun resolveComplaintOrDowntime(record: ComplaintDowntimeEntity) {
        viewModelScope.launch {
            val totalH = calculateDuration(record.startTimeFormatted, _currentTimestamp.value)
            val updated = record.copy(
                status = "RESOLVED",
                endTimeFormatted = _currentTimestamp.value,
                totalHours = if (record.totalHours != "00:00:00") record.totalHours else totalH,
                updatedBy = "${_currentUser.value.name} (${_currentUser.value.role})",
                isViewedByAdmin = true
            )
            repository.updateComplaintOrDowntime(updated)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.saveComplaint(getApplication(), updated)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "RESOLVE_COMPLAINT",
                    category = "complaint",
                    description = "Record #${record.id} in ${record.areaOrMachine} resolved by ${_currentUser.value.name} (${_currentUser.value.role})",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun markComplaintViewed(id: Long) {
        viewModelScope.launch {
            repository.markComplaintAsViewed(id)
        }
    }

    fun markAllComplaintsViewed() {
        viewModelScope.launch {
            repository.markAllComplaintsAsViewed()
        }
    }

    fun updateComplaintDetailsAndStatus(record: ComplaintDowntimeEntity, newStatus: String, newDetails: String) {
        viewModelScope.launch {
            val totalH = if (newStatus == "RESOLVED") calculateDuration(record.startTimeFormatted, _currentTimestamp.value) else record.totalHours
            val updated = record.copy(
                status = newStatus,
                details = if (newDetails.isNotBlank()) newDetails else record.details,
                endTimeFormatted = if (newStatus == "RESOLVED") _currentTimestamp.value else record.endTimeFormatted,
                totalHours = totalH,
                updatedBy = "${_currentUser.value.name} (${_currentUser.value.role})",
                isViewedByAdmin = true
            )
            repository.updateComplaintOrDowntime(updated)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "UPDATE_COMPLAINT_DETAILS",
                    category = "complaint",
                    description = "Record #${record.id} updated by ${_currentUser.value.name} (${_currentUser.value.role}) - Status: $newStatus",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun calculateDuration(startStr: String, endStr: String): String {
        return try {
            val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH)
            sdf.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
            val d1 = sdf.parse(startStr) ?: Date()
            val d2 = sdf.parse(endStr) ?: Date()
            val diffMs = (d2.time - d1.time).coerceAtLeast(0)
            val seconds = (diffMs / 1000) % 60
            val minutes = (diffMs / (1000 * 60)) % 60
            val hours = (diffMs / (1000 * 60 * 60))
            String.format(Locale.ENGLISH, "%02d:%02d:%02d", hours, minutes, seconds)
        } catch (e: Exception) {
            "00:00:00"
        }
    }

    fun deleteComplaintOrDowntime(record: ComplaintDowntimeEntity) {
        viewModelScope.launch {
            repository.deleteComplaintOrDowntime(record)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.deleteComplaint(getApplication(), record.id)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "SOFT_DELETE_DOWNTIME",
                    category = "downtime",
                    description = "Admin removed record #${record.id} (${record.areaOrMachine}) from active views",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Process Sheet Actions
    fun saveProcessSheet(sheet: ProcessSheetEntity) {
        viewModelScope.launch {
            repository.saveProcessSheet(sheet)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.saveProcessSheet(getApplication(), sheet)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "SAVE_PROCESS_SHEET",
                    category = "production",
                    description = "Process sheet saved: ${sheet.processKey} • ${sheet.batchNo}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Chat Actions
    fun sendChatMessage(text: String, attachmentType: String = "NONE") {
        if (text.isBlank() && attachmentType == "NONE") return
        viewModelScope.launch {
            val message = ChatMessageEntity(
                senderName = _currentUser.value.name,
                senderEmail = _currentUser.value.email,
                senderRole = _currentUser.value.role,
                messageText = text,
                timestampFormatted = _currentTimestamp.value,
                attachmentType = attachmentType
            )
            repository.sendChatMessage(message)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "SEND_CHAT_MESSAGE",
                    category = "chat_message",
                    description = "Operational transmission from ${_currentUser.value.name} ($attachmentType)",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun deleteChatMessage(id: Long) {
        viewModelScope.launch {
            repository.softDeleteChatMessage(id)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "SOFT_DELETE_CHAT_MESSAGE",
                    category = "chat_message",
                    description = "Message #$id soft-deleted by ${_currentUser.value.name}; audit retained",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Authentication & Account Security (5 wrong attempts -> 15 min lockout)
    fun attemptLogin(emailOrUser: String, pass: String, onResult: (Boolean, String) -> Unit) {
        val cleanInput = emailOrUser.trim().lowercase()
        val user = activeUserAccounts.value.firstOrNull {
            it.email.equals(cleanInput, ignoreCase = true) || it.name.equals(cleanInput, ignoreCase = true)
        }

        if (user == null) {
            onResult(false, "Account not found for \"$emailOrUser\". Please check or tap 'Create Account New'.")
            return
        }

        val currentTime = System.currentTimeMillis()
        if (user.lockoutUntil > currentTime) {
            val remainingSec = (user.lockoutUntil - currentTime) / 1000
            val mins = remainingSec / 60
            val secs = remainingSec % 60
            onResult(false, "Account locked due to 5 failed attempts! Please wait ${mins}m ${secs}s before trying again.")
            return
        }

        if (!user.isApproved) {
            onResult(false, "Account pending Admin approval! Please wait for a Plant Administrator to approve your account in the Control Panel.")
            return
        }

        // Check password (default fallback "123456" for demo accounts)
        val isPasswordCorrect = (pass == user.password) || (user.password.isBlank() && pass == "123456")

        if (isPasswordCorrect) {
            viewModelScope.launch {
                repository.updateUserAccount(
                    user.copy(
                        failedAttempts = 0,
                        lockoutUntil = 0L,
                        isOnline = true,
                        lastActiveTimestamp = System.currentTimeMillis(),
                        lastActiveFormatted = "Active now"
                    )
                )
            }
            loginSuccess(user.email, user.name, user.role)
            onResult(true, "Login successful as ${user.name} (${user.role})")
        } else {
            val nextFailed = user.failedAttempts + 1
            if (nextFailed >= 5) {
                val lockoutEnd = currentTime + (15 * 60 * 1000) // 15 minutes lockout
                viewModelScope.launch {
                    repository.updateUserAccount(
                        user.copy(failedAttempts = 5, lockoutUntil = lockoutEnd)
                    )
                    repository.logAudit(
                        AuditLogEntity(
                            actionType = "ACCOUNT_LOCKOUT_15MIN",
                            category = "security",
                            description = "Account ${user.email} locked out for 15 minutes after 5 consecutive failed login attempts.",
                            timestampFormatted = _currentTimestamp.value
                        )
                    )
                }
                onResult(false, "5 wrong attempts! Account is now locked for 15 minutes.")
            } else {
                viewModelScope.launch {
                    repository.updateUserAccount(user.copy(failedAttempts = nextFailed))
                }
                val remaining = 5 - nextFailed
                onResult(false, "Incorrect password. $remaining attempt(s) remaining before 15-minute lockout.")
            }
        }
    }

    fun quickLogin(role: String) {
        val approvedUser = activeUserAccounts.value.firstOrNull { it.role.equals(role, ignoreCase = true) && it.isApproved }
        val targetEmail = approvedUser?.email ?: when (role.uppercase()) {
            "ADMIN" -> "admin@aldella.com"
            "SUPERVISOR" -> "supervisor@aldella.com"
            else -> "operator1@aldella.com"
        }
        val targetName = approvedUser?.name ?: "Aldella $role"
        val targetRole = approvedUser?.role ?: role.uppercase()
        loginSuccess(targetEmail, targetName, targetRole)
    }

    fun loginSuccess(email: String, name: String, role: String) {
        _currentUser.value = UserSession(email, name, role)
        _isUserLoggedIn.value = true
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putString("user_email", email)
            .putString("user_name", name)
            .putString("user_role", role)
            .apply()
        updateUserOnlineHeartbeat(email, isOnline = true)
    }

    fun logout() {
        val email = _currentUser.value.email
        prefs.edit().putBoolean("is_logged_in", false).apply()
        _isUserLoggedIn.value = false
        updateUserOnlineHeartbeat(email, isOnline = false)
    }

    // New User Registration (Must be approved by Admin in Control Panel)
    fun registerNewAccount(
        name: String,
        email: String,
        pass: String,
        role: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        if (cleanEmail.isBlank() || cleanName.isBlank() || pass.isBlank()) {
            onResult(false, "All fields are required.")
            return
        }
        if (activeUserAccounts.value.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            onResult(false, "An account with email $cleanEmail already exists.")
            return
        }

        viewModelScope.launch {
            val newUser = UserAccountEntity(
                email = cleanEmail,
                name = cleanName,
                role = role.trim().uppercase(),
                password = pass.trim(),
                isApproved = false, // ADMIN ONLY APPROVES
                failedAttempts = 0,
                lockoutUntil = 0L,
                lastActiveFormatted = "Registered on ${_currentTimestamp.value} (Pending Admin Approval)"
            )
            repository.createUserAccount(newUser)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "USER_REGISTERED_PENDING_APPROVAL",
                    category = "security",
                    description = "New account registered: $cleanEmail ($cleanName, ${role.trim().uppercase()}). Awaiting Admin approval in Control Panel.",
                    timestampFormatted = _currentTimestamp.value
                )
            )
            onResult(true, "Account created successfully! Admin approval is required before you can log in.")
        }
    }

    // Admin Tools - Create Admin Account with Strict Limit of 4 Accounts
    fun createAdminAccountWithLimit(
        name: String,
        email: String,
        pass: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = name.trim()
        val currentAdminCount = activeUserAccounts.value.count { it.role == "ADMIN" }

        if (currentAdminCount >= 4) {
            onResult(false, "Admin account limit reached! Only 4 Admin accounts are permitted.")
            return
        }
        if (cleanEmail.isBlank() || cleanName.isBlank() || pass.isBlank()) {
            onResult(false, "Name, username/email, and password are required.")
            return
        }
        if (activeUserAccounts.value.any { it.email.equals(cleanEmail, ignoreCase = true) }) {
            onResult(false, "An account with $cleanEmail already exists.")
            return
        }

        viewModelScope.launch {
            val adminUser = UserAccountEntity(
                email = cleanEmail,
                name = cleanName,
                role = "ADMIN",
                password = pass.trim(),
                isApproved = true, // Directly approved since Admin created it
                lastActiveFormatted = "Created by Plant Admin on ${_currentTimestamp.value}"
            )
            repository.createUserAccount(adminUser)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "ADMIN_CREATED_ADMIN_ACCOUNT",
                    category = "security",
                    description = "Plant Admin ${_currentUser.value.name} created new Admin account: $cleanEmail ($cleanName). Admin count: ${currentAdminCount + 1}/4",
                    timestampFormatted = _currentTimestamp.value
                )
            )
            onResult(true, "Admin account created successfully! Can log in immediately with full Admin power.")
        }
    }

    fun approveUserAccount(user: UserAccountEntity) {
        viewModelScope.launch {
            repository.updateUserAccount(user.copy(isApproved = true))
            repository.logAudit(
                AuditLogEntity(
                    actionType = "ADMIN_APPROVE_ACCOUNT",
                    category = "security",
                    description = "Plant Admin ${_currentUser.value.name} approved account ${user.email} (${user.name}, ${user.role}). Login activated.",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Batch Sequencing & Protected Submission Logic
    fun getNextBatchSequence(areaKey: String, processKey: String): Int {
        val count = allProcessSheets.value.count {
            it.areaKey.equals(areaKey, ignoreCase = true) &&
            it.processKey.equals(processKey, ignoreCase = true) &&
            it.isSubmitted
        }
        return count + 1
    }

    fun getSaudiCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
        return sdf.format(Date())
    }

    fun getSaudiCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.ENGLISH)
        sdf.timeZone = TimeZone.getTimeZone("Asia/Riyadh")
        return sdf.format(Date())
    }

    fun getBatchOrdinal(n: Int): String {
        return when {
            n % 100 in 11..13 -> "${n}th"
            n % 10 == 1 -> "${n}st"
            n % 10 == 2 -> "${n}nd"
            n % 10 == 3 -> "${n}rd"
            else -> "${n}th"
        }
    }

    fun submitProcessSheetWithBatchTracking(
        sheet: ProcessSheetEntity,
        onComplete: (ProcessSheetEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val nextSeq = getNextBatchSequence(sheet.areaKey, sheet.processKey)
            val saudiNow = getSaudiCurrentDateTime()
            val saudiTime = getSaudiCurrentTime()

            val finalStartTime = if (sheet.startTime.isBlank() || sheet.startTime == "--") {
                _timerStartTime.value.takeIf { it != "--" } ?: saudiTime
            } else sheet.startTime

            val finalEndTime = if (sheet.endTime.isBlank() || sheet.endTime == "--") {
                saudiTime
            } else sheet.endTime

            val finalHours = if (sheet.totalHours.isBlank() || sheet.totalHours == "00:00:00") {
                formatSecondsToTime(_operationalTimerSeconds.value.coerceAtLeast(1L))
            } else sheet.totalHours

            val submittedSheet = sheet.copy(
                batchSequence = nextSeq,
                batchNo = if (sheet.batchNo.isBlank() || sheet.batchNo.startsWith("Batch")) "Batch $nextSeq" else sheet.batchNo,
                startTime = finalStartTime,
                endTime = finalEndTime,
                totalHours = finalHours,
                operationalTimer = finalHours,
                status = "COMPLETED",
                isSubmitted = true,
                submissionSaudiTime = saudiNow,
                recordedBy = _currentUser.value.name,
                timestamp = System.currentTimeMillis()
            )

            repository.saveProcessSheet(submittedSheet)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "SUBMIT_PROCESS_BATCH",
                    category = "production",
                    description = "${submittedSheet.areaKey} • ${submittedSheet.processKey}: ${getBatchOrdinal(nextSeq)} Batch Finished. Total Hours: $finalHours. Recorded by ${_currentUser.value.name}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
            resetOperationalTimer()
            onComplete(submittedSheet)
        }
    }

    /**
     * Start process run:
     * - Requires input already present
     * - Captures exact Saudi start time
     * - Saves to Room database with status = "RUNNING" so if app closes, reopening displays the exact same start time and inputs
     * - Next command will automatically be: End Time & Submit
     */
    fun startPersistentProcessRun(
        sheet: ProcessSheetEntity,
        onStarted: (ProcessSheetEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val saudiTime = getSaudiCurrentTime()
            val nowMs = System.currentTimeMillis()
            val nextSeq = getNextBatchSequence(sheet.areaKey, sheet.processKey)

            // Look for existing running sheet for this area + process
            val existing = allProcessSheets.value.firstOrNull {
                it.areaKey.equals(sheet.areaKey, ignoreCase = true) &&
                it.processKey.equals(sheet.processKey, ignoreCase = true) &&
                !it.isSubmitted && it.status == "RUNNING"
            }

            val finalStartTime = if (existing != null && existing.startTime != "--" && existing.startTime.isNotBlank()) {
                existing.startTime
            } else {
                saudiTime
            }

            val runningSheet = sheet.copy(
                id = existing?.id ?: 0L,
                startTime = finalStartTime,
                endTime = "--",
                status = "RUNNING",
                isSubmitted = false,
                batchSequence = if (sheet.batchSequence > 0) sheet.batchSequence else nextSeq,
                batchNo = if (sheet.batchNo.isBlank() || sheet.batchNo.startsWith("Batch")) "Batch ${if (sheet.batchSequence > 0) sheet.batchSequence else nextSeq}" else sheet.batchNo,
                recordedBy = _currentUser.value.name,
                timestamp = existing?.timestamp ?: nowMs
            )

            val newId = repository.saveProcessSheet(runningSheet)
            val result = runningSheet.copy(id = if (runningSheet.id > 0) runningSheet.id else newId)

            _timerStartTime.value = result.startTime
            _isTimerRunning.value = true

            repository.logAudit(
                AuditLogEntity(
                    actionType = "START_PROCESS_RUN",
                    category = "production",
                    description = "Process started: ${result.batchNo} in ${result.areaKey} • ${result.processKey} at ${result.startTime}. Inputs registered: ${result.productName} (${result.inputKg} kg). Recorded by ${_currentUser.value.name}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
            onStarted(result)
        }
    }

    /**
     * End process run & submit directly to Live Updates:
     * - Only Operator or Supervisor (or Admin) can give end time
     * - Captures exact Saudi end time
     * - Computes total hours and total kg
     * - Updates directly in Room database (isSubmitted = true, status = "COMPLETED")
     * - Immediately logged in Updates with operator name, duration, and all details
     */
    fun endPersistentProcessRun(
        sheet: ProcessSheetEntity,
        onComplete: (ProcessSheetEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val saudiNow = getSaudiCurrentDateTime()
            val saudiEndTime = getSaudiCurrentTime()
            val nextSeq = getNextBatchSequence(sheet.areaKey, sheet.processKey)

            val elapsedMs = (System.currentTimeMillis() - sheet.timestamp).coerceAtLeast(0L)
            val calculatedSeconds = elapsedMs / 1000L
            val finalHours = if (sheet.totalHours.isNotBlank() && sheet.totalHours != "00:00:00") {
                sheet.totalHours
            } else {
                formatSecondsToTime(calculatedSeconds.coerceAtLeast(1L))
            }

            val finalTotalKg = when {
                sheet.outputKg > 0.0 -> sheet.outputKg
                sheet.totalKgQty > 0.0 -> sheet.totalKgQty
                sheet.inputKg > 0.0 -> sheet.inputKg
                else -> 0.0
            }

            val completedSheet = sheet.copy(
                endTime = saudiEndTime,
                totalHours = finalHours,
                operationalTimer = finalHours,
                totalKgQty = finalTotalKg,
                status = "COMPLETED",
                isSubmitted = true,
                submissionSaudiTime = saudiNow,
                recordedBy = "${_currentUser.value.name} (${_currentUser.value.role})",
                batchSequence = if (sheet.batchSequence > 0) sheet.batchSequence else nextSeq
            )

            repository.saveProcessSheet(completedSheet)

            _timerEndTime.value = saudiEndTime
            _isTimerRunning.value = false

            repository.logAudit(
                AuditLogEntity(
                    actionType = "END_PROCESS_RUN_UPDATES",
                    category = "production",
                    description = "Batch finished & placed directly in Updates: ${completedSheet.batchNo} in ${completedSheet.areaKey} • ${completedSheet.processKey}. Total Hours: $finalHours, Total: $finalTotalKg kg, Start: ${completedSheet.startTime}, End: $saudiEndTime. Operator: ${_currentUser.value.name}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
            onComplete(completedSheet)
        }
    }

    // Column Header Management (Modifiable by Admin via Long Press)
    fun addProcessColumnHeader(areaKey: String, processKey: String, fieldName: String, fieldType: String = "Text") {
        if (fieldName.isBlank()) return
        viewModelScope.launch {
            repository.addCustomTemplateField(
                CustomTemplateFieldEntity(
                    fieldName = fieldName.trim(),
                    fieldType = fieldType,
                    targetArea = areaKey,
                    processKey = processKey,
                    isColumnVisible = true
                )
            )
            repository.logAudit(
                AuditLogEntity(
                    actionType = "ADMIN_ADD_PROCESS_COLUMN",
                    category = "template",
                    description = "Admin ${_currentUser.value.name} added column \"${fieldName.trim()}\" to $areaKey • $processKey",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun updateProcessColumnHeader(field: CustomTemplateFieldEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateCustomTemplateField(field.copy(fieldName = newName.trim()))
            repository.logAudit(
                AuditLogEntity(
                    actionType = "ADMIN_RENAME_PROCESS_COLUMN",
                    category = "template",
                    description = "Admin ${_currentUser.value.name} renamed column to \"${newName.trim()}\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun removeProcessColumnHeader(field: CustomTemplateFieldEntity) {
        viewModelScope.launch {
            repository.deleteCustomTemplateField(field)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "ADMIN_REMOVE_PROCESS_COLUMN",
                    category = "template",
                    description = "Admin ${_currentUser.value.name} removed column \"${field.fieldName}\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Admin Tools - Users: Add, Rename/Update, Deactivate, Restore, Hard Delete
    fun createAdminAccount(email: String, name: String) {
        createAdminAccountWithLimit(name, email, "123456") { _, _ -> }
    }

    fun createUserAccount(email: String, name: String, role: String = "OPERATOR") {
        if (email.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            repository.createUserAccount(
                UserAccountEntity(
                    email = email.trim(),
                    name = name.trim(),
                    role = role.trim().uppercase(),
                    lastActiveFormatted = "Created on ${_currentTimestamp.value}"
                )
            )
            repository.logAudit(
                AuditLogEntity(
                    actionType = "CREATE_USER_ACCOUNT",
                    category = "security",
                    description = "User created: ${email.trim()} (${name.trim()}, role: ${role.trim()}) by ${_currentUser.value.name}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun updateUserAccount(id: Long, email: String, name: String, role: String) {
        if (email.isBlank() || name.isBlank()) return
        viewModelScope.launch {
            repository.updateUserAccount(
                UserAccountEntity(
                    id = id,
                    email = email.trim(),
                    name = name.trim(),
                    role = role.trim().uppercase(),
                    lastActiveFormatted = "Updated on ${_currentTimestamp.value}"
                )
            )
            repository.logAudit(
                AuditLogEntity(
                    actionType = "UPDATE_USER_ACCOUNT",
                    category = "security",
                    description = "User #$id updated: ${email.trim()} (${name.trim()}, $role) by ${_currentUser.value.name}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun deleteUserAccount(id: Long, email: String) {
        viewModelScope.launch {
            repository.softDeleteUserAccount(id)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "DELETE_USER_ACCOUNT",
                    category = "security",
                    description = "Account $email deactivated by ${_currentUser.value.name}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun permanentlyDeleteUserAccount(user: UserAccountEntity) {
        viewModelScope.launch {
            repository.hardDeleteUserAccount(user)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "HARD_DELETE_USER_ACCOUNT",
                    category = "security",
                    description = "Account ${user.email} (${user.name}) permanently purged by ${_currentUser.value.name}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun restoreUserAccount(id: Long, email: String) {
        viewModelScope.launch {
            repository.restoreUserAccount(id)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "RESTORE_USER_ACCOUNT",
                    category = "security",
                    description = "Account $email restored by ${_currentUser.value.name}",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Admin Tools - Custom Template Fields: Add, Rename, Delete
    fun addCustomTemplateField(fieldName: String) {
        if (fieldName.isBlank()) return
        viewModelScope.launch {
            repository.addCustomTemplateField(
                CustomTemplateFieldEntity(fieldName = fieldName.trim())
            )
            repository.logAudit(
                AuditLogEntity(
                    actionType = "ADD_TEMPLATE_FIELD",
                    category = "template",
                    description = "Custom field \"${fieldName.trim()}\" added to global process template",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun renameCustomTemplateField(field: CustomTemplateFieldEntity, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.updateCustomTemplateField(field.copy(fieldName = newName.trim()))
            repository.logAudit(
                AuditLogEntity(
                    actionType = "RENAME_TEMPLATE_FIELD",
                    category = "template",
                    description = "Template field \"${field.fieldName}\" renamed to \"${newName.trim()}\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun deleteCustomTemplateField(field: CustomTemplateFieldEntity) {
        viewModelScope.launch {
            repository.deleteCustomTemplateField(field)
            repository.logAudit(
                AuditLogEntity(
                    actionType = "DELETE_TEMPLATE_FIELD",
                    category = "template",
                    description = "Custom field \"${field.fieldName}\" removed from global process template",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Process Area Management: Add, Rename, Remove, and Manage Sub-processes
    fun addProcessArea(name: String, processes: List<String> = listOf("Standard Processing")) {
        if (name.isBlank()) return
        val current = _processAreas.value.toMutableList()
        if (current.any { it.name.equals(name.trim(), ignoreCase = true) }) return
        current.add(ProcessAreaConfig(name = name.trim(), iconName = "ic_line", processes = processes))
        _processAreas.value = current
        viewModelScope.launch {
            repository.logAudit(
                AuditLogEntity(
                    actionType = "ADD_PROCESS_AREA",
                    category = "process_config",
                    description = "Added new factory process area \"${name.trim()}\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun renameProcessArea(oldName: String, newName: String) {
        if (oldName.isBlank() || newName.isBlank()) return
        _processAreas.value = _processAreas.value.map { area ->
            if (area.name.equals(oldName.trim(), ignoreCase = true)) {
                area.copy(name = newName.trim())
            } else {
                area
            }
        }
        viewModelScope.launch {
            repository.logAudit(
                AuditLogEntity(
                    actionType = "UPDATE_PROCESS_CONFIG",
                    category = "process_config",
                    description = "Renamed process area \"$oldName\" to \"$newName\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun removeProcessArea(areaName: String) {
        if (areaName.isBlank()) return
        _processAreas.value = _processAreas.value.filterNot {
            it.name.equals(areaName.trim(), ignoreCase = true)
        }
        viewModelScope.launch {
            repository.logAudit(
                AuditLogEntity(
                    actionType = "UPDATE_PROCESS_CONFIG",
                    category = "process_config",
                    description = "Removed process area \"$areaName\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun addProcessToArea(areaName: String, processName: String) {
        if (areaName.isBlank() || processName.isBlank()) return
        _processAreas.value = _processAreas.value.map { area ->
            if (area.name.equals(areaName.trim(), ignoreCase = true)) {
                val updatedProcesses = area.processes.toMutableList()
                if (!updatedProcesses.any { it.equals(processName.trim(), ignoreCase = true) }) {
                    updatedProcesses.add(processName.trim())
                }
                area.copy(processes = updatedProcesses)
            } else {
                area
            }
        }
        viewModelScope.launch {
            repository.logAudit(
                AuditLogEntity(
                    actionType = "ADD_PROCESS",
                    category = "process_config",
                    description = "Added process \"${processName.trim()}\" to area \"$areaName\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun renameProcessInArea(areaName: String, oldProcessName: String, newProcessName: String) {
        if (areaName.isBlank() || oldProcessName.isBlank() || newProcessName.isBlank()) return
        _processAreas.value = _processAreas.value.map { area ->
            if (area.name.equals(areaName.trim(), ignoreCase = true)) {
                val updatedProcesses = area.processes.map {
                    if (it.equals(oldProcessName.trim(), ignoreCase = true)) newProcessName.trim() else it
                }
                area.copy(processes = updatedProcesses)
            } else {
                area
            }
        }
        viewModelScope.launch {
            repository.logAudit(
                AuditLogEntity(
                    actionType = "RENAME_PROCESS",
                    category = "process_config",
                    description = "Renamed process \"$oldProcessName\" to \"$newProcessName\" in area \"$areaName\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun removeProcessFromArea(areaName: String, processName: String) {
        if (areaName.isBlank() || processName.isBlank()) return
        _processAreas.value = _processAreas.value.map { area ->
            if (area.name.equals(areaName.trim(), ignoreCase = true)) {
                val updatedProcesses = area.processes.filterNot { it.equals(processName.trim(), ignoreCase = true) }
                area.copy(processes = updatedProcesses)
            } else {
                area
            }
        }
        viewModelScope.launch {
            repository.logAudit(
                AuditLogEntity(
                    actionType = "REMOVE_PROCESS",
                    category = "process_config",
                    description = "Removed process \"$processName\" from area \"$areaName\"",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Process Sheets Management: Delete & Rename (touch and do it)
    fun deleteProcessSheet(sheet: ProcessSheetEntity) {
        viewModelScope.launch {
            repository.deleteProcessSheet(sheet)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.deleteProcessSheet(getApplication(), sheet.id)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "DELETE_PROCESS_SHEET",
                    category = "production",
                    description = "Admin deleted process sheet #${sheet.id} (${sheet.areaKey} • ${sheet.batchNo})",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun renameProcessSheet(sheet: ProcessSheetEntity, newBatchNo: String, newProductName: String) {
        if (newBatchNo.isBlank()) return
        viewModelScope.launch {
            val updated = sheet.copy(
                batchNo = newBatchNo.trim(),
                productName = if (newProductName.isNotBlank()) newProductName.trim() else sheet.productName
            )
            repository.updateProcessSheet(updated)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.saveProcessSheet(getApplication(), updated)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "RENAME_PROCESS_SHEET",
                    category = "production",
                    description = "Admin renamed sheet #${sheet.id} batch to \"${newBatchNo.trim()}\" (product: ${updated.productName})",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    fun renameComplaintOrDowntime(record: ComplaintDowntimeEntity, newMachine: String, newDetails: String) {
        if (newMachine.isBlank() && newDetails.isBlank()) return
        viewModelScope.launch {
            val updated = record.copy(
                areaOrMachine = if (newMachine.isNotBlank()) newMachine.trim() else record.areaOrMachine,
                details = if (newDetails.isNotBlank()) newDetails.trim() else record.details
            )
            repository.updateComplaintOrDowntime(updated)
            if (_isFirestoreAutoSync.value) {
                FirestoreManager.saveComplaint(getApplication(), updated)
            }
            repository.logAudit(
                AuditLogEntity(
                    actionType = "RENAME_DOWNTIME_RECORD",
                    category = "downtime",
                    description = "Admin renamed/updated record #${record.id} (${updated.areaOrMachine})",
                    timestampFormatted = _currentTimestamp.value
                )
            )
        }
    }

    // Share and Export
    fun exportDailyActivities(context: Context) {
        val sheets = allProcessSheets.value
        val complaints = allComplaintsAndDowntime.value
        val attendance = allAttendance.value

        val report = buildString {
            appendLine("ALDELLA COMPANY PRODUCTION — DAILY ACTIVITIES REPORT")
            appendLine("Generated at: ${_currentTimestamp.value} (KSA Riyadh Time)")
            appendLine("Audited by: ${_currentUser.value.name} (${_currentUser.value.email})")
            appendLine("=".repeat(50))
            appendLine("\n[ACTIVE/RECENT PRODUCTION SHEETS]")
            if (sheets.isEmpty()) {
                appendLine("No production records logged today.")
            } else {
                sheets.forEach {
                    appendLine("• ${it.areaKey} / ${it.processKey} - Batch: ${it.batchNo}, Product: ${it.productName}, Status: ${it.status}, Qty: ${it.totalKgQty} kg, Time: ${it.totalHours}")
                }
            }
            appendLine("\n[COMPLAINTS & MACHINE DOWNTIME]")
            if (complaints.isEmpty()) {
                appendLine("All machines operating normally. 0 downtime records.")
            } else {
                complaints.forEach {
                    appendLine("• [${it.type}] ${it.areaOrMachine}: ${it.details} | Status: ${it.status} | Start: ${it.startTimeFormatted}")
                }
            }
            appendLine("\n[ATTENDANCE & HOURS SUMMARY]")
            if (attendance.isEmpty()) {
                appendLine("No member attendance logs recorded.")
            } else {
                attendance.forEach {
                    appendLine("• ${it.fullName} (${it.memberId}) - Dept: ${it.department}, Shift: ${it.shiftType}, Total: ${it.totalWorkingHours}")
                }
            }
            appendLine("\nEnd of Report. Aldella Enterprise Systems.")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TITLE, "ALDELLA COMPANY PRODUCTION — DAILY ACTIVITIES")
            putExtra(Intent.EXTRA_SUBJECT, "ALDELLA PRODUCTION REPORT ${_currentTimestamp.value}")
            putExtra(Intent.EXTRA_TEXT, report)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Daily Activities")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    fun exportCsvReport(context: Context) {
        val sheets = allProcessSheets.value
        val csvBuilder = StringBuilder()
        csvBuilder.append("Area,Process,BatchNo,Product,QtyKg,Status,Date,Hours,InputKg,OutputKg,WasteKg,TempC\n")
        sheets.forEach {
            csvBuilder.append("\"${it.areaKey}\",\"${it.processKey}\",\"${it.batchNo}\",\"${it.productName}\",${it.totalKgQty},\"${it.status}\",\"${it.date}\",\"${it.totalHours}\",${it.inputKg},${it.outputKg},${it.productWasteKg},${it.temperatureC}\n")
        }

        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_SUBJECT, "Aldella Production CSV Export")
            putExtra(Intent.EXTRA_TEXT, csvBuilder.toString())
            type = "text/csv"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Export CSV Report")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    }

    // Comprehensive Daily Report Generators for PDF, Excel, and Docs Tabs
    // ================= 30-YEAR REPORT FILTERING ENGINE =================
    fun matchesFilter(recordDate: String, recordTimestamp: Long, criteria: ReportFilterCriteria): Boolean {
        val cal = Calendar.getInstance().apply {
            if (recordTimestamp > 0) timeInMillis = recordTimestamp
        }
        val calYear = cal.get(Calendar.YEAR).toString()
        val calMonth = String.format(Locale.ENGLISH, "%02d", cal.get(Calendar.MONTH) + 1)
        val calDay = String.format(Locale.ENGLISH, "%02d", cal.get(Calendar.DAY_OF_MONTH))

        // Check Year (Supports next 30 years: 2024 to 2056)
        if (criteria.year != "ALL") {
            val matchesYear = calYear == criteria.year || recordDate.contains(criteria.year)
            if (!matchesYear) return false
        }

        // Check Month
        if (criteria.month != "ALL") {
            val targetMonth = criteria.month.padStart(2, '0')
            val matchesMonth = calMonth == targetMonth ||
                    recordDate.contains("-$targetMonth-") ||
                    recordDate.contains("/$targetMonth/")
            if (!matchesMonth) return false
        }

        // Check Day
        if (criteria.day != "ALL") {
            val targetDay = criteria.day.padStart(2, '0')
            val matchesDay = calDay == targetDay ||
                    recordDate.startsWith("$targetDay-") ||
                    recordDate.startsWith("$targetDay/") ||
                    recordDate.contains("-$targetDay ") ||
                    recordDate.contains("/$targetDay ")
            if (!matchesDay) return false
        }

        return true
    }

    fun getFilteredProcessSheets(criteria: ReportFilterCriteria): List<ProcessSheetEntity> {
        return allProcessSheets.value.filter { s ->
            (criteria.areaKey == "ALL" || s.areaKey.equals(criteria.areaKey, ignoreCase = true)) &&
            (criteria.status == "ALL" || s.status.equals(criteria.status, ignoreCase = true)) &&
            matchesFilter(s.date, s.timestamp, criteria)
        }
    }

    fun getFilteredComplaints(criteria: ReportFilterCriteria): List<ComplaintDowntimeEntity> {
        return allComplaintsAndDowntime.value.filter { c ->
            (criteria.areaKey == "ALL" || c.areaOrMachine.contains(criteria.areaKey, ignoreCase = true)) &&
            (criteria.status == "ALL" || c.status.equals(criteria.status, ignoreCase = true)) &&
            matchesFilter(c.startTimeFormatted, c.timestamp, criteria)
        }
    }

    fun getFilteredAttendance(criteria: ReportFilterCriteria): List<AttendanceEntity> {
        return allAttendance.value.filter { a ->
            matchesFilter(a.attendanceDate, a.timestamp, criteria)
        }
    }

    fun getDailyReportPdfContent(criteria: ReportFilterCriteria = ReportFilterCriteria()): String {
        val sheets = getFilteredProcessSheets(criteria)
        val complaints = getFilteredComplaints(criteria)
        val attendance = getFilteredAttendance(criteria)
        val totalKg = sheets.sumOf { it.totalKgQty }
        val runningBatches = sheets.count { it.status == "RUNNING" }
        val completedBatches = sheets.count { it.status == "COMPLETED" }

        val filterSummary = buildString {
            append("Filter: Year=[${criteria.year}] Month=[${criteria.month}] Day=[${criteria.day}] Area=[${criteria.areaKey}]")
        }

        return buildString {
            appendLine("================================================================================")
            appendLine("                      ALDELLA FOOD COMPANY — PLANT OPERATIONS                   ")
            appendLine("                       OFFICIAL ENTERPRISE PRODUCTION REPORT                    ")
            appendLine("================================================================================")
            appendLine("Report ID: RPT-ALD-${System.currentTimeMillis() % 100000}       Date/Time (KSA): ${_currentTimestamp.value}")
            appendLine("Auditor: ${_currentUser.value.name} (${_currentUser.value.role})       Location: Riyadh Plant Lines")
            appendLine("Period Active: $filterSummary")
            appendLine("System Integrity: Validated across 30-Year Enterprise Archive (2024–2056)")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("EXECUTIVE SUMMARY (FILTERED DATA):")
            appendLine("• Total Batch Volume Logged: $totalKg KG across ${sheets.size} batches")
            appendLine("• Completed Batches: $completedBatches | Active Running: $runningBatches")
            appendLine("• Open Machine Downtimes / Complaints: ${complaints.count { it.status == "OPEN" }} (Total Logged: ${complaints.size})")
            appendLine("• Workforce Attendance Records: ${attendance.size} personnel recorded")
            appendLine("--------------------------------------------------------------------------------")
            appendLine("BATCH PRODUCTION LOG (Detailed):")
            if (sheets.isEmpty()) {
                appendLine("  [No batch records found matching the selected timeframe/filter]")
            } else {
                sheets.forEachIndexed { index, s ->
                    appendLine("  #${index + 1} ${s.areaKey.uppercase()} • ${s.processKey} [${s.batchNo}]")
                    appendLine("     Product: ${s.productName} (${s.chickenProduct}) | Status: ${s.status}")
                    appendLine("     Date: ${s.date} | Window: ${s.startTime} to ${s.endTime} | Hours: ${s.totalHours}")
                    appendLine("     Input: ${s.inputKg} kg | Output: ${s.outputKg} kg | Waste: ${s.productWasteKg} kg | Water: ${s.waterWasteLitres} L")
                    appendLine("     Core Temp: ${s.temperatureC}°C | Inspected by: ${s.recordedBy}")
                    if (s.remarks.isNotBlank()) appendLine("     Remarks: ${s.remarks}")
                    appendLine("  " + "-".repeat(70))
                }
            }
            appendLine("INCIDENTS & MACHINE DOWNTIME:")
            if (complaints.isEmpty()) {
                appendLine("  [Zero incidents or machine downtime logged for this timeframe]")
            } else {
                complaints.forEachIndexed { i, c ->
                    appendLine("  [${c.status}] #${i + 1} Area/Machine: ${c.areaOrMachine}")
                    appendLine("     Details: ${c.details}")
                    appendLine("     Start Time: ${c.startTimeFormatted} | End Time: ${c.endTimeFormatted} | Total: ${c.totalHours}")
                    appendLine("     Reported By: ${c.submittedBy} | Updated By: ${c.updatedBy.ifBlank { "Pending Admin" }}")
                    if (c.attachmentName.isNotBlank()) {
                        appendLine("     Attachment: ${c.attachmentName} (${c.attachmentType})")
                    }
                }
            }
            appendLine("--------------------------------------------------------------------------------")
            appendLine("CERTIFIED APPROVAL SIGN-OFF:")
            appendLine("Plant Operations Supervisor: _____________________    Date: ${_currentTimestamp.value.take(15)}")
            appendLine("Executive Quality Admin:     _____________________    Status: APPROVED")
            appendLine("================================================================================")
        }
    }

    fun getDailyReportExcelCsvContent(criteria: ReportFilterCriteria = ReportFilterCriteria()): String {
        val sheets = getFilteredProcessSheets(criteria)
        val sb = StringBuilder()
        sb.append("AREA,PROCESS,BATCH_NO,PRODUCT_NAME,PRODUCT_CODE,CHICKEN_PRODUCT,STATUS,START_TIME,END_TIME,TOTAL_HOURS,INPUT_KG,OUTPUT_KG,PRODUCT_WASTE_KG,WATER_WASTE_L,TEMPERATURE_C,RECORDED_BY,DATE,SUBMITTED_TIME\n")
        sheets.forEach { s ->
            sb.append("\"${s.areaKey}\",\"${s.processKey}\",\"${s.batchNo}\",\"${s.productName}\",\"${s.productCode}\",\"${s.chickenProduct}\",\"${s.status}\",\"${s.startTime}\",\"${s.endTime}\",\"${s.totalHours}\",${s.inputKg},${s.outputKg},${s.productWasteKg},${s.waterWasteLitres},${s.temperatureC},\"${s.recordedBy}\",\"${s.date}\",\"${s.submissionSaudiTime}\"\n")
        }
        return sb.toString()
    }

    fun getDailyReportDocsContent(criteria: ReportFilterCriteria = ReportFilterCriteria()): String {
        val sheets = getFilteredProcessSheets(criteria)
        val complaints = getFilteredComplaints(criteria)
        val attendance = getFilteredAttendance(criteria)

        return buildString {
            appendLine("ALDELLA FACTORY OPERATIONS — OFFICIAL PRODUCTION DOCUMENTATION")
            appendLine("Classification: Internal Enterprise Quality Record")
            appendLine("Generation Timestamp: ${_currentTimestamp.value}")
            appendLine("Timeframe Filter: Year=[${criteria.year}] Month=[${criteria.month}] Day=[${criteria.day}] Area=[${criteria.areaKey}]")
            appendLine("Auditor: ${_currentUser.value.name} (${_currentUser.value.email} • ${_currentUser.value.role})")
            appendLine("\n1. PURPOSE & SCOPE")
            appendLine("This operational document compiles all batch progression, quality inspection criteria, line downtimes, and staff hours recorded across Aldella factory lines for the selected audit timeframe.")
            appendLine("\n2. PRODUCTION PERFORMANCE SUMMARY")
            appendLine("Total lines reported: ${sheets.map { it.areaKey }.distinct().size}")
            appendLine("Total batches logged: ${sheets.size}")
            appendLine("Total output yield: ${sheets.sumOf { it.outputKg }} KG")
            appendLine("Total product waste recorded: ${sheets.sumOf { it.productWasteKg }} KG")
            appendLine("\n3. LINE-BY-LINE BATCH RECORDS")
            if (sheets.isEmpty()) {
                appendLine("No batches recorded matching the selected filter criteria.")
            } else {
                sheets.forEach { s ->
                    appendLine("• Line: ${s.areaKey} — Process: ${s.processKey}")
                    appendLine("  - Batch: ${s.batchNo} (${s.productName}) | Date: ${s.date}")
                    appendLine("  - Operation Window: ${s.startTime} to ${s.endTime} (Duration: ${s.totalHours})")
                    appendLine("  - Quantity In/Out: ${s.inputKg} kg / ${s.outputKg} kg | Loss: ${s.productWasteKg} kg")
                    appendLine("  - Process Temp: ${s.temperatureC} °C | Inspected by: ${s.recordedBy}")
                    if (s.remarks.isNotBlank()) appendLine("  - Field Notes: ${s.remarks}")
                }
            }
            appendLine("\n4. COMPLIANCE, DOWNTIME & RESOLUTIONS")
            if (complaints.isEmpty()) {
                appendLine("No mechanical stops or quality deviations logged for this timeframe.")
            } else {
                complaints.forEach { c ->
                    appendLine("• [${c.type}] Line/Unit: ${c.areaOrMachine} — Status: ${c.status}")
                    appendLine("  Incident Start: ${c.startTimeFormatted} | Cleared: ${c.endTimeFormatted} | Duration: ${c.totalHours}")
                    appendLine("  Description: ${c.details}")
                    appendLine("  Submitted by: ${c.submittedBy} | Updated by: ${c.updatedBy.ifBlank { "Pending Admin" }}")
                    if (c.attachmentName.isNotBlank()) appendLine("  Attached Media/File: ${c.attachmentName} (${c.attachmentType})")
                }
            }
            appendLine("\n5. WORKFORCE ATTENDANCE SUMMARY")
            if (attendance.isEmpty()) {
                appendLine("No staff attendance records logged for this timeframe.")
            } else {
                attendance.forEach { a ->
                    appendLine("• ${a.fullName} (${a.memberId}) — Dept: ${a.department} | Date: ${a.attendanceDate} | Hours: ${a.totalWorkingHours} | Status: ${a.statusRemarks.ifBlank { "Present" }}")
                }
            }
            appendLine("\nDocument automatically authenticated via Aldella Production Engine (2024–2056 Archive).")
        }
    }

    fun downloadReportDocument(context: Context, format: String, criteria: ReportFilterCriteria = ReportFilterCriteria()) {
        val yStr = if (criteria.year == "ALL") "AllYears" else criteria.year
        val mStr = if (criteria.month == "ALL") "AllMonths" else criteria.month
        val filterTag = "${yStr}_${mStr}"
        val (filename, content, mimeType) = when (format.uppercase()) {
            "PDF" -> Triple("Aldella_Report_${filterTag}_${System.currentTimeMillis() % 10000}.pdf", getDailyReportPdfContent(criteria), "application/pdf")
            "EXCEL" -> Triple("Aldella_Sheets_${filterTag}_${System.currentTimeMillis() % 10000}.csv", getDailyReportExcelCsvContent(criteria), "text/csv")
            else -> Triple("Aldella_Docs_${filterTag}_${System.currentTimeMillis() % 10000}.docx", getDailyReportDocsContent(criteria), "text/plain")
        }

        try {
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray())
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, "Download $format Report: $filename")
                putExtra(Intent.EXTRA_SUBJECT, "Aldella $format Production Report ($filterTag)")
                putExtra(Intent.EXTRA_TEXT, content)
                type = mimeType
            }
            val shareIntent = Intent.createChooser(sendIntent, "Download or Open $format with...")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
            Toast.makeText(context, "$format report ready: $filename", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openOrDownloadAttachment(context: Context, filename: String, attachmentType: String) {
        try {
            val safeName = filename.ifBlank { "Aldella_Media_Attachment" }
            val file = File(context.cacheDir, safeName)
            if (!file.exists()) {
                FileOutputStream(file).use { out ->
                    out.write("Aldella factory attachment content for $safeName ($attachmentType)\nTimestamp: ${_currentTimestamp.value}".toByteArray())
                }
            }

            val mime = when (attachmentType.uppercase()) {
                "IMAGE", "PHOTO" -> "image/*"
                "VIDEO" -> "video/*"
                "DOCUMENT", "PDF" -> "application/pdf"
                else -> "text/plain"
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, "Attachment: $safeName")
                putExtra(Intent.EXTRA_SUBJECT, "Attachment: $safeName")
                putExtra(Intent.EXTRA_TEXT, "Aldella Attachment: $safeName ($attachmentType) submitted for Admin review.")
                type = mime
            }
            val chooser = Intent.createChooser(sendIntent, "View, Open, or Download $safeName")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open attachment: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Share Report Document (Opens system share sheet to share to WhatsApp, Email, Drive, etc.)
    fun shareReportDocument(context: Context, format: String, criteria: ReportFilterCriteria = ReportFilterCriteria()) {
        val yStr = if (criteria.year == "ALL") "AllYears" else criteria.year
        val mStr = if (criteria.month == "ALL") "AllMonths" else criteria.month
        val filterTag = "${yStr}_${mStr}"
        val (filename, content, mimeType) = when (format.uppercase()) {
            "PDF" -> Triple("Aldella_Report_${filterTag}_${System.currentTimeMillis() % 10000}.pdf", getDailyReportPdfContent(criteria), "application/pdf")
            "EXCEL" -> Triple("Aldella_Sheets_${filterTag}_${System.currentTimeMillis() % 10000}.csv", getDailyReportExcelCsvContent(criteria), "text/csv")
            else -> Triple("Aldella_Docs_${filterTag}_${System.currentTimeMillis() % 10000}.docx", getDailyReportDocsContent(criteria), "text/plain")
        }

        try {
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { out ->
                out.write(content.toByteArray())
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, "Share $format Report: $filename")
                putExtra(Intent.EXTRA_SUBJECT, "Aldella $format Production Report ($filterTag)")
                putExtra(Intent.EXTRA_TEXT, content)
                type = mimeType
            }
            val shareIntent = Intent.createChooser(sendIntent, "Share $format Report via...")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Share error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ================= FULL ENTERPRISE BACKUP & RESTORE (ADMIN) =================
    fun exportFullDatabaseBackupJson(): String {
        val root = JSONObject()
        root.put("application", "Aldella Factory Manager")
        root.put("schemaVersion", "2.0")
        root.put("supportedSpan", "2024-2056 Enterprise Archiving (30 Years)")
        root.put("exportTimestampMillis", System.currentTimeMillis())
        root.put("exportDateFormatted", _currentTimestamp.value)
        root.put("exportedBy", _currentUser.value.name)
        root.put("exportedByEmail", _currentUser.value.email)
        root.put("exportedByRole", _currentUser.value.role)

        // 1. Process Sheets
        val sheetsArray = JSONArray()
        allProcessSheets.value.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("areaKey", s.areaKey)
            obj.put("processKey", s.processKey)
            obj.put("productName", s.productName)
            obj.put("productCode", s.productCode)
            obj.put("brand", s.brand)
            obj.put("chickenProduct", s.chickenProduct)
            obj.put("batchNo", s.batchNo)
            obj.put("date", s.date)
            obj.put("startTime", s.startTime)
            obj.put("endTime", s.endTime)
            obj.put("operationalTimer", s.operationalTimer)
            obj.put("status", s.status)
            obj.put("totalKgQty", s.totalKgQty)
            obj.put("totalHours", s.totalHours)
            obj.put("inputKg", s.inputKg)
            obj.put("outputKg", s.outputKg)
            obj.put("productWasteKg", s.productWasteKg)
            obj.put("waterWasteLitres", s.waterWasteLitres)
            obj.put("temperatureC", s.temperatureC)
            obj.put("remarks", s.remarks)
            obj.put("recordedBy", s.recordedBy)
            obj.put("batchSequence", s.batchSequence)
            obj.put("isSubmitted", s.isSubmitted)
            obj.put("submissionSaudiTime", s.submissionSaudiTime)
            obj.put("timestamp", s.timestamp)
            sheetsArray.put(obj)
        }
        root.put("processSheets", sheetsArray)

        // 2. Complaints & Downtime
        val complaintsArray = JSONArray()
        allComplaintsAndDowntime.value.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("type", c.type)
            obj.put("areaOrMachine", c.areaOrMachine)
            obj.put("details", c.details)
            obj.put("status", c.status)
            obj.put("startTimeFormatted", c.startTimeFormatted)
            obj.put("endTimeFormatted", c.endTimeFormatted)
            obj.put("totalHours", c.totalHours)
            obj.put("submittedBy", c.submittedBy)
            obj.put("updatedBy", c.updatedBy)
            obj.put("isViewedByAdmin", c.isViewedByAdmin)
            obj.put("attachmentsCount", c.attachmentsCount)
            obj.put("attachmentType", c.attachmentType)
            obj.put("attachmentName", c.attachmentName)
            obj.put("attachmentUri", c.attachmentUri)
            obj.put("timestamp", c.timestamp)
            complaintsArray.put(obj)
        }
        root.put("complaintsAndDowntime", complaintsArray)

        // 3. Attendance
        val attendanceArray = JSONArray()
        allAttendance.value.forEach { a ->
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("memberId", a.memberId)
            obj.put("fullName", a.fullName)
            obj.put("department", a.department)
            obj.put("shiftType", a.shiftType)
            obj.put("attendanceDate", a.attendanceDate)
            obj.put("timeIn1", a.timeIn1)
            obj.put("timeOut1", a.timeOut1)
            obj.put("timeIn2", a.timeIn2)
            obj.put("timeOut2", a.timeOut2)
            obj.put("timeIn3", a.timeIn3)
            obj.put("timeOut3", a.timeOut3)
            obj.put("statusRemarks", a.statusRemarks)
            obj.put("manualOverrideReason", a.manualOverrideReason)
            obj.put("totalWorkingHours", a.totalWorkingHours)
            obj.put("recordedBy", a.recordedBy)
            obj.put("timestamp", a.timestamp)
            attendanceArray.put(obj)
        }
        root.put("attendance", attendanceArray)

        // 4. Product Catalog
        val productsArray = JSONArray()
        _productCatalog.value.forEach { p ->
            val obj = JSONObject()
            obj.put("name", p.name)
            obj.put("code", p.code)
            productsArray.put(obj)
        }
        root.put("productCatalog", productsArray)

        // 5. Brand Catalog
        val brandsArray = JSONArray()
        _brandCatalog.value.forEach { b ->
            brandsArray.put(b.name)
        }
        root.put("brandCatalog", brandsArray)

        // 6. Batch Catalog
        val batchesArray = JSONArray()
        _batchCatalog.value.forEach { b ->
            batchesArray.put(b.name)
        }
        root.put("batchCatalog", batchesArray)

        // 7. Custom Template Fields
        val customFieldsArray = JSONArray()
        customTemplateFields.value.forEach { f ->
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("fieldName", f.fieldName)
            obj.put("fieldType", f.fieldType)
            obj.put("targetArea", f.targetArea)
            obj.put("processKey", f.processKey)
            obj.put("isColumnVisible", f.isColumnVisible)
            obj.put("timestamp", f.timestamp)
            customFieldsArray.put(obj)
        }
        root.put("customTemplateFields", customFieldsArray)

        return root.toString(2)
    }

    fun downloadFullBackupFile(context: Context) {
        try {
            val jsonContent = exportFullDatabaseBackupJson()
            val filename = "Aldella_Full_Backup_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.json"
            val file = File(context.cacheDir, filename)
            FileOutputStream(file).use { out ->
                out.write(jsonContent.toByteArray())
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TITLE, "Download Full Backup File: $filename")
                putExtra(Intent.EXTRA_SUBJECT, "Aldella Factory Manager Database Backup")
                putExtra(Intent.EXTRA_TEXT, jsonContent)
                type = "application/json"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Download / Save Backup File ($filename)")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)

            logAdminAudit(
                "EXPORT_FULL_BACKUP",
                "database_backup",
                "Admin exported full database backup ($filename) containing ${allProcessSheets.value.size} sheets, ${allComplaintsAndDowntime.value.size} complaints, ${allAttendance.value.size} attendance records."
            )
            Toast.makeText(context, "Backup file prepared: $filename", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to create backup: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    suspend fun restoreDatabaseFromJson(jsonString: String): Pair<Boolean, String> {
        return try {
            val root = JSONObject(jsonString)

            // 1. Process Sheets
            var restoredSheetsCount = 0
            if (root.has("processSheets")) {
                val array = root.getJSONArray("processSheets")
                val sheetsList = mutableListOf<ProcessSheetEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    sheetsList.add(
                        ProcessSheetEntity(
                            id = obj.optLong("id", 0),
                            areaKey = obj.optString("areaKey", "Line 1"),
                            processKey = obj.optString("processKey", "Processing"),
                            productName = obj.optString("productName", ""),
                            productCode = obj.optString("productCode", ""),
                            brand = obj.optString("brand", ""),
                            chickenProduct = obj.optString("chickenProduct", ""),
                            batchNo = obj.optString("batchNo", ""),
                            date = obj.optString("date", ""),
                            startTime = obj.optString("startTime", "--"),
                            endTime = obj.optString("endTime", "--"),
                            operationalTimer = obj.optString("operationalTimer", "00:00:00"),
                            status = obj.optString("status", "COMPLETED"),
                            totalKgQty = obj.optDouble("totalKgQty", 0.0),
                            totalHours = obj.optString("totalHours", "00:00:00"),
                            inputKg = obj.optDouble("inputKg", 0.0),
                            outputKg = obj.optDouble("outputKg", 0.0),
                            productWasteKg = obj.optDouble("productWasteKg", 0.0),
                            waterWasteLitres = obj.optDouble("waterWasteLitres", 0.0),
                            temperatureC = obj.optDouble("temperatureC", 0.0),
                            remarks = obj.optString("remarks", ""),
                            recordedBy = obj.optString("recordedBy", "Aldella Admin"),
                            batchSequence = obj.optInt("batchSequence", 1),
                            isSubmitted = obj.optBoolean("isSubmitted", true),
                            submissionSaudiTime = obj.optString("submissionSaudiTime", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (sheetsList.isNotEmpty()) {
                    repository.insertProcessSheetsBatch(sheetsList)
                    restoredSheetsCount = sheetsList.size
                }
            }

            // 2. Complaints & Downtime
            var restoredComplaintsCount = 0
            if (root.has("complaintsAndDowntime")) {
                val array = root.getJSONArray("complaintsAndDowntime")
                val list = mutableListOf<ComplaintDowntimeEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        ComplaintDowntimeEntity(
                            id = obj.optLong("id", 0),
                            type = obj.optString("type", "DOWNTIME"),
                            areaOrMachine = obj.optString("areaOrMachine", ""),
                            details = obj.optString("details", ""),
                            status = obj.optString("status", "OPEN"),
                            startTimeFormatted = obj.optString("startTimeFormatted", ""),
                            endTimeFormatted = obj.optString("endTimeFormatted", ""),
                            totalHours = obj.optString("totalHours", "00:00:00"),
                            submittedBy = obj.optString("submittedBy", "Admin"),
                            updatedBy = obj.optString("updatedBy", ""),
                            isViewedByAdmin = obj.optBoolean("isViewedByAdmin", true),
                            attachmentsCount = obj.optInt("attachmentsCount", 0),
                            attachmentType = obj.optString("attachmentType", "NONE"),
                            attachmentName = obj.optString("attachmentName", ""),
                            attachmentUri = obj.optString("attachmentUri", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    repository.insertComplaintsBatch(list)
                    restoredComplaintsCount = list.size
                }
            }

            // 3. Attendance
            var restoredAttendanceCount = 0
            if (root.has("attendance")) {
                val array = root.getJSONArray("attendance")
                val list = mutableListOf<AttendanceEntity>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        AttendanceEntity(
                            id = obj.optLong("id", 0),
                            memberId = obj.optString("memberId", ""),
                            fullName = obj.optString("fullName", ""),
                            department = obj.optString("department", "Production"),
                            shiftType = obj.optString("shiftType", "Day"),
                            attendanceDate = obj.optString("attendanceDate", ""),
                            timeIn1 = obj.optString("timeIn1", ""),
                            timeOut1 = obj.optString("timeOut1", ""),
                            timeIn2 = obj.optString("timeIn2", ""),
                            timeOut2 = obj.optString("timeOut2", ""),
                            timeIn3 = obj.optString("timeIn3", ""),
                            timeOut3 = obj.optString("timeOut3", ""),
                            statusRemarks = obj.optString("statusRemarks", ""),
                            manualOverrideReason = obj.optString("manualOverrideReason", ""),
                            totalWorkingHours = obj.optString("totalWorkingHours", "00:00:00"),
                            recordedBy = obj.optString("recordedBy", "Aldella Admin"),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    repository.insertAttendanceBatch(list)
                    restoredAttendanceCount = list.size
                }
            }

            // 4. Products & Catalogs
            if (root.has("productCatalog")) {
                val array = root.getJSONArray("productCatalog")
                val list = mutableListOf<ProductCatalogItem>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(ProductCatalogItem(obj.getString("name"), obj.optString("code", "")))
                }
                if (list.isNotEmpty()) _productCatalog.value = list
            }

            if (root.has("brandCatalog")) {
                val array = root.getJSONArray("brandCatalog")
                val list = mutableListOf<BrandCatalogItem>()
                for (i in 0 until array.length()) {
                    list.add(BrandCatalogItem(array.getString(i)))
                }
                if (list.isNotEmpty()) _brandCatalog.value = list
            }

            if (root.has("batchCatalog")) {
                val array = root.getJSONArray("batchCatalog")
                val list = mutableListOf<BatchCatalogItem>()
                for (i in 0 until array.length()) {
                    list.add(BatchCatalogItem(array.getString(i)))
                }
                if (list.isNotEmpty()) _batchCatalog.value = list
            }

            logAdminAudit(
                "RESTORE_FULL_BACKUP",
                "database_backup",
                "Admin restored full database backup: $restoredSheetsCount sheets, $restoredComplaintsCount complaints, $restoredAttendanceCount attendance records."
            )

            Pair(true, "Restored $restoredSheetsCount batch sheets, $restoredComplaintsCount complaints, $restoredAttendanceCount staff records successfully!")
        } catch (e: Exception) {
            Pair(false, "Restore failed: ${e.message}")
        }
    }

    // ================= FIREBASE FIRESTORE CLOUD STORAGE & BACKUP/RESTORE =================

    fun toggleFirestoreAutoSync(enabled: Boolean) {
        _isFirestoreAutoSync.value = enabled
        FirestoreManager.setAutoSyncEnabled(getApplication(), enabled)
        logAdminAudit(
            "TOGGLE_FIRESTORE_AUTOSYNC",
            "cloud_sync",
            "Permanent cloud auto-sync ${if (enabled) "ENABLED" else "DISABLED"} by ${_currentUser.value.name}"
        )
    }

    fun backupToFirestoreNow(context: Context, onComplete: (Boolean, String) -> Unit) {
        if (_isCloudBackupRunning.value) return
        _isCloudBackupRunning.value = true
        _firestoreSyncStatus.value = "Backing up to Firestore..."

        viewModelScope.launch {
            try {
                val sheets = allProcessSheets.value
                val users = activeUserAccounts.value
                val complaints = allComplaintsAndDowntime.value
                val attendance = allAttendance.value

                val result = FirestoreManager.backupAllToFirestore(
                    context = context,
                    sheets = sheets,
                    users = users,
                    complaints = complaints,
                    attendance = attendance
                )

                _isCloudBackupRunning.value = false
                _firestoreLastSyncTime.value = FirestoreManager.getLastSyncTime(context)

                if (result.success) {
                    _firestoreSyncStatus.value = "Synced with Firestore"
                    logAdminAudit(
                        "FIRESTORE_CLOUD_BACKUP",
                        "cloud_storage",
                        "Admin backed up ${result.sheetsCount} process batches, ${result.usersCount} users, ${result.complaintsCount} complaints, ${result.attendanceCount} attendance records to Firebase Firestore."
                    )
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    onComplete(true, result.message)
                } else {
                    _firestoreSyncStatus.value = "Backup Failed"
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    onComplete(false, result.message)
                }
            } catch (e: Exception) {
                _isCloudBackupRunning.value = false
                _firestoreSyncStatus.value = "Error: ${e.message}"
                val errorMsg = "Firestore backup failed: ${e.message}"
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                onComplete(false, errorMsg)
            }
        }
    }

    fun restoreFromFirestoreNow(context: Context, onComplete: (Boolean, String) -> Unit) {
        if (_isCloudRestoreRunning.value) return
        _isCloudRestoreRunning.value = true
        _firestoreSyncStatus.value = "Restoring from Firestore..."

        viewModelScope.launch {
            try {
                val result = FirestoreManager.restoreAllFromFirestore(context)
                _isCloudRestoreRunning.value = false
                _firestoreLastSyncTime.value = FirestoreManager.getLastSyncTime(context)

                if (result.success) {
                    if (result.sheets.isNotEmpty()) {
                        repository.insertProcessSheetsBatch(result.sheets)
                    }
                    if (result.complaints.isNotEmpty()) {
                        repository.insertComplaintsBatch(result.complaints)
                    }
                    if (result.attendance.isNotEmpty()) {
                        repository.insertAttendanceBatch(result.attendance)
                    }
                    for (user in result.users) {
                        try {
                            repository.createUserAccount(user)
                        } catch (ignored: Exception) { }
                    }

                    _firestoreSyncStatus.value = "Restored from Firestore"
                    logAdminAudit(
                        "FIRESTORE_CLOUD_RESTORE",
                        "cloud_storage",
                        "Admin restored ${result.sheets.size} batches, ${result.users.size} users, ${result.complaints.size} complaints, ${result.attendance.size} attendance records from Firebase Firestore."
                    )
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    onComplete(true, result.message)
                } else {
                    _firestoreSyncStatus.value = "Restore Failed"
                    Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                    onComplete(false, result.message)
                }
            } catch (e: Exception) {
                _isCloudRestoreRunning.value = false
                _firestoreSyncStatus.value = "Error: ${e.message}"
                val errorMsg = "Firestore restore failed: ${e.message}"
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                onComplete(false, errorMsg)
            }
        }
    }

    // ================= PRODUCT CATALOG MASTER MANAGEMENT (ADMIN) =================
    fun addProduct(name: String, code: String) {
        if (name.isBlank()) return
        val current = _productCatalog.value.toMutableList()
        if (current.none { it.name.equals(name.trim(), ignoreCase = true) }) {
            current.add(ProductCatalogItem(name.trim(), code.trim().ifBlank { "PRD-${System.currentTimeMillis() % 1000}" }))
            _productCatalog.value = current
            logAdminAudit("ADD_PRODUCT", "catalog", "Added product: '${name.trim()}' (${code.trim()})")
        }
    }

    fun renameProduct(oldName: String, newName: String, newCode: String) {
        if (newName.isBlank()) return
        val current = _productCatalog.value.map {
            if (it.name.equals(oldName, ignoreCase = true)) {
                ProductCatalogItem(newName.trim(), newCode.trim().ifBlank { it.code })
            } else it
        }
        _productCatalog.value = current
        logAdminAudit("RENAME_PRODUCT", "catalog", "Renamed product '$oldName' to '${newName.trim()}'")
    }

    fun deleteProductPermanently(name: String) {
        val current = _productCatalog.value.filterNot { it.name.equals(name, ignoreCase = true) }
        _productCatalog.value = current
        logAdminAudit("PERMANENT_DELETE_PRODUCT", "catalog", "Permanently deleted product '$name'")
    }

    // ================= BRAND CATALOG MASTER MANAGEMENT (ADMIN) =================
    fun addBrand(brand: String) {
        if (brand.isBlank()) return
        val current = _brandCatalog.value.toMutableList()
        if (current.none { it.name.equals(brand.trim(), ignoreCase = true) }) {
            current.add(BrandCatalogItem(brand.trim()))
            _brandCatalog.value = current
            logAdminAudit("ADD_BRAND", "catalog", "Added brand: '${brand.trim()}'")
        }
    }

    fun renameBrand(oldBrand: String, newBrand: String) {
        if (newBrand.isBlank()) return
        val current = _brandCatalog.value.map {
            if (it.name.equals(oldBrand, ignoreCase = true)) BrandCatalogItem(newBrand.trim()) else it
        }
        _brandCatalog.value = current
        logAdminAudit("RENAME_BRAND", "catalog", "Renamed brand '$oldBrand' to '${newBrand.trim()}'")
    }

    fun deleteBrandPermanently(brand: String) {
        val current = _brandCatalog.value.filterNot { it.name.equals(brand, ignoreCase = true) }
        _brandCatalog.value = current
        logAdminAudit("PERMANENT_DELETE_BRAND", "catalog", "Permanently deleted brand '$brand'")
    }

    // ================= BATCH IDENTIFIERS & RECORD MANAGEMENT (ADMIN) =================
    fun addBatchNo(batchNo: String) {
        if (batchNo.isBlank()) return
        val current = _batchCatalog.value.toMutableList()
        if (current.none { it.name.equals(batchNo.trim(), ignoreCase = true) }) {
            current.add(BatchCatalogItem(batchNo.trim()))
            _batchCatalog.value = current
            logAdminAudit("ADD_BATCH_IDENTIFIER", "catalog", "Added batch identifier: '${batchNo.trim()}'")
        }
    }

    fun renameBatchNo(oldBatchNo: String, newBatchNo: String) {
        if (newBatchNo.isBlank()) return
        val current = _batchCatalog.value.map {
            if (it.name.equals(oldBatchNo, ignoreCase = true)) BatchCatalogItem(newBatchNo.trim()) else it
        }
        _batchCatalog.value = current
        logAdminAudit("RENAME_BATCH_IDENTIFIER", "catalog", "Renamed batch '$oldBatchNo' to '${newBatchNo.trim()}'")
    }

    fun deleteBatchNoPermanently(batchNo: String) {
        val current = _batchCatalog.value.filterNot { it.name.equals(batchNo, ignoreCase = true) }
        _batchCatalog.value = current
        logAdminAudit("PERMANENT_DELETE_BATCH_IDENTIFIER", "catalog", "Permanently deleted batch identifier '$batchNo'")
    }

    fun deleteProcessSheetPermanently(sheet: ProcessSheetEntity) {
        viewModelScope.launch {
            repository.deleteProcessSheet(sheet)
            logAdminAudit(
                "PERMANENT_DELETE_BATCH_RECORD",
                "process_sheet",
                "Permanently deleted batch record #${sheet.id} (${sheet.batchNo} - ${sheet.areaKey} / ${sheet.processKey})"
            )
        }
    }

    private fun logAdminAudit(action: String, category: String, description: String) {
        viewModelScope.launch {
            repository.logAudit(
                AuditLogEntity(
                    actionType = action,
                    category = category,
                    description = description,
                    timestampFormatted = _currentTimestamp.value,
                    performedBy = _currentUser.value.name
                )
            )
        }
    }
}
