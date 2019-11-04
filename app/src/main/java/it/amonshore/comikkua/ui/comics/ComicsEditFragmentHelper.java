package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputLayout;
import com.tiper.MaterialSpinner;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Periodicity;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.ui.TextWatcherAdapter;

/**
 * Helper per il binding delle view di {@link ComicsEditFragment}.
 */
class ComicsEditFragmentHelper {

    interface ValidationCallback {
        void onValidation(boolean valid);
    }

    static ComicsEditFragmentHelper init(@NonNull LayoutInflater inflater, ViewGroup container,
                                         @NonNull ComicsViewModel viewModel,
                                         @NonNull LifecycleOwner lifecycleOwner) {
        final View view = inflater.inflate(R.layout.fragment_comics_edit, container, false);
        final ComicsEditFragmentHelper helper = new ComicsEditFragmentHelper();
        helper.numberFormat = NumberFormat.getNumberInstance(Locale.US);
        helper.bind(view, viewModel, lifecycleOwner);
        return helper;
    }

    final class Preview {
        TextView initial, name, publisher, authors, notes,
                last, next, missing;
        ImageView comicsMenu;
    }

    final class Editor {
        TextInputLayout nameLayout;
        EditText name, series, authors, price,
                notes;
        AutoCompleteTextView publisher;
        MaterialSpinner periodicity;
    }

    private View mRootView;
    private Preview preview;
    private Editor editor;
    @NonNull
    private ComicsWithReleases mComics;
    private NumberFormat numberFormat;
    private List<Periodicity> mPeriodicityList;
    private @NonNull ComicsViewModel mViewModel;
    private @NonNull LifecycleOwner mLifecycleOwner;

