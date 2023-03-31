package it.amonshore.comikkua.data.release;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.lifecycle.LiveData;
import it.amonshore.comikkua.ICallback;
import it.amonshore.comikkua.LiveEvent;
import it.amonshore.comikkua.data.ComikkuDatabase;

public class ReleaseRepository {

    private ReleaseDao mReleaseDao;

    public ReleaseRepository(Application application) {
        final ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mReleaseDao = db.releaseDao();
    }

    LiveData<List<Release>> getReleases(int comicsId) {
        return mReleaseDao.getReleases(comicsId);
    }

    /**
     * @param refDate      data di riferimento nel formato yyyyMMdd
     * @param refNextDate  data di riferimento del periodo successivo nel formato yyyyMMdd
     * @param refOtherDate data di riferimento per altri periodi nel formato yyyyMMdd
     * @param retainStart  limite inferiore per lastUpdate in ms
     * @return elenco ordinato di release
     */
    public LiveData<List<ComicsRelease>> getComicsReleases(@NonNull @Size(8) String refDate,
                                                           @NonNull @Size(8) String refNextDate,
                                                           @NonNull @Size(8) String refOtherDate,
                                                           long retainStart) {
        return mReleaseDao.getComicsReleases(refDate, refNextDate, refOtherDate, retainStart);
    }

    LiveData<List<ComicsRelease>> getComicsReleasesByComicsId(long comicsId) {
        return mReleaseDao.getComicsReleasesByComicsId(comicsId);
    }

    public LiveData<List<ComicsRelease>> getComicsReleasesByTag(String tag) {
        return mReleaseDao.getComicsReleasesByTag(tag);
    }

    public LiveData<List<ComicsRelease>> getComicsReleases(Long... ids) {
        return mReleaseDao.getComicsReleases(ids);
    }

    LiveData<Release> getRelease(long id) {
        return mReleaseDao.getRelease(id);
    }

    void insert(Release... release) {
        new InsertAsyncTask(mReleaseDao).execute(release);
    }

    long insertSync(Release release) {
        return mReleaseDao.insert(release);
    }

    Long[] insertSync(Release... release) {
        return mReleaseDao.insert(release);
    }

    void update(Release release) {
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

    void delete(Long... id) {
        new DeleteAsyncTask(mReleaseDao).execute(id);
    }

    int deleteByNumberSync(long comicsId, int... number) {
        return mReleaseDao.deleteByNumber(comicsId, number);
    }

    void remove(Long... id) {
        new RemoveAsyncTask(mReleaseDao, RemoveAsyncTask.REMOVE, null)
                .execute(id);
    }

    public void remove(Long[] id, ICallback<Integer> callback) {
        new RemoveAsyncTask(mReleaseDao, RemoveAsyncTask.REMOVE, callback)
                .execute(id);
    }

    public void undoRemoved() {
        new RemoveAsyncTask(mReleaseDao, RemoveAsyncTask.UNDO, null)
                .execute();
    }

    public void deleteRemoved() {
        new RemoveAsyncTask(mReleaseDao, RemoveAsyncTask.DELETE, null)
                .execute();
    }

    private static class InsertAsyncTask extends AsyncTask<Release, Void, Void> {

        private ReleaseDao mAsyncTaskDao;

        InsertAsyncTask(ReleaseDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Release... params) {
            mAsyncTaskDao.insert(params);
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

    private static class RemoveAsyncTask extends AsyncTask<Long, Void, Void> {

        final static int REMOVE = 0;
        final static int UNDO = 1;
        final static int DELETE = 2;

        private ReleaseDao mAsyncTaskDao;
        private ICallback<Integer> mCallback;
        private int mAction;

        RemoveAsyncTask(ReleaseDao dao, int action, ICallback<Integer> callback) {
            mAsyncTaskDao = dao;
            mAction = action;
            mCallback = callback;
        }

        @Override
        protected Void doInBackground(Long... params) {
            int count = 0;
            if (mAction == UNDO) {
                count = mAsyncTaskDao.undoRemoved();
            } else if (mAction == DELETE) {
                count = mAsyncTaskDao.deleteRemoved();
            } else if (mAction == REMOVE) {
                count = mAsyncTaskDao.updateRemoved(true, params);
            }

            if (mCallback != null) {
                mCallback.onCallback(count);
            }

            return null;
        }
    }

}
