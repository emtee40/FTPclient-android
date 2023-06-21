package de.qwerty287.ftpclient.ui.connections

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import de.qwerty287.ftpclient.MainActivity
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.data.AppDatabase
import de.qwerty287.ftpclient.data.Connection
import de.qwerty287.ftpclient.databinding.FragmentAddConnectionBinding
import de.qwerty287.ftpclient.providers.Provider
import de.qwerty287.ftpclient.providers.sftp.KeyFileManager
import de.qwerty287.ftpclient.ui.FragmentUtils.store
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.userauth.keyprovider.KeyFormat
import net.schmizz.sshj.userauth.keyprovider.KeyProviderUtil
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPSClient
import java.io.File

class AddConnectionFragment : Fragment() {

    private var _binding: FragmentAddConnectionBinding? = null
    private val binding get() = _binding!!

    private var connectionId: Int? = null
    private var tempKeyFile: File? = null

    private val result: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val uri = it.data?.data
                if (uri != null) {
                    tempKeyFile?.delete()
                    tempKeyFile = store.kfm.storeTemp(uri)
                    if (KeyProviderUtil.detectKeyFileFormat(tempKeyFile) == KeyFormat.Unknown) {
                        // invalid
                        tempKeyFile!!.delete()
                        tempKeyFile = null
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), R.string.key_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                    checkInputs()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAddConnectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun checkInputs() {
        binding.addConnection.isClickable = (!(binding.title.text.isNullOrBlank() ||
                binding.server.text.isNullOrBlank() ||
                binding.port.text.isNullOrBlank())) &&
                (!(binding.publicKey.isChecked && binding.typeGroup.checkedButtonId == R.id.type_sftp) || tempKeyFile != null)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val connectionId = arguments?.getInt("connection")
        if (connectionId != null) {
            (requireActivity() as AppCompatActivity).supportActionBar?.setTitle(R.string.edit_connection)
            loadConnection(connectionId)
            this.connectionId = connectionId
        }

        var portChanged = connectionId != null

        binding.apply {
            listOf(title, server, user, password).forEach {
                it.doOnTextChanged { _, _, _, _ ->
                    checkInputs()
                }
            }
        }

        binding.port.doOnTextChanged { _, _, _, _ ->
            portChanged = true
        }

        binding.typeFtp.setOnClickListener {
            binding.implicit.isVisible = false
            binding.privateData.isVisible = false
            binding.utf8.isVisible = true
            binding.passive.isVisible = true
            binding.publicKeyLayout.isVisible = false
            if (!portChanged) {
                binding.port.setText(FTPClient.DEFAULT_PORT.toString())
                // undo because doOnTextChanged is called
                portChanged = false
            }
        }

        binding.typeFtps.setOnClickListener {
            binding.implicit.isVisible = true
            binding.privateData.isVisible = true
            binding.utf8.isVisible = true
            binding.passive.isVisible = true
            binding.publicKeyLayout.isVisible = false
            if (!portChanged) {
                binding.port.setText(FTPSClient.DEFAULT_FTPS_PORT.toString())
                // undo because doOnTextChanged is called
                portChanged = false
            }
        }

        binding.typeSftp.setOnClickListener {
            binding.implicit.isVisible = false
            binding.privateData.isVisible = false
            binding.utf8.isVisible = false
            binding.passive.isVisible = false
            binding.publicKeyLayout.isVisible = true
            if (!portChanged) {
                binding.port.setText(SSHClient.DEFAULT_PORT.toString())
                // undo because doOnTextChanged is called
                portChanged = false
            }
        }

        binding.selectKeyFile.setOnClickListener {
            val requestFileIntent = Intent(Intent.ACTION_GET_CONTENT)
            requestFileIntent.type = "*/*" // TODO key file type?
            result.launch(Intent.createChooser(requestFileIntent, getString(R.string.select_file)))
        }

        binding.publicKey.setOnCheckedChangeListener { _, _ -> checkInputs() }

        binding.addConnection.setOnClickListener {
            storeConnection()
            findNavController().navigateUp()
        }
        binding.addConnection.isClickable = false
    }

    /**
     * Load and display a connection
     * @param id The id of the connection that will be loaded
     */
    private fun loadConnection(id: Int) {
        lifecycleScope.launch {
            val c = AppDatabase.getInstance(requireContext()).connectionDao().get(id.toLong())
            if (c != null) {
                binding.title.setText(c.title)
                binding.server.setText(c.server)
                binding.port.setText(c.port.toString())
                binding.user.setText(c.username)
                binding.publicKeyLayout.isVisible = c.type == Provider.SFTP
                binding.publicKey.isChecked = c.publicKey
                binding.password.setText(c.password)
                binding.startDirectory.setText(c.startDirectory)
                binding.typeGroup.check(
                    when (c.type) {
                        Provider.FTP -> R.id.type_ftp
                        Provider.FTPS -> R.id.type_ftps
                        Provider.SFTP -> R.id.type_sftp
                    }
                )
                binding.implicit.isChecked = c.implicit
                binding.implicit.isVisible = c.type == Provider.FTPS
                binding.privateData.isChecked = c.privateData
                binding.privateData.isVisible = c.type == Provider.FTPS
                binding.utf8.isChecked = c.utf8
                binding.utf8.isVisible = c.type != Provider.SFTP
                binding.passive.isChecked = c.passive
                binding.passive.isVisible = c.type != Provider.SFTP
                binding.safIntegration.isChecked = c.safIntegration

                if (c.publicKey) {
                    tempKeyFile = store.kfm.finalToTemp(c.id)
                }
            }
        }
    }

    private fun storeConnection() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext()).connectionDao()
            val prov = when (binding.typeGroup.checkedButtonId) {
                R.id.type_ftp -> Provider.FTP
                R.id.type_ftps -> Provider.FTPS
                R.id.type_sftp -> Provider.SFTP
                else -> Provider.FTP
            }
            val pubKey = binding.publicKey.isChecked && prov == Provider.SFTP
            if (connectionId == null) {
                val connection = Connection(
                    binding.title.text.toString(),
                    binding.server.text.toString(),
                    binding.port.text.toString().toInt(),
                    binding.user.text.toString(),
                    pubKey,
                    binding.password.text.toString(),
                    prov,
                    binding.implicit.isChecked,
                    binding.utf8.isChecked,
                    binding.passive.isChecked,
                    binding.privateData.isChecked,
                    binding.startDirectory.text.toString(),
                    binding.safIntegration.isChecked
                )
                connectionId = db.insert(connection).toInt()
            } else {
                val connection = Connection(
                    binding.title.text.toString(),
                    binding.server.text.toString(),
                    binding.port.text.toString().toInt(),
                    binding.user.text.toString(),
                    pubKey,
                    binding.password.text.toString(),
                    prov,
                    binding.implicit.isChecked,
                    binding.utf8.isChecked,
                    binding.passive.isChecked,
                    binding.privateData.isChecked,
                    binding.startDirectory.text.toString(),
                    binding.safIntegration.isChecked,
                    connectionId!!
                )
                db.update(connection)
            }
            if (pubKey) {
                store.kfm.tempToFinal(tempKeyFile!!, connectionId!!)
            }
            store.invalidateClient()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
