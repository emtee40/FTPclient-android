package de.qwerty287.ftpclient.ui.files.providers

import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import net.schmizz.sshj.xfer.InMemoryDestFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.InputStream
import java.io.OutputStream

class SFTPClient : Client {

    private val client = SSHClient()
    private var sftpClient: SFTPClient? = null
    private val sftp: SFTPClient
        get() = sftpClient ?: client.newSFTPClient().also {
            sftpClient = it
        }

    override fun login(user: String, password: String) {
        client.authPassword(user, password)
    }

    override val isConnected: Boolean
        get() = client.isConnected && client.isAuthenticated

    override fun connect(host: String, port: Int) {
        client.addHostKeyVerifier(PromiscuousVerifier()) // TODO allow accept/decline
        client.connect(host, port)
    }

    override var implicit: Boolean = false
    override var utf8: Boolean = false

    override fun rename(old: String, new: String): Boolean {
        sftp.rename(old, new)
        return true
    }

    override fun list(): List<File> {
        return list("/")
    }

    override fun list(path: String?): List<File> {
        val result = ArrayList<File>()
        sftp.ls(path).forEach {
            result.add(SFTPFile(it))
        }
        return result
    }

    override fun exit(): Boolean {
        sftp.close()
        client.disconnect()
        return true
    }

    override fun download(name: String, stream: OutputStream): Boolean {
        sftp.fileTransfer.download(name, object : InMemoryDestFile() {
            override fun getOutputStream(): OutputStream {
                return stream
            }
        })
        return true
    }

    override fun mkdir(path: String): Boolean {
        sftp.mkdir(path)
        return true
    }

    override fun rm(path: String): Boolean {
        sftp.rm(path)
        return true
    }

    override fun rmDir(path: String): Boolean {
        sftp.rmdir(path)
        return true
    }

    override fun upload(name: String, stream: InputStream): Boolean {
        sftp.fileTransfer.upload(object : InMemorySourceFile() {
            override fun getName(): String {
                return name
            }

            override fun getLength(): Long {
                return stream.available().toLong()
            }

            override fun getInputStream(): InputStream {
                return stream
            }
        }, name)
        return true
    }

}