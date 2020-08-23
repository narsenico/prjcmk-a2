package it.amonshore.comikkua.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.bumptech.glide.request.RequestOptions;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.ui.MainActivity;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static it.amonshore.comikkua.Constants.NOTIFICATION_GROUP;
import static it.amonshore.comikkua.Constants.NOTIFICATION_GROUP_ID;

public class ReleaseNotificationWorker extends Worker {

    private final static String CHANNEL_ID = "it.amonshore.comikkua.CHANNEL_RELEASES";
    private final static String WORK_NAME = ReleaseNotificationWorker.class.getName();

    public final static String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

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

                    final String text;
                    if (cr.release.hasNotes()) {
                        text = context.getString(R.string.notification_new_release_detail_notes, cr.release.number, cr.release.notes);
                    } else {
                        text = context.getString(R.string.notification_new_release_detail, cr.release.number);
                    }

                    final NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher5f) // TODO: icona app
                            .setContentTitle(cr.comics.name)
                            .setContentText(text)
                            .setNumber(cr.release.number) // TODO: non appare da nessuna parte!
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .setContentIntent(resultPendingIntent)
                            .setGroup(NOTIFICATION_GROUP);

                    // TODO: le imamagini sono supportate solo da una certa versione in poi
                    //  inutile generarle per quelle precedenti
                    if (cr.comics.hasImage()) {
                        final FutureTarget<Bitmap> future = Glide.with(context)
                                .asBitmap()
                                .load(Uri.parse(cr.comics.image))
                                .apply(RequestOptions.circleCropTransform())
                                .submit();

                        notification.setLargeIcon(future.get());
                    }

                    notificationManager.notify(++id, notification.build());

                    inboxStyle.addLine(context.getString(R.string.notification_new_release_complete, cr.comics.name, cr.release.number));
                }

//                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                final String contextText = context.getResources().getQuantityString(R.plurals.notification_new_releases_today, count, count);

                // e una per il sommario
                notificationManager.notify(NOTIFICATION_GROUP_ID, new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_launcher5f) // TODO: icona app
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
//                }
            }

            return Result.success();
        } catch (Exception ex) {
            LogHelper.e("ReleaseNotificationWorker.doWork error", ex);
            return Result.failure();
        }
    }

    public static void setup(@NonNull final Context context, @NonNull LifecycleOwner lifecycleOwner) {
        if (lifecycleOwner.getLifecycle().getCurrentState() == DESTROYED) {
            return;
        }

        boolean enabled;

        // con Android O è obbligatorio usare un canale per le notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.today_releases_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.today_releases_channel_description));

            final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            // recupero l'abilitazione alle notifiche direttamente dai settings dell'app
            enabled = notificationManager.areNotificationsEnabled();
        } else {
            final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            enabled = sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ENABLED, false);
            // rimango in ascolto dei cambiamenti da SettingsFragment
            final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences1, key) -> {
                if (key.equals(KEY_NOTIFICATIONS_ENABLED)) {
                    setupWorker(context, sharedPreferences1.getBoolean(KEY_NOTIFICATIONS_ENABLED, false));
                }
            };
            // il listener è tolto quando l'app non è più attiva, e rimessa quando ritorna attiva
            lifecycleOwner.getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
                switch (event) {
                    case ON_PAUSE:
                        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener);
                        break;
                    case ON_RESUME:
                        sharedPreferences.registerOnSharedPreferenceChangeListener(listener);
                        listener.onSharedPreferenceChanged(sharedPreferences, KEY_NOTIFICATIONS_ENABLED);
                        break;
                }
            });
        }

        setupWorker(context, enabled);
    }

    private static void setupWorker(@NonNull Context context, boolean enabled) {
        final WorkManager workManager = WorkManager.getInstance(context);

        LogHelper.d("%s enabled=%s", WORK_NAME, enabled);

        if (enabled) {
            // calcolo il delay al prossimo 08:00AM
            final Calendar now = Calendar.getInstance(Locale.getDefault());
            final Calendar morning = Calendar.getInstance(Locale.getDefault());
            if (now.get(Calendar.HOUR_OF_DAY) > 8) {
                morning.add(Calendar.DAY_OF_MONTH, 1);
            }
            morning.set(Calendar.HOUR_OF_DAY, 8);
            morning.set(Calendar.MINUTE, 0);
            morning.set(Calendar.SECOND, 0);

            final long delay = morning.getTimeInMillis() - now.getTimeInMillis();

            // una volta al giorno, alle 8AM, vado a controllare i dati per visualizzare delle notifiche
            final PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(ReleaseNotificationWorker.class, 1, TimeUnit.DAYS)
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .build();

            workManager.enqueueUniquePeriodicWork(WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP, // mantengo l'eventuale work già schedulato con lo stesso nome
                    work);
        } else {
            workManager.cancelUniqueWork(WORK_NAME);
        }
    }
}
