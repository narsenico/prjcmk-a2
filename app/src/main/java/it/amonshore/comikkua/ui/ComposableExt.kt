package it.amonshore.comikkua.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalContext
import it.amonshore.comikkua.toHumanReadable
import java.time.ZonedDateTime

@Composable
@ReadOnlyComposable
fun toHumanReadable(date: ZonedDateTime?): String {
    val context = LocalContext.current
    return date?.toHumanReadable(context) ?: ""
}