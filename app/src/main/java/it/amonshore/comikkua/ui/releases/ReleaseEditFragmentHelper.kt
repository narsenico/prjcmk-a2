package it.amonshore.comikkua.ui.releases

import android.content.Context
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
import it.amonshore.comikkua.R
import it.amonshore.comikkua.ReleaseDate
import it.amonshore.comikkua.joinToString
import it.amonshore.comikkua.asLocalDate
import it.amonshore.comikkua.asUtcMilliseconds
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.databinding.FragmentReleaseEditBinding
import it.amonshore.comikkua.parseInterval
import it.amonshore.comikkua.parseToDouble
import it.amonshore.comikkua.parseToString
import it.amonshore.comikkua.toHumanReadableLong
import it.amonshore.comikkua.toLocalDate
import it.amonshore.comikkua.toReleaseDate
import it.amonshore.comikkua.ui.DrawableTextViewTarget
import it.amonshore.comikkua.ui.ImageHelperKt

class ReleaseEditFragmentHelper(
    val context: Context,
    val binding: FragmentReleaseEditBinding,
    private val fragmentManager: FragmentManager,
    private val glideRequestManager: RequestManager
) {

    private var _datePicker: MaterialDatePicker<Long>? = null
    private var _selectedDate: ReleaseDate? = null
    private var _mainCardElevationPx = binding.release.releaseMainCard.elevation
    private lateinit var _release: Release

    val rootView = binding.root

    init {
        // da xml non riesco a impostarli
        binding.tilNumbers.editText!!.keyListener = DigitsKeyListener.getInstance("0123456789,-")

        // la modifica di una proprietà della release si riflette immediatamente sulla preview
        binding.tilNumbers.editText!!.doAfterTextChanged {
            binding.release.txtReleaseNumbers.text = it.toString().trim()
        }

        binding.tilNotes.editText!!.doAfterTextChanged {
            binding.release.txtReleaseNotes.text = it.toString().trim()
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
                if (hasFocus) {
                    _datePicker?.show(fragmentManager, "release_date_picker")
                }
            }
            setOnClickListener { view ->
                if (view.hasFocus()) {
                    _datePicker?.show(fragmentManager, "release_date_picker")
                }
            }
        }

        // icona alla destra del campo Date, elimina la data di rilascio
        binding.tilDate.setEndIconOnClickListener {
            _selectedDate = null
            binding.tilDate.editText!!.setText("")
            binding.release.txtReleaseDate.text = ""
        }
    }

    fun setComics(comics: ComicsWithReleases) {
        binding.release.txtReleaseTitle.text = comics.comics.name
        binding.release.txtReleaseNotes.text = comics.comics.notes
        binding.release.txtReleaseInfo.text = arrayOf(comics.comics.publisher, comics.comics.authors).joinToString(", ")
        binding.release.imgReleasePurchased.visibility = View.INVISIBLE
        binding.release.imgReleaseOrdered.visibility = View.INVISIBLE
        binding.release.imgReleaseMenu.visibility = View.INVISIBLE
        updatePurchased(false)
        if (comics.comics.hasImage()) {
            glideRequestManager
                .load(Uri.parse(comics.comics.image))
                .apply(ImageHelperKt.getInstance(context).squareOptions)
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
            prepareDatePicker(savedInstanceState.getString("date"))
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

    private fun prepareDatePicker(releaseDate: ReleaseDate?) {
        val startSelection = if (releaseDate == null) {
            binding.release.txtReleaseDate.text = ""
            binding.tilDate.editText!!.setText("")
            MaterialDatePicker.todayInUtcMilliseconds()
        } else {
            val date = releaseDate.toLocalDate()
            val humanized = date.toHumanReadableLong(context)
            binding.release.txtReleaseDate.text = humanized
            binding.tilDate.editText!!.setText(humanized)
            date.asUtcMilliseconds()
        }

        _datePicker = MaterialDatePicker.Builder.datePicker()
            .setSelection(startSelection)
            .setTitleText(context.getString(R.string.release_date))
            .setCalendarConstraints(CalendarConstraints.Builder().setOpenAt(startSelection).build())
            .build()
            .also {
                it.addOnPositiveButtonClickListener { selection ->
                    val localDate = if (selection == 0L) null else selection.asLocalDate()
                    _selectedDate = localDate?.toReleaseDate()
                    val humanized = localDate?.toHumanReadableLong(context)
                    binding.release.txtReleaseDate.text = humanized
                    binding.tilDate.editText!!.setText(humanized)
                }
            }
    }

    fun createReleases(): List<Release> {
        _release.date = _selectedDate
        _release.notes = binding.tilNotes.editText!!.text.toString().trim()
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
        outState.putString("date", _selectedDate)
        outState.putString("price", binding.tilPrice.editText!!.text.toString())
        outState.putString("notes", binding.tilNotes.editText!!.text.toString())
        outState.putBoolean("purchased", binding.chkPurchased.isChecked)
        outState.putBoolean("ordered", binding.chkOrdered.isChecked)
    }

    fun isValid(block: (Boolean) -> Unit) {
        val valid = true
        // TODO: verificare che numbers contenga una sequenza valida
        //  es: 4-3 non deve essere valido (modificare comportamento di Utility.parseInterval)
        //  es: 4-5-6 NO
        //  es: 4- NO etc.
        // TODO: controllo se esistono già certe uscite? nel vecchio non lo facevo
        block(valid)
    }
}