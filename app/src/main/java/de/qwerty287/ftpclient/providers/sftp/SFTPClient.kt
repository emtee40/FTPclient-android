package de.qwerty287.ftpclient.providers.sftp

import android.content.Context
import de.qwerty287.ftpclient.providers.Client
import de.qwerty287.ftpclient.providers.File
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.SFTPClient
import net.schmizz.sshj.xfer.InMemoryDestFile
import net.schmizz.sshj.xfer.InMemorySourceFile
import java.io.InputStream
import java.io.OutputStream
import java.util.*

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

    override fun loginPrivKey(user: String, key: java.io.File, passphrase: String) {
        val kp = client.loadKeys(key.path, passphrase)
        client.authPublickey(user, kp)
    }

    override val isConnected: Boolean
        get() = client.isConnected && client.isAuthenticated
    override var privateData: Boolean = false

    override fun connect(host: String, port: Int) {
        client.addHostKeyVerifier(KeyVerifier.fromContext(context))
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

    override fun file(path: String): File {
        val attr = sftp.stat(path)
        return object : File {
            override val name: String = path.split("/").last()
            override val size: Long = attr.size
            override val user: String = attr.uid.toString()
            override val group: String = attr.gid.toString()
            override val timestamp: Calendar = Calendar.getInstance().apply {
                time = Date(attr.mtime)
            }
            override val isDirectory: Boolean = attr.type == FileMode.Type.DIRECTORY
            override val isFile: Boolean = attr.type == FileMode.Type.REGULAR
            override val isSymbolicLink: Boolean = attr.type == FileMode.Type.SYMLINK
            override val isUnknown: Boolean = !(isDirectory || isFile || isSymbolicLink)
            override val link: String? = null
        }
    }

    override fun exit(): Boolean {
        sftp.close()
        client.disconnect()
        return true
    }

    override fun download(name: String, stream: OutputStream): Boolean {
        sftp.fileTransfer.download(name, object : InMemoryDestFile() {
            override fun getLength(): Long {
                throw NotImplementedError()
            }

            override fun getOutputStream(): OutputStream {
                return stream
            }

            override fun getOutputStream(append: Boolean): OutputStream {
                if (!append) return outputStream
                throw IllegalArgumentException("appending not supported")
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