package it.amonshore.comikkua.ui;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.release.ComicsRelease;

import static it.amonshore.comikkua.NotificationUtils.CHANNEL_ID;

public class ReleaseNotificationWorker extends Worker {

    private final static String NOTIFICATION_GROUP = "it.amonshore.comikkua.RELEASE_NOTIFICATION";
    private final static int NOTIFICATION_GROUP_ID = 1;

    public ReleaseNotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            final Context context = getApplicationContext();

            // TODO: per oggi? o domani?
            // recupero le release per oggi
            final String date = DateFormatterHelper.timeToString8(System.currentTimeMillis());
            final List<ComicsRelease> lst = ComikkuDatabase.getDatabase(context).releaseDao()
                    .getRawNotPurchasedReleases(date, date);
            final int count = lst.size();

            if (count > 0) {
                final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

                // creo l'intent per l'activity
                final Intent resultIntent = new Intent(context, MainActivity.class);
                final TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
                stackBuilder.addNextIntentWithParentStack(resultIntent);
                final PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                final NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();

                // TODO: per evitare di sovrascrivere notifiche vecchie ancora presenti nella home, usare un progressivo globale
                //  da memorizzare nelle sharedPreferences
                int id = NOTIFICATION_GROUP_ID;
                // creo una singola notifica per ogni release
                for (ComicsRelease cr : lst) {
                    notificationManager.notify(++id, new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_release)
                            .setContentTitle(cr.comics.name)
                            .setContentText(context.getString(R.string.notification_new_release_detail, cr.release.number))
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .setContentIntent(resultPendingIntent)
                            .setGroup(NOTIFICATION_GROUP)
                            .build());

                    inboxStyle.addLine(context.getString(R.string.notification_new_release_complete, cr.comics.name, cr.release.number));
                }

                final String contextText = context.getResources().getQuantityString(R.plurals.notification_new_releases_today, count, count);

                // e una per il sommario
                notificationManager.notify(NOTIFICATION_GROUP_ID, new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_release)
                        .setContentTitle(context.getText(R.string.notification_new_releases))
                        .setContentText(contextText)
                        .setStyle(inboxStyle
                            .setBigContentTitle(contextText))
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setContentIntent(resultPendingIntent)
                        .setGroup(NOTIFICATION_GROUP)
                        .setGroupSummary(true)
                        .build());
            }

            return Result.success();
        } catch (Exception ex) {
            LogHelper.e("ReleaseNotificationWorker.doWork error", ex);
            return Result.failure();
        }
    }
}
