package de.qwerty287.ftpclient.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Connection::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    "ftpclient"
                )
                    .addMigrations(
                        object : Migration(1, 2) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("ALTER TABLE 'connections' ADD COLUMN 'port' INTEGER NOT NULL DEFAULT 21")
                            }
                        }
                    )
                    .allowMainThreadQueries()
                    .build().also { instance = it }
            }
        }

    }
}