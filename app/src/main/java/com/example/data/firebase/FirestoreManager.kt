package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.ComplaintDowntimeEntity
import com.example.data.local.entities.ProcessSheetEntity
import com.example.data.local.entities.UserAccountEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class FirestoreBackupResult(
    val success: Boolean,
    val sheetsCount: Int = 0,
    val usersCount: Int = 0,
    val complaintsCount: Int = 0,
    val attendanceCount: Int = 0,
    val message: String = ""
)

data class FirestoreRestoreResult(
    val success: Boolean,
    val sheets: List<ProcessSheetEntity> = emptyList(),
    val users: List<UserAccountEntity> = emptyList(),
    val complaints: List<ComplaintDowntimeEntity> = emptyList(),
    val attendance: List<AttendanceEntity> = emptyList(),
    val message: String = ""
)

data class FirestoreStats(
    val sheetsCount: Int = 0,
    val usersCount: Int = 0,
    val complaintsCount: Int = 0,
    val attendanceCount: Int = 0,
    val lastSyncFormatted: String = "Never",
    val isConnected: Boolean = true
)

object FirestoreManager {
    private const val TAG = "FirestoreManager"
    private const val PREFS_NAME = "aldella_firestore_prefs"
    private const val KEY_PROJECT_ID = "firestore_project_id"
    private const val KEY_AUTO_SYNC = "firestore_auto_sync_enabled"
    private const val KEY_LAST_SYNC = "firestore_last_sync_time"

