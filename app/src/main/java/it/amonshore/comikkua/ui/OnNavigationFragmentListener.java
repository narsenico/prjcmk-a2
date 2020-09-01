package it.amonshore.comikkua.ui;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.view.ActionMode;
import it.amonshore.comikkua.ICallback;

public interface OnNavigationFragmentListener {

    void onFragmentInteraction(Uri uri);

    void onFragmentRequestActionMode(@Nullable ActionMode.Callback callback, String name, CharSequence title);
}
