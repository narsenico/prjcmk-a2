package it.amonshore.comikkua.workers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.Resource;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseDao;
import it.amonshore.comikkua.data.web.CmkWebRelease;
import it.amonshore.comikkua.data.web.CmkWebRepository;

import static androidx.lifecycle.Lifecycle.State.DESTROYED;

public class UpdateReleasesWorker extends Worker {

    private final static String WORK_NAME = UpdateReleasesWorker.class.getName();
    private final static String KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled";

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
            final CmkWebRepository cmkWebRepository = new CmkWebRepository(context);
            final Handler handler = new Handler(Looper.getMainLooper());

            // le operazioni di inserimento su DB verranno eseguite da questo executor
            final ExecutorService dbExecutor = Executors.newFixedThreadPool(10);
            final CompletionService<Void> completionService = new ExecutorCompletionService<>(dbExecutor);

            // estraggo tutte le testate e per ognuna di esse cerco se ci sono nuove uscite
            final List<ComicsWithReleases> ccs = comicsDao.getRawComicsWithReleases();

            for (ComicsWithReleases cs : ccs) {
                // observe deve essere esguita nel main thread
                handler.post(() -> observe(cmkWebRepository.getReleases(cs.comics.name, cs.getNextReleaseNumber()),
                        cs, releaseDao, completionService));
            }

            // mi aspetto che per ogni titolo venga eseguita una operazione (sia in caso di successo che di errore)
            LogHelper.d("%s waiting for %s future/s", WORK_NAME, ccs.size());
            try {
                for (int ii = 0; ii < ccs.size(); ii++) {
                    Future<Void> f = completionService.poll(10, TimeUnit.SECONDS);
                    try {
                        f.get(10, TimeUnit.SECONDS);
                    } catch (TimeoutException tex) {
                        //
                    }
                }
            } catch (InterruptedException iex) {
                LogHelper.e("UpdateReleasesWorker.doWork waiting completions", iex);
            }

            LogHelper.d("%s shutdown", WORK_NAME);

            dbExecutor.shutdown();
            dbExecutor.awaitTermination(10, TimeUnit.SECONDS);

            LogHelper.d("%s end", WORK_NAME);

            return Result.success();
        } catch (Exception ex) {
            LogHelper.e("UpdateReleasesWorker.doWork error", ex);
            return Result.failure();
        }
    }

    @MainThread
    private void observe(final CustomData<List<CmkWebRelease>> source,
                         final ComicsWithReleases comics,
                         final ReleaseDao releaseDao,
                         final CompletionService<Void> completionService) {
        final Observer<Resource<List<CmkWebRelease>>> observer = new Observer<Resource<List<CmkWebRelease>>>() {
            @Override
            public void onChanged(final Resource<List<CmkWebRelease>> resource) {
                switch (resource.status) {
                    case SUCCESS:
                        completionService.submit(() -> {
                            if (resource.data != null && resource.data.size() > 0) {
                                for (CmkWebRelease cwr : resource.data) {
                                    final Release release = Release.from(comics.comics.id, cwr);
                                    releaseDao.insert(release);
                                    LogHelper.i("%s '%s' new release #%s", WORK_NAME,
                                            comics.comics.name, release.number);
                                }
                            }
                        }, null);
                        source.removeObserver(this);
                        break;
                    case ERROR:
                        completionService.submit(() -> {
                            //
                        }, null);
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

        boolean enabled;

        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        enabled = sharedPreferences.getBoolean(KEY_AUTO_UPDATE_ENABLED, false);
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
                    .setRequiredNetworkType(NetworkType.CONNECTED)
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
