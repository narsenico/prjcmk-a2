package it.amonshore.comikkua.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import it.amonshore.comikkua.DateFormatterHelper
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.MultiRelease
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.uriEncode

fun Comics.toSharable(): String =
    arrayOf(name, publisher, authors)
        .filter { !it.isNullOrBlank() }
        .joinToString(separator = " - ")

fun Pair<Comics, Release>.toSharable(context: Context): String {
    val comics = first
    val release = second
    if (release.hasDate()) {
        return context.getString(
            R.string.share_release,
            comics.name,
            release.number,
            DateFormatterHelper.toHumanReadable(
                context,
                release.date,
                DateFormatterHelper.STYLE_SHORT
            ),
            if (release.hasNotes()) release.notes else comics.notes
        )
    }

    return context.getString(
        R.string.share_release_nodate,
        comics.name,
        release.number,
        if (release.hasNotes()) release.notes else comics.notes
    )
}

fun ComicsRelease.toSharable(context: Context): String {
    val cr = comics to release
    return cr.toSharable(context)
}

fun Activity.share(comics: Comics) {
    shareText(this, comics.toSharable())
}

fun Activity.share(release: MultiRelease) {
    val rows = mutableListOf<String>()
    rows.add(release.toSharable(this))
    for (other in release.otherReleases) {
        rows.add((release.comics to other).toSharable(this))
    }
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
    shareOn(this, comics, "https://www.google.com/search?q=%s&ie=UTF-8")
}

fun Activity.shareOnGoogle(release: ComicsRelease) {
    shareOn(this, release, "https://www.google.com/search?q=%s&ie=UTF-8")
}

fun Activity.shareOnStarShop(comics: Comics) {
    shareOn(
        this,
        comics,
        "https://www.starshop.it/#/dffullscreen/query=%s&query_name=match_and"
    )
}

fun Activity.shareOnStarShop(release: ComicsRelease) {
    shareOn(
        this,
        release,
        "https://www.starshop.it/#/dffullscreen/query=%s&query_name=match_and"
    )
}

fun Activity.shareOnAmazon(comics: Comics) {
    shareOn(this, comics, "https://www.amazon.it/s?k=%s&_encoding=UTF8")
}

fun Activity.shareOnAmazon(release: ComicsRelease) {
    shareOn(this, release, "https://www.amazon.it/s?k=%s&_encoding=UTF8")
}

fun Activity.shareOnPopStore(comics: Comics) {
    shareOn(this, comics, "https://popstore.it/cerca?controller=search&search_query=%s")
}

fun Activity.shareOnPopStore(release: ComicsRelease) {
    shareOn(this, release, "https://popstore.it/cerca?controller=search&search_query=%s")
}

private fun shareOn(activity: Activity, comics: Comics, format: String) {
    val encoded =
        arrayOf(comics.publisher, comics.name).filter { !it.isNullOrBlank() }
            .joinToString(separator = " ")
            .uriEncode()
    val searchUrl = String.format(format, encoded)

    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(searchUrl)
    }
    activity.startActivity(intent)
}

private fun shareOn(activity: Activity, release: ComicsRelease, format: String) {
    val encoded = arrayOf(
        release.comics.publisher,
        release.comics.name,
        release.release.number.toString()
    ).filter { !it.isNullOrBlank() }.joinToString(separator = " ").uriEncode()
    val searchUrl = String.format(format, encoded)

    val intent = Intent().apply {
        action = Intent.ACTION_VIEW
        data = Uri.parse(searchUrl)
    }
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