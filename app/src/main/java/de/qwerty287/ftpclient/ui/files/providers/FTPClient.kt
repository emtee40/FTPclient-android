package de.qwerty287.ftpclient.ui.files.providers

import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.InputStream
import java.io.OutputStream

class FTPClient : Client {
    private val client: FTPClient = FTPClient()

    override fun connect(host: String, port: Int) {
        client.connect(host, port)
    }

    override var implicit: Boolean = false

    override fun login(user: String, password: String) {
        client.login(user, password)
    }

    override val isConnected: Boolean
        get() = client.isConnected

    override fun upload(name: String, stream: InputStream) {
        client.storeFile(name, stream) // TODO return val
    }

    override fun download(name: String, stream: OutputStream) {
        client.retrieveFile(name, stream) // TODO return val
    }

    override fun mkdir(path: String) {
        client.makeDirectory(path) // TODO return val
    }

    override fun rm(path: String) {
        client.deleteFile(path) // TODO return val
    }

    override fun rmDir(path: String) {
        client.removeDirectory(path) // TODO return val
    }

    override fun rename(old: String, new: String) {
        client.rename(old, new) // TODO return val
    }

    override fun list(): List<File> {
        return convertFiles(client.listFiles())
    }

    override fun list(path: String?): List<File> {
        return convertFiles(client.listFiles(path))
    }

    override fun exit() {
        client.logout() // TODO return val
        client.disconnect()
    }

    companion object {
        internal fun convertFiles(files: Array<FTPFile>): List<File> {
            val result = ArrayList<File>()
            files.forEach {
                result.add(FTPFile(it))
            }
            return result
        }
    }
}