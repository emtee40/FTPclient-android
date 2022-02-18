package de.qwerty287.ftpclient.ui.files.providers

import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.util.*

class SFTPFile(private val file: RemoteResourceInfo) : File {
    override val name: String
        get() = file.name
    override val size: Long
        get() = file.attributes.size
    override val user: String
        get() = file.attributes.uid.toString()
    override val group: String
        get() = file.attributes.gid.toString() // TODO get real name
    override val timestamp: Calendar
        get() = Calendar.getInstance().apply {
            time = Date(file.attributes.mtime)
        }
    override val isDirectory: Boolean
        get() = file.isDirectory
    override val isFile: Boolean
        get() = file.isRegularFile
    override val isSymbolicLink: Boolean
        get() = file.attributes.type == FileMode.Type.SYMLINK
    override val isUnknown: Boolean
        get() = !(isDirectory || isFile || isSymbolicLink)
    override val link: String?
        get() = null
}