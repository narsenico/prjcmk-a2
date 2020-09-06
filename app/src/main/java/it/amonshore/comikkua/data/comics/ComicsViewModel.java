package it.amonshore.comikkua.data.comics;

import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;

import java.util.List;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import it.amonshore.comikkua.ICallback2;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.web.CmkWebRepository;
import it.amonshore.comikkua.data.web.FirebaseRepository;

public class ComicsViewModel extends AndroidViewModel {

    private final ComicsRepository mRepository;
//    private final CmkWebRepository mCmkWebRepository;
    private final FirebaseRepository mFirebaseRespository;
    private final MutableLiveData<String> mFilterComics = new MutableLiveData<>();
    private final LiveData<PagedList<ComicsWithReleases>> mAllComics;
    private String mLastFilter;

    public final LiveData<PagedList<ComicsWithReleases>> comicsWithReleasesList;
    public final MediatorLiveData<List<String>> comicBookTitles;

    // lo uso per salvare gli stati delle viste (ad esempio la posizione dello scroll di una lista in un fragment)
    public final Bundle states;

    public ComicsViewModel(Application application) {
        super(application);
        mRepository = new ComicsRepository(application);
//        mCmkWebRepository = new CmkWebRepository(application);
        mFirebaseRespository = new FirebaseRepository();
        states = new Bundle();

        final PagedList.Config config = new PagedList.Config.Builder()
                .setInitialLoadSizeHint(20)
                .setPageSize(20)
                .setEnablePlaceholders(true)
                .build();

        // LiveData con l'elenco completo
        mAllComics = new LivePagedListBuilder<>(mRepository.getComicsWithReleasesFactory(), config).build();

        // è eccitato dal MutableLiveData mFilterComics
        // se il suo valore è vuoto, oppure %%, viene ritornato l'intero set di comics
        // oppure solo quelli che matchano il nome (like)
        comicsWithReleasesList = Transformations.switchMap(mFilterComics, input -> {
            if (TextUtils.isEmpty(input) || input.equals("%%")) {
                return mAllComics;
            } else {
                return new LivePagedListBuilder<>(mRepository.getComicsWithReleasesFactory(input), config).build();
            }
        });

        // carico la lista dei titoli dalla rete
        // posto il valore solo in caso di SUCCESS perché se uso LiveDataEx.observeOnce()
        //  riceverei solo il primo valore, che sarà a livello LOADING (oppure ERROR), visto che l'observer verrebbe subito rimosso
        comicBookTitles = new MediatorLiveData<>();
//        comicBookTitles.addSource(mCmkWebRepository.getTitles(), resource -> {
        comicBookTitles.addSource(mFirebaseRespository.getTitles(), resource -> {
            LogHelper.d("comicBookTitles status=%s", resource.status);
            switch (resource.status) {
                case SUCCESS:
                    comicBookTitles.postValue(resource.data);
                    break;
                case LOADING:
                    // non mi interessa
                    break;
                case ERROR:
                    LogHelper.e("error retrieving web comics: " + resource.message);
                    break;
            }
        });
    }

    /**
     * Imposta un filtro sui comics.
     * Gli osservatori collegati a comicsWithReleasesList verranno eccitati di conseguenza.
     *
     * @param filter    testo da usare come filtro, null o vuoto per togliere il filtro
     */
    public void setFilter(String filter) {
        mLastFilter = filter;
        if (TextUtils.isEmpty(filter)) {
            mFilterComics.setValue(null);
        } else {
            mFilterComics.setValue("%" + filter.replaceAll("\\s+", "%") + "%");
        }
    }

    public void useLastFilter() {
        setFilter(mLastFilter);
    }

    public String getLastFilter() {
        return mLastFilter;
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

    public LiveData<List<String>> getAuthors() {
        return mRepository.getAuthors();
    }

    public LiveData<List<String>> getComicsName() {
        return mRepository.getComicsName();
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
        comics.lastUpdate = System.currentTimeMillis();
        return mRepository.updateSync(comics);
    }

    public void delete(long id) {
        mRepository.delete(id);
    }

    public void delete(Long... ids) {
        mRepository.delete(ids);
    }

    public void deleteAll() {
        mRepository.deleteAll();
    }

    public void remove(Iterable<Long> ids, ICallback2<Long[], Integer> callback) {
        remove(Utility.toArray(ids), callback);
    }

    public void remove(Long... ids) {
        mRepository.remove(ids);
    }

    public void remove(Long id, ICallback2<Long[], Integer> callback) {
        remove(new Long[] { id }, callback);
    }

    public void remove(Long[] ids, ICallback2<Long[], Integer> callback) {
        mRepository.remove(ids, callback);
    }

    public void undoRemoved() {
        mRepository.undoRemoved();
    }

    public void deleteRemoved() {
        mRepository.deleteRemoved();
    }
}
