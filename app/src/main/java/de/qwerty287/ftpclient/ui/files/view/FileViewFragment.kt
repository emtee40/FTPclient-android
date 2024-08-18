package de.qwerty287.ftpclient.ui.files.view

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.databinding.FragmentFileViewBinding
import de.qwerty287.ftpclient.ui.FragmentUtils.store
import de.qwerty287.ftpclient.ui.files.FileExtensions
import de.qwerty287.ftpclient.ui.files.utils.error.ErrorDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream


class FileViewFragment : Fragment() {

    private var _binding: FragmentFileViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var file: String

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
            try {
                val byteList = ArrayList<Int>()
                val isImage: Boolean
                withContext(Dispatchers.IO) {
                    store.getClient().download(file, object : OutputStream() {
                        override fun write(b: Int) {
                            byteList.add(b)
                        }
                    })

                    isImage = FileExtensions.isImage(file)
                }

                withContext(Dispatchers.Main) {
                    binding.loading.isVisible = false
                    if (isImage) {
                        binding.textView.isVisible = false
                        try {
                            BitmapLoader.load(byteList, binding.imageView)
                        } catch (e: BitmapLoader.LoadException) {
                            Toast.makeText(requireContext(), R.string.bad_file, Toast.LENGTH_LONG).show()
                            findNavController().navigateUp()
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
                    ErrorDialog(this@FileViewFragment, e)
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
            try {
                withContext(Dispatchers.IO) {
                    store.getClient().upload(file, object : InputStream() {
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
                }

                withContext(Dispatchers.Main) {
                    binding.textView.text = content
                    binding.textView.isVisible = true
                    binding.fileContentLayout.isVisible = false
                    updateMenu()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    ErrorDialog(this@FileViewFragment, e)
                }
            }

            withContext(Dispatchers.Main) {
                binding.fileContent.isEnabled = true
            }
        }
    }
}