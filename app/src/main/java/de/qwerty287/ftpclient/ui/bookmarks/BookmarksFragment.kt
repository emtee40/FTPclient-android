package de.qwerty287.ftpclient.ui.bookmarks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.databinding.FragmentBookmarksBinding


class BookmarksFragment : Fragment() {

    private var _binding: FragmentBookmarksBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentBookmarksBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onResume() {
        super.onResume()

        showBookmarks()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Get and display the bookmarks
     */
    private fun showBookmarks() {
        AppDatabase.getInstance(requireContext())
            .bookmarkDao().getAll().observe(viewLifecycleOwner, {
                if (it.isEmpty()) {
                    binding.noConnections.isVisible = true
                    binding.recyclerviewBookmarks.adapter = null
                } else {
                    binding.recyclerviewBookmarks.layoutManager = if (it.size == 1) {
                        LinearLayoutManager(requireContext())
                    } else {
                        GridLayoutManager(requireContext(), 2)
                    }
                    binding.recyclerviewBookmarks.adapter = BookmarkAdapter(
                        it,
                        findNavController(),
                        requireActivity().supportFragmentManager
                    )
                }
            })
    }
}