package it.amonshore.comikkua.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;

public class ReleaseDataSourceFactory extends DataSource.Factory<String, ComicsRelease> {

    private ReleaseDao mDao;
    private MutableLiveData<ReleaseDataSource> mSourceLiveData =
            new MutableLiveData<>();
    private ReleaseDataSource mLatestSource;

    public ReleaseDataSourceFactory(@NonNull ReleaseDao dao) {
        mDao = dao;
        mSourceLiveData = new MutableLiveData<>();
    }

    @NonNull
    @Override
    public DataSource<String, ComicsRelease> create() {
        mLatestSource = new ReleaseDataSource(mDao);
        mSourceLiveData.postValue(mLatestSource);
        return mLatestSource;
    }
}
