package it.amonshore.comikkua.data.comics;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import it.amonshore.comikkua.ICallback2;
import it.amonshore.comikkua.data.ComikkuDatabase;

class ComicsRepository {

    private ComicsDao mComicsDao;

    ComicsRepository(Application application) {
        final ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mComicsDao = db.comicsDao();
    }

    LiveData<List<Comics>> getComics() {
        return mComicsDao.getComics();
    }

    LiveData<Comics> getComics(long id) {
        return mComicsDao.getComics(id);
    }

    LiveData<Comics> getComics(String name) {
        return mComicsDao.getComics(name);
    }

    LiveData<ComicsWithReleases> getComicsWithReleases(long id) {
        return mComicsDao.getComicsWithReleases(id);
    }

    DataSource.Factory<Integer, ComicsWithReleases> getComicsWithReleasesFactory() {
        return mComicsDao.getComicsWithReleasesFactory();
    }

    DataSource.Factory<Integer, ComicsWithReleases> getComicsWithReleasesFactory(String likeName) {
        return mComicsDao.getComicsWithReleasesFactory(likeName);
    }

    LiveData<List<String>> getPublishers() {
        return mComicsDao.getPublishers();
    }

    LiveData<List<String>> getAuthors() {
        return mComicsDao.getAuthors();
    }

    LiveData<List<String>> getComicsName() {
        return mComicsDao.getComicsName();
    }

    void insert(Comics comics) {
        new InsertAsyncTask(mComicsDao).execute(comics);
    }

    long insertSync(Comics comics) {
        return mComicsDao.insert(comics);
    }

    void update(Comics comics) {
        new UpdateAsyncTask(mComicsDao).execute(comics);
    }

    int updateSync(Comics comics) {
        return mComicsDao.update(comics);
    }

    void delete(long id) {
        new DeleteAsyncTask(mComicsDao).execute(id);
    }

    void delete(Long... id) {
        new DeleteAsyncTask(mComicsDao).execute(id);
    }

    void deleteAll() {
        new DeleteAllAsyncTask(mComicsDao).execute();
    }

    void remove(Long... id) {
        new RemoveAsyncTask(mComicsDao, RemoveAsyncTask.REMOVE, null)
                .execute(id);
    }

    void remove(Long[] id, ICallback2<Long[], Integer> callback) {
        new RemoveAsyncTask(mComicsDao, RemoveAsyncTask.REMOVE, callback)
                .execute(id);
    }

    void undoRemoved() {
        new RemoveAsyncTask(mComicsDao, RemoveAsyncTask.UNDO, null)
                .execute();
    }

    void deleteRemoved() {
        new RemoveAsyncTask(mComicsDao, RemoveAsyncTask.DELETE, null)
                .execute();
    }

    private static class InsertAsyncTask extends AsyncTask<Comics, Void, Void> {

        private ComicsDao mAsyncTaskDao;

        InsertAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Comics... params) {
            mAsyncTaskDao.insert(params);
            return null;
        }
    }

    private static class UpdateAsyncTask extends AsyncTask<Comics, Void, Void> {

        private ComicsDao mAsyncTaskDao;

        UpdateAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Comics... params) {
            mAsyncTaskDao.update(params);
            return null;
        }
    }

    private static class DeleteAsyncTask extends AsyncTask<Long, Void, Integer> {

        private ComicsDao mAsyncTaskDao;

        DeleteAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Integer doInBackground(final Long... params) {
            return mAsyncTaskDao.delete(params);
        }
    }

    private static class DeleteAllAsyncTask extends AsyncTask<Void, Void, Integer> {

        private ComicsDao mAsyncTaskDao;

        DeleteAllAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Integer doInBackground(final Void... params) {
            return mAsyncTaskDao.deleteAll();
        }
    }

    private static class RemoveAsyncTask extends AsyncTask<Long, Void, Void> {

        final static int REMOVE = 0;
        final static int UNDO = 1;
        final static int DELETE = 2;

        private ComicsDao mAsyncTaskDao;
        private ICallback2<Long[], Integer> mCallback;
        private int mAction;

        RemoveAsyncTask(ComicsDao dao, int action, ICallback2<Long[], Integer> callback) {
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
                mCallback.onCallback(params, count);
            }

            return null;
        }
    }
}
