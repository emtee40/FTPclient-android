package de.qwerty287.ftpclient.providers

import android.content.Context
import de.qwerty287.ftpclient.providers.ftp.FTPClient
import de.qwerty287.ftpclient.providers.ftps.FTPSClient
import de.qwerty287.ftpclient.providers.sftp.SFTPClient

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