    const val COLLECTION_PROCESS_SHEETS = "production_processes"
    const val COLLECTION_USER_STATUS = "user_status"
    const val COLLECTION_COMPLAINTS = "complaints_downtime"
    const val COLLECTION_ATTENDANCE = "attendance_records"

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    // Await helper for Firebase Tasks
    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result ->
            if (continuation.isActive) continuation.resume(result)
        }
        addOnFailureListener { exception ->
            if (continuation.isActive) continuation.resumeWithException(exception)
        }
        addOnCanceledListener {
            if (continuation.isActive) continuation.cancel()
        }
    }

    /**
     * Initializes or retrieves the FirebaseFirestore instance.
     * Works with standard google-services.json OR manual Firebase configuration fallback.
     */
    fun getFirestore(context: Context): FirebaseFirestore? {
        if (firestoreInstance != null) return firestoreInstance

        synchronized(this) {
            if (firestoreInstance != null) return firestoreInstance

            try {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                    val projectId = prefs.getString(KEY_PROJECT_ID, "aldella-factory-operations") ?: "aldella-factory-operations"
                    val options = FirebaseOptions.Builder()
                        .setProjectId(projectId)
                        .setApplicationId("com.aistudio.applet.bsugof")
                        .setApiKey("AIzaSyAldellaFactoryOpsKeyPermanentStorage2026")
                        .build()
                    FirebaseApp.initializeApp(context, options)
                    Log.i(TAG, "Initialized FirebaseApp with fallback project: $projectId")
                }

                val db = FirebaseFirestore.getInstance()
                firestoreInstance = db
                return db
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing FirebaseFirestore: ${e.message}", e)
                return null
            }
        }
    }

    fun isAutoSyncEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_AUTO_SYNC, true) // Enabled by default for permanent cloud persistence
    }

    fun setAutoSyncEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_SYNC, enabled)
            .apply()
    }

    fun getLastSyncTime(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_SYNC, "Never") ?: "Never"
    }

    private fun updateLastSyncTime(context: Context) {
        val saudiFormatter = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Riyadh")
        }
        val nowFormatted = saudiFormatter.format(Date()) + " (KSA)"
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_SYNC, nowFormatted)
            .apply()
    }

    // =========================================================================
    // INDIVIDUAL ENTITY CLOUD STORAGE (Real-time & Permanent Sync)
    // =========================================================================

    suspend fun saveProcessSheet(context: Context, sheet: ProcessSheetEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context) ?: return@withContext false
            val data = mapOf(
                "id" to sheet.id,
                "areaKey" to sheet.areaKey,
                "processKey" to sheet.processKey,
                "productName" to sheet.productName,
                "productCode" to sheet.productCode,
                "brand" to sheet.brand,
                "chickenProduct" to sheet.chickenProduct,
                "batchNo" to sheet.batchNo,
                "date" to sheet.date,
                "startTime" to sheet.startTime,
                "endTime" to sheet.endTime,
                "operationalTimer" to sheet.operationalTimer,
                "status" to sheet.status,
                "totalKgQty" to sheet.totalKgQty,
                "totalHours" to sheet.totalHours,
                "inputKg" to sheet.inputKg,
                "outputKg" to sheet.outputKg,
                "productWasteKg" to sheet.productWasteKg,
                "waterWasteLitres" to sheet.waterWasteLitres,
                "temperatureC" to sheet.temperatureC,
                "wasteType" to sheet.wasteType,
                "remarks" to sheet.remarks,
                "copy1Attached" to sheet.copy1Attached,
                "copy2Attached" to sheet.copy2Attached,
                "recordedBy" to sheet.recordedBy,
                "batchSequence" to sheet.batchSequence,
                "isSubmitted" to sheet.isSubmitted,
                "submissionSaudiTime" to sheet.submissionSaudiTime,
                "timestamp" to sheet.timestamp,
                "cloudSyncedAt" to System.currentTimeMillis()
            )
            val docId = if (sheet.id > 0) sheet.id.toString() else "${sheet.areaKey}_${sheet.batchNo}_${sheet.timestamp}"
            db.collection(COLLECTION_PROCESS_SHEETS)
                .document(docId)
                .set(data, SetOptions.merge())
                .awaitTask()
            updateLastSyncTime(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save process sheet to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deleteProcessSheet(context: Context, sheetId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context) ?: return@withContext false
            db.collection(COLLECTION_PROCESS_SHEETS)
                .document(sheetId.toString())
                .delete()
                .awaitTask()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete process sheet from Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun syncUserStatus(context: Context, user: UserAccountEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context) ?: return@withContext false
            val data = mapOf(
                "id" to user.id,
                "email" to user.email,
                "name" to user.name,
                "role" to user.role,
                "isApproved" to user.isApproved,
                "isDeleted" to user.isDeleted,
                "isOnline" to user.isOnline,
                "lastActiveFormatted" to user.lastActiveFormatted,
                "lastActiveTimestamp" to user.lastActiveTimestamp,
                "timestamp" to user.timestamp,
                "cloudSyncedAt" to System.currentTimeMillis()
            )
            val docId = if (user.id > 0) user.id.toString() else user.email.replace(".", "_")
            db.collection(COLLECTION_USER_STATUS)
                .document(docId)
                .set(data, SetOptions.merge())
                .awaitTask()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user status to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun saveComplaint(context: Context, complaint: ComplaintDowntimeEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context) ?: return@withContext false
            val data = mapOf(
                "id" to complaint.id,
                "type" to complaint.type,
                "areaOrMachine" to complaint.areaOrMachine,
                "details" to complaint.details,
                "status" to complaint.status,
                "startTimeFormatted" to complaint.startTimeFormatted,
                "endTimeFormatted" to complaint.endTimeFormatted,
                "totalHours" to complaint.totalHours,
                "submittedBy" to complaint.submittedBy,
                "updatedBy" to complaint.updatedBy,
                "isViewedByAdmin" to complaint.isViewedByAdmin,
                "attachmentsCount" to complaint.attachmentsCount,
                "attachmentType" to complaint.attachmentType,
                "attachmentName" to complaint.attachmentName,
                "attachmentUri" to complaint.attachmentUri,
                "timestamp" to complaint.timestamp,
                "cloudSyncedAt" to System.currentTimeMillis()
            )
            val docId = if (complaint.id > 0) complaint.id.toString() else "${complaint.type}_${complaint.timestamp}"
            db.collection(COLLECTION_COMPLAINTS)
                .document(docId)
                .set(data, SetOptions.merge())
                .awaitTask()
            updateLastSyncTime(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save complaint to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deleteComplaint(context: Context, complaintId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context) ?: return@withContext false
            db.collection(COLLECTION_COMPLAINTS)
                .document(complaintId.toString())
                .delete()
                .awaitTask()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete complaint from Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun saveAttendance(context: Context, attendance: AttendanceEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context) ?: return@withContext false
            val data = mapOf(
                "id" to attendance.id,
                "memberId" to attendance.memberId,
                "fullName" to attendance.fullName,
                "department" to attendance.department,
                "shiftType" to attendance.shiftType,
                "attendanceDate" to attendance.attendanceDate,
                "timeIn1" to attendance.timeIn1,
                "timeOut1" to attendance.timeOut1,
                "timeIn2" to attendance.timeIn2,
                "timeOut2" to attendance.timeOut2,
                "timeIn3" to attendance.timeIn3,
                "timeOut3" to attendance.timeOut3,
                "statusRemarks" to attendance.statusRemarks,
                "manualOverrideReason" to attendance.manualOverrideReason,
                "totalWorkingHours" to attendance.totalWorkingHours,
                "recordedBy" to attendance.recordedBy,
                "timestamp" to attendance.timestamp,
                "cloudSyncedAt" to System.currentTimeMillis()
            )
            val docId = if (attendance.id > 0) attendance.id.toString() else "${attendance.memberId}_${attendance.attendanceDate}"
            db.collection(COLLECTION_ATTENDANCE)
                .document(docId)
                .set(data, SetOptions.merge())
                .awaitTask()
            updateLastSyncTime(context)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save attendance to Firestore: ${e.message}", e)
            false
        }
    }

    suspend fun deleteAttendance(context: Context, attendanceId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context) ?: return@withContext false
            db.collection(COLLECTION_ATTENDANCE)
                .document(attendanceId.toString())
                .delete()
                .awaitTask()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete attendance from Firestore: ${e.message}", e)
            false
        }
    }

    // =========================================================================
    // FULL CLOUD BACKUP (Upload all records to Firestore)
    // =========================================================================

    suspend fun backupAllToFirestore(
        context: Context,
        sheets: List<ProcessSheetEntity>,
        users: List<UserAccountEntity>,
        complaints: List<ComplaintDowntimeEntity>,
        attendance: List<AttendanceEntity>
    ): FirestoreBackupResult = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context)
                ?: return@withContext FirestoreBackupResult(
                    success = false,
                    message = "Could not initialize Firestore. Check connection or project settings."
                )

            var savedSheets = 0
            var savedUsers = 0
            var savedComplaints = 0
            var savedAttendance = 0

            // 1. Process Sheets
            for (sheet in sheets) {
                val data = mapOf(
                    "id" to sheet.id,
                    "areaKey" to sheet.areaKey,
                    "processKey" to sheet.processKey,
                    "productName" to sheet.productName,
                    "productCode" to sheet.productCode,
                    "brand" to sheet.brand,
                    "chickenProduct" to sheet.chickenProduct,
                    "batchNo" to sheet.batchNo,
                    "date" to sheet.date,
                    "startTime" to sheet.startTime,
                    "endTime" to sheet.endTime,
                    "operationalTimer" to sheet.operationalTimer,
                    "status" to sheet.status,
                    "totalKgQty" to sheet.totalKgQty,
                    "totalHours" to sheet.totalHours,
                    "inputKg" to sheet.inputKg,
                    "outputKg" to sheet.outputKg,
                    "productWasteKg" to sheet.productWasteKg,
                    "waterWasteLitres" to sheet.waterWasteLitres,
                    "temperatureC" to sheet.temperatureC,
                    "wasteType" to sheet.wasteType,
                    "remarks" to sheet.remarks,
                    "copy1Attached" to sheet.copy1Attached,
                    "copy2Attached" to sheet.copy2Attached,
                    "recordedBy" to sheet.recordedBy,
                    "batchSequence" to sheet.batchSequence,
                    "isSubmitted" to sheet.isSubmitted,
                    "submissionSaudiTime" to sheet.submissionSaudiTime,
                    "timestamp" to sheet.timestamp,
                    "cloudSyncedAt" to System.currentTimeMillis()
                )
                db.collection(COLLECTION_PROCESS_SHEETS)
                    .document(sheet.id.toString())
                    .set(data, SetOptions.merge())
                    .awaitTask()
                savedSheets++
            }

            // 2. User Statuses
            for (user in users) {
                val data = mapOf(
                    "id" to user.id,
                    "email" to user.email,
                    "name" to user.name,
                    "role" to user.role,
                    "isApproved" to user.isApproved,
                    "isDeleted" to user.isDeleted,
                    "isOnline" to user.isOnline,
                    "lastActiveFormatted" to user.lastActiveFormatted,
                    "lastActiveTimestamp" to user.lastActiveTimestamp,
                    "timestamp" to user.timestamp,
                    "cloudSyncedAt" to System.currentTimeMillis()
                )
                db.collection(COLLECTION_USER_STATUS)
                    .document(user.id.toString())
                    .set(data, SetOptions.merge())
                    .awaitTask()
                savedUsers++
            }

            // 3. Complaints & Downtime
            for (complaint in complaints) {
                val data = mapOf(
                    "id" to complaint.id,
                    "type" to complaint.type,
                    "areaOrMachine" to complaint.areaOrMachine,
                    "details" to complaint.details,
                    "status" to complaint.status,
                    "startTimeFormatted" to complaint.startTimeFormatted,
                    "endTimeFormatted" to complaint.endTimeFormatted,
                    "totalHours" to complaint.totalHours,
                    "submittedBy" to complaint.submittedBy,
                    "updatedBy" to complaint.updatedBy,
                    "isViewedByAdmin" to complaint.isViewedByAdmin,
                    "attachmentsCount" to complaint.attachmentsCount,
                    "attachmentType" to complaint.attachmentType,
                    "attachmentName" to complaint.attachmentName,
                    "attachmentUri" to complaint.attachmentUri,
                    "timestamp" to complaint.timestamp,
                    "cloudSyncedAt" to System.currentTimeMillis()
                )
                db.collection(COLLECTION_COMPLAINTS)
                    .document(complaint.id.toString())
                    .set(data, SetOptions.merge())
                    .awaitTask()
                savedComplaints++
            }

            // 4. Attendance
            for (record in attendance) {
                val data = mapOf(
                    "id" to record.id,
                    "memberId" to record.memberId,
                    "fullName" to record.fullName,
                    "department" to record.department,
                    "shiftType" to record.shiftType,
                    "attendanceDate" to record.attendanceDate,
                    "timeIn1" to record.timeIn1,
                    "timeOut1" to record.timeOut1,
                    "timeIn2" to record.timeIn2,
                    "timeOut2" to record.timeOut2,
                    "timeIn3" to record.timeIn3,
                    "timeOut3" to record.timeOut3,
                    "statusRemarks" to record.statusRemarks,
                    "manualOverrideReason" to record.manualOverrideReason,
                    "totalWorkingHours" to record.totalWorkingHours,
                    "recordedBy" to record.recordedBy,
                    "timestamp" to record.timestamp,
                    "cloudSyncedAt" to System.currentTimeMillis()
                )
                db.collection(COLLECTION_ATTENDANCE)
                    .document(record.id.toString())
                    .set(data, SetOptions.merge())
                    .awaitTask()
                savedAttendance++
            }

            updateLastSyncTime(context)

            FirestoreBackupResult(
                success = true,
                sheetsCount = savedSheets,
                usersCount = savedUsers,
                complaintsCount = savedComplaints,
                attendanceCount = savedAttendance,
                message = "Permanent cloud storage completed: $savedSheets batches, $savedUsers users, $savedComplaints complaints, $savedAttendance attendance records."
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firestore full backup failed: ${e.message}", e)
            FirestoreBackupResult(
                success = false,
                message = "Backup error: ${e.message}"
            )
        }
    }

    // =========================================================================
    // FULL CLOUD RESTORE (Download all records from Firestore)
    // =========================================================================

    suspend fun restoreAllFromFirestore(context: Context): FirestoreRestoreResult = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore(context)
                ?: return@withContext FirestoreRestoreResult(
                    success = false,
                    message = "Could not initialize Firestore. Check connection or project settings."
                )

            // 1. Process Sheets
            val sheetsSnap = db.collection(COLLECTION_PROCESS_SHEETS).get().awaitTask()
            val sheetsList = mutableListOf<ProcessSheetEntity>()
            for (doc in sheetsSnap.documents) {
                parseProcessSheet(doc)?.let { sheetsList.add(it) }
            }

            // 2. User Statuses
            val usersSnap = db.collection(COLLECTION_USER_STATUS).get().awaitTask()
            val usersList = mutableListOf<UserAccountEntity>()
            for (doc in usersSnap.documents) {
                parseUserAccount(doc)?.let { usersList.add(it) }
            }

            // 3. Complaints
            val complaintsSnap = db.collection(COLLECTION_COMPLAINTS).get().awaitTask()
            val complaintsList = mutableListOf<ComplaintDowntimeEntity>()
            for (doc in complaintsSnap.documents) {
                parseComplaint(doc)?.let { complaintsList.add(it) }
            }

            // 4. Attendance
            val attendanceSnap = db.collection(COLLECTION_ATTENDANCE).get().awaitTask()
            val attendanceList = mutableListOf<AttendanceEntity>()
            for (doc in attendanceSnap.documents) {
                parseAttendance(doc)?.let { attendanceList.add(it) }
            }

            updateLastSyncTime(context)

            FirestoreRestoreResult(
                success = true,
                sheets = sheetsList,
                users = usersList,
                complaints = complaintsList,
                attendance = attendanceList,
                message = "Restored ${sheetsList.size} batches, ${usersList.size} users, ${complaintsList.size} complaints, ${attendanceList.size} attendance records from Firestore!"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Firestore full restore failed: ${e.message}", e)
            FirestoreRestoreResult(
                success = false,
                message = "Cloud restore failed: ${e.message}"
            )
        }
    }

    // =========================================================================
    // PARSING HELPERS FROM FIRESTORE DOCUMENTS
    // =========================================================================

    private fun parseProcessSheet(doc: DocumentSnapshot): ProcessSheetEntity? {
        return try {
            val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
            ProcessSheetEntity(
                id = id,
                areaKey = doc.getString("areaKey") ?: "Line 1",
                processKey = doc.getString("processKey") ?: "Process",
                productName = doc.getString("productName") ?: "",
                productCode = doc.getString("productCode") ?: "",
                brand = doc.getString("brand") ?: "",
                chickenProduct = doc.getString("chickenProduct") ?: "",
                batchNo = doc.getString("batchNo") ?: "",
                date = doc.getString("date") ?: "",
                startTime = doc.getString("startTime") ?: "--",
                endTime = doc.getString("endTime") ?: "--",
                operationalTimer = doc.getString("operationalTimer") ?: "00:00:00",
                status = doc.getString("status") ?: "COMPLETED",
                totalKgQty = doc.getDouble("totalKgQty") ?: 0.0,
                totalHours = doc.getString("totalHours") ?: "00:00:00",
                inputKg = doc.getDouble("inputKg") ?: 0.0,
                outputKg = doc.getDouble("outputKg") ?: 0.0,
                productWasteKg = doc.getDouble("productWasteKg") ?: 0.0,
                waterWasteLitres = doc.getDouble("waterWasteLitres") ?: 0.0,
                temperatureC = doc.getDouble("temperatureC") ?: 0.0,
                wasteType = doc.getString("wasteType") ?: "Product / Waste",
                remarks = doc.getString("remarks") ?: "",
                copy1Attached = doc.getBoolean("copy1Attached") ?: false,
                copy2Attached = doc.getBoolean("copy2Attached") ?: false,
                recordedBy = doc.getString("recordedBy") ?: "Aldella Admin",
                batchSequence = doc.getLong("batchSequence")?.toInt() ?: 1,
                isSubmitted = doc.getBoolean("isSubmitted") ?: true,
                submissionSaudiTime = doc.getString("submissionSaudiTime") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse ProcessSheet document ${doc.id}: ${e.message}")
            null
        }
    }

    private fun parseUserAccount(doc: DocumentSnapshot): UserAccountEntity? {
        return try {
            val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
            UserAccountEntity(
                id = id,
                email = doc.getString("email") ?: "",
                name = doc.getString("name") ?: "",
                role = doc.getString("role") ?: "OPERATOR",
                isApproved = doc.getBoolean("isApproved") ?: true,
                isDeleted = doc.getBoolean("isDeleted") ?: false,
                isOnline = doc.getBoolean("isOnline") ?: false,
                lastActiveFormatted = doc.getString("lastActiveFormatted") ?: "",
                lastActiveTimestamp = doc.getLong("lastActiveTimestamp") ?: System.currentTimeMillis(),
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse UserAccount document ${doc.id}: ${e.message}")
            null
        }
    }

    private fun parseComplaint(doc: DocumentSnapshot): ComplaintDowntimeEntity? {
        return try {
            val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
            ComplaintDowntimeEntity(
                id = id,
                type = doc.getString("type") ?: "DOWNTIME",
                areaOrMachine = doc.getString("areaOrMachine") ?: "",
                details = doc.getString("details") ?: "",
                status = doc.getString("status") ?: "OPEN",
                startTimeFormatted = doc.getString("startTimeFormatted") ?: "",
                endTimeFormatted = doc.getString("endTimeFormatted") ?: "",
                totalHours = doc.getString("totalHours") ?: "00:00:00",
                submittedBy = doc.getString("submittedBy") ?: "",
                updatedBy = doc.getString("updatedBy") ?: "",
                isViewedByAdmin = doc.getBoolean("isViewedByAdmin") ?: false,
                attachmentsCount = doc.getLong("attachmentsCount")?.toInt() ?: 0,
                attachmentType = doc.getString("attachmentType") ?: "NONE",
                attachmentName = doc.getString("attachmentName") ?: "",
                attachmentUri = doc.getString("attachmentUri") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Complaint document ${doc.id}: ${e.message}")
            null
        }
    }

    private fun parseAttendance(doc: DocumentSnapshot): AttendanceEntity? {
        return try {
            val id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
            AttendanceEntity(
                id = id,
                memberId = doc.getString("memberId") ?: "",
                fullName = doc.getString("fullName") ?: "",
                department = doc.getString("department") ?: "Production",
                shiftType = doc.getString("shiftType") ?: "Day",
                attendanceDate = doc.getString("attendanceDate") ?: "",
                timeIn1 = doc.getString("timeIn1") ?: "",
                timeOut1 = doc.getString("timeOut1") ?: "",
                timeIn2 = doc.getString("timeIn2") ?: "",
                timeOut2 = doc.getString("timeOut2") ?: "",
                timeIn3 = doc.getString("timeIn3") ?: "",
                timeOut3 = doc.getString("timeOut3") ?: "",
                statusRemarks = doc.getString("statusRemarks") ?: "",
                manualOverrideReason = doc.getString("manualOverrideReason") ?: "",
                totalWorkingHours = doc.getString("totalWorkingHours") ?: "00:00:00",
                recordedBy = doc.getString("recordedBy") ?: "Aldella Admin",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse Attendance document ${doc.id}: ${e.message}")
            null
        }
    }
}
