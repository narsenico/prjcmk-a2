package it.amonshore.comikkua.ui

import android.net.Uri
import androidx.appcompat.view.ActionMode

interface OnNavigationFragmentListener {
    fun onFragmentInteraction(uri: Uri?)

    fun onFragmentRequestActionMode(
        name: String,
        title: CharSequence? = null,
        callback: ActionMode.Callback? = null
    )

    /**
     * Richiede una snackbar con un pulsante "annulla".
     * Una eventuale snackbar già presesente al momento della richiesta viene dismessa.
     *
     *
     * La callback viene chiamata quando la snackbar viene dismessa:
     * - timeout => true
     * - richiesta altra snackbar => true
     * - annulla => false
     *
     * @param text     testo della snackbar
     * @param timeout  timeout scaduto il quale la snackbar viene dismessa
     * @param callback callback chiamato ogni quando la snackbar viene dismessa
     */
    fun requestSnackBar(text: String, timeout: Int, callback: (Boolean) -> Unit)

    /**
     * Dismette una eventuale snackbar presente.
     */
    fun dismissSnackBar()
}