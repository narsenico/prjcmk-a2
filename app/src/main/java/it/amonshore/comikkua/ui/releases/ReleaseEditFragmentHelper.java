package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.method.DigitsKeyListener;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.request.target.CustomViewTarget;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseViewModel;
import it.amonshore.comikkua.ui.DrawableTextViewTarget;
import it.amonshore.comikkua.ui.GlideHelper;
import it.amonshore.comikkua.ui.TextWatcherAdapter;

public class ReleaseEditFragmentHelper {

    interface ValidationCallback {
        void onValidation(boolean valid);
    }

    static ReleaseEditFragmentHelper init(@NonNull LayoutInflater inflater, ViewGroup container,
                                          @NonNull ReleaseViewModel viewModel,
                                          @NonNull LifecycleOwner lifecycleOwner,
                                          @NonNull FragmentManager fragmentManager,
                                          @NonNull RequestManager glideRequestManager) {
        final View view = inflater.inflate(R.layout.fragment_release_edit, container, false);
        final ReleaseEditFragmentHelper helper = new ReleaseEditFragmentHelper();
        helper.numberFormat = NumberFormat.getNumberInstance(Locale.US);
        helper.mGlideRequestManager = glideRequestManager;
        helper.bind(view, viewModel, lifecycleOwner, fragmentManager);
        return helper;
    }

    final class Preview {
        TextView numbers, date, title, info, notes;
        ImageView purchased, ordered, menu;
        View mainCard, background;
    }

    final class Editor {
        TextInputLayout numbersLayout, dateLayout;
        EditText numbers, date, price, notes;
        SwitchMaterial purchased, ordered;
        @Nullable
        MaterialDatePicker<Long> datePicker;
        long selectedDateInUtc;
    }

    private View mRootView;
    private Preview preview;
    private Editor editor;
    @NonNull
    private ComicsWithReleases mComics;
    @NonNull
    private Release mRelease;
    private NumberFormat numberFormat;
    @NonNull
    private ReleaseViewModel mViewModel;
    @NonNull
    private LifecycleOwner mLifecycleOwner;
    private RequestManager mGlideRequestManager;

