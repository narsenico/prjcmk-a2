package it.amonshore.comikkua.ui.comics

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.bumptech.glide.RequestManager
import com.google.android.material.textfield.TextInputLayout
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.release.Periodicity
import it.amonshore.comikkua.data.release.getPeriodicityList
import it.amonshore.comikkua.databinding.FragmentComicsEditBinding
import it.amonshore.comikkua.parseToDouble
import it.amonshore.comikkua.parseToString
import it.amonshore.comikkua.toKey
import it.amonshore.comikkua.ui.DrawableTextViewTarget
import it.amonshore.comikkua.ui.ImageHelperKt

private const val NAME = "name"
private const val PUBLISHER = "publisher"
private const val AUTHORS = "authors"
private const val NOTES = "notes"
private const val SERIES = "series"
private const val PRICE = "price"
private const val PERIODICITY = "periodicity"
private const val COMICS_IMAGE = "comics_image"

class ComicsEditFragmentHelper(
    val context: Context,
    val binding: FragmentComicsEditBinding,
    private val glideRequestManager: RequestManager
) {

    private val _periodicityList = getPeriodicityList(context)
    private val _comicsImageViewTarget = DrawableTextViewTarget(binding.comics.txtComicsInitial)

    private var _comics: Comics? = null
    private var _comicsImagePath: String? = null

    val rootView = binding.root

    init {
        binding.tilPeriodicity.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_item,
            _periodicityList
        )

        binding.tilName.editText!!.doAfterTextChanged {
            if (_comicsImagePath == null) {
                val name = it.toString().trim()
                binding.comics.txtComicsName.text = name
                binding.comics.txtComicsInitial.text = name.getInitial()
            }
        }

        binding.tilPublisher.editText!!.doAfterTextChanged {
            binding.comics.txtComicsPublisher.text = it.toString().trim()
        }

        binding.tilAuthors.editText!!.doAfterTextChanged {
            binding.comics.txtComicsAuthors.text = it.toString().trim()
        }

        binding.tilNotes.editText!!.doAfterTextChanged {
            binding.comics.txtComicsNotes.text = it.toString().trim()
        }
    }

    fun setPublishers(publishers: List<String>) {
        binding.tilPublisher.autocomplete().setAdapter(
            ArrayAdapter(
                context,
                android.R.layout.simple_dropdown_item_1line,
                publishers
            )
        )
    }

    fun setAuthors(authors: List<String>) {
        binding.tilAuthors.autocomplete().setAdapter(
            ArrayAdapter(
                context,
                android.R.layout.simple_dropdown_item_1line,
                authors
            )
        )
    }

    fun setComics(comics: ComicsWithReleases, savedInstanceState: Bundle?) {
        _comics = comics.comics
        binding.tilName.editText!!.isEnabled = !comics.comics.isSourced
        binding.tilPublisher.editText!!.isEnabled = !comics.comics.isSourced

        binding.comics.imgSourced.visibility = if (comics.comics.isSourced) View.VISIBLE else View.GONE

        if (savedInstanceState != null) {
            setLayoutWithSavedInstanceState(savedInstanceState)
        } else {
            setLayoutWithSavedInstanceStateWithComics(comics.comics)
        }

        val lastRelease = comics.lastPurchasedRelease
        binding.comics.txtComicsReleaseLast.text =
            if (lastRelease == null) context.getString(R.string.release_last_none) else
                context.getString(R.string.release_last, lastRelease.number)

        val nextRelease = comics.nextToPurchaseRelease
        binding.comics.txtComicsReleaseNext.text =
            if (nextRelease == null) context.getString(R.string.release_next_none) else
                context.getString(R.string.release_next, nextRelease.number)

        val missingCount = comics.notPurchasedReleaseCount
        binding.comics.txtComicsReleaseMissing.text =
            context.getString(R.string.release_missing, missingCount)
    }

    fun setComicsImagePath(comicsImagePath: String?) {
        updateComicsImageAndInitial(comicsImagePath, binding.tilName.getText())
    }

    fun saveInstanceState(outState: Bundle) {
        outState.putString(NAME, binding.tilName.getText())
        outState.putString(PUBLISHER, binding.tilPublisher.getText())
        outState.putString(SERIES, binding.tilSeries.getText())
        outState.putString(AUTHORS, binding.tilAuthors.getText())
        outState.putString(NOTES, binding.tilNotes.getText())
        outState.putString(PRICE, binding.tilPrice.getText())
        outState.putString(PERIODICITY, _periodicityList.getKey(binding.tilPeriodicity.selection))
        outState.putString(COMICS_IMAGE, _comicsImagePath)
    }

    fun setError(errorType: UiComicsEditResultErrorType) {
        when (errorType) {
            UiComicsEditResultErrorType.None -> {
                binding.tilName.isErrorEnabled = false
            }

            UiComicsEditResultErrorType.EmptyName -> {
                binding.tilName.error = context.getText(R.string.comics_name_empty_error)
                binding.tilName.isErrorEnabled = true
            }

            UiComicsEditResultErrorType.InvalidId -> {
                binding.tilName.error = context.getText(R.string.comics_saving_error)
                binding.tilName.isErrorEnabled = true
            }

            UiComicsEditResultErrorType.NameAlreadyUsed -> {
                binding.tilName.error = context.getText(R.string.comics_name_notunique_error)
                binding.tilName.isErrorEnabled = true
            }

            UiComicsEditResultErrorType.ImageError -> {
                Toast.makeText(
                    binding.root.context,
                    R.string.comics_saving_error,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    fun getComics(): Comics {
        val comics = _comics!!
        return comics.copy(
            name = binding.tilName.getText(),
            publisher = binding.tilPublisher.getText(),
            series = binding.tilSeries.getText(),
            authors = binding.tilAuthors.getText(),
            notes = binding.tilNotes.getText(),
            periodicity = _periodicityList.getKey(binding.tilPeriodicity.selection),
            price = parseToDouble(binding.tilPrice.getText()),
            image = _comicsImagePath?.let { Uri.parse(it).toString() }
        )
    }

    private fun setLayoutWithSavedInstanceState(savedInstanceState: Bundle) {
        binding.comics.txtComicsName.text = savedInstanceState.getString(NAME)
        binding.comics.txtComicsPublisher.text = savedInstanceState.getString(PUBLISHER)
        binding.comics.txtComicsAuthors.text = savedInstanceState.getString(AUTHORS)
        binding.comics.txtComicsNotes.text = savedInstanceState.getString(NOTES)
        binding.tilName.setText(savedInstanceState.getString(NAME))
        binding.tilPublisher.setText(savedInstanceState.getString(PUBLISHER))
        binding.tilSeries.setText(savedInstanceState.getString(SERIES))
        binding.tilAuthors.setText(savedInstanceState.getString(AUTHORS))
        binding.tilPrice.setText(savedInstanceState.getString(PRICE))
        binding.tilNotes.setText(savedInstanceState.getString(NOTES))
        binding.tilPeriodicity.selection =
            _periodicityList.indexByKey(savedInstanceState.getString(PERIODICITY))
        updateComicsImageAndInitial(
            savedInstanceState.getString(COMICS_IMAGE),
            savedInstanceState.getString(NAME)
        )
    }

    private fun setLayoutWithSavedInstanceStateWithComics(comics: Comics) {
        binding.comics.txtComicsName.text = comics.name
        binding.comics.txtComicsPublisher.text = comics.publisher
        binding.comics.txtComicsAuthors.text = comics.authors
        binding.comics.txtComicsNotes.text = comics.notes
        binding.tilName.setText(comics.name)
        binding.tilPublisher.setText(comics.publisher)
        binding.tilSeries.setText(comics.series)
        binding.tilAuthors.setText(comics.authors)
        binding.tilPrice.setText(parseToString(comics.price))
        binding.tilNotes.setText(comics.notes)
        binding.tilPeriodicity.selection = _periodicityList.indexByKey(comics.periodicity)
        updateComicsImageAndInitial(comics.image, comics.name)
    }

    private fun updateComicsImageAndInitial(comicsImagePath: String?, name: String?) {
        _comicsImagePath = comicsImagePath?.let {
            binding.comics.txtComicsInitial.text = ""
            glideRequestManager.load(it)
                .apply(ImageHelperKt.getInstance(context).circleOptions)
                .into(_comicsImageViewTarget)
            it
        } ?: run {
            binding.comics.txtComicsInitial.text = name.getInitial()
            binding.comics.txtComicsInitial.setBackgroundResource(R.drawable.background_comics_initial_noborder)
            null
        }
    }

    private fun TextInputLayout.autocomplete(): AutoCompleteTextView =
        editText as AutoCompleteTextView

    private fun TextInputLayout.setText(text: String?) =
        editText!!.setText(text)

    private fun TextInputLayout.getText() =
        editText!!.text.toString().trim()

    private fun List<Periodicity>.indexByKey(key: String?) =
        if (key == null) -1 else withIndex().find { it.value.period.toKey() == key }?.index ?: 0

    private fun List<Periodicity>.getKey(position: Int) =
        if (position > 0) get(position).period.toKey() else null

    private fun String?.getInitial() = if (isNullOrBlank()) "" else substring(0, 1)
}