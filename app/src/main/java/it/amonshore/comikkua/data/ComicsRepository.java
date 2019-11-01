package it.amonshore.comikkua.data;

import android.app.Application;
import android.os.AsyncTask;
import android.text.TextUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

public class ComicsRepository {

    private ComicsDao mComicsDao;
    final LiveData<PagedList<ComicsWithReleases>> comicsWithReleasesList;
    final MutableLiveData<String> search = new MutableLiveData<>();

    ComicsRepository(Application application) {
        ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mComicsDao = db.comicsDao();

        final PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(20)
                .setPageSize(20)
                .setEnablePlaceholders(true)
                .build();

        comicsWithReleasesList = Transformations.switchMap(search, input -> {
           if (TextUtils.isEmpty(input) || input.equals("%%")) {
               return new LivePagedListBuilder<>(mComicsDao.comicsWithReleases(), config).build();
           } else {
               return new LivePagedListBuilder<>(mComicsDao.comicsWithReleases(input), config).build();
           }
        });

//        comicsWithReleasesList = new LivePagedListBuilder<>(
//                mComicsDao.comicsWithReleases(), config)
//                .build();


    }

    LiveData<List<Comics>> getComics() {
        return mComicsDao.getComics();
    }

    LiveData<Comics> getComics(long id) {
        return mComicsDao.getComics(id);
    }

    LiveData<List<ComicsWithReleases>> getComicsWithReleases() {
        return mComicsDao.getComicsWithReleases();
    }

    LiveData<ComicsWithReleases> getComicsWithReleases(long id) {
        return mComicsDao.getComicsWithReleases(id);
    }

    public void insert(Comics comics) {
        new insertAsyncTask(mComicsDao).execute(comics);
    }

    public void update(Comics comics) {
        new updateAsyncTask(mComicsDao).execute(comics);
    }

    public void delete(long id) {
        new deleteAsyncTask(mComicsDao).execute(id);
    }

    public void delete(Long... id) {
        new deleteAsyncTask(mComicsDao).execute(id);
    }

    public void deleteAll() {
        new deleteAllAsyncTask(mComicsDao).execute();
    }

    private static class insertAsyncTask extends AsyncTask<Comics, Void, Void> {

        private ComicsDao mAsyncTaskDao;

        insertAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Comics... params) {
            mAsyncTaskDao.insert(params);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<Comics, Void, Void> {

        private ComicsDao mAsyncTaskDao;

        updateAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Comics... params) {
            mAsyncTaskDao.update(params);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<Long, Void, Void> {

        private ComicsDao mAsyncTaskDao;

        deleteAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            mAsyncTaskDao.delete(params);
            return null;
        }
    }

    private static class deleteAllAsyncTask extends AsyncTask<Void, Void, Void> {

        private ComicsDao mAsyncTaskDao;

        deleteAllAsyncTask(ComicsDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mAsyncTaskDao.deleteAll();
            return null;
        }
    }
}
