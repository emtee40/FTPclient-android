package de.qwerty287.ftpclient.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Connection::class, Bookmark::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun connectionDao(): ConnectionDao
    abstract fun bookmarkDao(): BookmarkDao

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
                        },
                        object : Migration(2, 3) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("CREATE TABLE IF NOT EXISTS 'bookmarks' ('_id' INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, 'title' TEXT NOT NULL, 'directory' TEXT NOT NULL, 'connection' INTEGER NOT NULL)")
                            }
                        },
                        object : Migration(3, 4) {
                            override fun migrate(database: SupportSQLiteDatabase) {
                                database.execSQL("ALTER TABLE 'connections' ADD COLUMN 'secure' INTEGER NOT NULL DEFAULT 0")
                            }
                        }
                    )
                    .build().also { instance = it }
            }
        }

    }
}