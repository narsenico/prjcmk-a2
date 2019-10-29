package it.amonshore.comikkua.data;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.lifecycle.LiveData;

public class ReleaseRepository {

    private ReleaseDao mReleaseDao;

    ReleaseRepository(Application application) {
        ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mReleaseDao = db.releaseDao();
    }

    LiveData<List<Release>> getReleases(int comicsId) {
        return mReleaseDao.getReleases(comicsId);
    }

    public void insert (Release release) {
        new ReleaseRepository.insertAsyncTask(mReleaseDao).execute(release);
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
}
