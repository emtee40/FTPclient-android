package de.qwerty287.ftpclient.providers.ftp

import de.qwerty287.ftpclient.providers.File
import org.apache.commons.net.ftp.FTPFile
import java.util.*

class FTPFile(file: FTPFile) : File {
    override val name: String = file.name
    override val size: Long = file.size
    override val user: String = file.user
    override val group: String = file.group
    override val timestamp: Calendar = file.timestamp
    override val isDirectory: Boolean = file.isDirectory
    override val isFile: Boolean = file.isFile
    override val isSymbolicLink: Boolean = file.isSymbolicLink
    override val isUnknown: Boolean = file.isUnknown
    override val link: String? = file.link
}