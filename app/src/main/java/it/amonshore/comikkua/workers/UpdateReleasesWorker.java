package it.amonshore.comikkua.workers;

import android.content.Context;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.web.CmkWebRepository;

public class UpdateReleasesWorker extends Worker {

    private final static String WORK_NAME = UpdateReleasesWorker.class.getName();

    public UpdateReleasesWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            final Context context = getApplicationContext();
//            final ComicsDao comicsDao = ComikkuDatabase.getDatabase(context).comicsDao();
//            final CmkWebRepository cmkWebRepository = new CmkWebRepository(context);
//
//            final List<ComicsWithReleases> ccs = comicsDao.getRawComicsWithReleases();
//
//            for (ComicsWithReleases cs : ccs) {
//                final Release release = cs.getLastRelease();
//
//                cmkWebRepository.getReleases()
//            }

            // TODO: per ogni testata richiedere la nuova release

            return null;
        } catch (Exception ex) {
            LogHelper.e("UpdateReleasesWorker.doWork error", ex);
            return Result.failure();
        }
    }

    public static void setup(@NonNull Context context, boolean enabled) {
        final WorkManager workManager = WorkManager.getInstance(context);

        if (enabled) {
            final OneTimeWorkRequest work = new OneTimeWorkRequest.Builder(UpdateReleasesWorker.class)
                    .build();
            workManager.enqueue(work);

            // TODO: creare work periodico
        } else {
            workManager.cancelUniqueWork(WORK_NAME);
        }
    }
}
