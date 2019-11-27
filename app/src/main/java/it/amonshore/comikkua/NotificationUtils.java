package it.amonshore.comikkua;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceManager;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import it.amonshore.comikkua.ui.ReleaseNotificationWorker;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;

public final class NotificationUtils {

    public final static String CHANNEL_ID = "it.amonshore.comikkua.CHANNEL_RELEASES";
    private final static String NOTIFICATION_WORK_NAME = "it.amonshore.comikkua.RELEASE_NOTIFICATION_WORK";
    private final static String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";

    public static void setupNotifications(@NonNull final Context context, @NonNull LifecycleOwner lifecycleOwner) {
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
            enabled = sharedPreferences.getBoolean("notifications_enabled", false);
            // rimango in ascolto dei cambiamenti da SettingsFragment
            final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences1, key) -> {
                if (key.equals(KEY_NOTIFICATIONS_ENABLED)) {
                    setupNotificationWork(context, sharedPreferences1.getBoolean(KEY_NOTIFICATIONS_ENABLED, false));
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

        setupNotificationWork(context, enabled);
    }

    private static void setupNotificationWork(@NonNull Context context, boolean enabled) {
        final WorkManager workManager = WorkManager.getInstance(context);

        LogHelper.d("setupNotificationWork enabled=" + enabled);

        if (enabled) {
//            if (BuildConfig.DEBUG) {
//                final OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(ReleaseNotificationWorker.class)
//                        .build();
//
//                workManager.enqueue(work);
//            } else {
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

                workManager.enqueueUniquePeriodicWork(NOTIFICATION_WORK_NAME,
                        ExistingPeriodicWorkPolicy.KEEP, // mantengo l'eventuale work già schedulato con lo stesso nome
                        work);
//            }
        } else {
            workManager.cancelUniqueWork(NOTIFICATION_WORK_NAME);
        }
    }
}
