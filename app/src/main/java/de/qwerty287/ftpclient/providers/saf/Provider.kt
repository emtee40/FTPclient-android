package de.qwerty287.ftpclient.providers.saf

import android.database.Cursor
import android.database.MatrixCursor
import android.os.CancellationSignal
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsProvider
import android.webkit.MimeTypeMap
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.data.Connection
import de.qwerty287.ftpclient.providers.Client
import de.qwerty287.ftpclient.providers.File
import kotlinx.coroutines.runBlocking
import java.io.FileInputStream
import java.io.FileOutputStream

class Provider : DocumentsProvider() {
    companion object {
        private fun fileToMap(file: File, dr: DocRepresentation, parent: Boolean): HashMap<String, Any?> {
            val meta = HashMap<String, Any?>()
            meta[DocumentsContract.Document.COLUMN_DOCUMENT_ID] = (if (parent) dr.child(file.name) else dr).toString()
            meta[DocumentsContract.Document.COLUMN_MIME_TYPE] =
                if (file.isDirectory) DocumentsContract.Document.MIME_TYPE_DIR else mimeFromFile(file.name)
            meta[DocumentsContract.Document.COLUMN_DISPLAY_NAME] = file.name
            meta[DocumentsContract.Document.COLUMN_SUMMARY] = null
            meta[DocumentsContract.Document.COLUMN_LAST_MODIFIED] = file.timestamp.timeInMillis
            meta[DocumentsContract.Document.COLUMN_FLAGS] = DocumentsContract.Document.FLAG_SUPPORTS_DELETE or
                    DocumentsContract.Document.FLAG_SUPPORTS_WRITE or
                    DocumentsContract.Document.FLAG_SUPPORTS_RENAME /* TODO extension: or
                    DocumentsContract.Document.FLAG_SUPPORTS_COPY or
                    DocumentsContract.Document.FLAG_SUPPORTS_MOVE*/
            meta[DocumentsContract.Document.COLUMN_SIZE] = file.size.toInt()
            meta[DocumentsContract.Document.COLUMN_ICON] = null
            return meta
        }

        private fun mimeFromFile(name: String): String {
            return MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(name))
                ?: "application/octet-stream"
            // TODO: find an unspecific mime
        }

