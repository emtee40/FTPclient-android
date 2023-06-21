package de.qwerty287.ftpclient.ui.files.utils.error

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import androidx.navigation.NavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.qwerty287.ftpclient.R

class ErrorDialog(context: Context, e: Exception, supportsRetry: Boolean, private val navController: NavController, private val actions: ErrorDialogActions) {
    init {
        val dialog = MaterialAlertDialogBuilder(context) // show error dialog
            .setTitle(R.string.error_occurred)
            .setMessage(R.string.error_descriptions)
            .setNegativeButton(R.string.ok) { d: DialogInterface, _: Int ->
                cancel(d)
            }
            .apply {
                if (supportsRetry) {
                    setPositiveButton(R.string.retry) { d: DialogInterface, _: Int ->
                        actions.retry()
                        d.dismiss()
                    }
                }
            }
            .setNeutralButton(R.string.copy) { d: DialogInterface, _: Int ->
                val clipboardManager =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText(
                        context.getString(R.string.app_name),
                        e.stackTraceToString()
                    )
                )
                cancel(d)
            }
            .setOnCancelListener {
                cancel(it)
            }
            .create()
        dialog.setCanceledOnTouchOutside(false)
        dialog.show()
    }

    private fun cancel(d: DialogInterface) {
        d.dismiss()
        actions.close()
        navController.navigateUp()
    }
}