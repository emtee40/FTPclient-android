package de.qwerty287.ftpclient.ui

import androidx.fragment.app.Fragment
import de.qwerty287.ftpclient.MainActivity

object FragmentUtils {
    val Fragment.store: MainActivity.StateStore get() = (requireActivity() as MainActivity).state
}