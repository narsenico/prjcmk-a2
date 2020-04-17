package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.google.android.material.textfield.TextInputLayout;
import com.tiper.MaterialSpinner;

import java.io.File;
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
import it.amonshore.comikkua.ICallback;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsViewModel;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Periodicity;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.ui.DrawableTextViewTarget;
import it.amonshore.comikkua.ui.ImageHelper;
import it.amonshore.comikkua.ui.TextWatcherAdapter;

/**
 * Helper per il binding delle view di {@link ComicsEditFragment}.
 */
class ComicsEditFragmentHelper {

    static ComicsEditFragmentHelper init(@NonNull LayoutInflater inflater, ViewGroup container,
                                         @NonNull ComicsViewModel viewModel,
                                         @NonNull LifecycleOwner lifecycleOwner,
                                         @NonNull RequestManager glideRequestManager) {
        final View view = inflater.inflate(R.layout.fragment_comics_edit, container, false);
        final ComicsEditFragmentHelper helper = new ComicsEditFragmentHelper();
        helper.mNumberFormat = NumberFormat.getNumberInstance(Locale.US);
        helper.mGlideRequestManager = glideRequestManager;
        helper.bind(view, viewModel, lifecycleOwner);
        return helper;
    }

    final class Preview {
        TextView initial, name, publisher, authors, notes,
                last, next, missing;
        Uri comicsImageUri;
    }

    final class Editor {
        TextInputLayout nameLayout;
        EditText name, series, price,
                notes;
        AutoCompleteTextView publisher, authors;
        MaterialSpinner periodicity;
    }

    private View mRootView;
    private Preview preview;
    private Editor editor;
    @NonNull
    private ComicsWithReleases mComics;
    private NumberFormat mNumberFormat;
    private List<Periodicity> mPeriodicityList;
    @NonNull
    private ComicsViewModel mViewModel;
    @NonNull
    private LifecycleOwner mLifecycleOwner;
    @NonNull
    private RequestManager mGlideRequestManager;
    private CustomViewTarget<TextView, Drawable> mComicsImageViewTarget;
    boolean mMoveImageFromCache;

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

        editor = new Editor();
        editor.nameLayout = view.findViewById(R.id.til_name);
        editor.name = editor.nameLayout.getEditText();
        editor.publisher = (AutoCompleteTextView) ((TextInputLayout) view.findViewById(R.id.til_publisher)).getEditText();
        editor.series = ((TextInputLayout) view.findViewById(R.id.til_series)).getEditText();
        editor.authors = (AutoCompleteTextView) ((TextInputLayout) view.findViewById(R.id.til_authors)).getEditText();
        editor.price = ((TextInputLayout) view.findViewById(R.id.til_price)).getEditText();
        editor.notes = ((TextInputLayout) view.findViewById(R.id.til_notes)).getEditText();
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
                // l'iniziale la mostra solo se non c'è l'immagine
                if (preview.comicsImageUri == null) {
                    final String value = s.toString().trim();
                    preview.name.setText(value);
                    preview.initial.setText(value.length() > 0 ? value.substring(0, 1) : "");
                }
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

        // passo l'elenco degli autori all'autocompletamento
        viewModel.getAuthors().observe(lifecycleOwner, new Observer<List<String>>() {
            @Override
            public void onChanged(List<String> strings) {
                editor.authors.setAdapter(new ArrayAdapter<>(mRootView.getContext(),
                        android.R.layout.simple_dropdown_item_1line,
                        strings));
                // non mi serve più osservare l'elenco degli autori
                viewModel.getAuthors().removeObserver(this);
            }
        });

