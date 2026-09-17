package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserAccountEntity::class,
        UserProfileEntity::class,
        FoodItemEntity::class,
        WorkoutSessionEntity::class,
        WorkoutLogEntryEntity::class,
        WeightCheckInEntity::class,
        ChatMessageEntity::class,
        CustomExerciseEntity::class,
        CustomWorkoutPlanEntity::class,
        ProgressPhotoEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun coachDao(): BulkCoachDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `user_accounts` (
                        `email` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `passwordHash` TEXT NOT NULL,
                        `createdAtEpoch` INTEGER NOT NULL,
                        PRIMARY KEY(`email`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_user_accounts_email` ON `user_accounts` (`email`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                val instance = Room.databaseBuilder(
                    appContext,
                    AppDatabase::class.java,
                    "elite_bulk_coach.db"
                )
                    .addMigrations(MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                        }
                    })
                    .build()
                INSTANCE = instance

                // Trigger seeding check asynchronously
                CoroutineScope(Dispatchers.IO).launch {
                    DatabaseSeeder.seedDatabaseIfEmpty(appContext, instance)
                }

                instance
            }
        }
    }
}
