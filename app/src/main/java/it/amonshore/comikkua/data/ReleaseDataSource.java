package it.amonshore.comikkua.data;

import androidx.annotation.NonNull;
import androidx.paging.PageKeyedDataSource;

public class ReleaseDataSource extends PageKeyedDataSource<String, ComicsRelease> {

    private ReleaseDao mReleasesDao;

    public ReleaseDataSource(@NonNull ReleaseDao dao) {
        mReleasesDao = dao;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<String> params,
                            @NonNull LoadInitialCallback<String, ComicsRelease> callback) {

    }

    @Override
    public void loadBefore(@NonNull LoadParams<String> params,
                           @NonNull LoadCallback<String, ComicsRelease> callback) {

    }

    @Override
    public void loadAfter(@NonNull LoadParams<String> params,
                          @NonNull LoadCallback<String, ComicsRelease> callback) {

    }
}
