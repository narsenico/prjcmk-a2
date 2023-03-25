package it.amonshore.comikkua.data.comics

import android.app.Application
import androidx.lifecycle.AndroidViewModel

class ComicsViewModelKt(application: Application) : AndroidViewModel(application) {

    private val _comicsRepository = ComicsRepositoryKt(application)

    fun getComicsWithReleases(id: Long) = _comicsRepository.getComicsWithReleases(id)
}