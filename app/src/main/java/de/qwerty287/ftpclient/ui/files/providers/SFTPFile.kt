package de.qwerty287.ftpclient.ui.files.providers

import net.schmizz.sshj.sftp.FileMode
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.util.*

class SFTPFile(file: RemoteResourceInfo) : File {
    override val name: String = file.name
    override val size: Long = file.attributes.size
    override val user: String = file.attributes.uid.toString()
    override val group: String = file.attributes.gid.toString() // TODO get real name
    override val timestamp: Calendar = Calendar.getInstance().apply {
            time = Date(file.attributes.mtime)
        }
    override val isDirectory: Boolean = file.isDirectory
    override val isFile: Boolean = file.isRegularFile
    override val isSymbolicLink: Boolean = file.attributes.type == FileMode.Type.SYMLINK
    override val isUnknown: Boolean = !(isDirectory || isFile || isSymbolicLink)
    override val link: String? = null
}