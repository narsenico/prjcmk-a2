package it.amonshore.comikkua.ui;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;
import it.amonshore.comikkua.ICallback;

public interface OnNavigationFragmentListener {

    void onFragmentInteraction(Uri uri);

    void onFragmentRequestActionMode(@Nullable ActionMode.Callback callback, String name, CharSequence title);

    /**
     * Richiede una snackbar con un pulsante "annulla".
     * Una eventuale snackbar già presesente al momento della richiesta viene dismessa.
     * <p>
     * La callback viene chiamata quando la snackbar viene dismessa:
     * - timeout => true
     * - richiesta altra snackbar => true
     * - annulla => false
     *
     * @param text     testo della snackbar
     * @param timeout  timeout scaduto il quale la snackbar viene dismessa
     * @param callback callback chiamato ogni quando la snackbar viene dismessa
     */
    void requestSnackbar(@NonNull String text, int timeout, @NonNull ICallback<Boolean> callback);

    /**
     * Dismette una eventuale snackbar presente.
     */
    void dismissSnackbar();
}
