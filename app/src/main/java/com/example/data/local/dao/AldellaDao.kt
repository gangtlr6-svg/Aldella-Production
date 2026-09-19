package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AldellaDao {

    // Attendance
    @Query("SELECT * FROM attendance_records ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: AttendanceEntity): Long

    @Update
    suspend fun updateAttendance(attendance: AttendanceEntity)

    @Delete
    suspend fun deleteAttendance(attendance: AttendanceEntity)

    // Complaints & Downtime
    @Query("SELECT * FROM complaint_downtime_records ORDER BY timestamp DESC")
    fun getAllComplaintsAndDowntime(): Flow<List<ComplaintDowntimeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaintOrDowntime(record: ComplaintDowntimeEntity): Long

    @Update
    suspend fun updateComplaintOrDowntime(record: ComplaintDowntimeEntity)

    @Delete
    suspend fun deleteComplaintOrDowntime(record: ComplaintDowntimeEntity)

    @Query("UPDATE complaint_downtime_records SET isViewedByAdmin = 1 WHERE id = :id")
    suspend fun markComplaintAsViewed(id: Long)

    @Query("UPDATE complaint_downtime_records SET isViewedByAdmin = 1")
    suspend fun markAllComplaintsAsViewed()

    // Process Sheets
    @Query("SELECT * FROM process_sheets ORDER BY timestamp DESC")
    fun getAllProcessSheets(): Flow<List<ProcessSheetEntity>>

    @Query("SELECT * FROM process_sheets WHERE areaKey = :areaKey AND processKey = :processKey ORDER BY timestamp DESC")
    fun getSheetsForProcess(areaKey: String, processKey: String): Flow<List<ProcessSheetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessSheet(sheet: ProcessSheetEntity): Long

    @Update
    suspend fun updateProcessSheet(sheet: ProcessSheetEntity)

    @Delete
    suspend fun deleteProcessSheet(sheet: ProcessSheetEntity)

    // Chat Messages
    @Query("SELECT * FROM chat_messages WHERE isSoftDeleted = 0 ORDER BY timestamp ASC")
    fun getActiveChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET isSoftDeleted = 1 WHERE id = :id")
    suspend fun softDeleteChatMessage(id: Long)

    // Audit Logs
    @Query("SELECT * FROM admin_audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity): Long

    // User Accounts
    @Query("SELECT * FROM user_accounts WHERE isDeleted = 0 ORDER BY id ASC")
    fun getActiveUserAccounts(): Flow<List<UserAccountEntity>>

    @Query("SELECT * FROM user_accounts WHERE isDeleted = 1 ORDER BY id ASC")
    fun getDeletedUserAccounts(): Flow<List<UserAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAccount(user: UserAccountEntity): Long

    @Update
    suspend fun updateUserAccount(user: UserAccountEntity)

    @Query("UPDATE user_accounts SET isOnline = :isOnline, lastActiveTimestamp = :lastActiveTimestamp, lastActiveFormatted = :lastActiveFormatted WHERE email = :email")
    suspend fun updateUserOnlineStatus(email: String, isOnline: Boolean, lastActiveTimestamp: Long, lastActiveFormatted: String)

    @Query("UPDATE user_accounts SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteUserAccount(id: Long)

    @Query("UPDATE user_accounts SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreUserAccount(id: Long)

    @Delete
    suspend fun hardDeleteUserAccount(user: UserAccountEntity)

    // Custom Template Fields
    @Query("SELECT * FROM custom_template_fields ORDER BY timestamp DESC")
    fun getCustomTemplateFields(): Flow<List<CustomTemplateFieldEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomTemplateField(field: CustomTemplateFieldEntity): Long

    @Update
    suspend fun updateCustomTemplateField(field: CustomTemplateFieldEntity)

    @Delete
    suspend fun deleteCustomTemplateField(field: CustomTemplateFieldEntity)

    // Batch Inserts for Backup & Restore
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcessSheetsBatch(sheets: List<ProcessSheetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComplaintsBatch(records: List<ComplaintDowntimeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceBatch(records: List<AttendanceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomTemplateFieldsBatch(fields: List<CustomTemplateFieldEntity>)
}
