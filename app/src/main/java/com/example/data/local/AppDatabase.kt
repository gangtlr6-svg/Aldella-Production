package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AldellaDao
import com.example.data.local.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AttendanceEntity::class,
        ComplaintDowntimeEntity::class,
        ProcessSheetEntity::class,
        ChatMessageEntity::class,
        AuditLogEntity::class,
        UserAccountEntity::class,
        CustomTemplateFieldEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun aldellaDao(): AldellaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aldella_factory.db"
                ).fallbackToDestructiveMigration()
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.aldellaDao())
                    }
                }
            }

            suspend fun populateInitialData(dao: AldellaDao) {
                // Initial accounts matching screenshot
                dao.insertUserAccount(
                    UserAccountEntity(
                        email = "admin@aldella.com",
                        name = "Aldella Admin",
                        role = "ADMIN",
                        lastActiveFormatted = "ONLINE • Active workspace",
                        isOnline = true,
                        lastActiveTimestamp = System.currentTimeMillis()
                    )
                )
                dao.insertUserAccount(
                    UserAccountEntity(
                        email = "supervisor@aldella.com",
                        name = "Production Supervisor",
                        role = "SUPERVISOR",
                        lastActiveFormatted = "Active 5m ago",
                        isOnline = false,
                        lastActiveTimestamp = System.currentTimeMillis() - (5 * 60 * 1000)
                    )
                )
                dao.insertUserAccount(
                    UserAccountEntity(
                        email = "operator1@aldella.com",
                        name = "Operator 1",
                        role = "OPERATOR",
                        lastActiveFormatted = "Active 15m ago",
                        isOnline = false,
                        lastActiveTimestamp = System.currentTimeMillis() - (15 * 60 * 1000)
                    )
                )

                // Initial open complaint matching screenshot
                dao.insertComplaintOrDowntime(
                    ComplaintDowntimeEntity(
                        type = "COMPLAINT",
                        areaOrMachine = "Breaded",
                        details = "Breaded line feeder conveyor sensor calibration issue requiring technical check.",
                        status = "OPEN",
                        startTimeFormatted = "Fri Sep 18 2026 11:38:49 GMT+0300",
                        endTimeFormatted = "End Awaiting Admin resolution",
                        totalHours = "00:00:00",
                        submittedBy = "MG"
                    )
                )

                // Initial live tasks matching screenshot
                dao.insertProcessSheet(
                    ProcessSheetEntity(
                        areaKey = "Defrosting",
                        processKey = "RF Process",
                        productName = "Chicken Breast Fillet",
                        productCode = "PRD-CBF-01",
                        brand = "Aldella Premium",
                        chickenProduct = "Fresh Defrosted Chicken",
                        batchNo = "Batch 1",
                        date = "18-09-2026",
                        startTime = "11:41:57",
                        endTime = "--",
                        operationalTimer = "10:16:58",
                        status = "RUNNING",
                        totalKgQty = 1250.0,
                        totalHours = "10:16:58",
                        inputKg = 1300.0,
                        outputKg = 1250.0,
                        productWasteKg = 25.0,
                        waterWasteLitres = 50.0,
                        temperatureC = 3.8,
                        wasteType = "Product / Waste",
                        remarks = "Continuous RF defrosting in progress. Temperatures within standard range.",
                        recordedBy = "MG"
                    )
                )

                dao.insertProcessSheet(
                    ProcessSheetEntity(
                        areaKey = "Xray",
                        processKey = "Xray Process",
                        productName = "Marinated Diced Chicken",
                        productCode = "PRD-MDC-04",
                        brand = "Aldella Select",
                        chickenProduct = "Processed Poultry",
                        batchNo = "Batch 1",
                        date = "18-09-2026",
                        startTime = "11:44:02",
                        endTime = "--",
                        operationalTimer = "10:14:53",
                        status = "RUNNING",
                        totalKgQty = 980.0,
                        totalHours = "10:14:53",
                        inputKg = 1000.0,
                        outputKg = 980.0,
                        productWasteKg = 12.0,
                        waterWasteLitres = 8.0,
                        temperatureC = 2.4,
                        wasteType = "Product / Waste",
                        remarks = "In-line X-ray bone and density inspection running smoothly.",
                        recordedBy = "MG"
                    )
                )

                // Initial chat messages
                dao.insertChatMessage(
                    ChatMessageEntity(
                        senderName = "Aldella Admin",
                        senderEmail = "admin@aldella.com",
                        senderRole = "ADMIN",
                        messageText = "Good morning team. Please ensure all batch sheets for morning shifts have timestamps verified.",
                        timestampFormatted = "18-09-2026 09:15"
                    )
                )
                dao.insertChatMessage(
                    ChatMessageEntity(
                        senderName = "Production Supervisor",
                        senderEmail = "supervisor@aldella.com",
                        senderRole = "SUPERVISOR",
                        messageText = "Noted. Defrosting and X-ray batches are currently active and logged.",
                        timestampFormatted = "18-09-2026 09:30"
                    )
                )

                // Initial audit logs matching screenshot
                dao.insertAuditLog(
                    AuditLogEntity(
                        actionType = "SOFT_DELETE_DOWNTIME",
                        category = "downtime",
                        description = "Admin removed downtime from active views; attachments and audit retained",
                        timestampFormatted = "Fri Sep 18 2026 17:30:44 GMT+0300"
                    )
                )
                dao.insertAuditLog(
                    AuditLogEntity(
                        actionType = "SOFT_DELETE_CHAT_MESSAGE",
                        category = "chat_message",
                        description = "Admin or Supervisor deleted chat message; audit retained",
                        timestampFormatted = "Fri Sep 18 2026 10:58:15 GMT+0300"
                    )
                )
                dao.insertAuditLog(
                    AuditLogEntity(
                        actionType = "UPDATE_PROCESS_CONFIG",
                        category = "process_config",
                        description = "Global production layout verified and updated",
                        timestampFormatted = "Fri Sep 18 2026 07:12:01 GMT+0300"
                    )
                )
                dao.insertAuditLog(
                    AuditLogEntity(
                        actionType = "SEND_CHAT_MESSAGE",
                        category = "chat_message",
                        description = "Voice/Text transmission logged to shared operational room",
                        timestampFormatted = "Fri Sep 18 2026 06:43:35 GMT+0300"
                    )
                )
            }
        }
    }
}
