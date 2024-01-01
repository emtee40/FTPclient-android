package de.qwerty287.ftpclient.ui.files.utils.error

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.qwerty287.ftpclient.R
import de.qwerty287.ftpclient.ui.FragmentUtils.store

class ErrorDialog(
    private val fragment: Fragment,
    e: Exception,
    retry: (() -> Unit)? = null,
) {
    init {
        e.printStackTrace()
        val dialog = MaterialAlertDialogBuilder(fragment.requireContext()) // show error dialog
            .setTitle(R.string.error_occurred)
            .setMessage(R.string.error_descriptions)
            .setNegativeButton(R.string.ok) { d: DialogInterface, _: Int ->
                cancel(d)
            }
            .apply {
                if (retry != null) {
                    setPositiveButton(R.string.retry) { d: DialogInterface, _: Int ->
                        retry()
                        d.dismiss()
                    }
                }
            }
            .setNeutralButton(R.string.copy) { d: DialogInterface, _: Int ->
                val clipboardManager =
                    fragment.requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        fragment.getString(R.string.app_name),
                        e.stackTraceToString()
                    )
                )
                cancel(d)
            }
            .setOnCancelListener(this::cancel)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun cancel(d: DialogInterface) {
        d.dismiss()
        fragment.store.exitClient()
        fragment.findNavController().navigateUp()
    }
}