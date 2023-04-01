package it.amonshore.comikkua.ui.releases

import android.net.Uri
import android.os.Bundle
import android.text.method.DigitsKeyListener
import android.view.View
import android.view.View.OnFocusChangeListener
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.RequestManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import it.amonshore.comikkua.*
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.databinding.FragmentReleaseEditBinding
import it.amonshore.comikkua.ui.DrawableTextViewTarget
import it.amonshore.comikkua.ui.ImageHelper

class ReleaseEditFragmentHelper(
    val binding: FragmentReleaseEditBinding,
    private val fragmentManager: FragmentManager,
    private val glideRequestManager: RequestManager
) {

    private var _datePicker: MaterialDatePicker<Long>? = null
    private var _selectedDateInUtc: Long = 0
    private var _mainCardElevationPx = binding.release.releaseMainCard.elevation
    private lateinit var _release: Release

    val rootView = binding.root

    init {
        // da xml non riesco a impostarli
        binding.tilNumbers.editText!!.keyListener = DigitsKeyListener.getInstance("0123456789,-")

        // la modifica di una proprietà della release si riflette immediatamente sulla preview
        binding.tilNumbers.editText!!.doAfterTextChanged {
            binding.release.txtReleaseNumbers.text = toString().trim { it <= ' ' }
        }

        binding.tilNotes.editText!!.doAfterTextChanged {
            binding.release.txtReleaseNotes.text = toString().trim { it <= ' ' }
        }

        binding.chkPurchased.setOnCheckedChangeListener { _, isChecked ->
            updatePurchased(
                isChecked
            )
        }

        binding.chkOrdered.setOnCheckedChangeListener { _, isChecked ->
            binding.release.imgReleaseOrdered.visibility =
                if (isChecked) View.VISIBLE else View.INVISIBLE
        }

        // il campo Date non è editabile
        // quando acquisisce il focus apro il picker
        // quando verrà chiuso il focus rimarrà al campo date
        // quindi al click del campo Date, se ha già il focus riapro il picker

        binding.tilDate.editText!!.apply {
            inputType = EditorInfo.TYPE_NULL
            onFocusChangeListener = OnFocusChangeListener { _, hasFocus ->
                if (hasFocus && _datePicker != null) {
                    _datePicker!!.show(fragmentManager, "release_date_picker")
                }
            }
            setOnClickListener { view ->
                if (_datePicker != null && view.hasFocus()) {
                    _datePicker!!.show(fragmentManager, "release_date_picker")
                }
            }
        }

        // icona alla destra del campo Date, elimina la data di rilascio
        binding.tilDate.setEndIconOnClickListener {
            _selectedDateInUtc = 0
            binding.tilDate.editText!!.setText("")
            binding.release.txtReleaseDate.text = ""
        }
    }

    fun setComics(comics: ComicsWithReleases) {
        binding.release.txtReleaseTitle.text = comics.comics.name
        binding.release.txtReleaseNotes.text = comics.comics.notes
        binding.release.txtReleaseInfo.text = Utility.join(
            ", ",
            true,
            comics.comics.publisher,
            comics.comics.authors
        )
        binding.release.imgReleasePurchased.visibility = View.INVISIBLE
        binding.release.imgReleaseOrdered.visibility = View.INVISIBLE
        binding.release.imgReleaseMenu.visibility = View.INVISIBLE
        updatePurchased(false)
        if (comics.comics.hasImage()) {
            glideRequestManager
                .load(Uri.parse(comics.comics.image))
                .apply(ImageHelper.getGlideSquareOptions())
                .into(DrawableTextViewTarget(binding.release.txtReleaseNumbers))
        }
    }

    fun setRelease(release: Release, savedInstanceState: Bundle?) {
        _release = release
        if (savedInstanceState != null) {
            binding.release.txtReleaseNumbers.text = savedInstanceState.getString("numbers")
            binding.release.imgReleasePurchased.visibility =
                if (savedInstanceState.getBoolean("purchased")) View.VISIBLE else View.INVISIBLE
            binding.release.imgReleaseOrdered.visibility =
                if (savedInstanceState.getBoolean("ordered")) View.VISIBLE else View.INVISIBLE
            binding.tilNumbers.editText!!.setText(savedInstanceState.getString("numbers"))
            binding.tilPrice.editText!!.setText(savedInstanceState.getString("price"))
            binding.chkPurchased.isChecked = savedInstanceState.getBoolean("purchased")
            binding.chkOrdered.isChecked = savedInstanceState.getBoolean("ordered")
            binding.tilNotes.editText!!.setText(savedInstanceState.getString("notes"))
            prepareDatePicker(savedInstanceState.getLong("dateUtc"))
        } else {
            binding.release.txtReleaseNumbers.text = release.number.toString()
            binding.release.imgReleasePurchased.visibility =
                if (_release.purchased) View.VISIBLE else View.INVISIBLE
            binding.release.imgReleaseOrdered.visibility =
                if (_release.ordered) View.VISIBLE else View.INVISIBLE
            binding.tilNumbers.editText!!.setText(_release.number.toString())
            binding.tilPrice.editText!!.setText(parseToString(_release.price))
            binding.chkPurchased.isChecked = _release.purchased
            binding.chkOrdered.isChecked = _release.ordered
            binding.tilNotes.editText!!.setText(_release.notes)
            prepareDatePicker(_release.date)
        }
        updatePurchased(binding.chkPurchased.isChecked)
    }

    private fun updatePurchased(isChecked: Boolean) {
        if (isChecked) {
            binding.release.imgReleasePurchased.visibility = View.VISIBLE
            binding.release.releaseMainCard.elevation = 0f
            binding.release.releaseBackground.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    R.color.colorItemPurchased
                )
            )
        } else {
            binding.release.imgReleasePurchased.visibility = View.INVISIBLE
            binding.release.releaseMainCard.elevation = _mainCardElevationPx
            binding.release.releaseBackground.setBackgroundColor(
                ContextCompat.getColor(
                    binding.root.context,
                    R.color.colorItemNotPurchased
                )
            )
        }
    }

    private fun prepareDatePicker(date: String?) {
        // MaterialDatePicker accetta date in UTC
        if (date.isNullOrBlank()) {
            prepareDatePicker(0)
        } else {
            prepareDatePicker(DateFormatterHelper.toUTCCalendar(date).timeInMillis)
        }
    }

    private fun prepareDatePicker(dateUtc: Long) {
        // MaterialDatePicker accetta date in UTC
        _selectedDateInUtc = dateUtc
        val startSelection: Long
        if (_selectedDateInUtc == 0L) {
            binding.release.txtReleaseDate.text = ""
            binding.tilDate.editText!!.setText("")
            // oggi
            startSelection =
                DateFormatterHelper.toUTCCalendar(System.currentTimeMillis()).timeInMillis
        } else {
            val humanized = DateFormatterHelper.toHumanReadable(
                binding.tilDate.context,
                DateFormatterHelper.timeToString8(
                    DateFormatterHelper.fromUTCCalendar(
                        _selectedDateInUtc
                    ).timeInMillis
                ),
                DateFormatterHelper.STYLE_FULL
            )
            binding.release.txtReleaseDate.text = humanized
            binding.tilDate.editText!!.setText(humanized)
            startSelection = _selectedDateInUtc
        }
        _datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(startSelection)
            .setTitleText("Release date")
            .setCalendarConstraints(CalendarConstraints.Builder().setOpenAt(startSelection).build())
            .build()
        _datePicker!!.addOnPositiveButtonClickListener { selection: Long ->
            _selectedDateInUtc = selection
            val humanized = DateFormatterHelper.toHumanReadable(
                binding.tilDate.context,
                DateFormatterHelper.timeToString8(DateFormatterHelper.fromUTCCalendar(selection).timeInMillis),
                DateFormatterHelper.STYLE_FULL
            )
            binding.release.txtReleaseDate.text = humanized
            binding.tilDate.editText!!.setText(humanized)
        }
    }

    fun createReleases(): List<Release> {
        _release.date = if (_selectedDateInUtc == 0L) null else DateFormatterHelper.timeToString8(
            DateFormatterHelper.fromUTCCalendar(_selectedDateInUtc).timeInMillis
        )
        _release.notes = binding.tilNotes.editText!!.text.toString().trim { it <= ' ' }
        _release.purchased = binding.chkPurchased.isChecked
        _release.ordered = binding.chkOrdered.isChecked
        _release.price = parseToDouble(binding.tilPrice.editText!!.text.toString())

        val numbers = parseInterval(
            binding.tilNumbers.editText!!.text.toString().trim { it <= ' ' }
        )
        return numbers.map {
            // in questo caso imposto subito lastUpdate perché queste release potrebbero sovrascrivere quelle già esistenti
            // così facendo risultano ancora visibili nell'elenco release anche se già acquistate per tutto il periodo di retain (vedi ReleaeDao.getAllReleases)
            Release.create(_release, System.currentTimeMillis()).apply {
                number = it
            }
        }
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putString("numbers", binding.tilNumbers.editText!!.text.toString())
        outState.putLong("dateUtc", _selectedDateInUtc)
        outState.putString("price", binding.tilPrice.editText!!.text.toString())
        outState.putString("notes", binding.tilNotes.editText!!.text.toString())
        outState.putBoolean("purchased", binding.chkPurchased.isChecked)
        outState.putBoolean("ordered", binding.chkOrdered.isChecked)
    }

    fun isValid(callback: ICallback<Boolean?>) {
        val valid = true
        // TODO: verificare che numbers contenga una sequenza valida
        //  es: 4-3 non deve essere valido (modificare comportamento di Utility.parseInterval)
        //  es: 4-5-6 NO
        //  es: 4- NO etc.
        // TODO: controllo se esistono già certe uscite? nel vecchio non lo facevo
        callback.onCallback(valid)
    }
}