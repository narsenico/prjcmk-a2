package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.ComicsWithReleases;
import it.amonshore.comikkua.data.Release;

/**
 * Helper per il binding delle view di {@link ComicsEditFragment}.
 */
class ComicsEditFragmentHelper {

    static ComicsEditFragmentHelper init(@NonNull LayoutInflater inflater, ViewGroup container) {
        final View view = inflater.inflate(R.layout.fragment_comics_edit, container, false);
        final ComicsEditFragmentHelper helper = new ComicsEditFragmentHelper();
        helper.numberFormat = NumberFormat.getNumberInstance(Locale.getDefault());
        helper.bind((view));
        return helper;
    }

    final class Preview {
        TextView initial, name, publisher, authors, notes,
                last, next, missing;
        ImageView comicsMenu;
    }

    final class Editor {
        TextInputLayout nameLayout;
        EditText name, publisher, series, authors, price,
                notes, periodicity;
    }

    View rootView;
    Preview preview;
    Editor editor;
    private ComicsWithReleases mComics;
    private NumberFormat numberFormat;

    private void bind(@NonNull View view) {
        rootView = view;

        preview = new Preview();
        preview.initial = view.findViewById(R.id.txt_comics_initial);
        preview.name = view.findViewById(R.id.txt_comics_name);
        preview.publisher = view.findViewById(R.id.txt_comics_publisher);
        preview.authors = view.findViewById(R.id.txt_comics_authors);
        preview.notes = view.findViewById(R.id.txt_comics_notes);
        preview.last = view.findViewById(R.id.txt_comics_release_last);
        preview.next = view.findViewById(R.id.txt_comics_release_next);
        preview.missing = view.findViewById(R.id.txt_comics_release_missing);
        preview.comicsMenu = view.findViewById(R.id.img_comics_menu);

        editor = new Editor();
        editor.nameLayout = view.findViewById(R.id.til_name);
        editor.name = editor.nameLayout.getEditText();
        editor.publisher = ((TextInputLayout)view.findViewById(R.id.til_publisher)).getEditText();
        editor.series = ((TextInputLayout)view.findViewById(R.id.til_series)).getEditText();
        editor.authors = ((TextInputLayout)view.findViewById(R.id.til_authors)).getEditText();
        editor.price = ((TextInputLayout)view.findViewById(R.id.til_price)).getEditText();

        // TODO: NON VA BENE! perché è possibile scrivere cose del tipo 10,50,12
        //  usare NumberFormat con locale USA e vaffaculo

        assert editor.price != null;
        editor.price.setKeyListener(DigitsKeyListener.getInstance("0123456789" + DecimalFormatSymbols.getInstance(Locale.getDefault()).getDecimalSeparator()));

        editor.notes = ((TextInputLayout)view.findViewById(R.id.til_notes)).getEditText();
        editor.periodicity = ((TextInputLayout)view.findViewById(R.id.til_periodicity)).getEditText();
    }

    void setComics(@NonNull Context context, @Nullable ComicsWithReleases comics) {
        if (comics == null) {
            mComics = ComicsWithReleases.createNew();
        } else {
            mComics = comics;
        }

        preview.initial.setText(mComics.comics.name.substring(0, 1));
        preview.name.setText(mComics.comics.name);
        preview.publisher.setText(mComics.comics.publisher);
        preview.authors.setText(mComics.comics.authors);
        preview.notes.setText(mComics.comics.notes);

        final Release lastRelease = mComics.getLastPurchasedRelease();
        preview.last.setText(lastRelease == null ? context.getString(R.string.release_last_none):
                context.getString(R.string.release_last, lastRelease.number));

        final Release nextRelease = mComics.getNextToPurchaseRelease();
        preview.next.setText(nextRelease == null ? context.getString(R.string.release_next_none) :
                context.getString(R.string.release_next, nextRelease.number));

        final int missingCount = mComics.getMissingReleaseCount();
        preview.missing.setText(context.getString(R.string.release_missing, missingCount));

        preview.comicsMenu.setVisibility(View.GONE);

        editor.name.setText(mComics.comics.name);
        editor.publisher.setText(mComics.comics.publisher);
        editor.series.setText(mComics.comics.series);
        editor.authors.setText(mComics.comics.authors);
        editor.price.setText(numberFormat.format(mComics.comics.price));
        editor.notes.setText(mComics.comics.notes);
        editor.periodicity.setText(mComics.comics.periodicity);
    }

    ComicsWithReleases writeComics() {
        mComics.comics.name = editor.name.getText().toString();
        mComics.comics.publisher = editor.publisher.getText().toString();
        mComics.comics.series = editor.series.getText().toString();
        mComics.comics.authors = editor.authors.getText().toString();
        mComics.comics.notes = editor.authors.getText().toString();

        if (editor.price.length() > 0) {
            try {
                mComics.comics.price = numberFormat.parse(editor.price.getText().toString()).doubleValue();
            } catch (ParseException e) {
                mComics.comics.price = 0;
                LogHelper.e("Error parsing comics price", e);
            }
        } else {
            mComics.comics.price = 0;
        }

        // TODO: periodicy

        return mComics;
    }

    boolean isValid() {
        boolean valid = true;
        if (editor.name.getText().toString().trim().length() == 0) {
            editor.nameLayout.setError(editor.nameLayout.getContext().getText(R.string.comics_name_empty_error));
            valid = false;
        } else {
            editor.nameLayout.setErrorEnabled(false);
        }
        return valid;
    }
}
