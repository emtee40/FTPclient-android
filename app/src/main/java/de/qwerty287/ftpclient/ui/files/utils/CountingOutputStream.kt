package de.qwerty287.ftpclient.ui.files.utils

import java.io.OutputStream

class CountingOutputStream(private val stream: OutputStream, private val onWrite: (Int) -> Unit) : OutputStream() {
    private var written = 0

    override fun write(p0: Int) {
        stream.write(p0)
        written++
        onWrite(written)
    }

    override fun close() {
        stream.close()
    }

    override fun flush() {
        stream.flush()
    }

    override fun write(b: ByteArray?) {
        stream.write(b)
        written += b?.size ?: 0
        onWrite(written)
    }

    override fun write(b: ByteArray?, off: Int, len: Int) {
        stream.write(b, off, len)
        written += b?.size ?: 0
        onWrite(written)
    }
}