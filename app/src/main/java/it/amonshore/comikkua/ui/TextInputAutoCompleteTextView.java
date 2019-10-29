package it.amonshore.comikkua.ui;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ArrayRes;
import com.google.android.material.textfield.TextInputLayout;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import android.util.AttributeSet;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.ArrayAdapter;

import it.amonshore.comikkua.R;

public class TextInputAutoCompleteTextView extends AppCompatAutoCompleteTextView {

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
            int entries = typedArray.getResourceId(R.styleable.TextInputAutoCompleteTextView_entries, 0);
            if (entries > 0) {
                setEntries(entries);
            }
            if (typedArray.getBoolean(R.styleable.TextInputAutoCompleteTextView_spinnerMode, false)) {
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

    public void setEntries(@ArrayRes int resId) {
        final String[] array = getResources().getStringArray(resId);
        setEntries(array);
    }

    public void setEntries(String[] entries) {
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_dropdown_item_1line, entries);
        setAdapter(adapter);
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
