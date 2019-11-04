package it.amonshore.comikkua.data;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Size;
import androidx.lifecycle.LiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

public class ReleaseRepository {

    private ReleaseDao mReleaseDao;

    ReleaseRepository(Application application) {
        final ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mReleaseDao = db.releaseDao();
    }

    LiveData<List<Release>> getReleases(int comicsId) {
        return mReleaseDao.getReleases(comicsId);
    }

    LiveData<List<ComicsRelease>> getAllReleases(@NonNull @Size(6) String refDate,
                                                 long retainStart) {
        return mReleaseDao.getAllReleases(refDate, retainStart);
    }

    public void insert (Release release) {
        new ReleaseRepository.insertAsyncTask(mReleaseDao).execute(release);
    }

    public void update(Release release) {
        new updateAsyncTask(mReleaseDao).execute(release);
    }

    public void delete(Long... id) {
        new deleteAsyncTask(mReleaseDao).execute(id);
    }

    private static class insertAsyncTask extends AsyncTask<Release, Void, Void> {

        private ReleaseDao mAsyncTaskDao;

        insertAsyncTask(ReleaseDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Release... params) {
            mAsyncTaskDao.insert(params[0]);
            return null;
        }
    }

    private static class updateAsyncTask extends AsyncTask<Release, Void, Void> {

        private ReleaseDao mAsyncTaskDao;

        updateAsyncTask(ReleaseDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Release... params) {
            mAsyncTaskDao.update(params);
            return null;
        }
    }

    private static class deleteAsyncTask extends AsyncTask<Long, Void, Void> {

        private ReleaseDao mAsyncTaskDao;

        deleteAsyncTask(ReleaseDao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Long... params) {
            mAsyncTaskDao.delete(params);
            return null;
        }
    }

}
