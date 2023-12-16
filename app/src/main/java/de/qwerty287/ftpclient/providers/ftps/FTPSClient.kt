package de.qwerty287.ftpclient.providers.ftps

import android.content.Context
import de.qwerty287.ftpclient.providers.Client
import de.qwerty287.ftpclient.providers.File
import de.qwerty287.ftpclient.providers.ftp.FTPClient
import de.qwerty287.ftpclient.providers.ftp.FTPFile
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPCmd
import org.apache.commons.net.ftp.FTPSClient
import java.io.InputStream
import java.io.OutputStream

class FTPSClient(private var context: Context) : Client {
    private var client: FTPSClient = FTPSClient().applyDefaults()

    override fun connect(host: String, port: Int) {
        client.connect(host, port)
    }

    override var implicit: Boolean = false
        set(value) {
            client = FTPSClient(value).applyDefaults()
        }
    override var utf8: Boolean = false
        set(value) {
            if (value) client.controlEncoding = "UTF-8"
            field = value
        }
    override var passive: Boolean = false
        set(value) {
            if (value) client.enterLocalPassiveMode()
            else client.enterLocalActiveMode()
            field = value
        }
    private var supportsMlsCommands = false

    override fun login(user: String, password: String) {
        client.login(user, password)
        client.setFileType(FTP.BINARY_FILE_TYPE)
        supportsMlsCommands = client.hasFeature(FTPCmd.MLST)
    }

    override fun loginPrivKey(user: String, key: java.io.File, passphrase: String) {
        throw NotImplementedError("FTPS does not support private keys")
    }

    override val isConnected: Boolean
        get() = client.isConnected
    override var privateData: Boolean = false
        set(value) {
            if (value) {
                client.execPROT("P")
            }
            field = value
        }

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
        return FTPClient.convertFiles(if (supportsMlsCommands) client.mlistDir() else client.listFiles())
    }

    override fun list(path: String?): List<File> {
        return FTPClient.convertFiles(if (supportsMlsCommands) client.mlistDir(path) else client.listFiles(path))
    }

    override fun file(path: String): File {
        if (!supportsMlsCommands) {
            // TODO improve this
            throw IllegalStateException("server does not support MLST command")
        }
        return FTPFile(client.mlistFile(path))
    }

    override fun exit(): Boolean {
        if (!client.logout()) {
            return false
        }
        client.disconnect()
        return true
    }

    private fun FTPSClient.applyDefaults(): FTPSClient {
        return apply {
            autodetectUTF8 = true
            trustManager = MemorizingTrustManager.fromContext(context)
        }
    }
}
