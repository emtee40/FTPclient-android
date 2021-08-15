package de.qwerty287.ftpclient.ui.files

import android.app.AlertDialog
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.data.Connection
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.databinding.FragmentFilesBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import java.io.File


class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var directory: String

    private val ftpClient = FTPClient()
    private lateinit var connection: Connection

    private val result: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val uri = it.data?.data
                if (uri != null) {
                    val inputStream = requireContext().contentResolver.openInputStream(uri)
                    val success: Boolean = try {
                        ftpClient.storeFile(
                            getAbsoluteFilePath(getFilenameFromUri(uri)),
                            inputStream
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                    inputStream?.close()
                    Snackbar.make(binding.root, if (success) {
                        R.string.upload_completed
                    } else {
                        R.string.upload_failed
                           }, Snackbar.LENGTH_SHORT ).show()
                    updateUi()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        directory = arguments?.getString("directory", "") ?: ""

        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerviewFiles.layoutManager = LinearLayoutManager(requireContext())

        updateUi()

        binding.fabAddDir.setOnClickListener {
            val view2 = layoutInflater.inflate(R.layout.dialog_entry, null)
            val editText = view2.findViewById<EditText>(R.id.edittext_dialog)
            editText.hint = getString(R.string.dir_name)
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.enter_dir_name)
                .setView(view2)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    val dirName = editText.text.toString()
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            val success = try {
                                ftpClient.makeDirectory(getAbsoluteFilePath(dirName))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }
                            showSnackbar(success, R.string.dir_creation_completed, R.string.dir_creation_failed)
                            updateUi()
                        }
                    }
                }
                .create()
                .show()
        }

        binding.fabAddFile.setOnClickListener {
            val requestFileIntent = Intent(Intent.ACTION_GET_CONTENT)
            requestFileIntent.type = "*/*"
            result.launch(Intent.createChooser(requestFileIntent, getString(R.string.select_file)))
        }
    }

    private fun updateUi() {
        var files: Array<FTPFile>

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.progressIndicatorFiles.isVisible = true
                }
                try {
                    connection = arguments?.getInt("connection")?.let {
                        AppDatabase.getInstance(requireContext()).connectionDao()
                            .get(it.toLong())
                    }!!
                    ftpClient.connect(connection.server)
                    ftpClient.login(connection.username, connection.password)
                    files = if (directory == "") {
                        ftpClient.listFiles()
                    } else {
                        ftpClient.listFiles(directory)
                    }
                    withContext(Dispatchers.Main) {
                        if (files.isEmpty()) {
                            binding.textviewEmptyDir.isVisible = true
                        } else {
                            binding.recyclerviewFiles.adapter =
                                FilesAdapter(requireContext(), files, {
                                    if (it.isDirectory) {
                                        val options = Bundle()
                                        options.putString("directory", "$directory/${it.name}")
                                        options.putInt("connection", connection.id)
                                        findNavController().navigate(
                                            R.id.action_FilesFragment_to_FilesFragment,
                                            options
                                        )
                                    } else if (it.isFile) {
                                        FileActionsBottomSheet(
                                            it,
                                            requireActivity().supportFragmentManager,
                                            ftpClient,
                                            directory,
                                            { updateUi() },
                                            { itBool, suc, fail -> showSnackbar(itBool, suc, fail) })
                                    }
                                }, {
                                    if (it.isDirectory) {
                                        DirectoryActionsBottomSheet(
                                            it,
                                            requireActivity().supportFragmentManager,
                                            ftpClient,
                                            directory, { updateUi() }, {itBool, suc, fail -> showSnackbar(itBool, suc, fail) })
                                    }
                                })
                        }
                        binding.progressIndicatorFiles.isVisible = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        binding.progressIndicatorFiles.isVisible = false
                        AlertDialog.Builder(requireContext())
                            .setTitle(R.string.error_occurred)
                            .setMessage(R.string.error_descriptions)
                            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                                findNavController().navigateUp()
                            }
                            .setNeutralButton(R.string.copy) { _: DialogInterface, _: Int ->
                                val clipboardManager =
                                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText(
                                        getString(R.string.app_name),
                                        e.stackTraceToString()
                                    )
                                )
                                findNavController().navigateUp()
                            }
                            .create()
                            .show()
                    }
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    ftpClient.logout()
                    ftpClient.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun getAbsoluteFilePath(fileName: String): String {
        return if (directory != "") {
            "$directory/$fileName"
        } else {
            fileName
        }
    }

    private fun getFilenameFromUri(uri: Uri): String {
        val uriString = uri.toString()
        val file = File(uriString)
        var displayName = "0"

        if (uriString.startsWith("content://")) {
            var cursor: Cursor? = null
            try {
                cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    displayName =
                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        } else if (uriString.startsWith("file://")) {
            displayName = file.name
        }
        return displayName
    }

    private fun showSnackbar(success: Boolean, @StringRes successRes: Int, @StringRes failedRes: Int) {
        Snackbar.make(binding.root, if (success) {
            successRes
        } else {
            failedRes
        }, Snackbar.LENGTH_SHORT).show()
    }
}