package it.amonshore.comikkua.ui.comics

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.*
import it.amonshore.comikkua.*
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsRepositoryKt
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.ui.ImageHelper
import kotlinx.coroutines.launch
import java.io.File

enum class UiComicsEditResultErrorType {
    None,
    EmptyName,
    NameAlreadyUsed,
    InvalidId,
    ImageError,
}

sealed class UiComicsEditResult {
    object Saved : UiComicsEditResult()
    data class SaveError(val errorType: UiComicsEditResultErrorType) : UiComicsEditResult()
}

class ComicsEditViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepositoryKt(application)
    private val _result = MutableLiveData<UiComicsEditResult>()

    val result: LiveData<UiComicsEditResult> = _result

    val publishers = liveData {
        emit(_comicsRepository.getPublishers())
    }

    val authors = liveData {
        emit(_comicsRepository.getAuthors())
    }

    fun getComicsWithReleasesOrNull(comicsId: Long): LiveData<ComicsWithReleases> = liveData {
        if (comicsId == Comics.NEW_COMICS_ID) {
            emit(ComicsWithReleases.createNew())
        } else {
            emit(_comicsRepository.getComicsWithReleases(comicsId))
        }
    }

    fun insertOrUpdateComics(comics: Comics) = viewModelScope.launch {
        isComicsValid(comics)
            .flatMap { saveNewComicsImage(comics) }
            .flatMap { comics -> deleteDirtyComicsImages(comics) }
            .onSuccess { comics ->
                _comicsRepository.upsert(comics)
                _result.postValue(UiComicsEditResult.Saved)
            }
            .onFailure {
                _result.postValue(UiComicsEditResult.SaveError(it))
            }
    }

    private suspend fun isComicsValid(comics: Comics): ResultEx<Unit, UiComicsEditResultErrorType> {
        if (comics.id == Comics.NO_COMICS_ID) {
            return UiComicsEditResultErrorType.InvalidId.toFailure()
        }

        if (comics.name.isBlank()) {
            return UiComicsEditResultErrorType.EmptyName.toFailure()
        }

        if (comics.id == Comics.NEW_COMICS_ID &&
            _comicsRepository.existsComicsWithName(comics.name)
        ) {
            return UiComicsEditResultErrorType.NameAlreadyUsed.toFailure()
        }

        return ResultEx.Success()
    }

    private fun saveNewComicsImage(comics: Comics): ResultEx<Comics, UiComicsEditResultErrorType> {
        if (!comics.hasImage()) {
            return ResultEx.Success(comics)
        }

        return try {
            val context: Context = getApplication()
            val tempImageFile = File(Uri.parse(comics.image).path!!)
            val newImageFile = File(context.filesDir, ImageHelper.newImageFileName(comics.id))
            tempImageFile.renameTo(newImageFile)
                .orFail { UiComicsEditResultErrorType.ImageError }
                .map { comics.apply { comics.image = Uri.fromFile(newImageFile).toString() } }
        } catch (ex: Exception) {
            LogHelper.e("Error saving image", ex)
            ResultEx.Failure(UiComicsEditResultErrorType.ImageError)
        }
    }

    private fun deleteDirtyComicsImages(comics: Comics): ResultEx<Comics, UiComicsEditResultErrorType> =
        try {
            val context: Context = getApplication()
            val comicsId = comics.id
            val currentImagePath = comics.image?.let { Uri.parse(it).path }
            context.filesDir.listFiles { _, name ->
                ImageHelper.isValidImageFileName(
                    name,
                    comicsId
                )
            }?.filter { file ->
                if (currentImagePath != null) file.toString() != currentImagePath else true
            }?.forEach { file ->
                file.delete()
            }

            ResultEx.Success(comics)
        } catch (ex: Exception) {
            LogHelper.e("Error deleting old images", ex)
            ResultEx.Failure(UiComicsEditResultErrorType.ImageError)
        }
}