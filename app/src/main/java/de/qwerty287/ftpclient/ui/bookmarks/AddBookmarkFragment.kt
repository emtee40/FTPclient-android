package de.qwerty287.ftpclient.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.data.Bookmark
import de.qwerty287.ftpclient.databinding.FragmentAddBookmarkBinding
import kotlinx.coroutines.launch

class AddBookmarkFragment : Fragment() {

    private var _binding: FragmentAddBookmarkBinding? = null
    private val binding get() = _binding!!

    private var connectionId: Int? = null
    private var bookmark: Bookmark? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val directory = arguments?.getString("directory")

        if (directory != null) {
            connectionId = arguments?.getInt("connectionId")
        }

        _binding = FragmentAddBookmarkBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (connectionId != null) {
            binding.directory.setText(arguments?.getString("directory"))
        } else {
            (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.edit_bookmark)
            loadBookmark()
        }

        fun checkInputs() {
            binding.addBookmark.isClickable = !(binding.title.text.isNullOrBlank())
        }

        binding.title.doOnTextChanged { _, _, _, _ ->
            checkInputs()
        }

        binding.addBookmark.setOnClickListener {
            lifecycleScope.launch {
                val dao = AppDatabase.getInstance(requireContext()).bookmarkDao()
                if (connectionId != null) {
                    dao.insert(
                        Bookmark(
                            binding.title.text.toString(),
                            binding.directory.text.toString(),
                            connectionId!!
                        )
                    )
                } else if (bookmark != null) {
                    dao.update(
                        Bookmark(
                            binding.title.text.toString(),
                            binding.directory.text.toString(),
                            bookmark!!.connection,
                            bookmark!!.id
                        )
                    )
                }
                findNavController().navigateUp()
            }
        }
        binding.addBookmark.isClickable = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadBookmark() {
        lifecycleScope.launch {
            val bookmarkId = arguments?.getInt("bookmarkId")
            if (bookmarkId != null) {
                bookmark = AppDatabase.getInstance(requireContext()).bookmarkDao().get(bookmarkId.toLong())
                binding.title.setText(bookmark?.title)
                binding.directory.setText(bookmark?.directory)
            }
        }
    }
}