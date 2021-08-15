package de.qwerty287.ftpclient.ui.connections

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

        binding.fabAddConnection.setOnClickListener {
            findNavController().navigate(R.id.action_ConnectionsFragment_to_AddConnectionFragment)
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

    private fun showConnections() {
        AppDatabase.getInstance(requireContext())
            .connectionDao().getAll().observe(viewLifecycleOwner, {
            if (it.isEmpty()) {
                binding.noConnections.isVisible = true
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