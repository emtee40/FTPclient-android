package de.qwerty287.ftpclient.providers

enum class Provider {
    FTP,
    FTPS,
    SFTP;

    fun get(): Client {
        return when (this) {
            FTP -> FTPClient()
            FTPS -> FTPSClient()
            SFTP -> SFTPClient()
        }
    }
}