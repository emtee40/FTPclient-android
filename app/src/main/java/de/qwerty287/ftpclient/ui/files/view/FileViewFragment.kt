@file:Suppress("BlockingMethodInNonBlockingContext")

package de.qwerty287.ftpclient.ui.files.view

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.data.Connection
import de.qwerty287.ftpclient.databinding.FragmentFileViewBinding
import de.qwerty287.ftpclient.providers.Client
import de.qwerty287.ftpclient.ui.files.FileExtensions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream


class FileViewFragment : Fragment() {

    private var _binding: FragmentFileViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var file: String

    private var client: Client? = null
    private lateinit var connection: Connection

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        file = requireArguments().getString("file")!!

        _binding = FragmentFileViewBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    if (client?.isConnected != true) {
                        // get connection from connection id, which is stored in the arguments
                        connection = AppDatabase.getInstance(requireContext()).connectionDao()
                            .get(requireArguments().getInt("connection").toLong())!!

                        client = connection.client(requireContext())
                    }

                    val byteList = ArrayList<Int>()
                    client!!.download(file, object : OutputStream() {
                        override fun write(b: Int) {
                            byteList.add(b)
                        }
                    })

                    val isImage = FileExtensions.isImage(file)
                    withContext(Dispatchers.Main) {
                        binding.loading.isVisible = false
                        if (isImage) {
                            binding.textView.isVisible = false
                            val byteArray = ByteArray(byteList.size)
                            for (i in 0 until byteList.size) {
                                byteArray[i] = byteList[i].toByte()
                            }
                            val bmp = BitmapFactory.decodeByteArray(byteArray, 0, byteList.size)
                            if (bmp == null) {
                                Toast.makeText(requireContext(), R.string.bad_file, Toast.LENGTH_LONG).show()
                                findNavController().navigateUp()
                            } else {
                                binding.imageView.setImageBitmap(bmp)
                            }
                        } else {
                            binding.imageView.isVisible = false
                            var fileStr = ""
                            for (b in byteList) {
                                fileStr += b.toChar().toString()
                            }
                            binding.textView.text = fileStr
                            binding.fileContent.setText(fileStr)

                            requireActivity().addMenuProvider(object : MenuProvider {
                                private lateinit var menu: Menu

                                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                                    this.menu = menu
                                    menuInflater.inflate(R.menu.view_menu, menu)
                                }

                                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                                    return when (menuItem.itemId) {
                                        R.id.edit_menu -> {
                                            binding.textView.isVisible = false
                                            binding.fileContentLayout.isVisible = true
                                            menuItem.isVisible = false
                                            menu.getItem(1).isVisible = true
                                            true
                                        }

                                        R.id.save_menu -> {
                                            saveFile {
                                                menuItem.isVisible = false
                                                menu.getItem(0).isVisible = true
                                            }
                                            true
                                        }

                                        else -> false
                                    }
                                }
                            }, viewLifecycleOwner)
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val dialog = MaterialAlertDialogBuilder(requireContext()) // show error dialog
                            .setTitle(R.string.error_occurred)
                            .setMessage(R.string.error_descriptions)
                            .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                                findNavController().navigateUp()
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

    private fun saveFile(updateMenu: () -> Unit) {
        binding.fileContent.isEnabled = false
        val content = binding.fileContent.text.toString()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    client!!.upload(file, object : InputStream() {
                        private var index = 0

                        override fun read(): Int {
                            if (index >= content.length) {
                                return -1
                            }
                            val b = content[index]
                            index++
                            return b.code
                        }
                    })

                    withContext(Dispatchers.Main) {
                        binding.textView.text = content
                        binding.textView.isVisible = true
                        binding.fileContentLayout.isVisible = false
                        updateMenu()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        val dialog = MaterialAlertDialogBuilder(requireContext()) // show error dialog
                            .setTitle(R.string.error_occurred)
                            .setMessage(R.string.error_descriptions)
                            .setPositiveButton(R.string.ok) { d: DialogInterface, _: Int ->
                                d.dismiss()
                            }
                            .setNeutralButton(R.string.copy) { d: DialogInterface, _: Int ->
                                val clipboardManager =
                                    requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText(
                                        getString(R.string.app_name),
                                        e.stackTraceToString()
                                    )
                                )
                                d.dismiss()
                            }
                            .create()
                        dialog.setCanceledOnTouchOutside(false)
                        dialog.show()
                    }
                }
                withContext(Dispatchers.Main) {
                    binding.fileContent.isEnabled = true
                }
            }
        }
    }
}