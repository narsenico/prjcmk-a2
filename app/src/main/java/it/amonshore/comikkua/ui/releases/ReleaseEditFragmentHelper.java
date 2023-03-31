package it.amonshore.comikkua.ui.releases;

import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import com.bumptech.glide.RequestManager;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LifecycleOwner;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.ICallback;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.databinding.FragmentReleaseEditBinding;
import it.amonshore.comikkua.ui.DrawableTextViewTarget;
import it.amonshore.comikkua.ui.ImageHelper;
import it.amonshore.comikkua.ui.TextWatcherAdapter;

public class ReleaseEditFragmentHelper {

    @NonNull
    static ReleaseEditFragmentHelper init(@NonNull FragmentReleaseEditBinding binding,
                                          @NonNull LifecycleOwner lifecycleOwner,
                                          @NonNull FragmentManager fragmentManager,
                                          @NonNull RequestManager glideRequestManager) {
        final ReleaseEditFragmentHelper helper = new ReleaseEditFragmentHelper();
        helper._binding = binding;
        helper._numberFormat = NumberFormat.getNumberInstance(Locale.US);
        helper._glideRequestManager = glideRequestManager;
        helper.bind(lifecycleOwner, fragmentManager);
        return helper;
    }

    private Release _release;
    private NumberFormat _numberFormat;
    private RequestManager _glideRequestManager;
    private FragmentReleaseEditBinding _binding;
    private MaterialDatePicker<Long> _datePicker;
    private long _selectedDateInUtc;
    private float _mainCardElevationPx;

