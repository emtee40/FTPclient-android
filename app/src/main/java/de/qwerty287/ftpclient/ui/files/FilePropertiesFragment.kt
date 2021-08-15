package de.qwerty287.ftpclient.ui.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.databinding.FragmentFilePropertiesBinding
import org.apache.commons.net.ftp.FTPFile

class FilePropertiesFragment : Fragment() {

    private var _binding: FragmentFilePropertiesBinding? = null
    private val binding get() = _binding!!
    private var file: FTPFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            file = arguments?.getSerializable("file") as FTPFile?
        } catch (e: Exception) {
            e.printStackTrace()
            findNavController().navigateUp()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFilePropertiesBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (file == null) {
            findNavController().navigateUp()
            return
        }

        binding.filename.text = file!!.name
        binding.fileSize.text = getFileByteString()
        binding.fileOwner.text = file!!.user
        binding.fileGroup.text = file!!.group
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getFileByteString(): CharSequence {
        if (file!!.size < 1024) {
            return String.format(getString(R.string.bytes), file!!.size)
        } else if (file!!.size < 1024 * 1024) {
            return String.format(getString(R.string.kilobytes), (file!!.size / 1024.0))
        } else if (file!!.size < 1024L * 1024 * 1024) {
            return String.format(getString(R.string.megabytes), (file!!.size / (1024.0 * 1024)))
        } else if (file!!.size < 1024L * 1024 * 1024 * 1024) {
            return String.format(getString(R.string.gigabytes), (file!!.size / (1024.0 * 1024 * 1024)))
        }
        return String.format(getString(R.string.terabytes), (file!!.size / (1024.0 * 1024 * 1024 * 1024)))
    }
}
