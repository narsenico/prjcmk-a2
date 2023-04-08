package it.amonshore.comikkua.workers

import android.app.Notification
import android.app.Notification.InboxStyle
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.TaskStackBuilder
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import it.amonshore.comikkua.Constants
import it.amonshore.comikkua.LogHelperKt
import it.amonshore.comikkua.R
import it.amonshore.comikkua.data.release.*
import it.amonshore.comikkua.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

class ReleasesNotificationWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) context@{
        try {
            val context = applicationContext
            val repository = ReleaseRepository(context)
            // TODO: non è meglio considerare anche quelle non comprate dell'ultima settimana? o è troppo invadente?
            val releaseDateFrom = LocalDate.now()
            val releaseDateTo = LocalDate.now()
            val releases = repository.getNotPurchasedComicsReleases(releaseDateFrom, releaseDateTo)

            if (releases.isEmpty()) {
                LogHelperKt.d { "There aren't releases to notify" }
                return@context Result.success()
            }

            LogHelperKt.d { "There are ${releases.size} releases to notify" }

            val intent = createNotificationIntent(context)
            var id = Constants.NOTIFICATION_GROUP_ID
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            produceNotificationFromReleases(context, releases, intent) {
                notificationManager.notify(++id, it)
            }
            val summaryNotification = createSummaryNotification(context, releases, intent)
            notificationManager.notify(Constants.NOTIFICATION_GROUP_ID, summaryNotification)

            return@context Result.success()
        } catch (ex: Exception) {
            LogHelperKt.e("Error in release notification", ex)
            return@context Result.failure()
        }
    }

    private fun createNotificationIntent(context: Context): PendingIntent {
        val resultIntent = Intent(context, MainActivity::class.java)
        val stackBuilder = TaskStackBuilder.create(context)
            .addNextIntentWithParentStack(resultIntent)

        val pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
        return pendingIntent!!
    }

    private inline fun produceNotificationFromReleases(
        context: Context,
        releases: List<ComicsRelease>,
        intent: PendingIntent,
        onProduced: (Notification) -> Unit
    ) {
        for (release in releases) {
            val notification = createNotificationFromRelease(context, release, intent)
            onProduced(notification)
        }
    }

    private fun createNotificationFromRelease(
        context: Context,
        comicsRelease: ComicsRelease,
        intent: PendingIntent
    ): Notification {
        val comics = comicsRelease.comics
        val release = comicsRelease.release
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher5f)
            .setContentTitle(comics.name)
            .setContentText(release.getContentText(context))
            .setNumber(release.number) // TODO: non appare da nessuna parte!
            .setAutoCancel(true)
            .setContentIntent(intent)
            .setGroup(Constants.NOTIFICATION_GROUP)

        if (comics.hasImage()) {
            val largeIcon = Glide.with(context)
                .asBitmap()
                .load(Uri.parse(comics.image))
                .apply(RequestOptions.circleCropTransform())
                .submit()
                .get()

            builder.setLargeIcon(largeIcon)
        }

        return builder.build()
    }

    private fun createSummaryNotification(
        context: Context,
        releases: List<ComicsRelease>,
        intent: PendingIntent
    ): Notification {
        val count = releases.size
        val contextText = context.resources.getQuantityString(
            R.plurals.notification_new_releases_today,
            count,
            count
        )
        val inboxStyle = InboxStyle().with(context, releases).setBigContentTitle(contextText)

        return Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher5f) // TODO: icona app
            .setContentTitle(context.getText(R.string.notification_new_releases))
            .setContentText(contextText)
            .setStyle(inboxStyle)
            .setAutoCancel(true)
            .setContentIntent(intent)
            .setGroup(Constants.NOTIFICATION_GROUP)
            .setGroupSummary(true)
            .build()
    }

    private fun Release.getContentText(context: Context) =
        if (hasNotes()) context.getString(
            R.string.notification_new_release_detail_notes,
            number,
            notes
        )
        else context.getString(R.string.notification_new_release_detail, number)

    private fun InboxStyle.with(context: Context, releases: List<ComicsRelease>): InboxStyle {
        for ((comics, release) in releases) {
            addLine(
                context.getString(
                    R.string.notification_new_release_complete,
                    comics.name,
                    release.number
                )
            )
        }
        return this
    }

    companion object {
        const val CHANNEL_ID = "it.amonshore.comikkua.CHANNEL_RELEASES"
        val WORK_NAME: String = ReleasesNotificationWorker::class.java.name
    }
}