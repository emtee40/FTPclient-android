package de.qwerty287.ftpclient.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Connection::class], version = 1)
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
                    .allowMainThreadQueries()
                    .build().also { instance = it }
            }
        }


    }
}