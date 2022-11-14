package de.qwerty287.ftpclient.ui.files.utils

import java.io.InputStream

class CountingInputStream(private val stream: InputStream, private val onRead: (Int) -> Unit) : InputStream() {
    private var read = 0

    override fun read(): Int {
        read++
        onRead(read)
        return stream.read()
    }

    override fun available(): Int {
        return stream.available()
    }

    override fun close() {
        stream.close()
    }

    override fun markSupported(): Boolean {
        return stream.markSupported()
    }

    override fun mark(readlimit: Int) {
        stream.mark(readlimit)
    }

    override fun read(b: ByteArray?): Int {
        read += b?.size ?: 0
        onRead(read)
        return stream.read(b)
    }

    override fun reset() {
        stream.reset()
    }

    override fun read(b: ByteArray?, off: Int, len: Int): Int {
        read += b?.size ?: 0
        onRead(read)
        return stream.read(b, off, len)
    }

    override fun skip(n: Long): Long {
        return stream.skip(n)
    }

}