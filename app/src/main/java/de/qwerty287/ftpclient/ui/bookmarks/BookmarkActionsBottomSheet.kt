package de.qwerty287.ftpclient.ui.bookmarks

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.data.Bookmark
import de.qwerty287.ftpclient.databinding.BottomSheetBookmarkActionsBinding
import kotlinx.coroutines.launch

class BookmarkActionsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(bookmarkId: Int): BookmarkActionsBottomSheet {
            val args = Bundle()
            args.putInt("bookmarkId", bookmarkId)
            val fragment = BookmarkActionsBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomSheetBookmarkActionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var bookmark: Bookmark
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetBookmarkActionsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())
        lifecycleScope.launch {
            bookmark = db.bookmarkDao().get(requireArguments().getInt("bookmarkId").toLong())!!
            binding.bookmarkName.text = bookmark.title
        }

        binding.deleteBookmark.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_bookmark_confirmation)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    lifecycleScope.launch {
                        db.bookmarkDao().delete(bookmark)
                    }
                }
                .create()
                .show()
            dismiss()
        }

        binding.editBookmark.setOnClickListener {
            val options = Bundle()
            options.putInt("bookmarkId", bookmark.id)
            findNavController().navigate(R.id.action_BookmarksFragment_to_AddBookmarkFragment, options)
            dismiss()
        }

        binding.copyBookmark.setOnClickListener {
            val newBookmark = Bookmark(bookmark.title, bookmark.directory, bookmark.connection)
            lifecycleScope.launch {
                db.bookmarkDao().insert(newBookmark)
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}