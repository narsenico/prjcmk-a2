package it.amonshore.comikkua.data;

import android.app.Application;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagedList;
import it.amonshore.comikkua.Utility;

public class ComicsViewModel extends AndroidViewModel {

    private ComicsRepository mRepository;
    public final LiveData<PagedList<ComicsWithReleases>> comicsWithReleasesList;
    public final MutableLiveData<String> filterName;

    public ComicsViewModel(Application application) {
        super(application);
        mRepository = new ComicsRepository(application);
        comicsWithReleasesList = mRepository.comicsWithReleasesList;
        filterName = mRepository.filterName;
    }

    public LiveData<List<Comics>> getComics() {
        return mRepository.getComics();
    }

    public LiveData<Comics> getComics(long id) {
        return mRepository.getComics(id);
    }

    public LiveData<Comics> getComics(String name) {
        return mRepository.getComics(name);
    }

    public LiveData<List<ComicsWithReleases>> getComicsWithReleases() {
        return mRepository.getComicsWithReleases();
    }

    public LiveData<ComicsWithReleases> getComicsWithReleases(long id) {
        return mRepository.getComicsWithReleases(id);
    }

    public LiveData<List<String>> getPublishers() {
        return mRepository.getPublishers();
    }

    public void insert(Comics comics) {
        mRepository.insert(comics);
    }

    public void update(Comics comics) {
        mRepository.update(comics);
    }

    public void delete(long id) {
        mRepository.delete(id);
    }

    public void delete(Iterable<Long> ids) {
        mRepository.delete(Utility.toArray(ids));
    }

    public void deleteAll() {
        mRepository.deleteAll();
    }
}
