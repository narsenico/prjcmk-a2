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
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import it.amonshore.comikkua.ICallback2;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.web.FirebaseRepository;
import kotlinx.coroutines.CoroutineScope;

public class ComicsViewModel extends AndroidViewModel {

    private final ComicsRepository mRepository;
    private final FirebaseRepository mFirebaseRespository;
    private final MutableLiveData<String> mFilterComics = new MutableLiveData<>();
    private final LiveData<PagingData<ComicsWithReleases>> mAllComics;
    private String mLastFilter;

    public final LiveData<PagingData<ComicsWithReleases>> comicsWithReleasesList;
    public final MediatorLiveData<List<String>> comicBookTitles;
    // indica se è in corso un caricamento di dati da remoto
    public final MutableLiveData<Boolean> loading;

    // lo uso per salvare gli stati delle viste (ad esempio la posizione dello scroll di una lista in un fragment)
    public final Bundle states;

    public ComicsViewModel(Application application) {
        super(application);
        mRepository = new ComicsRepository(application);
        mFirebaseRespository = new FirebaseRepository(application);
        states = new Bundle();
        loading = new MutableLiveData<>();

        final PagingConfig pagingConfig = new PagingConfig(20, 20, true);

        // LiveData con l'elenco completo
        final CoroutineScope viewModelScope = ViewModelKt.getViewModelScope(this);
        final Pager<Integer, ComicsWithReleases> allComicsPager = new Pager<>(pagingConfig,
                mRepository::getComicsWithReleasesPagingSource);
        mAllComics = PagingLiveData.cachedIn(PagingLiveData.getLiveData(allComicsPager), viewModelScope);

        // è eccitato dal MutableLiveData mFilterComics
        // se il suo valore è vuoto, oppure %%, viene ritornato l'intero set di comics
        // oppure solo quelli che matchano il nome (like)
        comicsWithReleasesList = Transformations.switchMap(mFilterComics, input -> {
            if (TextUtils.isEmpty(input) || input.equals("%%")) {
                return mAllComics;
            } else {
//                return new LivePagedListBuilder<>(mRepository.getComicsWithReleasesPagingSource(input), config).build();
                // TODO: deve esserci un modo per farla più performante
                final Pager<Integer, ComicsWithReleases> filteredComicsPager = new Pager<>(pagingConfig,
                        () -> mRepository.getComicsWithReleasesPagingSource(input));
                return PagingLiveData.cachedIn(PagingLiveData.getLiveData(filteredComicsPager), viewModelScope);
            }
        });

        // carico la lista dei titoli dalla rete
        // posto il valore solo in caso di SUCCESS perché se uso LiveDataEx.observeOnce()
        //  riceverei solo il primo valore, che sarà a livello LOADING (oppure ERROR), visto che l'observer verrebbe subito rimosso
        comicBookTitles = new MediatorLiveData<>();
        comicBookTitles.addSource(mFirebaseRespository.getComicNames(), resource -> {
            LogHelper.d("comicBookTitles status=%s", resource.status);
            switch (resource.status) {
                case SUCCESS:
                    comicBookTitles.postValue(resource.data);
                    loading.postValue(false);
                    break;
                case LOADING:
                    loading.postValue(true);
                    break;
                case ERROR:
                    LogHelper.e("error retrieving web comics: " + resource.message);
                    loading.postValue(false);
                    break;
            }
        });

        // TODO: leggere in una botta sola tutti i comics da remoto e salvarli in una tabella nuova?
        //  per poi usare questa (paginata) come fonte di dati
        //  così almeno posso fare delle ricerche complesse (perché all'utente sarà permesso fare delle ricerche)
        //  mentre direttamente con Firestore non posso
        //  però significa anche avere altri dati locali
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
