package de.qwerty287.ftpclient.ui.files

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.databinding.BottomSheetFileActionsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile

class FileActionsBottomSheet(
    private val file: FTPFile,
    fm: FragmentManager,
    private val client: FTPClient,
    private val directory: String,
    private val updateParent: (() -> Unit),
    private val showSnackbar: ((Boolean, Int, Int) -> Unit)
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetFileActionsBinding? = null
    private val binding get() = _binding!!

    private val result: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val uri = it.data?.data
                if (uri != null) {
                    val outputStream = requireContext().contentResolver.openOutputStream(uri)
                    val success = try {
                        client.retrieveFile(getAbsoluteFilePath(), outputStream)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        false
                    }
                    outputStream?.close()
                    showSnackbar(success, R.string.download_completed, R.string.download_failed)
                }
                dismiss()
            }
        }
    }

    init {
        show(fm, "FileActionsBottomSheet")
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
            findNavController().navigate(R.id.action_to_FilePropertiesFragment, options)
            dismiss()
        }

        binding.deleteFile.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            val success = try {
                                client.deleteFile(getAbsoluteFilePath())
                            } catch (e: Exception) {
                                e.printStackTrace()
                                false
                            }
                            showSnackbar(success, R.string.delete_completed, R.string.deletion_failed)
                            updateParent()
                        }
                    }
                }
                .create()
                .show()
            dismiss()
        }

        binding.renameFile.setOnClickListener {
            val view2 = layoutInflater.inflate(R.layout.dialog_entry, null)
            val editText = view2.findViewById<EditText>(R.id.edittext_dialog)
            editText.setText(file.name)
            AlertDialog.Builder(requireContext())
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