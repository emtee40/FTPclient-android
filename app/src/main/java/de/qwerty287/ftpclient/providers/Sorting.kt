package de.qwerty287.ftpclient.providers

class Sorting(var method: Method = Method.NAME, var descending: Boolean = false) {
    enum class Method {
        NAME, TIMESTAMP, SIZE
    }

    fun sort(files: List<File>): List<File> {
        return if (descending) {
            when (method) {
                Method.NAME -> files.sortedByDescending { it.name }
                Method.TIMESTAMP -> files.sortedByDescending { it.timestamp }
                Method.SIZE -> files.sortedByDescending {it.size }
            }
        } else {
            when (method) {
                Method.NAME -> files.sortedBy { it.name }
                Method.TIMESTAMP -> files.sortedBy { it.timestamp }
                Method.SIZE -> files.sortedBy {it.size }
            }
        }
    }
}