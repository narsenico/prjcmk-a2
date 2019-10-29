package it.amonshore.comikkua.data;

import android.app.Application;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.StreamSupport;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import it.amonshore.comikkua.Utility;

public class ComicsViewModel extends AndroidViewModel {

    private ComicsRepository mRepository;

    public ComicsViewModel (Application application) {
        super(application);
        mRepository = new ComicsRepository(application);
    }

    public LiveData<List<Comics>> getComics() { return mRepository.getComics(); }

    public LiveData<Comics> getComics(long id) { return mRepository.getComics(id); }

    public LiveData<List<ComicsWithReleases>> getComicsWithReleases() { return mRepository.getComicsWithReleases(); }

    public LiveData<ComicsWithReleases> getComicsWithReleases(long id) { return mRepository.getComicsWithReleases(id); }

    public void insert(Comics comics) { mRepository.insert(comics); }

    public void update(Comics comics) { mRepository.update(comics); }

    public void delete(long id) { mRepository.delete(id); }

    public void delete(Iterable<Long> ids) {
        mRepository.delete(Utility.toArray(ids));
    }
}
