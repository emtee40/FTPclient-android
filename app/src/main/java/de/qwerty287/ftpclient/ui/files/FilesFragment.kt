package de.qwerty287.ftpclient.ui.files

import android.app.AlertDialog
import android.content.*
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
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
                    showSnackbar(success, R.string.upload_completed, R.string.upload_failed)
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

        setHasOptionsMenu(true)
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

        binding.swipeRefresh.setOnRefreshListener {
            updateUi()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.files_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId) {
            R.id.go_to -> {
                val view = layoutInflater.inflate(R.layout.dialog_entry, null)
                val editText: EditText = view.findViewById(R.id.edittext_dialog)
                editText.setText(directory)
                editText.setHint(R.string.go_to)
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.go_to)
                    .setView(view)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val options = Bundle()
                        options.putString("directory", editText.text.toString())
                        options.putInt("connection", connection.id)
                        findNavController().navigate(
                            R.id.action_FilesFragment_to_FilesFragment,
                            options
                        )
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Update the UI. This:
     * * connects to the server and logins with the login credentials
     * * retrieves the file list from the FTP server
     * * and setups the [RecyclerView][androidx.recyclerview.widget.RecyclerView] or shows a [TextView][android.widget.TextView] if the directory contains nothing
     */
    private fun updateUi() {
        var files: Array<FTPFile>

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = true
                }
                try {
                    if (!ftpClient.isConnected) {
                        connection = arguments?.getInt("connection")?.let {
                            AppDatabase.getInstance(requireContext()).connectionDao()
                                .get(it.toLong())
                        }!! // get connection from connection id, which is stored in the arguments
                        ftpClient.connect(connection.server, connection.port)
                        ftpClient.login(
                            connection.username,
                            connection.password
                        ) // connect to server and login with login credentials
                    }
                    files = if (directory == "") { // get files
                        ftpClient.listFiles()
                    } else {
                        ftpClient.listFiles(directory)
                    }
                    withContext(Dispatchers.Main) {
                        if (files.isEmpty()) { // set up recyclerview or textview
                            binding.textviewEmptyDir.isVisible = true
                            binding.recyclerviewFiles.isVisible = false
                        } else {
                            binding.textviewEmptyDir.isVisible = false
                            binding.recyclerviewFiles.isVisible = true
                            binding.recyclerviewFiles.adapter =
                                FilesAdapter(requireContext(), files, { // how to handle single clicks on items
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
                                }, { // how to handle long clicks on items
                                    if (it.isDirectory) {
                                        DirectoryActionsBottomSheet(
                                            it,
                                            requireActivity().supportFragmentManager,
                                            ftpClient,
                                            directory, { updateUi() }, { itBool, suc, fail -> showSnackbar(itBool, suc, fail) })
                                    } else if (it.isFile) {
                                        FileActionsBottomSheet(
                                            it,
                                            requireActivity().supportFragmentManager,
                                            ftpClient,
                                            directory,
                                            { updateUi() },
                                            { itBool, suc, fail -> showSnackbar(itBool, suc, fail) })
                                    }
                                })
                        }
                        binding.swipeRefresh.isRefreshing = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        binding.swipeRefresh.isRefreshing = false
                        val dialog = AlertDialog.Builder(requireContext()) // show error dialog
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
                        dialog.setCanceledOnTouchOutside(false)
                        dialog.show()
                    }
                }
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Get the absolute file path of a file in the directory
     * @param fileName The name of the file, without parent directories
     */
    private fun getAbsoluteFilePath(fileName: String): String {
        return if (directory != "") {
            "$directory/$fileName"
        } else {
            fileName
        }
    }

    /**
     * Get the filename of a file in the mentioned [Uri][android.net.Uri]
     * @param uri [Uri][android.net.Uri] of the file
     * @return The display name/file name of the file behind the URI
     */
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

    /**
     * Show an information snackbar
     * @param success If the operation was successful
     * @param successRes The text that is shown if the operation was successful
     * @param failedRes The text that is shown if the operation failed
     */
    private fun showSnackbar(success: Boolean, @StringRes successRes: Int, @StringRes failedRes: Int) {
        Snackbar.make(binding.root, if (success) {
            successRes
        } else {
            failedRes
        }, Snackbar.LENGTH_SHORT).show()
    }
}