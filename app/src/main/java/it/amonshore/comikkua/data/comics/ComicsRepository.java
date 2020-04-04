package it.amonshore.comikkua.data.comics;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import it.amonshore.comikkua.data.ComikkuDatabase;

public class ComicsRepository {

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

    public void insert(Comics comics) {
        new InsertAsyncTask(mComicsDao).execute(comics);
    }

    public long insertSync(Comics comics) {
        return mComicsDao.insert(comics);
    }

    public void update(Comics comics) {
        new UpdateAsyncTask(mComicsDao).execute(comics);
    }

    public int updateSync(Comics comics) {
        return mComicsDao.update(comics);
    }

    public void delete(long id) {
        new DeleteAsyncTask(mComicsDao).execute(id);
    }

    public void delete(Long... id) {
        new DeleteAsyncTask(mComicsDao).execute(id);
    }

    public void deleteAll() {
        new DeleteAllAsyncTask(mComicsDao).execute();
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

    private static class DeleteAsyncTask extends AsyncTask<Long, Void, Void> {

        private ComicsDao mAsyncTaskDao;

        DeleteAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            mAsyncTaskDao.delete(params);
            return null;
        }
    }

    private static class DeleteAllAsyncTask extends AsyncTask<Void, Void, Void> {

        private ComicsDao mAsyncTaskDao;

        DeleteAllAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mAsyncTaskDao.deleteAll();
            return null;
        }
    }
}
