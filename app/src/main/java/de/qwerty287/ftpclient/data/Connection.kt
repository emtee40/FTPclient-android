package de.qwerty287.ftpclient.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import de.qwerty287.ftpclient.providers.Client
import de.qwerty287.ftpclient.providers.Provider
import de.qwerty287.ftpclient.providers.sftp.KeyFileManager

@Entity(tableName = "connections")
data class Connection(
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "server") val server: String,
    @ColumnInfo(name = "port") val port: Int,
    @ColumnInfo(name = "username") val username: String,
    @ColumnInfo(name = "private_key") val privateKey: Boolean,
    @ColumnInfo(name = "password") val password: String,
    @ColumnInfo(name = "type") val type: Provider,
    @ColumnInfo(name = "implicit") val implicit: Boolean,
    @ColumnInfo(name = "utf8") val utf8: Boolean,
    @ColumnInfo(name = "passive") val passive: Boolean,
    @ColumnInfo(name = "private_data") val privateData: Boolean,
    @ColumnInfo(name = "start_directory") val startDirectory: String,
    @ColumnInfo(name = "saf_integration") val safIntegration: Boolean,
    @ColumnInfo(name = "ask_password") val askPassword: Boolean,
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Int = 0
) {
    fun client(context: Context, userPassword: String?): Client {
        val client = type.get(context)

        client.implicit = implicit
        client.utf8 = utf8
        client.connect(server, port)
        client.passive = passive
        val pw = if (askPassword) {
            userPassword!!
        } else {
            password
        }
        if (privateKey) {
            client.loginPrivKey(username, KeyFileManager.fromContext(context).file(id), pw)
        } else {
            client.login(username, pw) // connect to server and login with login credentials
        }
        client.privateData = privateData

        return client
    }
}
