package de.qwerty287.ftpclient.ui.files.providers

import java.io.InputStream
import java.io.OutputStream

interface Client {
    fun connect(host: String, port: Int)
    var implicit: Boolean
    var utf8: Boolean
    fun login(user: String, password: String)
    val isConnected: Boolean
    fun upload(name: String, stream: InputStream): Boolean
    fun download(name: String, stream: OutputStream): Boolean
    fun mkdir(path: String): Boolean
    fun rm(path: String): Boolean
    fun rmDir(path: String): Boolean
    fun rename(old: String, new: String): Boolean
    fun list(): List<File>
    fun list(path: String?): List<File>
    fun exit(): Boolean
}