package de.qwerty287.ftpclient.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "directory") val directory: String,
    @ColumnInfo(name = "connection") val connection: Int,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0
)
