package de.qwerty287.ftpclient.providers.sftp

import android.content.Context
import de.qwerty287.ftpclient.providers.Client
import de.qwerty287.ftpclient.providers.File
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.Factory
import net.schmizz.sshj.common.SSHException
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil
import net.schmizz.sshj.userauth.password.PasswordUtils
import net.schmizz.sshj.xfer.InMemoryDestFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.InputStream
import java.io.OutputStream

class SFTPClient(private val context: Context) : Client {

    private val client = SSHClient()
    private var sftpClient: SFTPClient? = null
    private val sftp: SFTPClient
        get() = sftpClient ?: client.newSFTPClient().also {
            sftpClient = it
        }

    override fun login(user: String, password: String) {
        client.authPassword(user, password)
    }

    override fun loginPubKey(user: String, key: java.io.File, passphrase: String) {
        val format = KeyProviderUtil.detectKeyFileFormat(key)
        val fkp = Factory.Named.Util.create(
            client.transport.config.fileKeyProviderFactories,
            format.toString()
        )
            ?: throw SSHException("No provider available for $format key file")
        fkp.init(key, PasswordUtils.createOneOff(passphrase.toCharArray()))
        client.authPublickey(user, fkp)
    }

    override val isConnected: Boolean
        get() = client.isConnected && client.isAuthenticated

    override fun connect(host: String, port: Int) {
        client.addHostKeyVerifier(KeyVerifier(context))
        client.connect(host, port)
    }

    override var implicit: Boolean = false
    override var utf8: Boolean = false
    override var passive: Boolean = false

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