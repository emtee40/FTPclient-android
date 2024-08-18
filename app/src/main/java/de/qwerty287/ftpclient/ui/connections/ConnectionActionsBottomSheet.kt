package de.qwerty287.ftpclient.ui.connections

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
import de.qwerty287.ftpclient.data.Connection
import de.qwerty287.ftpclient.databinding.BottomSheetConnectionActionsBinding
import de.qwerty287.ftpclient.ui.FragmentUtils.store
import kotlinx.coroutines.launch

class ConnectionActionsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance(connectionId: Int): ConnectionActionsBottomSheet {
            val args = Bundle()
            args.putInt("connectionId", connectionId)
            val fragment = ConnectionActionsBottomSheet()
            fragment.arguments = args
            return fragment
        }
    }

    private var _binding: BottomSheetConnectionActionsBinding? = null
    private val binding get() = _binding!!

    private lateinit var connection: Connection
    private lateinit var db: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = BottomSheetConnectionActionsBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = AppDatabase.getInstance(requireContext())
        lifecycleScope.launch {
            connection = db.connectionDao().get(requireArguments().getInt("connectionId").toLong())!!
            binding.connectionName.text = connection.title
        }

        binding.deleteConnection.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_connection_confirmation)
                .setMessage(R.string.delete_connection_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok) { _: DialogInterface, _: Int ->
                    lifecycleScope.launch {
                        db.bookmarkDao().getAllByConnection(connection.id.toLong()).forEach {
                            db.bookmarkDao().delete(it)
                        }
                        if (connection.privateKey) {
                            store.kfm.delete(connection.id)
                        }
                        db.connectionDao().delete(connection)
                    }
                }
                .create()
                .show()
            dismiss()
        }

        binding.editConnection.setOnClickListener {
            val options = Bundle()
            options.putInt("connection", connection.id)
            findNavController().navigate(R.id.action_ConnectionsFragment_to_AddConnectionFragment, options)
            dismiss()
        }

        binding.copyConnection.setOnClickListener {
            val newConn = Connection(
                connection.title,
                connection.server,
                connection.port,
                connection.username,
                connection.privateKey,
                connection.password,
                connection.type,
                connection.implicit,
                connection.utf8,
                connection.passive,
                connection.privateData,
                connection.startDirectory,
                connection.safIntegration,
                connection.askPassword,
            )
            lifecycleScope.launch {
                val newId = db.connectionDao().insert(newConn)
                if (connection.privateKey) {
                    store.kfm.copy(connection.id, newId.toInt())
                }
            }
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}