    private void bind(@NonNull View view, @NonNull ComicsViewModel viewModel, @NonNull LifecycleOwner lifecycleOwner) {
        mRootView = view;
        mViewModel = viewModel;
        mLifecycleOwner = lifecycleOwner;

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
        editor.publisher = (AutoCompleteTextView)((TextInputLayout)view.findViewById(R.id.til_publisher)).getEditText();
        editor.series = ((TextInputLayout)view.findViewById(R.id.til_series)).getEditText();
        editor.authors = ((TextInputLayout)view.findViewById(R.id.til_authors)).getEditText();
        editor.price = ((TextInputLayout)view.findViewById(R.id.til_price)).getEditText();
        editor.notes = ((TextInputLayout)view.findViewById(R.id.til_notes)).getEditText();
        editor.periodicity = view.findViewById(R.id.til_periodicity);

        mPeriodicityList = Periodicity.createList(mRootView.getContext());

        final ArrayAdapter<Periodicity> periodicityArrayAdapter =
                new ArrayAdapter<>(mRootView.getContext(), android.R.layout.simple_spinner_item, mPeriodicityList);
        periodicityArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        editor.periodicity.setAdapter(periodicityArrayAdapter);
        editor.periodicity.setSelection(0);

        // la modifica di una proprietà del comics si riflette immediatamente sulla preview
        editor.name.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                final String value = s.toString().trim();
                preview.name.setText(value);
                preview.initial.setText(value.length() > 0 ? value.substring(0, 1) : "");
            }
        });
        editor.publisher.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                preview.publisher.setText(s.toString().trim());
            }
        });
        editor.authors.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                preview.authors.setText(s.toString().trim());
            }
        });
        editor.notes.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                preview.notes.setText(s.toString().trim());
            }
        });

        // passo l'elenco dei publisher all'autocompletamento
        viewModel.getPublishers().observe(lifecycleOwner, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> strings) {
                editor.publisher.setAdapter(new ArrayAdapter<>(mRootView.getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        strings));
                // non mi serve più osservare l'elenco dei publisher
                viewModel.getPublishers().removeObserver(this);
            }
        });
    }

    @NonNull
    private String getSelectedPeriodicityKey() {
        final int pos = editor.periodicity.getSelection();
        if (pos >= 0) {
            return mPeriodicityList.get(pos).key;
        } else {
            return mPeriodicityList.get(0).key; // none/irregular
        }
    }

    @NonNull
    View getRootView() {
        return mRootView;
    }

    @NonNull
    ComicsWithReleases getComics() {
        return mComics;
    }

    boolean isNew() {
        return mComics.comics.id == Comics.NEW_COMICS_ID;
    }

    void setComics(@NonNull Context context, @Nullable ComicsWithReleases comics, Bundle savedInstanceState) {
        if (comics == null) {
            mComics = ComicsWithReleases.createNew();
        } else {
            mComics = comics;
        }

        if (savedInstanceState != null) {
            final String name = savedInstanceState.getString("name");
            preview.initial.setText(name == null || name.length() == 0 ? "" : name.substring(0, 1));
            preview.name.setText(savedInstanceState.getString("name"));
            preview.publisher.setText(savedInstanceState.getString("publisher"));
            preview.authors.setText(savedInstanceState.getString("authors"));
            preview.notes.setText(savedInstanceState.getString("notes"));
            editor.name.setText(savedInstanceState.getString("name"));
            editor.publisher.setText(savedInstanceState.getString("publisher"));
            editor.series.setText(savedInstanceState.getString("series"));
            editor.authors.setText(savedInstanceState.getString("authors"));
            editor.price.setText(savedInstanceState.getString("price"));
            editor.notes.setText(savedInstanceState.getString("notes"));
            editor.periodicity.setSelection(Periodicity.getIndexByKey(mPeriodicityList, savedInstanceState.getString("periodicity")));
        } else {
            preview.initial.setText(mComics.comics.getInitial());
            preview.name.setText(mComics.comics.name);
            preview.publisher.setText(mComics.comics.publisher);
            preview.authors.setText(mComics.comics.authors);
            preview.notes.setText(mComics.comics.notes);
            editor.name.setText(mComics.comics.name);
            editor.publisher.setText(mComics.comics.publisher);
            editor.series.setText(mComics.comics.series);
            editor.authors.setText(mComics.comics.authors);
            editor.price.setText(numberFormat.format(mComics.comics.price));
            editor.notes.setText(mComics.comics.notes);
            editor.periodicity.setSelection(Periodicity.getIndexByKey(mPeriodicityList, mComics.comics.periodicity));
        }

        // questi non cambiano mai quindi non ho bisogno di recuperarli anche da savedInstanceState
        final Release lastRelease = mComics.getLastPurchasedRelease();
        preview.last.setText(lastRelease == null ? context.getString(R.string.release_last_none):
                context.getString(R.string.release_last, lastRelease.number));

        final Release nextRelease = mComics.getNextToPurchaseRelease();
        preview.next.setText(nextRelease == null ? context.getString(R.string.release_next_none) :
                context.getString(R.string.release_next, nextRelease.number));

        final int missingCount = mComics.getNotPurchasedReleaseCount();
        preview.missing.setText(context.getString(R.string.release_missing, missingCount));

        preview.comicsMenu.setVisibility(View.GONE);
    }

    ComicsWithReleases writeComics() {
        mComics.comics.name = editor.name.getText().toString().trim();
        mComics.comics.publisher = editor.publisher.getText().toString().trim();
        mComics.comics.series = editor.series.getText().toString().trim();
        mComics.comics.authors = editor.authors.getText().toString().trim();
        mComics.comics.notes = editor.notes.getText().toString().trim();
        mComics.comics.periodicity = getSelectedPeriodicityKey();

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

        // aggiornando questo campo segnalo che i dati sono cambiati
        // (in questo modo il sistema di paging sa che qualcosa è cambiato)
        // non è il massimo perché effettivamente potrebbe non essere cambiato nulla, ma tant'è!
        mComics.comics.lastUpdate = System.currentTimeMillis();
        return mComics;
    }

    void saveInstanceState(@NonNull Bundle outState) {
        outState.putString("name", editor.name.getText().toString());
        outState.putString("publisher", editor.publisher.getText().toString());
        outState.putString("series", editor.series.getText().toString());
        outState.putString("authors", editor.authors.getText().toString());
        outState.putString("notes", editor.notes.getText().toString());
        outState.putString("price", editor.price.getText().toString());
        outState.putString("periodicity", getSelectedPeriodicityKey());
    }

    void isValid(@NonNull final ValidationCallback callback) {
        boolean valid = true;
        final String name = editor.name.getText().toString().trim();
        if (name.length() == 0) {
            editor.nameLayout.setError(editor.nameLayout.getContext().getText(R.string.comics_name_empty_error));
            callback.onValidation(false);
        } else {
            // potrei aggiungere la condizione
            // name.equals(mComics.comics.name)
            // ma non funzionerebbe nel malaugurato caso di chiamare writeComics prima di isValid
            final LiveData<Comics> ld = mViewModel.getComics(name);
            ld.observe(mLifecycleOwner, new Observer<Comics>() {
                @Override
                public void onChanged(Comics comics) {
                    ld.removeObserver(this);
                    // il nome è valido se non è usato da nessun comics
                    // oppure quello che usa è lo stesso comics che sto modificando
                    if (comics == null || comics.id == mComics.comics.id) {
                        editor.nameLayout.setErrorEnabled(false);
                        callback.onValidation(true);
                    } else {
                        editor.nameLayout.setError(editor.nameLayout.getContext().getText(R.string.comics_name_notunique_error));
                        callback.onValidation(false);
                    }
                }
            });
        }
    }
}
