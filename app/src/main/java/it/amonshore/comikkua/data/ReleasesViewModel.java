package it.amonshore.comikkua.data;

import android.app.Application;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

public class ReleasesViewModel extends AndroidViewModel {

    private ReleaseRepository mRepository;

    public ReleasesViewModel (Application application) {
        super(application);
        mRepository = new ReleaseRepository(application);
    }

    LiveData<List<Release>> getReleases(int comicsId) { return mRepository.getReleases(comicsId); }

    public void insert(Release release) { mRepository.insert(release); }
}
