package de.qwerty287.ftpclient.providers.sftp

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.qwerty287.ftpclient.MainActivity
import de.qwerty287.ftpclient.R
import net.schmizz.sshj.common.KeyType
import net.schmizz.sshj.common.SecurityUtils
import net.schmizz.sshj.transport.verification.OpenSSHKnownHosts
import java.io.File
import java.security.PublicKey
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class KeyVerifier(private val context: Context) : OpenSSHKnownHosts(File(context.filesDir, FILENAME)) {
    companion object {
        fun fromContext(context: Context): KeyVerifier {
            return if (context is MainActivity) context.state.kv else KeyVerifier(context)
        }

        private const val FILENAME = "known_hosts"
    }

    override fun hostKeyChangedAction(hostname: String?, key: PublicKey?): Boolean {
        return interact(true, hostname, key)
    }

    override fun hostKeyUnverifiableAction(hostname: String?, key: PublicKey?): Boolean {
        return interact(false, hostname, key)
    }

    private fun addKey(hostname: String?, key: PublicKey?) {
        entries().add(HostEntry(null, hostname, KeyType.fromKey(key), key))
        write()
    }

    private fun interact(keyChanged: Boolean, hostname: String?, key: PublicKey?): Boolean {
        var trust: Boolean? = null

        val lock = ReentrantLock()
        val condition = lock.newCondition()
        (context as Activity).runOnUiThread {
            val materialAlertDialogBuilder = MaterialAlertDialogBuilder(context)
                .setTitle(if (keyChanged) R.string.host_key_changed else R.string.host_key_unknown)
                .setMessage(
                    context.getString(
                        if (keyChanged) R.string.host_key_changed_msg else R.string.host_key_unknown_msg,
                        hostname, KeyType.fromKey(key), SecurityUtils.getFingerprint(key)
                    )
                )
                .setPositiveButton(R.string.mtm_decision_always) { _: DialogInterface?, _: Int ->
                    lock.withLock {
                        trust = true
                        condition.signal()
                    }
                }
                .setNeutralButton(R.string.mtm_decision_abort) { _: DialogInterface?, _: Int ->
                    lock.withLock {
                        trust = false
                        condition.signal()
                    }
                }
                .setOnCancelListener { _: DialogInterface? ->
                    lock.withLock {
                        trust = false
                        condition.signal()
                    }
                }
            materialAlertDialogBuilder.show()
        }
        try {
            lock.withLock {
                condition.await()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        if (trust!!) {
            addKey(hostname, key)
        }
        return trust!!
    }
}