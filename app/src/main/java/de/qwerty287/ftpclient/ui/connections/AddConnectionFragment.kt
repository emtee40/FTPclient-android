package de.qwerty287.ftpclient.ui.connections

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.data.Connection
import de.qwerty287.ftpclient.databinding.FragmentAddConnectionBinding
import kotlinx.coroutines.launch

class AddConnectionFragment : Fragment() {

    private var _binding: FragmentAddConnectionBinding? = null
    private val binding get() = _binding!!

    private var connectionId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAddConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val connectionId = arguments?.getInt("connection")
        if (connectionId != null) {
            loadConnection(connectionId)
            this.connectionId = connectionId
        }

        fun checkInputs() {
            binding.addConnection.isClickable = !(binding.title.text.isNullOrBlank() ||
                    binding.server.text.isNullOrBlank() || // TODO check guest mode
                    binding.user.text.isNullOrBlank() ||
                    binding.password.text.isNullOrBlank())
        }

        binding.apply {
            listOf(title, server, user, password).forEach {
                it.doOnTextChanged { _, _, _, _ ->
                    checkInputs()
                }
            }
        }

        binding.addConnection.setOnClickListener {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext()).connectionDao()
                if (connectionId == null) {
                    val connection = Connection(binding.title.text.toString(),
                    binding.server.text.toString(),
                    binding.user.text.toString(), binding.password.text.toString())
                    db.insert(connection)
                } else {
                    val connection = Connection(binding.title.text.toString(),
                    binding.server.text.toString(),
                    binding.user.text.toString(), binding.password.text.toString(), connectionId)
                    db.update(connection)
                }
            }
            findNavController().navigateUp()
        }
        binding.addConnection.isClickable = false
    }

    private fun loadConnection(id: Int) {
        lifecycleScope.launch {
            val c = AppDatabase.getInstance(requireContext()).connectionDao().get(id.toLong())
            if (c != null) {
                binding.title.setText(c.title)
                binding.server.setText(c.server)
                binding.user.setText(c.username)
                binding.password.setText(c.password)
            }

        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}