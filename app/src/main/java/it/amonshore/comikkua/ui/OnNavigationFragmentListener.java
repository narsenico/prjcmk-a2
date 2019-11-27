package it.amonshore.comikkua.ui;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.view.ActionMode;

public interface OnNavigationFragmentListener {

    void onFragmentInteraction(Uri uri);

    void onFragmentRequestActionMode(@Nullable ActionMode.Callback callback, String name, CharSequence title);

    void onSubtitleChanged(@StringRes int resId);
}
