package de.qwerty287.ftpclient.ui.files.providers

import java.io.InputStream
import java.io.OutputStream

interface Client {
    fun connect(host: String, port: Int)
    var implicit: Boolean
    fun login(user: String, password: String)
    val isConnected: Boolean
    fun upload(name: String, stream: InputStream)
    fun download(name: String, stream: OutputStream)
    fun mkdir(path: String)
    fun rm(path: String)
    fun rmDir(path: String)
    fun rename(old: String, new: String)
    fun list(): List<File>
    fun list(path: String?): List<File>
    fun exit()
}