        mComicsImageViewTarget = new DrawableTextViewTarget(preview.initial);
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
//            final String name = savedInstanceState.getString("name");
//            preview.initial.setText(name == null || name.length() == 0 ? "" : name.substring(0, 1));
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
            updateComicsImageAndInitial(savedInstanceState.getString("comics_image"), savedInstanceState.getString("name"));
        } else {
//            preview.initial.setText(mComics.comics.getInitial());
            preview.name.setText(mComics.comics.name);
            preview.publisher.setText(mComics.comics.publisher);
            preview.authors.setText(mComics.comics.authors);
            preview.notes.setText(mComics.comics.notes);
            editor.name.setText(mComics.comics.name);
            editor.publisher.setText(mComics.comics.publisher);
            editor.series.setText(mComics.comics.series);
            editor.authors.setText(mComics.comics.authors);
            editor.price.setText(mNumberFormat.format(mComics.comics.price));
            editor.notes.setText(mComics.comics.notes);
            editor.periodicity.setSelection(Periodicity.getIndexByKey(mPeriodicityList, mComics.comics.periodicity));
            updateComicsImageAndInitial(mComics.comics.image, mComics.comics.name);
        }

        // questi non cambiano mai quindi non ho bisogno di recuperarli anche da savedInstanceState
        final Release lastRelease = mComics.getLastPurchasedRelease();
        preview.last.setText(lastRelease == null ? context.getString(R.string.release_last_none) :
                context.getString(R.string.release_last, lastRelease.number));

        final Release nextRelease = mComics.getNextToPurchaseRelease();
        preview.next.setText(nextRelease == null ? context.getString(R.string.release_next_none) :
                context.getString(R.string.release_next, nextRelease.number));

        final int missingCount = mComics.getNotPurchasedReleaseCount();
        preview.missing.setText(context.getString(R.string.release_missing, missingCount));
    }

    private void updateComicsImageAndInitial(String uriString, String name) {
        if (TextUtils.isEmpty(uriString)) {
            updateComicsImageAndInitial((Uri) null, name);
        } else {
            updateComicsImageAndInitial(Uri.parse(uriString), name);
        }
    }

    private void updateComicsImageAndInitial(Uri uri, String name) {
        // mostro l'iniziale solo se non c'è l'immagine
        preview.comicsImageUri = uri;
        if (preview.comicsImageUri == null) {
            preview.initial.setText(name == null || name.length() == 0 ? "" : name.substring(0, 1));
            preview.initial.setBackgroundResource(R.drawable.background_comics_initial_noborder);
        } else {
            preview.initial.setText("");
            mGlideRequestManager.load(preview.comicsImageUri)
                    .apply(ImageHelper.getGlideCircleOptions())
                    .into(mComicsImageViewTarget);
        }
    }

    /**
     * Imposta l'immagine del comics.
     *
     * @param uri l'uri dell'immagine, può essere null
     */
    void setComicsImage(@Nullable Uri uri) {
        mMoveImageFromCache = true;
        updateComicsImageAndInitial(uri, editor.name.getText().toString());
    }

    boolean hasComicsImage() {
        return preview.comicsImageUri != null;
    }

    /**
     * Aggiorna il comics con gli input dell'utente.
     *
     * @return il comics aggiornato
     */
    ComicsWithReleases writeComics() {
        mComics.comics.name = editor.name.getText().toString().trim();
        mComics.comics.publisher = editor.publisher.getText().toString().trim();
        mComics.comics.series = editor.series.getText().toString().trim();
        mComics.comics.authors = editor.authors.getText().toString().trim();
        mComics.comics.notes = editor.notes.getText().toString().trim();
        mComics.comics.periodicity = getSelectedPeriodicityKey();

        // mantengo il vecchio uri dell'immagine, verrà sostituito solo in un secomdo momento
        //  quando anche la nuova immagine verrà spostata dalla cache a files/

        if (editor.price.length() > 0) {
            try {
                mComics.comics.price = mNumberFormat.parse(editor.price.getText().toString()).doubleValue();
            } catch (ParseException e) {
                mComics.comics.price = 0;
                LogHelper.e("Error parsing comics price", e);
            }
        } else {
            mComics.comics.price = 0;
        }

        return mComics;
    }

    void complete(boolean result) {
        if (result) {
            // elimino l'eventuale vecchia immagine
            if (mComics.comics.hasImage()) {
                for (File file : mRootView.getContext().getFilesDir().listFiles((dir, name) -> ImageHelper.isValidImageFileName(name, mComics.comics.id))) {
                    if (!file.delete()) {
                        LogHelper.e("Error removing image '%s'", file);
                    }
                }
            }
            // sposto la nuova immagine dalla cache a files/ e la rinomino utilizzando come suffisso l'id del comics
            if (preview.comicsImageUri != null) {
                // nuovo uri dell'immagine
                mComics.comics.image = Uri.fromFile(new File(mRootView.getContext().getFilesDir(),
                        ImageHelper.newImageFileName(mComics.comics.id))).toString();
                LogHelper.d("======> image uri '%s'", mComics.comics.image);
                // sposto l'immagine nella nuova destinazione
                final File srcFile = new File(preview.comicsImageUri.getPath());
                final File dstFile = new File(Uri.parse(mComics.comics.image).getPath());
                LogHelper.d("======> move file from '%s' to '%s'", srcFile, dstFile);
                if (!srcFile.renameTo(dstFile)) {
                    LogHelper.e("Error moving image temp file '%s' to files/", srcFile);
                }
            } else {
                mComics.comics.image = null;
            }
        } else {
            // TODO: cosa fare? nulla, non posso eliminare il file dalla cache perché può ancora servire nel fragment
        }
    }

    void saveInstanceState(@NonNull Bundle outState) {
        outState.putString("name", editor.name.getText().toString());
        outState.putString("publisher", editor.publisher.getText().toString());
        outState.putString("series", editor.series.getText().toString());
        outState.putString("authors", editor.authors.getText().toString());
        outState.putString("notes", editor.notes.getText().toString());
        outState.putString("price", editor.price.getText().toString());
        outState.putString("periodicity", getSelectedPeriodicityKey());
        outState.putString("comics_image", preview.comicsImageUri == null ? null : preview.comicsImageUri.getPath());
    }

    void isValid(@NonNull final ICallback<Boolean> callback) {
        boolean valid = true;
        final String name = editor.name.getText().toString().trim();
        if (name.length() == 0) {
            editor.nameLayout.setError(editor.nameLayout.getContext().getText(R.string.comics_name_empty_error));
            callback.onCallback(false);
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
                        callback.onCallback(true);
                    } else {
                        editor.nameLayout.setError(editor.nameLayout.getContext().getText(R.string.comics_name_notunique_error));
                        callback.onCallback(false);
                    }
                }
            });
        }
    }
}
