package com.example.molkky_analysis.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.molkky_analysis.data.model.ThrowDraft
import com.example.molkky_analysis.data.model.ThrowRecord
import com.example.molkky_analysis.data.model.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID // For default session ID in migration if needed

@Database(
    entities = [User::class, ThrowRecord::class, ThrowDraft::class],
    version = 3, // Incremented version
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun throwDao(): ThrowDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2 (previous migration for adding throw_draft)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `throw_draft` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `user_id` INTEGER NOT NULL,
                        `distance` REAL NOT NULL,
                        `angle` TEXT NOT NULL,
                        `weather` TEXT,
                        `humidity` REAL,
                        `temperature` REAL,
                        `soil` TEXT,
                        `molkky_weight` REAL,
                        `is_success` INTEGER NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        FOREIGN KEY(`user_id`) REFERENCES `User`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_throw_draft_user_id` ON `throw_draft` (`user_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_throw_draft_timestamp` ON `throw_draft` (`timestamp`)")
            }
        }

        // Migration from version 2 to 3 (adds session_id to throw_draft)
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new session_id column.
                // For existing rows, assign a default marker or a new UUID.
                // Using a fixed marker "LEGACY_DRAFT" for simplicity for existing rows.
                // New drafts created by the app will use proper UUIDs.
                db.execSQL("ALTER TABLE `throw_draft` ADD COLUMN `session_id` TEXT NOT NULL DEFAULT 'LEGACY_DRAFT'")
                // Add index for session_id related queries
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_throw_draft_user_id_session_id` ON `throw_draft` (`user_id`, `session_id`)")
            }
        }


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "molkky_analysis_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Add new migration
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.userDao().insertUser(
                                        User(id = 1, name = "Player 1", created_at = System.currentTimeMillis())
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}