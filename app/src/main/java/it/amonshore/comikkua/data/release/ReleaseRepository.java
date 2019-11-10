package it.amonshore.comikkua.data.release;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.lifecycle.LiveData;
import it.amonshore.comikkua.data.ComikkuDatabase;

public class ReleaseRepository {

    private ReleaseDao mReleaseDao;

    ReleaseRepository(Application application) {
        final ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mReleaseDao = db.releaseDao();
    }

    LiveData<List<Release>> getReleases(int comicsId) {
        return mReleaseDao.getReleases(comicsId);
    }

    LiveData<List<ComicsRelease>> getAllReleases(@NonNull @Size(8) String refDate,
                                                 @NonNull @Size(8) String refNextDate,
                                                 @NonNull @Size(8) String refOtherDate,
                                                 long retainStart) {
        return mReleaseDao.getAllReleases(refDate, refNextDate, refOtherDate, retainStart);
    }

    public LiveData<Release> getRelease(long id) {
        return mReleaseDao.getRelease(id);
    }

    public void insert (Release release) {
        new InsertAsyncTask(mReleaseDao).execute(release);
    }

    public void update(Release release) {
        new UpdateAsyncTask(mReleaseDao).execute(release);
    }

    public void updatePurchased(boolean purchased, long lastUpdate, Long... id) {
        new UpdatePurchasedAsyncTask(mReleaseDao, purchased, lastUpdate).execute(id);
    }

    public void togglePurchased(long lastUpdate, Long... id) {
        new TogglePurchasedAsyncTask(mReleaseDao, lastUpdate).execute(id);
    }

    public void updateOrdered(boolean ordered, long lastUpdate, Long... id) {
        new UpdateOrderedAsyncTask(mReleaseDao, ordered, lastUpdate).execute(id);
    }

    public void toggleOrdered(long lastUpdate, Long... id) {
        new ToggleOrderedAsyncTask(mReleaseDao, lastUpdate).execute(id);
    }

    public void delete(Long... id) {
        new DeleteAsyncTask(mReleaseDao).execute(id);
    }

    private static class InsertAsyncTask extends AsyncTask<Release, Void, Void> {

        private ReleaseDao mAsyncTaskDao;

        InsertAsyncTask(ReleaseDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Release... params) {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }

    private static class UpdateAsyncTask extends AsyncTask<Release, Void, Void> {

        private ReleaseDao mAsyncTaskDao;

        UpdateAsyncTask(ReleaseDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Release... params) {
            mAsyncTaskDao.update(params);
            return null;
        }
    }

    private static class UpdatePurchasedAsyncTask extends AsyncTask<Long, Void, Void> {

        private ReleaseDao mAsyncTaskDao;
        private boolean mPurchased;
        private long mLastUpdate;

        UpdatePurchasedAsyncTask(ReleaseDao dao, boolean purchased, long lastUpdate) {
            mAsyncTaskDao = dao;
            mPurchased = purchased;
            mLastUpdate = lastUpdate;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            mAsyncTaskDao.updatePurchased(mPurchased, mLastUpdate, params);
            return null;
        }
    }

    private static class TogglePurchasedAsyncTask extends AsyncTask<Long, Void, Void> {

        private ReleaseDao mAsyncTaskDao;
        private long mLastUpdate;

        TogglePurchasedAsyncTask(ReleaseDao dao, long lastUpdate) {
            mAsyncTaskDao = dao;
            mLastUpdate = lastUpdate;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            if (params.length > 0) {
                final Release release = mAsyncTaskDao.getRawRelease(params[0]);
                mAsyncTaskDao.updatePurchased(!release.purchased, mLastUpdate, params);
            }
            return null;
        }
    }

    private static class UpdateOrderedAsyncTask extends AsyncTask<Long, Void, Void> {

        private ReleaseDao mAsyncTaskDao;
        private boolean mOrdered;
        private long mLastUpdate;

        UpdateOrderedAsyncTask(ReleaseDao dao, boolean ordered, long lastUpdate) {
            mAsyncTaskDao = dao;
            mOrdered = ordered;
            mLastUpdate = lastUpdate;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            mAsyncTaskDao.updateOrdered(mOrdered, mLastUpdate, params);
            return null;
        }
    }

    private static class ToggleOrderedAsyncTask extends AsyncTask<Long, Void, Void> {

        private ReleaseDao mAsyncTaskDao;
        private long mLastUpdate;

        ToggleOrderedAsyncTask(ReleaseDao dao, long lastUpdate) {
            mAsyncTaskDao = dao;
            mLastUpdate = lastUpdate;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            if (params.length > 0) {
                final Release release = mAsyncTaskDao.getRawRelease(params[0]);
                mAsyncTaskDao.updateOrdered(!release.ordered, mLastUpdate, params);
            }
            return null;
        }
    }

    private static class DeleteAsyncTask extends AsyncTask<Long, Void, Void> {

        private ReleaseDao mAsyncTaskDao;

        DeleteAsyncTask(ReleaseDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            mAsyncTaskDao.delete(params);
            return null;
        }
    }

}
