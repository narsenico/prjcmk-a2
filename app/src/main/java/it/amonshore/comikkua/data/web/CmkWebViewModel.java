package it.amonshore.comikkua.data.web;

import android.app.Application;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelKt;
import androidx.paging.Pager;
import androidx.paging.PagingConfig;
import androidx.paging.PagingData;
import androidx.paging.PagingLiveData;
import it.amonshore.comikkua.ICallback;
import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.Resource;
import kotlinx.coroutines.CoroutineScope;

public class CmkWebViewModel extends AndroidViewModel {

    private final CmkWebRepository mCmkWebRepository;
    private final MutableLiveData<String> mFilterComics;
    private final LiveData<PagingData<AvailableComics>> mAllAvailableComics;
    private final LiveData<PagingData<AvailableComics>> mFilteredAvailableComics;
    private String mLastFilter;

    public CmkWebViewModel(@NonNull Application application) {
        super(application);

        mCmkWebRepository = new CmkWebRepository(application);
        mFilterComics = new MutableLiveData<>();

        // preparo paging livedata per i comics
        final PagingConfig pagingConfig = new PagingConfig(10, 10, true);
        final CoroutineScope viewModelScope = ViewModelKt.getViewModelScope(this);
        final Pager<Integer, AvailableComics> allComicsPager = new Pager<>(pagingConfig,
                mCmkWebRepository::getAvailableComicsPagingSource);
        mAllAvailableComics = PagingLiveData.cachedIn(PagingLiveData.getLiveData(allComicsPager), viewModelScope);

        mFilteredAvailableComics = Transformations.switchMap(mFilterComics, input -> {
            if (TextUtils.isEmpty(input) || input.equals("%%")) {
                return mAllAvailableComics;
            } else {
                // TODO: deve esserci un modo per farla più performante
                final Pager<Integer, AvailableComics> filteredComicsPager = new Pager<>(pagingConfig,
                        () -> mCmkWebRepository.getAvailableComicsPagingSource(input));
                return PagingLiveData.cachedIn(PagingLiveData.getLiveData(filteredComicsPager), viewModelScope);
            }
        });
    }

    public void setFilter(String filter) {
        mLastFilter = filter;
        mFilterComics.postValue(filter);
    }

    public String getLastFilter() {
        return mLastFilter;
    }

    public LiveData<PagingData<AvailableComics>> getAvailableComics() {
        return mAllAvailableComics;
    }

    public LiveData<PagingData<AvailableComics>> getFilteredAvailableComics() {
        return mFilteredAvailableComics;
    }
}
