package it.amonshore.comikkua.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import it.amonshore.comikkua.LogHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.MultiRelease
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.data.release.notes
import it.amonshore.comikkua.data.release.toPair
import it.amonshore.comikkua.joinToString
import it.amonshore.comikkua.toHumanReadable
import it.amonshore.comikkua.uriEncode

fun Comics.toSharable(): String =
    arrayOf(name, publisher, authors)
        .joinToString(" - ")

fun ComicsRelease.toSharable(context: Context): String = toPair().toSharable(context)

fun Pair<Comics, Release>.toSharable(context: Context): String {
    if (second.date != null) {
        return context.getString(
            R.string.share_release,
            first.name,
            second.number,
            second.date!!.toHumanReadable(context),
            notes()
        )
    }

    return context.getString(
        R.string.share_release_nodate,
        first.name,
        second.number,
        notes()
    )
}

fun Activity.share(comics: Comics) {
    shareText(this, comics.toSharable())
}

fun Activity.share(release: MultiRelease) {
    val rows = release.getAllReleases()
        .map { (release.comics to it).toSharable(this) }
        .toList()
    shareText(this, rows)
}

fun Activity.shareRelease(release: ComicsRelease) {
    if (release is MultiRelease) {
        share(release)
    } else {
        shareText(this, release.toSharable(this))
    }
}

fun Activity.share(releases: List<ComicsRelease>) {
    if (releases.isNotEmpty()) {
        val rows = releases.map { it.toSharable(this) }
        shareText(this, rows)
    }
}

fun Activity.shareOnGoogle(comics: Comics) {
    openUrl(this, Uri.parse("https://www.google.com/search?q=${comics.encoded()}&ie=UTF-8"))
}

fun Activity.shareOnGoogle(release: ComicsRelease) {
    openUrl(this, Uri.parse("https://www.google.com/search?q=${release.encoded()}&ie=UTF-8"))
}

fun Activity.shareOnStarShop(comics: Comics) {
    openUrl(
        this,
        Uri.parse("https://www.starshop.it/?mot_q=${comics.encoded()}")
    )
}

fun Activity.shareOnStarShop(release: ComicsRelease) {
    openUrl(
        this,
        Uri.parse("https://www.starshop.it/?mot_q=${release.comics.name.uriEncode()}+${release.release.number}")
    )
}

fun Activity.shareOnAmazon(comics: Comics) {
    openUrl(this, Uri.parse("https://www.amazon.it/s?k=${comics.encoded()}&_encoding=UTF8"))
}

fun Activity.shareOnAmazon(release: ComicsRelease) {
    openUrl(this, Uri.parse("https://www.amazon.it/s?k=${release.encoded()}&_encoding=UTF8"))
}

fun Activity.shareOnPopStore(comics: Comics) {
    openUrl(
        this,
        Uri.parse("https://popstore.it/module/clerk/search?controller=search&orderby=position&orderway=desc&search-cat-select=0&search_query=${comics.encoded()}&submit_search=&fc=module&module=clerk")
    )
}

fun Activity.shareOnPopStore(release: ComicsRelease) {
    openUrl(
        this,
        Uri.parse(
            "https://popstore.it/module/clerk/search?controller=search&orderby=position&orderway=desc&search-cat-select=0&search_query=${release.encoded(3)}&submit_search=&fc=module&module=clerk"
        )
    )
}

fun Activity.shareOnMangaYo(comics: Comics) {
    openUrl(
        this,
        Uri.parse("https://mangayo.it/?mot_q=${comics.encoded()}")
    )
}

fun Activity.shareOnMangaYo(release: ComicsRelease) {
    openUrl(
        this,
        Uri.parse("https://mangayo.it/?mot_q=${release.encoded()}")
    )
}

private fun Comics.encoded() = name.uriEncode()
private fun ComicsRelease.encoded(releaseLength :Int = 0) = if (release.number == 0) {
    comics.name.uriEncode()
} else if (releaseLength == 0) {
    "${comics.name} ${release.number}".uriEncode()
} else {
    "${comics.name} ${release.number.toString().padStart(releaseLength, '0')}".uriEncode()
}

private fun openUrl(activity: Activity, uri: Uri) {
    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = uri
    }
    LogHelper.d { "open url $uri" }
    activity.startActivity(intent)
}

private const val TEXT_PLAIN = "text/plain"

private fun shareText(activity: Activity, row: String) {
    if (row.isNotBlank()) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, row)
        }

        activity.startActivity(
            Intent.createChooser(
                intent,
                activity.getText(R.string.share_chooser_title)
            )
        )
    }
}

private fun shareText(activity: Activity, rows: List<String>) {
    if (rows.isNotEmpty()) {
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            type = TEXT_PLAIN
            putExtra(Intent.EXTRA_TEXT, rows.joinToString("\n"))
        }

        activity.startActivity(
            Intent.createChooser(
                intent,
                activity.getText(R.string.share_chooser_title)
            )
        )
    }
}