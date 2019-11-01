package it.amonshore.comikkua.ui;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ArrayRes;
import com.google.android.material.textfield.TextInputLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ArrayAdapter;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;

public class TextInputAutoCompleteTextView extends AppCompatAutoCompleteTextView {

    private String[] mKeys;
    private String[] mEntries;

    public TextInputAutoCompleteTextView(Context context) {
        super(context);
    }

    public TextInputAutoCompleteTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TextInputAutoCompleteTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    protected void init(Context context, AttributeSet attrs) {
        final TypedArray typedArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.TextInputAutoCompleteTextView, 0, 0);
        try {
            int keys = typedArray.getResourceId(R.styleable.TextInputAutoCompleteTextView_keys, 0);
            if (keys > 0) {
                setKeys(keys);
            }
            int entries = typedArray.getResourceId(R.styleable.TextInputAutoCompleteTextView_entries, 0);
            if (entries > 0) {
                setEntries(entries);
            }
            if (typedArray.getBoolean(R.styleable.TextInputAutoCompleteTextView_isSpinner, false)) {
                spinnerMode();
            }
        } finally {
            typedArray.recycle();
        }
    }

    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        final InputConnection ic = super.onCreateInputConnection(outAttrs);
        if (ic != null && outAttrs.hintText == null) {
            // If we don't have a hint and our parent is a TextInputLayout, use it's hint for the
            // EditorInfo. This allows us to display a hint in 'extract mode'.
            final ViewParent parent = getParent();
            if (parent instanceof TextInputLayout) {
                outAttrs.hintText = ((TextInputLayout) parent).getHint();
            }
        }
        return ic;
    }

    public void setKeys(@ArrayRes int resId) {
        final String[] array = getResources().getStringArray(resId);
        setKeys(array);
    }

    public void setKeys(String[] keys) {
        mKeys = keys;
    }

    public void setEntries(@ArrayRes int resId) {
        final String[] array = getResources().getStringArray(resId);
        setEntries(array);
    }

    public void setEntries(String[] entries) {
        mEntries = entries;

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, entries);
        setAdapter(adapter);
    }

    public int getSelectedIndex() {
        if (mEntries != null && mKeys != null) {
            final int pos = Utility.indexOf(mEntries, getText().toString());
            if (pos >= 0 && pos < mKeys.length) {
                return pos;
            } else {
                return -1;
            }
        }
        return -1;
    }

    @Nullable
    public String getSelectedKey() {
        final int index = getSelectedIndex();
        return index < 0 ? null : mKeys[index];
    }

    public void setSelectedKey(String key) {
        if (mEntries != null && mKeys != null) {
            final int pos = Utility.indexOf(mKeys, key);
            if (pos >= 0 && pos < mEntries.length) {
                setText(mEntries[pos]);
            } else {
                setText("");
            }
        } else {
            setText("");
        }
    }

    public void spinnerMode() {
        setKeyListener(null);
        setOnFocusChangeListener((v, hasFocus) -> {
           if (hasFocus) {
               showDropDown();
           } else {
               dismissDropDown();
           }
        });
        setOnTouchListener((v, e) -> {
            performClick();
            showDropDown();
            return false;
        });
    }
}
