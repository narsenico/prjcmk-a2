package it.amonshore.comikkua.ui.releases;

import android.view.MenuItem;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;

abstract class ReleaseViewHolderCallback {

    @MenuRes
    int mMenuRes;

    ReleaseViewHolderCallback(@MenuRes int menuRes) {
        mMenuRes = menuRes;
    }

    abstract void onReleaseClick(long comicsId, long id, int position);
    abstract void onReleaseMenuSelected(@NonNull MenuItem item, long comicsId, long id, int position);
}
