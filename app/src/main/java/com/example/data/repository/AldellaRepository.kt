package com.example.data.repository

import com.example.data.local.dao.AldellaDao
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

class AldellaRepository(private val dao: AldellaDao) {

    val allAttendance: Flow<List<AttendanceEntity>> = dao.getAllAttendance()
    val allComplaintsAndDowntime: Flow<List<ComplaintDowntimeEntity>> = dao.getAllComplaintsAndDowntime()
    val allProcessSheets: Flow<List<ProcessSheetEntity>> = dao.getAllProcessSheets()
    val activeChatMessages: Flow<List<ChatMessageEntity>> = dao.getActiveChatMessages()
    val allAuditLogs: Flow<List<AuditLogEntity>> = dao.getAllAuditLogs()
    val activeUserAccounts: Flow<List<UserAccountEntity>> = dao.getActiveUserAccounts()
    val deletedUserAccounts: Flow<List<UserAccountEntity>> = dao.getDeletedUserAccounts()
    val customTemplateFields: Flow<List<CustomTemplateFieldEntity>> = dao.getCustomTemplateFields()

    fun getSheetsForProcess(areaKey: String, processKey: String): Flow<List<ProcessSheetEntity>> =
        dao.getSheetsForProcess(areaKey, processKey)

    suspend fun saveAttendance(record: AttendanceEntity) = dao.insertAttendance(record)
    suspend fun updateAttendance(record: AttendanceEntity) = dao.updateAttendance(record)
    suspend fun deleteAttendance(record: AttendanceEntity) = dao.deleteAttendance(record)

    suspend fun saveComplaintOrDowntime(record: ComplaintDowntimeEntity) = dao.insertComplaintOrDowntime(record)
    suspend fun updateComplaintOrDowntime(record: ComplaintDowntimeEntity) = dao.updateComplaintOrDowntime(record)
    suspend fun deleteComplaintOrDowntime(record: ComplaintDowntimeEntity) = dao.deleteComplaintOrDowntime(record)
    suspend fun markComplaintAsViewed(id: Long) = dao.markComplaintAsViewed(id)
    suspend fun markAllComplaintsAsViewed() = dao.markAllComplaintsAsViewed()

    suspend fun saveProcessSheet(sheet: ProcessSheetEntity) = dao.insertProcessSheet(sheet)
    suspend fun updateProcessSheet(sheet: ProcessSheetEntity) = dao.updateProcessSheet(sheet)
    suspend fun deleteProcessSheet(sheet: ProcessSheetEntity) = dao.deleteProcessSheet(sheet)

    suspend fun sendChatMessage(message: ChatMessageEntity) = dao.insertChatMessage(message)
    suspend fun softDeleteChatMessage(id: Long) = dao.softDeleteChatMessage(id)

    suspend fun logAudit(log: AuditLogEntity) = dao.insertAuditLog(log)

    suspend fun createUserAccount(user: UserAccountEntity) = dao.insertUserAccount(user)
    suspend fun updateUserAccount(user: UserAccountEntity) = dao.updateUserAccount(user)
    suspend fun updateUserOnlineStatus(email: String, isOnline: Boolean, lastActiveTimestamp: Long, lastActiveFormatted: String) =
        dao.updateUserOnlineStatus(email, isOnline, lastActiveTimestamp, lastActiveFormatted)
    suspend fun softDeleteUserAccount(id: Long) = dao.softDeleteUserAccount(id)
    suspend fun hardDeleteUserAccount(user: UserAccountEntity) = dao.hardDeleteUserAccount(user)
    suspend fun restoreUserAccount(id: Long) = dao.restoreUserAccount(id)

    suspend fun addCustomTemplateField(field: CustomTemplateFieldEntity) = dao.insertCustomTemplateField(field)
    suspend fun updateCustomTemplateField(field: CustomTemplateFieldEntity) = dao.updateCustomTemplateField(field)
    suspend fun deleteCustomTemplateField(field: CustomTemplateFieldEntity) = dao.deleteCustomTemplateField(field)

    suspend fun insertProcessSheetsBatch(sheets: List<ProcessSheetEntity>) = dao.insertProcessSheetsBatch(sheets)
    suspend fun insertComplaintsBatch(records: List<ComplaintDowntimeEntity>) = dao.insertComplaintsBatch(records)
    suspend fun insertAttendanceBatch(records: List<AttendanceEntity>) = dao.insertAttendanceBatch(records)
    suspend fun insertCustomTemplateFieldsBatch(fields: List<CustomTemplateFieldEntity>) = dao.insertCustomTemplateFieldsBatch(fields)
}
