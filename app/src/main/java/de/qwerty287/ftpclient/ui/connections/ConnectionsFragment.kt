package de.qwerty287.ftpclient.ui.connections

import android.os.Bundle
import android.view.*
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.databinding.FragmentConnectionsBinding


class ConnectionsFragment : Fragment() {

    private var _binding: FragmentConnectionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentConnectionsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHasOptionsMenu(true)

        binding.fabAddConnection.setOnClickListener {
            findNavController().navigate(R.id.action_ConnectionsFragment_to_AddConnectionFragment)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.connection_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.bookmarks_menu -> {
                findNavController().navigate(R.id.action_ConnectionsFragment_to_BookmarksFragment)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()

        showConnections()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Get and display the connections
     */
    private fun showConnections() {
        AppDatabase.getInstance(requireContext())
            .connectionDao().getAll().observe(viewLifecycleOwner, {
            if (it.isEmpty()) {
                binding.noConnections.isVisible = true
                binding.recyclerviewMain.adapter = null
            } else {
                binding.recyclerviewMain.layoutManager = if (it.size == 1) {
                    LinearLayoutManager(requireContext())
                } else {
                    GridLayoutManager(requireContext(), 2)
                }
                binding.recyclerviewMain.adapter = ConnectionAdapter(
                    it,
                    findNavController(),
                    requireActivity().supportFragmentManager
                )
            }
        })
    }
}