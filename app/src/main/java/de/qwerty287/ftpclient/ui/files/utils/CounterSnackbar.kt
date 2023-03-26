package de.qwerty287.ftpclient.ui.files.utils

import android.app.Activity
import android.view.View
import com.google.android.material.snackbar.Snackbar
import de.qwerty287.ftpclient.R

class CounterSnackbar(view: View, private val textTmpl: String, private val activity: Activity, show: Boolean = true) {
    private val snackbar = Snackbar.make(
        view,
        String.format(textTmpl, view.context.getString(R.string.not_started)),
        Snackbar.LENGTH_INDEFINITE
    )
    private val percentText = view.context.getString(R.string.percent)

    private var lastUpdate: Double = 0.0

    init {
        if (show) {
            snackbar.show()
        }
    }

    fun update(newProgress: Int, size: Int) {
        if (!snackbar.isShown) snackbar.show()
        if (newProgress.toDouble() / size > lastUpdate + 0.05) {
            activity.runOnUiThread {
                snackbar.setText(
                    String.format(
                        textTmpl,
                        String.format(percentText, (100 * newProgress.toDouble() / size).toInt())
                    )
                )
            }
            lastUpdate = (newProgress.toDouble() / size)
        }
    }

    fun dismiss() {
        snackbar.dismiss()
    }
}