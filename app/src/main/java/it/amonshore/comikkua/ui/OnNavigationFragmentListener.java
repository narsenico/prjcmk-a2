package it.amonshore.comikkua.ui;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.appcompat.view.ActionMode;

public interface OnNavigationFragmentListener {

    void onFragmentInteraction(Uri uri);

    void onFragmentRequestActionMode(@Nullable ActionMode.Callback callback, String name, CharSequence title);
}
