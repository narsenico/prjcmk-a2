package it.amonshore.comikkua.workers;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.navigation.NavDeepLinkBuilder;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.Resource;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseDao;
import it.amonshore.comikkua.data.web.CmkWebRelease;
import it.amonshore.comikkua.data.web.FirebaseRepository;
import it.amonshore.comikkua.ui.releases.NewReleasesFragmentArgs;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;
import static it.amonshore.comikkua.Constants.NOTIFICATION_GROUP;

public class UpdateReleasesWorker extends Worker {

    private final static String CHANNEL_ID = "it.amonshore.comikkua.CHANNEL_AUTO_UPDATE";
    private final static String WORK_NAME = UpdateReleasesWorker.class.getName();
    private final static String KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled";
    private static final int NOTIFICATION_ID = 150;

    public static final String PREVENT_NOTIFICATION = "prevent_notification";
    public static final String RELEASE_COUNT = "release_count";
    public static final String RELEASE_TAG = "release_tag";

    public UpdateReleasesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            final Context context = getApplicationContext();
            final ComikkuDatabase db = ComikkuDatabase.getDatabase(context);
            final ComicsDao comicsDao = db.comicsDao();
            final ReleaseDao releaseDao = db.releaseDao();
            final FirebaseRepository firebaseRepository = new FirebaseRepository();
            final Handler handler = new Handler(Looper.getMainLooper());

            // le operazioni di inserimento su DB verranno eseguite da questo executor
            final ExecutorService dbExecutor = Executors.newFixedThreadPool(10);
            // le operazioni ritornarno il numero di nuove release inserite
            final CompletionService<Integer> completionService = new ExecutorCompletionService<>(dbExecutor);

            // estraggo tutte le testate e per ognuna di esse cerco se ci sono nuove uscite
            final List<ComicsWithReleases> ccs = comicsDao.getRawComicsWithReleases();
            // tag per identificare le release create e inserite in questo worker
            final String tag = UUID.randomUUID().toString();

            for (ComicsWithReleases cs : ccs) {
                // observe deve essere esguita nel main thread
//                handler.post(() -> observe(cmkWebRepository.getReleases(cs.comics.name, cs.getNextReleaseNumber()),
                handler.post(() -> observe(firebaseRepository.getReleases(cs.comics.name, cs.getNextReleaseNumber()),
                        cs, releaseDao, tag, completionService));
            }

            // mi aspetto che per ogni titolo venga eseguita una operazione (anche in caso di errore)
            // ogni operazione ritorna il numero di nuove release inserite
            LogHelper.d("%s waiting for %s future/s", WORK_NAME, ccs.size());
            int newReleasesCount = 0;
            try {
                for (int ii = 0; ii < ccs.size(); ii++) {
                    Future<Integer> futureInsert = completionService.poll(10, TimeUnit.SECONDS);
                    // se il completamento va in timeout ritorna null
                    if (futureInsert == null) {
                        LogHelper.w("Timeout updating release for '%s'", ccs.get(ii).comics.name);
                    } else {
                        try {
                            newReleasesCount += futureInsert.get(10, TimeUnit.SECONDS);
                        } catch (TimeoutException tex) {
                            //
                        }
                    }
                }
            } catch (InterruptedException iex) {
                LogHelper.e("UpdateReleasesWorker.doWork waiting completions", iex);
            }

            LogHelper.d("%s shutdown", WORK_NAME);

            dbExecutor.shutdown();
            dbExecutor.awaitTermination(10, TimeUnit.SECONDS);

