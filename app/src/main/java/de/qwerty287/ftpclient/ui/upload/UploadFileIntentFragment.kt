package de.qwerty287.ftpclient.ui.upload

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.databinding.FragmentConnectionsBinding
import kotlinx.coroutines.launch


class UploadFileIntentFragment : Fragment() {

    companion object {
        internal var exit = false
    }

    private var _binding: FragmentConnectionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentConnectionsBinding.inflate(inflater, container, false)
        binding.fabAddConnection.isVisible = false
        return binding.root

    }

    override fun onResume() {
        super.onResume()

        if (exit) requireActivity().finish()

        lifecycleScope.launch { showConnectionsAndBookmarks() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Get and display the connections and bookmarks
     */
    private suspend fun showConnectionsAndBookmarks() {
        val bm = AppDatabase.getInstance(requireContext()).bookmarkDao().getListOfAll()
        val connections = AppDatabase.getInstance(requireContext()).connectionDao().getListOfAll()
        if (connections.isEmpty()) { // we don't have to check for empty bookmarks, if there are no connections, there are no bookmarks
            binding.noConnections.isVisible = true
            binding.recyclerviewMain.adapter = null
        } else {
            binding.recyclerviewMain.layoutManager = if (connections.size == 1) {
                LinearLayoutManager(requireContext())
            } else {
                GridLayoutManager(requireContext(), 2)
            }
            binding.recyclerviewMain.adapter = ConnectionAndBookmarkAdapter(
                connections,
                bm,
                findNavController(),
                requireArguments().getString("uri"),
                requireArguments().getString("text")
            )
        }
    }
}