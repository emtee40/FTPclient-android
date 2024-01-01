package de.qwerty287.ftpclient.ui.files

import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.providers.File
import de.qwerty287.ftpclient.ui.FragmentUtils.store
import de.qwerty287.ftpclient.ui.files.utils.CounterSnackbar
import de.qwerty287.ftpclient.ui.files.utils.CountingOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DownloadMultipleDialog(fragment: FilesFragment) {
    private var result: ActivityResultLauncher<Intent>
    private var selected: ArrayList<File> = ArrayList()
    private var dialog: AlertDialog? = null

    private fun reset() {
        dialog?.dismiss()
        dialog = null
        selected = ArrayList()
    }

    init {
        with(fragment) {
            result = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        val uri = it.data?.data
                        if (uri != null) {
                            var succeeded = 0
                            var failed = 0
                            val df = DocumentFile.fromTreeUri(requireContext(), uri)!!
                            selected.forEach { file ->
                                val docFile = df.createFile("", file.name)!!
                                val sb = CounterSnackbar(
                                    view!!,
                                    getString(
                                        R.string.downloading_multi, succeeded + failed + 1,
                                        selected.size
                                    ), requireActivity()
                                )
                                val outputStream =
                                    CountingOutputStream(requireContext().contentResolver.openOutputStream(docFile.uri)!!) { written ->
                                        sb.update(
                                            written,
                                            file.size.toInt()
                                        )
                                    }
                                val s = try {
                                    store.getClient().download(getAbsoluteFilePath(file.name), outputStream)
                                } catch (e: NullPointerException) {
                                    false
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    false
                                }
                                sb.dismiss()
                                outputStream.close()
                                if (s) {
                                    succeeded++
                                } else {
                                    failed++
                                }
                            }
                            Snackbar.make(
                                view!!,
                                getString(R.string.download_summary, succeeded, failed),
                                Snackbar.LENGTH_SHORT
                            ).show()
                            reset()
                        }
                    }
                }
            }
        }
    }

    fun open(context: Context, files: List<File>) {
        val onlyFiles = files.filter { it.isFile }
        val names = onlyFiles.map { it.name }.toTypedArray()
        dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.download_multiple)
            .setMultiChoiceItems(names, null) { _, pos, checked ->
                if (checked) {
                    selected.add(onlyFiles[pos])
                } else {
                    selected.remove(onlyFiles[pos])
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> reset() }
            .setNeutralButton(R.string.download_all) { _, _ ->
                selected = ArrayList(onlyFiles)
                result.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
            }
            .setPositiveButton(R.string.download_file) { _, _ ->
                if (selected.size < 1) {
                    reset()
                } else {
                    result.launch(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE))
                }
            }
            .setOnCancelListener {
                reset()
            }
            .show()
    }

    companion object {
        fun available(files: List<File>): Boolean {
            return files.any { it.isFile }
        }
    }
}