    private void bind(@NonNull LifecycleOwner lifecycleOwner,
                      @NonNull FragmentManager fragmentManager) {
        // da xml non riesco a impostarli
        _binding.tilNumbers.getEditText().setKeyListener(DigitsKeyListener.getInstance("0123456789,-"));

        // la modifica di una proprietà della release si riflette immediatamente sulla preview
        _binding.tilNumbers.getEditText().addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                _binding.release.txtReleaseNumbers.setText(s.toString().trim());
            }
        });

        _binding.tilNotes.getEditText().addTextChangedListener(new TextWatcherAdapter() {
            @Override
            public void afterTextChanged(Editable s) {
                _binding.release.txtReleaseNotes.setText(s.toString().trim());
            }
        });

        _mainCardElevationPx = _binding.release.releaseMainCard.getElevation();
        _binding.chkPurchased.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updatePurchased(isChecked);
        });

        _binding.chkOrdered.setOnCheckedChangeListener((buttonView, isChecked) -> {
            _binding.release.imgReleaseOrdered.setVisibility(isChecked ? View.VISIBLE : View.INVISIBLE);
        });

        // il campo Date non è editabile
        // quando acquisisce il focus apro il picker
        // quando verrà chiuso il focus rimarrà al campo date
        // quindi al click del campo Date, se ha già il focus riapro il picker
        _binding.tilDate.getEditText().setInputType(EditorInfo.TYPE_NULL);
        _binding.tilDate.getEditText().setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && _datePicker != null) {
                _datePicker.show(fragmentManager, "release_date_picker");
            }
        });
        _binding.tilDate.getEditText().setOnClickListener(v -> {
            if (_datePicker != null && v.hasFocus()) {
                _datePicker.show(fragmentManager, "release_date_picker");
            }
        });

        // icona alla destra del campo Date, elimina la data di rilascio
        _binding.tilDate.setEndIconOnClickListener(v -> {
            _selectedDateInUtc = 0;
            _binding.tilDate.getEditText().setText("");
            _binding.release.txtReleaseDate.setText("");
        });
    }

    @NonNull
    View getRootView() {
        return _binding.getRoot();
    }

    void setComics(@NonNull ComicsWithReleases comics) {
        _binding.release.txtReleaseTitle.setText(comics.comics.name);
        _binding.release.txtReleaseNotes.setText(comics.comics.notes);
        _binding.release.txtReleaseInfo.setText(Utility.join(", ", true, comics.comics.publisher, comics.comics.authors));
        _binding.release.imgReleasePurchased.setVisibility(View.INVISIBLE);
        _binding.release.imgReleaseOrdered.setVisibility(View.INVISIBLE);
        _binding.release.imgReleaseMenu.setVisibility(View.INVISIBLE);
        updatePurchased(false);

        if (comics.comics.hasImage()) {
            _glideRequestManager
                    .load(Uri.parse(comics.comics.image))
                    .apply(ImageHelper.getGlideSquareOptions())
                    .into(new DrawableTextViewTarget(_binding.release.txtReleaseNumbers));
        }
    }

    void setRelease(@NonNull Release release, Bundle savedInstanceState) {
        _release = release;

        if (savedInstanceState != null) {
            _binding.release.txtReleaseNumbers.setText(savedInstanceState.getString("numbers"));
            _binding.release.imgReleasePurchased.setVisibility(savedInstanceState.getBoolean("purchased") ? View.VISIBLE : View.INVISIBLE);
            _binding.release.imgReleaseOrdered.setVisibility(savedInstanceState.getBoolean("ordered") ? View.VISIBLE : View.INVISIBLE);
            _binding.tilNumbers.getEditText().setText(savedInstanceState.getString("numbers"));
            _binding.tilPrice.getEditText().setText(savedInstanceState.getString("price"));
            _binding.chkPurchased.setChecked(savedInstanceState.getBoolean("purchased"));
            _binding.chkOrdered.setChecked(savedInstanceState.getBoolean("ordered"));
            _binding.tilNotes.getEditText().setText(savedInstanceState.getString("notes"));
            prepareDatePicker(savedInstanceState.getLong("dateUtc"));
        } else {
            _binding.release.txtReleaseNumbers.setText(String.format(Locale.getDefault(), "%d", _release.number));
            _binding.release.imgReleasePurchased.setVisibility(_release.purchased ? View.VISIBLE : View.INVISIBLE);
            _binding.release.imgReleaseOrdered.setVisibility(_release.ordered ? View.VISIBLE : View.INVISIBLE);
            _binding.tilNumbers.getEditText().setText(String.format(Locale.getDefault(), "%d", _release.number));
            _binding.tilPrice.getEditText().setText(_numberFormat.format(_release.price));
            _binding.chkPurchased.setChecked(_release.purchased);
            _binding.chkOrdered.setChecked(_release.ordered);
            _binding.tilNotes.getEditText().setText(_release.notes);
            prepareDatePicker(_release.date);
        }

        updatePurchased(_binding.chkPurchased.isChecked());
    }

    private void updatePurchased(boolean isChecked) {
        if (isChecked) {
            _binding.release.imgReleasePurchased.setVisibility(View.VISIBLE);
            _binding.release.releaseMainCard.setElevation(0);
            _binding.release.releaseBackground.setBackgroundColor(ContextCompat.getColor(_binding.getRoot().getContext(), R.color.colorItemPurchased));
        } else {
            _binding.release.imgReleasePurchased.setVisibility(View.INVISIBLE);
            _binding.release.releaseMainCard.setElevation(_mainCardElevationPx);
            _binding.release.releaseBackground.setBackgroundColor(ContextCompat.getColor(_binding.getRoot().getContext(), R.color.colorItemNotPurchased));
        }
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

        _selectedDateInUtc = dateUtc;

        final long startSelection;
        if (_selectedDateInUtc == 0) {
            _binding.release.txtReleaseDate.setText("");
            _binding.tilDate.getEditText().setText("");
            // oggi
            startSelection = DateFormatterHelper.toUTCCalendar(System.currentTimeMillis()).getTimeInMillis();
        } else {
            final String humanized = DateFormatterHelper.toHumanReadable(_binding.tilDate.getContext(),
                    DateFormatterHelper.timeToString8(DateFormatterHelper.fromUTCCalendar(_selectedDateInUtc).getTimeInMillis()),
                    DateFormatterHelper.STYLE_FULL);
            _binding.release.txtReleaseDate.setText(humanized);
            _binding.tilDate.getEditText().setText(humanized);
            startSelection = _selectedDateInUtc;
        }

        _datePicker = MaterialDatePicker.Builder.datePicker()
                .setSelection(startSelection)
                .setTitleText("Release date")
                .setCalendarConstraints(new CalendarConstraints.Builder().setOpenAt(startSelection).build())
                .build();

        _datePicker.addOnPositiveButtonClickListener(selection -> {
            _selectedDateInUtc = selection;

            final String humanized = DateFormatterHelper.toHumanReadable(_binding.tilDate.getContext(),
                    DateFormatterHelper.timeToString8(DateFormatterHelper.fromUTCCalendar(selection).getTimeInMillis()),
                    DateFormatterHelper.STYLE_FULL);
            _binding.release.txtReleaseDate.setText(humanized);
            _binding.tilDate.getEditText().setText(humanized);
        });
    }

    Release[] createReleases() {
        _release.date = _selectedDateInUtc == 0 ? null :
                DateFormatterHelper.timeToString8(DateFormatterHelper.fromUTCCalendar(_selectedDateInUtc).getTimeInMillis());
        _release.notes = _binding.tilNotes.getEditText().getText().toString().trim();
        _release.purchased = _binding.chkPurchased.isChecked();
        _release.ordered = _binding.chkOrdered.isChecked();

        if (_binding.tilPrice.getEditText().length() > 0) {
            try {
                _release.price = _numberFormat.parse(_binding.tilPrice.getEditText().getText().toString()).doubleValue();
            } catch (ParseException e) {
                _release.price = 0;
                LogHelper.e("Error parsing release price", e);
            }
        } else {
            _release.price = 0;
        }

        final int[] numbers = Utility.parseInterval(_binding.tilNumbers.getEditText().getText().toString().trim(), ",", "-");
        final Release[] releases = new Release[numbers.length];

        for (int ii = 0; ii < numbers.length; ii++) {
            // in questo caso imposto subito lastUpdate perché queste release potrebbero sovrascrivere quelle già esistenti
            // così facendo risultano ancora visibili nell'elenco release anche se già acquistate per tutto il periodo di retain (vedi ReleaeDao.getAllReleases)
            releases[ii] = Release.create(_release, System.currentTimeMillis());
            releases[ii].number = numbers[ii];
        }

        return releases;
    }

    void saveInstanceState(@NonNull Bundle outState) {
        outState.putString("numbers", _binding.tilNumbers.getEditText().getText().toString());
        outState.putLong("dateUtc", _selectedDateInUtc);
        outState.putString("price", _binding.tilPrice.getEditText().getText().toString());
        outState.putString("notes", _binding.tilNotes.getEditText().getText().toString());
        outState.putBoolean("purchased", _binding.chkPurchased.isChecked());
        outState.putBoolean("ordered", _binding.chkOrdered.isChecked());
    }

    void isValid(@NonNull final ICallback<Boolean> callback) {
        boolean valid = true;
        // TODO: verificare che numbers contenga una sequenza valida
        //  es: 4-3 non deve essere valido (modificare comportamento di Utility.parseInterval)
        //  es: 4-5-6 NO
        //  es: 4- NO etc.
        // TODO: controllo se esistono già certe uscite? nel vecchio non lo facevo
        callback.onCallback(valid);
    }
}
