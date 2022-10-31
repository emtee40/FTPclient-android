@file:Suppress("BlockingMethodInNonBlockingContext")

package de.qwerty287.ftpclient.ui.files

import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.databinding.BottomSheetFileActionsBinding
import de.qwerty287.ftpclient.ui.files.providers.Client
import de.qwerty287.ftpclient.ui.files.providers.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileActionsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(file: File, client: Client, connId: Int, directory: String, updateParent: () -> Unit, showSnackbar: (Boolean, Int, Int) -> Unit): FileActionsBottomSheet {
            val args = Bundle()
            args.putSerializable("file", file)
            args.putString("directory", directory)
            args.putInt("connection", connId)
            val fragment = FileActionsBottomSheet()
            fragment.arguments = args
            fragment.client = client
            fragment.updateParent = updateParent
            fragment.showSnackbar = showSnackbar
            return fragment
        }
    }

    private var _binding: BottomSheetFileActionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var file: File
    private lateinit var client: Client
    private lateinit var directory: String
    private lateinit var updateParent: (() -> Unit)
    private lateinit var showSnackbar: ((Boolean, Int, Int) -> Unit)

    private val result: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    val uri = it.data?.data
                    if (uri != null) {
                        val outputStream = requireContext().contentResolver.openOutputStream(uri)
                        val success = try {
                            client.download(getAbsoluteFilePath(), outputStream!!)
                        } catch (e: NullPointerException) {
                            false
                        } catch (e: Exception) {
                            e.printStackTrace()
                            false
                        }
                        outputStream?.close()
                        showSnackbar(success, R.string.download_completed, R.string.download_failed)
                    }
                }
            }
            if (it.data?.data != null) dismiss()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        file = if (Build.VERSION.SDK_INT >= 33) {
            requireArguments().getSerializable("file", File::class.java) as File
        } else {
            requireArguments().getSerializable("file") as File
        }
        directory = requireArguments().getString("directory")!!
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetFileActionsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.filename.text = file.name

        if (file.isDirectory) {
            binding.downloadFile.isVisible = false
        }
        if (!FileExtensions.previewable(file.name)) {
            binding.previewFile.isVisible = false
        }

        binding.previewFile.setOnClickListener {
            val options = Bundle()
            options.putInt("connection", requireArguments().getInt("connection"))
            options.putString("file", getAbsoluteFilePath())
            findNavController().navigate(R.id.action_FilesFragment_to_FileViewFragment, options)
            dismiss()
        }

        binding.downloadFile.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_TITLE, file.name)
            result.launch(intent)
        }

        binding.fileProperties.setOnClickListener {
            val options = Bundle()
            options.putSerializable("file", file)
            findNavController().navigate(R.id.action_FilesFragment_to_FilePropertiesFragment, options)
            dismiss()
        }

        binding.deleteFile.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(
                    if (file.isFile) {
                        R.string.delete_confirmation
                    } else {
                        R.string.delete_dir_confirmation
                    }
                )
                if (file.isDirectory) {
                    setMessage(R.string.dir_delete_message)
                }
                setNegativeButton(R.string.cancel, null)
                setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            val success = try {
                                if (file.isFile) {
                                    client.rm(getAbsoluteFilePath())
                                } else {
                                    client.rmDir(getAbsoluteFilePath())
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }
                            showSnackbar(success, R.string.delete_completed, R.string.deletion_failed)
                            updateParent()
                        }
                    }
                }
                create().show()
            }
            dismiss()
        }

        binding.renameFile.setOnClickListener {
            val view2 = layoutInflater.inflate(R.layout.dialog_entry, null)
            val editText = view2.findViewById<EditText>(R.id.edittext_dialog)
            editText.setText(file.name)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.enter_new_filename)
                .setView(view2)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    val newName = editText.text.toString()
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            val success = try {
                                client.rename(getAbsoluteFilePath(), getAbsoluteFilePath(newName))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }
                            showSnackbar(success, R.string.renaming_completed, R.string.renaming_failed)
                            updateParent()
                        }
                    }
                }
                .create()
                .show()
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Get the absolute file path of the file
     * @param fileName The name of the file, without parent directories
     */
    private fun getAbsoluteFilePath(fileName: String = file.name): String {
        return if (directory != "") {
            "$directory/$fileName"
        } else {
            fileName
        }
    }
}