package it.amonshore.comikkua.data.comics;

import android.app.Application;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.paging.PagedList;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.Utility;

public class ComicsViewModel extends AndroidViewModel {

    private ComicsRepository mRepository;
    public final LiveData<PagedList<ComicsWithReleases>> comicsWithReleasesList;

    // lo uso per salvare gli stati delle viste (ad esempio la posizione dello scroll di una lista in un fragment)
    public final Bundle states;

    public ComicsViewModel(Application application) {
        super(application);
        mRepository = new ComicsRepository(application);
        comicsWithReleasesList = mRepository.comicsWithReleasesList;
        states = new Bundle();
    }

    /**
     * Imposta un filtro sui comics.
     * Gli osservatori collegati a comicsWithReleasesList verranno eccitati di conseguenza.
     *
     * @param filter    testo da usare come filtro, null o vuoto per togliere il filtro
     */
    public void setFilter(String filter) {
        if (TextUtils.isEmpty(filter)) {
            mRepository.filterComics.setValue(null);
        } else {
            mRepository.filterComics.setValue("%" + filter.replaceAll("\\s+", "%") + "%");
        }
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

    public LiveData<ComicsWithReleases> getComicsWithReleases(long id) {
        return mRepository.getComicsWithReleases(id);
    }

    public LiveData<List<String>> getPublishers() {
        return mRepository.getPublishers();
    }

    public void insert(Comics comics) {
        mRepository.insert(comics);
    }

    public long insertSync(Comics comics) {
        return mRepository.insertSync(comics);
    }

    public void update(Comics comics) {
        mRepository.update(comics);
    }

    public int updateSync(Comics comics) {
        return mRepository.updateSync(comics);
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
