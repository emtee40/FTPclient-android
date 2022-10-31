package de.qwerty287.ftpclient.ui.files

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.databinding.FragmentFilePropertiesBinding
import de.qwerty287.ftpclient.ui.files.providers.File
import java.text.SimpleDateFormat

class FilePropertiesFragment : Fragment() {

    private var _binding: FragmentFilePropertiesBinding? = null
    private val binding get() = _binding!!
    private var file: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            file = if (Build.VERSION.SDK_INT >= 33) {
                arguments?.getSerializable("file", File::class.java)
            } else {
                arguments?.getSerializable("file") as File?
            }
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

        if (file!!.isDirectory) {
            binding.fileSizeLayout.isVisible = false
        }
        if (file!!.isSymbolicLink) {
            binding.fileSymbolicLinkLayout.isVisible = true
        }

        binding.filename.text = file!!.name
        binding.fileSize.text = getFileByteString()
        binding.fileOwner.text = file!!.user
        binding.fileGroup.text = file!!.group
        binding.fileTimestamp.text = SimpleDateFormat.getDateTimeInstance().format(file!!.timestamp.time)
        binding.fileSymbolicLink.text = file!!.link
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Returns a [CharSequence] that contains the file size with the correct prefix and unit
     * @return The [CharSequence]
     */
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
