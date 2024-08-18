package de.qwerty287.ftpclient.ui.files

import android.content.DialogInterface
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.*
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.core.view.MenuProvider
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
import de.qwerty287.ftpclient.providers.File
import de.qwerty287.ftpclient.providers.SortingFilter
import de.qwerty287.ftpclient.ui.FragmentUtils.store
import de.qwerty287.ftpclient.ui.files.utils.CounterSnackbar
import de.qwerty287.ftpclient.ui.files.utils.CountingInputStream
import de.qwerty287.ftpclient.ui.files.utils.error.ErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream


class FilesFragment : Fragment() {

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var directory: String

    private var _connection: Connection? = null
    private var connection: Connection
        get() = _connection!!
        set(value) {
            _connection = value
        }
    private var connectionPassword: String? = null
    private val sortingFilter = SortingFilter()
    private lateinit var files: List<File>
    private val downloadMultipleDialog = DownloadMultipleDialog(this)
    private lateinit var downloadMultipleMenuItem: MenuItem

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
                        val success = uploadFile(
                            clipUri,
                            getString(R.string.uploading_multi, succeeded + failed + 1, clipData.itemCount)
                        )
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
            store.getClient(connection, connectionPassword).upload(
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
        if (_connection == null) {
            // not initialized yet, so store the directory
            directory = requireArguments().getString("directory", "")
        }

        _binding = FragmentFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerviewFiles.layoutManager = LinearLayoutManager(requireContext())

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
                inflater.inflate(R.menu.files_menu, menu)
                menu.findItem(
                    when (sortingFilter.method) {
                        SortingFilter.Method.NAME -> R.id.sort_name
                        SortingFilter.Method.TIMESTAMP -> R.id.sort_timestamp
                        SortingFilter.Method.SIZE -> R.id.sort_size
                        SortingFilter.Method.SERVER -> R.id.sort_server
                    }
                )?.isChecked = true
                menu.findItem(R.id.sort_descending)?.isChecked = sortingFilter.descending
                downloadMultipleMenuItem = menu.findItem(R.id.download_multiple)!!
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return menuItemSelected(menuItem)
            }
        }, viewLifecycleOwner)

        initConnection()

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
                                store.getClient(connection, connectionPassword).mkdir(getAbsoluteFilePath(dirName))
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

    private fun menuItemSelected(item: MenuItem): Boolean {
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

            R.id.sort_name -> {
                item.isChecked = true
                sortingFilter.method = SortingFilter.Method.NAME
                updateUi()
                true
            }

            R.id.sort_timestamp -> {
                item.isChecked = true
                sortingFilter.method = SortingFilter.Method.TIMESTAMP
                updateUi()
                true
            }

            R.id.sort_size -> {
                item.isChecked = true
                sortingFilter.method = SortingFilter.Method.SIZE
                updateUi()
                true
            }

            R.id.sort_server -> {
                item.isChecked = true
                sortingFilter.method = SortingFilter.Method.SERVER
                updateUi()
                true
            }

            R.id.sort_descending -> {
                item.isChecked = !item.isChecked
                sortingFilter.descending = item.isChecked
                updateUi()
                true
            }

            R.id.show_hidden -> {
                item.isChecked = !item.isChecked
                sortingFilter.showHidden = item.isChecked
                updateUi()
                true
            }

            R.id.download_multiple -> {
                downloadMultipleDialog.open(requireContext(), files)
                true
            }

            else -> false
        }
    }

    private fun initConnection() {
        lifecycleScope.launch {
            if (_connection == null) {
                // get connection from connection id, which is stored in the arguments
                connection = AppDatabase.getInstance(requireContext()).connectionDao()
                    .get(requireArguments().getInt("connection").toLong())!!

                if (directory == "") {
                    directory = connection.startDirectory
                }

                if (connection.askPassword) {
                    // open dialog
                    val view = layoutInflater.inflate(R.layout.dialog_entry, null)
                    val editText: EditText = view.findViewById(R.id.edittext_dialog)
                    editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    editText.setHint(R.string.password)

                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle(R.string.enter_password)
                        .setView(view)
                        .setPositiveButton(R.string.ok) { _, _ ->
                            connectionPassword = editText.text.toString()
                            initUi()
                        }
                        .setNegativeButton(R.string.cancel) { d, _ ->
                            d.dismiss()
                            findNavController().navigateUp()
                        }
                        .setOnCancelListener { d ->
                            d.dismiss()
                            findNavController().navigateUp()
                        }
                        .show()
                } else {
                    initUi()
                }

            } else {
                updateUi()
            }
        }
    }

    private fun initUi() {
        checkForUploadUri()
        checkForUploadUrisMulti()
        updateUi()
    }

    /**
     * Update the UI. This:
     * * connects to the server and logins with the login credentials
     * * retrieves the file list from the FTP server
     * * and sets up the [RecyclerView][androidx.recyclerview.widget.RecyclerView] or shows a [TextView][android.widget.TextView] if the directory contains nothing
     */
    private fun updateUi() {
        if (_binding == null) {
            // it seems that this isn't ready yet
            return
        }

        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                binding.swipeRefresh.isRefreshing = true
            }
            try {
                withContext(Dispatchers.IO) {
                    files = sortingFilter.sortFilter(
                        if (directory == "") { // get files
                            store.getClient(connection, connectionPassword).list()
                        } else {
                            store.getClient(connection, connectionPassword).list(directory)
                        }
                    )
                }

                withContext(Dispatchers.Main) {
                    downloadMultipleMenuItem.isVisible = DownloadMultipleDialog.available(files)
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
                                    options.putString(
                                        "directory",
                                        subDirectory(it)
                                    )
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

    /**
     * Get a subdirectory. Callers must make sure that the [file] is either
     * a directory or a link, and if it's a link, [File.link] must not be null.
     */
    private fun subDirectory(file: File): String {
        val name = if (file.isDirectory) file.name else file.link!!
        if (name.startsWith("/")) {
            return name
        }
        return File.joinPaths(directory, name)
    }

    private fun newFileBottomSheet(file: File) {
        FileActionsBottomSheet.newInstance(
            file,
            connection.id,
            directory,
            CounterSnackbar(
                binding.root,
                getString(R.string.downloading),
                requireActivity(),
                false,
            ),
            this::updateUi,
            this::showSnackbar
        )
            .show(requireActivity().supportFragmentManager, "FileActionsBottomSheet")
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
        ErrorDialog(this, e, this::updateUi)
    }

    /**
     * Get the absolute file path of a file in the directory
     * @param fileName The name of the file, without parent directories
     */
    internal fun getAbsoluteFilePath(fileName: String): String {
        return File.joinPaths(directory, fileName)
    }

    /**
     * Get the filename of a file in the mentioned [Uri][android.net.Uri]
     * @param uri [Uri][android.net.Uri] of the file
     * @return The display name/file name of the file behind the URI
     */
    private fun getFilenameFromUri(uri: Uri): String {
        val uriString = uri.toString()
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
            displayName = java.io.File(uriString).name
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
        if (requireArguments().getString("uri") == null && requireArguments().getString("text") == null) return
        val isText = requireArguments().getString("text") != null
        val uri = if (isText) null else Uri.parse(requireArguments().getString("uri"))
        val text = if (isText) requireArguments().getString("text") else null

        requireArguments().remove("uri")
        requireArguments().remove("text")

        val sb =
            CounterSnackbar(binding.root, getString(R.string.uploading), requireActivity())
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                binding.swipeRefresh.isRefreshing = true
            }
            withContext(Dispatchers.IO) {
                val inputStream = (if (isText) {
                    ByteArrayInputStream(text!!.toByteArray())
                } else {
                    requireContext().contentResolver.openInputStream(uri!!)
                })?.let {
                    CountingInputStream(it) { read ->
                        sb.update(read, read + it.available())
                    }
                }
                val success: Boolean = try {
                    store.getClient(connection, connectionPassword).upload(
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

    private fun checkForUploadUrisMulti() {
        val uris = if (Build.VERSION.SDK_INT >= 33) {
            requireArguments().getParcelableArrayList("uris", Uri::class.java)
        } else {
            requireArguments().getParcelableArrayList("uris")
        } ?: return

        requireArguments().remove("uris")

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                withContext(Dispatchers.Main) {
                    binding.swipeRefresh.isRefreshing = true
                }
                var succeeded = 0
                var failed = 0
                for (i in uris) {
                    val success = uploadFile(i, getString(R.string.uploading_multi, succeeded + failed + 1, uris.size))
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