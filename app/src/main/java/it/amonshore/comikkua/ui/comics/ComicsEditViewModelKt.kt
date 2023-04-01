package it.amonshore.comikkua.ui.comics

import android.app.Application
import androidx.lifecycle.*
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsRepositoryKt
import it.amonshore.comikkua.data.comics.ComicsWithReleases
import it.amonshore.comikkua.data.web.CmkWebRepositoryKt
import kotlinx.coroutines.launch

enum class UiComicsEditResultErrorType {
    None,
    EmptyName,
    NameAlreadyUsed,
    InvalidId,
}

sealed class UiComicsEditResult {
    object Saved : UiComicsEditResult()
    data class SaveError(val errorType: UiComicsEditResultErrorType) : UiComicsEditResult()
}

class ComicsEditViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepositoryKt(application)
    private val _cmkWebRepository = CmkWebRepositoryKt(application)
    private val _result = MutableLiveData<UiComicsEditResult>()

    val result: LiveData<UiComicsEditResult> = _result

    val publishers = liveData {
        emit(_comicsRepository.getPublishers())
    }

    val authors = liveData {
        emit(_comicsRepository.getAuthors())
    }

    val availableComics = _cmkWebRepository.getNotFollowedComics().asLiveData()

    fun getComicsWithReleasesOrNull(comicsId: Long): LiveData<ComicsWithReleases> = liveData {
        if (comicsId == Comics.NEW_COMICS_ID) {
            emit(ComicsWithReleases.createNew())
        } else {
            emit(_comicsRepository.getComicsWithReleases(comicsId))
        }
    }

    fun insertOrUpdateComics(comics: Comics) = viewModelScope.launch {
        when (val errorType = isComicsValid(comics)) {
            UiComicsEditResultErrorType.None -> {
                _comicsRepository.upsert(comics)
                // TODO: salvare immagine
                _result.postValue(UiComicsEditResult.Saved)
            }
            else -> {
                _result.postValue(UiComicsEditResult.SaveError(errorType))
            }
        }
    }

    private suspend fun isComicsValid(comics: Comics): UiComicsEditResultErrorType {
        if (comics.id == Comics.NO_COMICS_ID) {
            return UiComicsEditResultErrorType.InvalidId
        }

        if (comics.name.isBlank()) {
            return UiComicsEditResultErrorType.EmptyName
        }

        if (comics.id == Comics.NEW_COMICS_ID &&
            _comicsRepository.existsComicsWithName(comics.name)
        ) {
            return UiComicsEditResultErrorType.NameAlreadyUsed
        }

        return UiComicsEditResultErrorType.None
    }
}