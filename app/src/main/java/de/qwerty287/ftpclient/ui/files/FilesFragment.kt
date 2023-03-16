@file:Suppress("BlockingMethodInNonBlockingContext")

package de.qwerty287.ftpclient.ui.files

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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.data.Connection
import de.qwerty287.ftpclient.databinding.FragmentFilesBinding
import de.qwerty287.ftpclient.providers.Client
import de.qwerty287.ftpclient.providers.File
import de.qwerty287.ftpclient.ui.files.utils.CounterSnackbar
import de.qwerty287.ftpclient.ui.files.utils.CountingInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream


class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var directory: String

    private var client: Client? = null
    private lateinit var connection: Connection

    private val result: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val uri = it.data?.data
                val clipData = it.data?.clipData
                if (uri != null) {
                    withContext(Dispatchers.Main) {
                        binding.swipeRefresh.isRefreshing = true
                    }
                    val success = uploadFile(uri, getString(R.string.uploading))
                    showSnackbar(success, R.string.upload_completed, R.string.upload_failed)
                    updateUi()
                } else if (clipData != null) {
                    withContext(Dispatchers.Main) {
                        binding.swipeRefresh.isRefreshing = true
                    }
                    var succeeded = 0
                    var failed = 0
                    for (i in 0 until clipData.itemCount) {
                        val clipUri = clipData.getItemAt(i).uri
                        val success = uploadFile(clipUri, getString(R.string.uploading_multi, succeeded + failed + 1, clipData.itemCount))
                        if (success) {
                            succeeded += 1
                        } else {
                            failed += 1
                        }
                    }
                    Snackbar.make(
                        binding.root,
                        String.format(requireContext().getString(R.string.upload_summary), succeeded, failed),
                        Snackbar.LENGTH_SHORT
                    ).show()
                    updateUi()
                }
            }
        }
    }

    private fun uploadFile(uri: Uri, text: String): Boolean {
        val sb = CounterSnackbar(binding.root, text, requireActivity())
        val inputStream = requireContext().contentResolver.openInputStream(uri)?.let {
            CountingInputStream(it) { read ->
                sb.update(read, read + it.available())
            }
        }
        val success: Boolean = try {
            client!!.upload(
                getAbsoluteFilePath(getFilenameFromUri(uri)),
                inputStream!!
            )
        } catch (e: NullPointerException) {
            false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
        sb.dismiss()
        inputStream?.close()
        return success
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
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.enter_dir_name)
                .setView(view2)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    val dirName = editText.text.toString()
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            val success = try {
                                client!!.mkdir(getAbsoluteFilePath(dirName))
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
            requestFileIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
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
        return when (item.itemId) {
            R.id.go_to -> {
                val view = layoutInflater.inflate(R.layout.dialog_entry, null)
                val editText: EditText = view.findViewById(R.id.edittext_dialog)
                editText.setText(directory)
                editText.setHint(R.string.go_to)
                MaterialAlertDialogBuilder(requireContext())
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

            R.id.update_ui -> {
                updateUi()
                true
            }

            R.id.bookmark -> {
                val options = Bundle()
                options.putString("directory", directory)
                options.putInt("connectionId", connection.id)
                findNavController().navigate(R.id.action_FilesFragment_to_AddBookmarkFragment, options)
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
        var files: List<File>

        if (_binding == null) {
            // seems that this isn't ready yet
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = true
                }
                try {
                    if (client?.isConnected != true) {
                        connection = arguments?.getInt("connection")?.let {
                            AppDatabase.getInstance(requireContext()).connectionDao()
                                .get(it.toLong())
                        }!! // get connection from connection id, which is stored in the arguments

                        client = connection.client()

                        checkForUploadUri()
                    }

                    files = if (directory == "") { // get files
                        client!!.list()
                    } else {
                        client!!.list(directory)
                    }.sortedBy { item -> item.name }

                    withContext(Dispatchers.Main) {
                        if (files.isEmpty()) { // set up RecyclerView or TextView
                            binding.textviewEmptyDir.isVisible = true
                            binding.recyclerviewFiles.isVisible = false
                        } else {
                            binding.textviewEmptyDir.isVisible = false
                            binding.recyclerviewFiles.isVisible = true
                            binding.recyclerviewFiles.adapter =
                                FilesAdapter(files, { // how to handle single clicks on items
                                    if (it.isDirectory || (it.isSymbolicLink && it.link != null)) {
                                        val options = Bundle()
                                        options.putString("directory", "$directory/${if (it.isDirectory) it.name else it.link}")
                                        options.putInt("connection", connection.id)
                                        findNavController().navigate(
                                            R.id.action_FilesFragment_to_FilesFragment,
                                            options
                                        )
                                    } else if (!it.isUnknown) {
                                        newFileBottomSheet(it)
                                    }
                                }) { // how to handle long clicks on items
                                    if (!it.isUnknown) {
                                        newFileBottomSheet(it)
                                    }
                                }
                        }
                        binding.swipeRefresh.isRefreshing = false
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        showErrorDialog(e)
                    }
                }
            }

        }
    }

    private fun newFileBottomSheet(file: File) {
        FileActionsBottomSheet.newInstance(
            file,
            client!!,
            connection.id,
            directory,
            CounterSnackbar(
                binding.root,
                getString(R.string.downloading),
                requireActivity(),
                false
            ),
            { updateUi() },
            { itBool, suc, fail -> showSnackbar(itBool, suc, fail) }).show(requireActivity().supportFragmentManager, "FileActionsBottomSheet")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Shows error dialog
     */
    private fun showErrorDialog(e: Exception) {
        binding.swipeRefresh.isRefreshing = false
        lifecycleScope.launch {
            val dialog = MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.error_occurred)
                .setMessage(R.string.error_descriptions)
                .setPositiveButton(R.string.retry) { _: DialogInterface, _: Int ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            try {
                                client = connection.client() // try to reconnect
                                updateUi()
                            } catch (e: Exception) {
                                showErrorDialog(e)
                            }
                        }
                    }
                }
                .setOnCancelListener { findNavController().navigateUp() }
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
                .setNegativeButton(R.string.ok) { _: DialogInterface, _: Int ->
                    findNavController().navigateUp()
                }
                .create()
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
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
        val file = java.io.File(uriString)
        var displayName = "0"

        if (uriString.startsWith("content://")) {
            var cursor: Cursor? = null
            try {
                cursor = requireActivity().contentResolver.query(uri, null, null, null, null)
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    displayName =
                        cursor.getString(index)
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
        Snackbar.make(
            binding.root, if (success) {
                successRes
            } else {
                failedRes
            }, Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun checkForUploadUri() {
        if (arguments?.getString("uri") == null && arguments?.getString("text") == null) return
        val isText = arguments?.getString("text") != null
        val uri = if (isText) null else Uri.parse(arguments?.getString("uri"))
        val text = if (isText) arguments?.getString("text") else null

        val sb =
            CounterSnackbar(binding.root, getString(R.string.uploading), requireActivity())
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                binding.swipeRefresh.isRefreshing = true
            }
            withContext(Dispatchers.IO) {
                val inputStream = (if (isText) {
                    object : InputStream() {
                        var index = 0
                        val chars = text!!.toCharArray()
                        override fun read(): Int {
                            val i = if (chars.size > index) chars[index].code else -1
                            index++
                            return i
                        }

                        override fun available(): Int {
                            return chars.size - index
                        }
                    }
                } else {
                    requireContext().contentResolver.openInputStream(uri!!)
                })?.let {
                    CountingInputStream(it) { read ->
                        sb.update(read, read + it.available())
                    }
                }
                val success: Boolean = try {
                    client!!.upload(
                        getAbsoluteFilePath(
                            if (isText) {
                                text!!.split(" ", limit = 2)[0]
                            } else {
                                getFilenameFromUri(uri!!)
                            }
                        ),
                        inputStream!!
                    )
                } catch (e: NullPointerException) {
                    false
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
                sb.dismiss()
                inputStream?.close()
                showSnackbar(success, R.string.upload_completed, R.string.upload_failed)
                updateUi()
            }
        }
    }
}