        private val fileColumns = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SUMMARY,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_ICON
        )
    }

    private val filesCache = HashMap<String, File>()
    private val clientsCache = HashMap<Int, Client>()
    private val connections: HashMap<Int, Connection> = HashMap()

    override fun onCreate(): Boolean {
        val db = AppDatabase.getInstance(context!!)
        runBlocking {
            val connList = db.connectionDao().getListOfAllSAF()
            for (c in connList) {
                connections[c.id] = c
            }
        }
        return true
    }

    override fun queryRoots(projection: Array<out String>?): Cursor {
        val cols = projection ?: arrayOf(
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,
            DocumentsContract.Root.COLUMN_ICON
        )
        val result = MatrixCursor(cols)

        for (i in connections) {
            val meta = HashMap<String, Any?>()
            meta[DocumentsContract.Root.COLUMN_ROOT_ID] = i.value.id
            meta[DocumentsContract.Root.COLUMN_SUMMARY] = i.value.title
            meta[DocumentsContract.Root.COLUMN_FLAGS] = 0
            meta[DocumentsContract.Root.COLUMN_TITLE] = context!!.getString(R.string.app_name)
            meta[DocumentsContract.Root.COLUMN_DOCUMENT_ID] = DocRepresentation.fromConn(i.value).toString()
            meta[DocumentsContract.Root.COLUMN_MIME_TYPES] = null
            meta[DocumentsContract.Root.COLUMN_AVAILABLE_BYTES] = null
            meta[DocumentsContract.Root.COLUMN_ICON] = R.mipmap.ic_launcher

            result.newRow().apply {
                for (j in meta) {
                    if (cols.contains(j.key)) {
                        add(j.key, j.value)
                    }
                }
            }
        }

        return result
    }

    override fun queryDocument(documentId: String, projection: Array<out String>?): Cursor {
        val dr = DocRepresentation.parse(documentId)
        val file: File = getFileWithCache(dr)

        val result = MatrixCursor(projection ?: fileColumns)
        result.newRow().apply {
            for (i in fileToMap(file, dr, false)) {
                if (projection == null || projection.contains(i.key)) {
                    add(i.key, i.value)
                }
            }
        }
        return result
    }

    override fun queryChildDocuments(
        parentDocumentId: String,
        projection: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val dr = DocRepresentation.parse(parentDocumentId)

        val conn = connections[dr.connId]!!
        val result = MatrixCursor(projection ?: fileColumns)
        for (i in getClient(conn).list(File.joinPaths(conn.startDirectory, dr.name))) {
            filesCache[dr.child(i.name).toString()] = i
            result.newRow().apply {
                for (j in fileToMap(i, dr, true)) {
                    if (projection == null || projection.contains(j.key)) {
                        add(j.key, j.value)
                    }
                }
            }
        }
        return result
    }

    override fun openDocument(documentId: String, mode: String?, signal: CancellationSignal?): ParcelFileDescriptor {
        val dr = DocRepresentation.parse(documentId)

        val localFile = java.io.File(context!!.cacheDir, dr.toString().hashCode().toString())
        val conn = connections[dr.connId]!!
        val client = getClient(conn)

        if (!client.download(File.joinPaths(conn.startDirectory, dr.name), FileOutputStream(localFile))) {
            throw RuntimeException("could not download file")
        }

        return ParcelFileDescriptor.open(
            localFile, when (mode) {
                // TODO writable?
                "r" -> ParcelFileDescriptor.MODE_READ_ONLY
                "rw" -> ParcelFileDescriptor.MODE_READ_WRITE
                "w" -> ParcelFileDescriptor.MODE_WRITE_ONLY
                else -> throw IllegalArgumentException("mode \"$mode\" is not supported")
            }, Handler(context!!.mainLooper) // TODO check this
        ) {
            if (mode == "rw" || mode == "w") {
                client.upload(File.joinPaths(conn.startDirectory, dr.name), FileInputStream(localFile))
            }
            localFile.delete()
        }
    }

    override fun deleteDocument(documentId: String) {
        val dr = DocRepresentation.parse(documentId)
        val conn = connections[dr.connId]!!
        val client = getClient(conn)
        if (getFileWithCache(dr).isDirectory) {
            client.rmDir(File.joinPaths(conn.startDirectory, dr.name))
        } else {
            client.rm(File.joinPaths(conn.startDirectory, dr.name))
        }
    }

    override fun renameDocument(documentId: String, displayName: String): String {
        val dr = DocRepresentation.parse(documentId)
        val conn = connections[dr.connId]!!
        val client = getClient(conn)
        val oldDisplayName = dr.name.split("/").last()
        val newName = dr.name.removeSuffix(oldDisplayName) + displayName
        val newDr = DocRepresentation(conn.id, newName)
        if (!client.rename(
                File.joinPaths(conn.startDirectory, dr.name),
                File.joinPaths(conn.startDirectory, newDr.name)
            )
        ) {
            throw RuntimeException("could not rename file")
        }
        return newDr.toString()
    }

    private fun getFileWithCache(dr: DocRepresentation): File {
        if (!filesCache.containsKey(dr.toString())) {
            val conn = connections[dr.connId]!!
            var exception: Exception? = null
            val thread = Thread {
                try {
                    val fl = getClient(conn).file(File.joinPaths(conn.startDirectory, dr.name))
                    filesCache[dr.toString()] = fl
                } catch (e: Exception) {
                    exception = e
                }
            }
            thread.start()
            thread.join()
            if (exception != null) {
                throw RuntimeException(exception)
            }
        }
        return filesCache[dr.toString()]!!
    }

    private fun getClient(conn: Connection): Client {
        // TODO close clients?
        if (!clientsCache.containsKey(conn.id)) {
            // we don't have to set for the password, it's not allowed to enable password storing and SAF
            clientsCache[conn.id] = conn.client(context!!, null)
        }
        return clientsCache[conn.id]!!
    }

}