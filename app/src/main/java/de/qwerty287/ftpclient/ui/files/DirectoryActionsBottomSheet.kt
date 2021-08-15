package de.qwerty287.ftpclient.ui.files

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.databinding.BottomSheetDirectoryActionsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile

class DirectoryActionsBottomSheet(
    private val directory: FTPFile,
    fm: FragmentManager,
    private val client: FTPClient,
    private val currentDirectory: String,
    private val updateParent: () -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: BottomSheetDirectoryActionsBinding? = null
    private val binding get() = _binding!!

    init {
        show(fm, "DirectoryActionsBottomSheet")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetDirectoryActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.dirName.text = directory.name

        binding.dirProperties.setOnClickListener {
            val options = Bundle()
            options.putSerializable("file", directory)
            findNavController().navigate(R.id.action_to_FilePropertiesFragment, options)
            dismiss()
        }

        binding.deleteDir.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_dir_confirmation)
                .setMessage(R.string.dir_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            client.removeDirectory(getAbsoluteFilePath())
                            updateParent()
                        }
                    }
                }
                .create()
                .show()
            dismiss()
        }

        binding.renameDir.setOnClickListener {
            val view2 = layoutInflater.inflate(R.layout.dialog_entry, null)
            val editText = view2.findViewById<EditText>(R.id.edittext_dialog)
            editText.setText(directory.name)
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.enter_new_filename)
                .setView(view2)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    val newName = editText.text.toString()
                    lifecycleScope.launch {
                        withContext(Dispatchers.IO) {
                            client.rename(getAbsoluteFilePath(), getAbsoluteFilePath(newName))
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

    private fun getAbsoluteFilePath(fileName: String = directory.name): String {
        return if (currentDirectory != "") {
            "$currentDirectory/$fileName"
        } else {
            fileName
        }
    }
}