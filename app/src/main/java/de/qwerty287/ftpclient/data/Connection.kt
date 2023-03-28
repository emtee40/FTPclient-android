package de.qwerty287.ftpclient.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.qwerty287.ftpclient.providers.Client
import de.qwerty287.ftpclient.providers.Provider

@Entity(tableName = "connections")
data class Connection(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "server") val server: String,
    @ColumnInfo(name = "port") val port: Int,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "type") val type: Provider,
    @ColumnInfo(name = "implicit") val implicit: Boolean,
    @ColumnInfo(name = "utf8") val utf8: Boolean,
    @ColumnInfo(name = "passive") val passive: Boolean,
    @ColumnInfo(name = "start_directory") val startDirectory: String,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0
) {
    fun client(): Client {
        val client = type.get()

        client.implicit = implicit
        client.utf8 = utf8
        client.connect(server, port)
        client.passive = passive
        client.login(username, password) // connect to server and login with login credentials

        return client
    }
}
