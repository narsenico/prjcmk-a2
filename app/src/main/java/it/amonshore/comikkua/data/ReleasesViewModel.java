package it.amonshore.comikkua.data;

import android.app.Application;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.paging.PagedList;
import it.amonshore.comikkua.Utility;

public class ReleasesViewModel extends AndroidViewModel {

    private ReleaseRepository mRepository;
    public final LiveData<PagedList<ComicsRelease>> comicsReleaseList;

    public ReleasesViewModel (Application application) {
        super(application);
        mRepository = new ReleaseRepository(application);
        comicsReleaseList = mRepository.comicsReleaseList;
    }

    LiveData<List<Release>> getReleases(int comicsId) { return mRepository.getReleases(comicsId); }

    public void insert(Release release) { mRepository.insert(release); }

    public void delete(Iterable<Long> ids) {
        mRepository.delete(Utility.toArray(ids));
    }

}
