package it.amonshore.comikkua.ui;

import android.view.Menu;

import androidx.annotation.MenuRes;
import androidx.appcompat.view.ActionMode;

public abstract class ActionModeController implements ActionMode.Callback {

    private int mMenuRes;

    public ActionModeController(@MenuRes int menuRes) {
        mMenuRes = menuRes;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        if (mMenuRes != 0) {
            mode.getMenuInflater().inflate(mMenuRes, menu);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }
}
