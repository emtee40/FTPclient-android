package de.qwerty287.ftpclient.ui.files.providers

import org.apache.commons.net.ftp.FTPSClient
import java.io.InputStream
import java.io.OutputStream

class FTPSClient : Client {
    private var client: FTPSClient = FTPSClient()

    override fun connect(host: String, port: Int) {
        client.connect(host, port)
    }

    override var implicit: Boolean = false
    set(value) {
        client = FTPSClient(value)
    }

    override fun login(user: String, password: String) {
        client.login(user, password)
    }

    override val isConnected: Boolean
        get() = client.isConnected

    override fun upload(name: String, stream: InputStream) {
        client.storeFile(name, stream)
    }

    override fun download(name: String, stream: OutputStream) {
        client.retrieveFile(name, stream)
    }

    override fun mkdir(path: String) {
        client.makeDirectory(path)
    }

    override fun rm(path: String) {
        client.deleteFile(path)
    }

    override fun rmDir(path: String) {
        client.removeDirectory(path)
    }

    override fun rename(old: String, new: String) {
        client.rename(old, new)
    }

    override fun list(): List<File> {
        return FTPClient.convertFiles(client.listFiles())
    }

    override fun list(path: String?): List<File> {
        return FTPClient.convertFiles(client.listFiles(path))
    }

    override fun exit() {
        client.logout()
        client.disconnect()
    }
}