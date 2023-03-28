package de.qwerty287.ftpclient.providers

import android.content.Context

enum class Provider {
    FTP,
    FTPS,
    SFTP;

    fun get(context: Context): Client {
        return when (this) {
            FTP -> FTPClient()
            FTPS -> FTPSClient(context)
            SFTP -> SFTPClient()
        }
    }
}