            // se sono state inserite nuove release lo notifico
            if (newReleasesCount > 0 && !getInputData().getBoolean(PREVENT_NOTIFICATION, false)) {
                final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
                if (notificationManager.areNotificationsEnabled()) {
                    // creo l'intent per l'activity

//                    final Intent resultIntent = new Intent(context, MainActivity.class);
//                    final TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
//                    stackBuilder.addNextIntentWithParentStack(resultIntent);
//                    final PendingIntent resultPendingIntent =
//                            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

                    // punt direttamente al fragment con le novità, passando il tag come argomento
                    final PendingIntent resultPendingIntent = new NavDeepLinkBuilder(context)
                            .setGraph(R.navigation.nav_graph)
                            .setDestination(R.id.newReleasesFragment)
                            .setArguments(new NewReleasesFragmentArgs.Builder(tag)
                                    .build().toBundle())
                            .createPendingIntent();

                    final String title = context.getResources().getQuantityString(R.plurals.notification_auto_update,
                            newReleasesCount, newReleasesCount);

                    final NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_launcher5f) // TODO: icona app
                            .setContentTitle(title)
//                            .setContentText(text)
                            .setNumber(newReleasesCount) // TODO: non appare da nessuna parte!
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setAutoCancel(true)
                            .setContentIntent(resultPendingIntent)
                            .setGroup(NOTIFICATION_GROUP);

                    notificationManager.notify(NOTIFICATION_ID, notification.build());
                }
            }

            LogHelper.d("%s end", WORK_NAME);

            // rendo disponibile in output il numero di release inserite
            // e il tag con cui individuarle facilmente
            return Result.success(new Data.Builder()
                    .putInt(RELEASE_COUNT, newReleasesCount)
                    .putString(RELEASE_TAG, tag)
                    .build());
        } catch (Exception ex) {
            LogHelper.e("UpdateReleasesWorker.doWork error", ex);
            return Result.failure();
        }
    }

    @MainThread
    private void observe(final CustomData<List<CmkWebRelease>> source,
                         final ComicsWithReleases comics,
                         final ReleaseDao releaseDao,
                         final String tag,
                         final CompletionService<Integer> completionService) {
        final Observer<Resource<List<CmkWebRelease>>> observer = new Observer<Resource<List<CmkWebRelease>>>() {
            @Override
            public void onChanged(final Resource<List<CmkWebRelease>> resource) {
                switch (resource.status) {
                    case SUCCESS:
                        completionService.submit(() -> {
                            if (resource.data != null && resource.data.size() > 0) {
                                for (CmkWebRelease cwr : resource.data) {
                                    final Release release = Release.from(comics.comics.id, cwr);
                                    release.tag = tag;
                                    releaseDao.insert(release);
                                    LogHelper.i("%s '%s' new release #%s", WORK_NAME,
                                            comics.comics.name, release.number);
                                }
                                return resource.data.size();
                            } else {
                                return 0;
                            }
                        });
                        source.removeObserver(this);
                        break;
                    case ERROR:
                        // 0 inserimenti
                        completionService.submit(() -> 0);
                        LogHelper.e("%s error=%s", WORK_NAME, resource.message);
                        source.removeObserver(this);
                        break;
                }
            }
        };

        // richiede il main thread
        source.observeForever(observer);
    }

    public static void setup(@NonNull Context context, @NonNull LifecycleOwner lifecycleOwner) {
        if (lifecycleOwner.getLifecycle().getCurrentState() == DESTROYED) {
            return;
        }

        // con Android O è obbligatorio usare un canale per le notifiche
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    context.getString(R.string.auto_update_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(context.getString(R.string.auto_update_channel_description));

            final NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean enabled = sharedPreferences.getBoolean(KEY_AUTO_UPDATE_ENABLED, false);
        // rimango in ascolto dei cambiamenti da SettingsFragment
        final SharedPreferences.OnSharedPreferenceChangeListener listener = (sharedPreferences1, key) -> {
            if (key.equals(KEY_AUTO_UPDATE_ENABLED)) {
                setupWorker(context, sharedPreferences1.getBoolean(KEY_AUTO_UPDATE_ENABLED, false));
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
                    listener.onSharedPreferenceChanged(sharedPreferences, KEY_AUTO_UPDATE_ENABLED);
                    break;
            }
        });

        setupWorker(context, enabled);
    }

    private static void setupWorker(@NonNull Context context, boolean enabled) {
        final WorkManager workManager = WorkManager.getInstance(context);

        LogHelper.d("%s enabled=%s", WORK_NAME, enabled);

        if (enabled) {
            final Constraints constraints = new Constraints.Builder()
                    // TODO: non funziona! in mancanza di connessione la richiesta rimane in ENQUEUED, mi aspetto che vada in uno stato di errore
                    //  in ogni caso Firestore usa la cache se offline quindi mi va bene togliere il constraint
//                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();

            final PeriodicWorkRequest work = new PeriodicWorkRequest.Builder(UpdateReleasesWorker.class,
                    12, TimeUnit.HOURS)
                    .setConstraints(constraints)
                    .build();

            workManager.enqueueUniquePeriodicWork(WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    work);
        } else {
            workManager.cancelUniqueWork(WORK_NAME);
        }
    }
}
