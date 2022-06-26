package de.qwerty287.ftpclient.ui.files.providers

import org.apache.commons.net.ftp.FTPSClient
import java.io.InputStream
import java.io.OutputStream

class FTPSClient : Client {
    private var client: FTPSClient = FTPSClient().apply {
        autodetectUTF8 = true
    }

    override fun connect(host: String, port: Int) {
        client.connect(host, port)
    }

    override var implicit: Boolean = false
        set(value) {
            client = FTPSClient(value).apply {
                autodetectUTF8 = true
            }
        }
    override var utf8: Boolean = false
        set(value) {
            if (value) client.controlEncoding = "UTF-8"
            field = value
        }

    override fun login(user: String, password: String) {
        client.login(user, password)
    }

    override val isConnected: Boolean
        get() = client.isConnected

    override fun upload(name: String, stream: InputStream): Boolean {
        return client.storeFile(name, stream)
    }

    override fun download(name: String, stream: OutputStream): Boolean {
        return client.retrieveFile(name, stream)
    }

    override fun mkdir(path: String): Boolean {
        return client.makeDirectory(path)
    }

    override fun rm(path: String): Boolean {
        return client.deleteFile(path)
    }

    override fun rmDir(path: String): Boolean {
        return client.removeDirectory(path)
    }

    override fun rename(old: String, new: String): Boolean {
        return client.rename(old, new)
    }

    override fun list(): List<File> {
        return FTPClient.convertFiles(client.listFiles())
    }

    override fun list(path: String?): List<File> {
        return FTPClient.convertFiles(client.listFiles(path))
    }

    override fun exit(): Boolean {
        if (!client.logout()) {
            return false
        }
        client.disconnect()
        return true
    }
}