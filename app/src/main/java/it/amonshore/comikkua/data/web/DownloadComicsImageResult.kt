package it.amonshore.comikkua.data.web

import android.net.Uri

sealed class DownloadComicsImageResult {
    data class Success(val uri: Uri) : DownloadComicsImageResult()
    object NotFound : DownloadComicsImageResult()
    data class Error(val error: Throwable) : DownloadComicsImageResult()
}