package it.amonshore.comikkua.data;

import android.app.Application;
import android.os.AsyncTask;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;

public class ReleaseRepository {

    private ReleaseDao mReleaseDao;
    public final LiveData<PagedList<ComicsRelease>> comicsReleaseList;

    ReleaseRepository(Application application) {
        ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mReleaseDao = db.releaseDao();

//        final ReleaseDataSourceFactory factory = new ReleaseDataSourceFactory(mReleaseDao);
        final PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(20)
                .setPageSize(20)
                .setEnablePlaceholders(false)
                .build();

//        comicsReleaseList = new LivePagedListBuilder<>(factory, config).build();
        comicsReleaseList = new LivePagedListBuilder<>(mReleaseDao.allReleases(), config).build();
    }

    LiveData<List<Release>> getReleases(int comicsId) {
        return mReleaseDao.getReleases(comicsId);
    }

    public void insert (Release release) {
        new ReleaseRepository.insertAsyncTask(mReleaseDao).execute(release);
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
