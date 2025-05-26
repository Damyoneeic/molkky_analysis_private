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

@Database(
    entities = [User::class, ThrowRecord::class, ThrowDraft::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun throwDao(): ThrowDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "molkky_analysis_db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .addCallback(object : RoomDatabase.Callback() { // ★ ここからCallbackを追加
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // DBが初めて作成されたときにデフォルトユーザーを挿入
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.userDao().insertUser(
                                        User(id = 1, name = "Player 1", created_at = System.currentTimeMillis())
                                    )
                                    // 必要であれば他のデフォルトユーザーも追加
                                }
                            }
                        }
                    }) // ★ここまでCallback
                    .build()
                INSTANCE = instance
                instance
            }
        }

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
    }
}