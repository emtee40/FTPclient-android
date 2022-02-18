package de.qwerty287.ftpclient.ui.files.providers

import java.io.Serializable
import java.util.*

interface File : Serializable {
    val name: String
    val size: Long
    val user: String
    val group: String
    val timestamp: Calendar
    val isDirectory: Boolean
    val isFile: Boolean
    val isSymbolicLink: Boolean
    val isUnknown: Boolean
    val link: String?
}