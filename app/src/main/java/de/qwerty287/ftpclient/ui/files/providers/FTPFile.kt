package de.qwerty287.ftpclient.ui.files.providers

import org.apache.commons.net.ftp.FTPFile
import java.util.*

class FTPFile(private val file: FTPFile) : File {
    override val name: String
        get() = file.name
    override val size: Long
        get() = file.size
    override val user: String
        get() = file.user
    override val group: String
        get() = file.group
    override val timestamp: Calendar
        get() = file.timestamp
    override val isDirectory: Boolean
        get() = file.isDirectory
    override val isFile: Boolean
        get() = file.isFile
    override val isSymbolicLink: Boolean
        get() = file.isSymbolicLink
    override val isUnknown: Boolean
        get() = file.isUnknown
    override val link: String?
        get() = file.link

}