    private void bind(@NonNull View view, @NonNull ReleaseViewModel viewModel,
                      @NonNull LifecycleOwner lifecycleOwner,
                      @NonNull FragmentManager fragmentManager) {
        mRootView = view;
        mViewModel = viewModel;
        mLifecycleOwner = lifecycleOwner;

        preview = new Preview();
        preview.numbers = view.findViewById(R.id.txt_release_numbers);
        preview.date = view.findViewById(R.id.txt_release_date);
        preview.title = view.findViewById(R.id.txt_release_title);
        preview.info = view.findViewById(R.id.txt_release_info);
        preview.notes = view.findViewById(R.id.txt_release_notes);
        preview.purchased = view.findViewById(R.id.img_release_purchased);
        preview.ordered = view.findViewById(R.id.img_release_ordered);
        preview.menu = view.findViewById(R.id.img_release_menu);
        preview.mainCard = view.findViewById(R.id.release_main_card);
        preview.background = view.findViewById(R.id.release_background);

        editor = new Editor();
        editor.numbersLayout = view.findViewById(R.id.til_numbers);
        editor.numbers = editor.numbersLayout.getEditText();
        editor.dateLayout = view.findViewById(R.id.til_date);
        editor.date = editor.dateLayout.getEditText();
        editor.price = ((TextInputLayout) view.findViewById(R.id.til_price)).getEditText();
        editor.notes = ((TextInputLayout) view.findViewById(R.id.til_notes)).getEditText();
        editor.purchased = view.findViewById(R.id.chk_purchased);
        editor.ordered = view.findViewById(R.id.chk_ordered);

        // da xml non riesco a impostarli
        editor.numbers.setKeyListener(DigitsKeyListener.getInstance("0123456789,-"));

        // la modifica di una proprietà della release si riflette immediatamente sulla preview
        editor.numbers.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                preview.numbers.setText(s.toString().trim());
            }
        });

        editor.notes.addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                preview.notes.setText(s.toString().trim());
            }
        });

        final float mainCardElevationPx = preview.mainCard.getElevation(); /*TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2f,
                preview.mainCard.getResources().getDisplayMetrics());*/
        editor.purchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                preview.purchased.setVisibility(View.VISIBLE);
                preview.mainCard.setElevation(0);
                preview.background.setBackgroundColor(ContextCompat.getColor(mRootView.getContext(), R.color.colorItemPurchased));
            } else {
                preview.purchased.setVisibility(View.INVISIBLE);
                preview.mainCard.setElevation(mainCardElevationPx);
                preview.background.setBackgroundColor(ContextCompat.getColor(mRootView.getContext(), R.color.colorItemNotPurchased));
            }
        });

        editor.ordered.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preview.ordered.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        });

        // il campo Date non è editabile
        // quando acquisisce il focus apro il picker
        // quando verrà chiuso il focus rimarrà al campo date
        // quindi al click del campo Date, se ha già il focus riapro il picker
        editor.date.setInputType(EditorInfo.TYPE_NULL);
        editor.date.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && editor.datePicker != null) {
                editor.datePicker.show(fragmentManager, "release_date_picker");
            }
        });
        editor.date.setOnClickListener(v -> {
            if (editor.datePicker != null && v.hasFocus()) {
                editor.datePicker.show(fragmentManager, "release_date_picker");
            }
        });

        // icona alla destra del campo Date, elimina la data di rilascio
        editor.dateLayout.setEndIconOnClickListener(v -> {
            editor.selectedDateInUtc = 0;
            editor.date.setText("");
            preview.date.setText("");
        });
    }

    @NonNull
    View getRootView() {
        return mRootView;
    }

    @NonNull
    ComicsWithReleases getComics() {
        return mComics;
    }

    @NonNull
    Release getRelease() {
        return mRelease;
    }

    boolean isNew() {
        return mRelease.id == Release.NEW_RELEASE_ID;
    }

    void setRelease(@NonNull Context context, @NotNull ComicsWithReleases comics, @Nullable Release release, Bundle savedInstanceState) {
        mComics = comics;
        if (release == null) {
            // creo una release nuova impostando in automatico numero e data uscita (in base alla periodicità)
            mRelease = mComics.createNextRelease();
        } else {
            mRelease = release;
        }

        if (savedInstanceState != null) {
            preview.numbers.setText(savedInstanceState.getString("numbers"));
            preview.notes.setText(savedInstanceState.getString("notes", mComics.comics.notes));
            preview.purchased.setVisibility(savedInstanceState.getBoolean("purchased") ? View.VISIBLE : View.INVISIBLE);
            preview.ordered.setVisibility(savedInstanceState.getBoolean("ordered") ? View.VISIBLE : View.INVISIBLE);
            editor.numbers.setText(savedInstanceState.getString("numbers"));
            editor.price.setText(savedInstanceState.getString("price"));
            editor.purchased.setChecked(savedInstanceState.getBoolean("purchased"));
            editor.ordered.setChecked(savedInstanceState.getBoolean("ordered"));
            editor.notes.setText(savedInstanceState.getString("notes"));
            prepareDatePicker(savedInstanceState.getLong("dateUtc"));
        } else {
            preview.numbers.setText(String.format(Locale.getDefault(), "%d", mRelease.number));
            preview.notes.setText(Utility.nvl(mRelease.notes, mComics.comics.notes));
            preview.purchased.setVisibility(mRelease.purchased ? View.VISIBLE : View.INVISIBLE);
            preview.ordered.setVisibility(mRelease.ordered ? View.VISIBLE : View.INVISIBLE);
            editor.numbers.setText(String.format(Locale.getDefault(), "%d", mRelease.number));
            editor.price.setText(numberFormat.format(mRelease.price));
            editor.purchased.setChecked(mRelease.purchased);
            editor.ordered.setChecked(mRelease.ordered);
            editor.notes.setText(mRelease.notes);
            prepareDatePicker(mRelease.date);
        }

        // questi non cambiano mai quindi non ho bisogno di recuperarli anche da savedInstanceState
        preview.title.setText(mComics.comics.name);
        preview.info.setText(Utility.join(", ", true, mComics.comics.publisher, mComics.comics.authors));

        if (comics.comics.hasImage()) {
            mGlideRequestManager
                    .load(Uri.parse(comics.comics.image))
                    .apply(GlideHelper.getSquareOptions())
                    .into(new DrawableTextViewTarget(preview.numbers));
        }

        preview.menu.setVisibility(View.INVISIBLE);
    }

    private void prepareDatePicker(String date) {
        // MaterialDatePicker accetta date in UTC

        if (Utility.isNullOrEmpty(date)) {
            prepareDatePicker(0);
        } else {
            prepareDatePicker(DateFormatterHelper.toUTCCalendar(date).getTimeInMillis());
        }
    }

    private void prepareDatePicker(long dateUtc) {
        // MaterialDatePicker accetta date in UTC

        editor.selectedDateInUtc = dateUtc;

        final long startSelection;
        if (editor.selectedDateInUtc == 0) {
            preview.date.setText("");
            editor.date.setText("");
            // oggi
            startSelection = DateFormatterHelper.toUTCCalendar(System.currentTimeMillis()).getTimeInMillis();
        } else {
            final String humanized = DateFormatterHelper.toHumanReadable(editor.date.getContext(),
                    DateFormatterHelper.timeToString8(DateFormatterHelper.fromUTCCalendar(editor.selectedDateInUtc).getTimeInMillis()),
                    DateFormatterHelper.STYLE_FULL);
            preview.date.setText(humanized);
            editor.date.setText(humanized);
            startSelection = editor.selectedDateInUtc;
        }

        editor.datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(startSelection)
                .setTitleText("Release date")
                .setCalendarConstraints(new CalendarConstraints.Builder().setOpenAt(startSelection).build())
                .build();

        editor.datePicker.addOnPositiveButtonClickListener(selection -> {
            editor.selectedDateInUtc = selection;

            final String humanized = DateFormatterHelper.toHumanReadable(editor.date.getContext(),
                    DateFormatterHelper.timeToString8(DateFormatterHelper.fromUTCCalendar(selection).getTimeInMillis()),
                    DateFormatterHelper.STYLE_FULL);
            preview.date.setText(humanized);
            editor.date.setText(humanized);
        });
    }

    Release[] createReleases() {
        mRelease.date = editor.selectedDateInUtc == 0 ? null :
                DateFormatterHelper.timeToString8(DateFormatterHelper.fromUTCCalendar(editor.selectedDateInUtc).getTimeInMillis());
        mRelease.notes = editor.notes.getText().toString().trim();
        mRelease.purchased = editor.purchased.isChecked();
        mRelease.ordered = editor.ordered.isChecked();

        if (editor.price.length() > 0) {
            try {
                mRelease.price = numberFormat.parse(editor.price.getText().toString()).doubleValue();
            } catch (ParseException e) {
                mRelease.price = 0;
                LogHelper.e("Error parsing release price", e);
            }
        } else {
            mRelease.price = 0;
        }

        final int[] numbers = Utility.parseInterval(editor.numbers.getText().toString().trim(), ",", "-");
        final Release[] releases = new Release[numbers.length];

        for (int ii=0; ii<numbers.length; ii++) {
            // in questo caso imposto subito lastUpdate perché queste release potrebbero sovrascrivere quelle già esistenti
            // così facendo risultano ancora visibili nell'elenco release anche se già acquistate per tutto il periodo di retain (vedi ReleaeDao.getAllReleases)
            releases[ii] = Release.create(mRelease, System.currentTimeMillis());
            releases[ii].number = numbers[ii];
        }

        return releases;
    }

    void saveInstanceState(@NonNull Bundle outState) {
        outState.putString("numbers", editor.numbers.getText().toString());
        outState.putLong("dateUtc", editor.selectedDateInUtc);
        outState.putString("price", editor.price.getText().toString());
        outState.putString("notes", editor.notes.getText().toString());
        outState.putBoolean("purchased", editor.purchased.isChecked());
        outState.putBoolean("ordered", editor.ordered.isChecked());
    }

    void isValid(@NonNull final ValidationCallback callback) {
        boolean valid = true;
        // TODO: verificare che numbers contenga una sequenza valida
        //  es: 4-3 non deve essere valido (modificare comportamento di Utility.parseInterval)
        //  es: 4-5-6 NO
        //  es: 4- NO etc.
        // TODO: controllo se esistono già certe uscite? nel vecchio non lo facevo
        callback.onValidation(valid);
    }
}
