package de.qwerty287.ftpclient.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connections")
data class Connection(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "server") val server: String,
    @ColumnInfo(name = "port") val port: Int,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "password") val password: String,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0
)
