package it.amonshore.comikkua.ui.comics;

import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagingDataAdapter;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.web.CmkWebComics;
import it.amonshore.comikkua.ui.ImageHelper;

public class PagedListCmkWebComicsAdapter extends PagingDataAdapter<CmkWebComics, CmkWebComicsViewHolder> {

    private SelectionTracker<String> mSelectionTracker;
    private IComicsViewHolderCallback<String> mComicsViewHolderCallback;
    private RequestManager mRequestManager;

    private PagedListCmkWebComicsAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public void onBindViewHolder(@NonNull CmkWebComicsViewHolder holder, int position) {
        final CmkWebComics item = getItem(position);
        if (item != null) {
            holder.bind(item, mSelectionTracker.isSelected(item.id), mRequestManager);
        } else {
            holder.clear();
        }
    }

    @NonNull
    @Override
    public CmkWebComicsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return CmkWebComicsViewHolder.create(LayoutInflater.from(parent.getContext()), parent, mComicsViewHolderCallback);
    }

    public String getSelectionKey(int position) {
        final CmkWebComics item = getItem(position);
        if (item == null) {
            return null;
        } else {
            return item.id;
        }
    }

    public int getPosition(String selectionKey) {
        long nn = SystemClock.elapsedRealtimeNanos();
        try {
            for (int ii = 0; ; ii++) {
                final CmkWebComics item = getItem(ii);
                if (item == null) {
                    return RecyclerView.NO_POSITION;
                } else if (item.id.equals(selectionKey)) {
                    return ii;
                }
            }
        } finally {
            LogHelper.d("getPosition of %s out of %s in %sns",
                    selectionKey, getItemCount(), SystemClock.elapsedRealtimeNanos() - nn);
        }
    }

    SelectionTracker<String> getSelectionTracker() {
        return mSelectionTracker;
    }

    interface OnItemSelectedListener {

        void onSelectionChanged(@Nullable Iterator<String> keys, int size);
    }

    public interface ComicsCallback {

        void onComicsClick(@NonNull CmkWebComics comics);

        void onComicsMenuSelected(@NonNull CmkWebComics comics);
    }

    static class Builder {
        private final RecyclerView mRecyclerView;
        private OnItemSelectedListener mOnItemSelectedListener;
        private RequestManager mRequestManager;
        private ComicsCallback comicsCallback;

        Builder(@NonNull RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

        Builder withComicsCallback(@NonNull ComicsCallback callback) {
            comicsCallback = callback;
            return this;
        }

        Builder withOnItemSelectedListener(OnItemSelectedListener listener) {
            mOnItemSelectedListener = listener;
            return this;
        }

        Builder withGlide(RequestManager requestManager) {
            mRequestManager = requestManager;
            return this;
        }

        PagedListCmkWebComicsAdapter build() {
            final PagedListCmkWebComicsAdapter adapter = new PagedListCmkWebComicsAdapter();
            adapter.mComicsViewHolderCallback = new IComicsViewHolderCallback<String>() {
                @Override
                public void onComicsClick(String comicsId, int position) {
                    // se capita che venga scatenato il click anche se è in corso una selezione devo skippare
                    if (comicsCallback != null && !adapter.mSelectionTracker.hasSelection()) {
                        final CmkWebComics comics = adapter.getItem(position);
                        if (comics != null) {
                            comicsCallback.onComicsClick(comics);
                        }
                    }
                }

                @Override
                public void onComicsMenuSelected(String comicsId, int position) {
                    if (comicsCallback != null) {
                        final CmkWebComics comics = adapter.getItem(position);
                        if (comics != null) {
                            comicsCallback.onComicsMenuSelected(comics);
                        }
                    }
                }
            };
//            // questo è necessario insieme all'override di getItemId() per funzionare con SelectionTracker
//            adapter.setHasStableIds(true);
            mRecyclerView.setAdapter(adapter);

            final MyItemKeProvider itemKeyProvider = new MyItemKeProvider(mRecyclerView, ItemKeyProvider.SCOPE_MAPPED);
            final SelectionTracker.Builder<String> builder = new SelectionTracker.Builder<>(
                    "comics-selection",
                    mRecyclerView,
                    itemKeyProvider,
                    new ComicsItemDetailsLookup<>(mRecyclerView, CmkWebComicsViewHolder.class),
                    StorageStrategy.createStringStorage());

//            if (mOnItemActivatedListener != null) {
//                builder.withOnItemActivatedListener(mOnItemActivatedListener);
//            }

            adapter.mSelectionTracker = builder.build();

            if (mOnItemSelectedListener != null) {
                adapter.mSelectionTracker.addObserver(new SelectionTracker.SelectionObserver<String>() {
                    @Override
                    public void onSelectionChanged() {
                        if (adapter.mSelectionTracker.hasSelection()) {
                            final Selection<String> selection = adapter.getSelectionTracker().getSelection();
                            mOnItemSelectedListener.onSelectionChanged(selection.iterator(), selection.size());
                        } else {
                            mOnItemSelectedListener.onSelectionChanged(null, 0);
                        }
                    }

                    @Override
                    public void onSelectionRestored() {
                        LogHelper.d("fire selection changed onSelectionRestored");
                        if (adapter.mSelectionTracker.hasSelection()) {
                            final Selection<String> selection = adapter.getSelectionTracker().getSelection();
                            mOnItemSelectedListener.onSelectionChanged(selection.iterator(), selection.size());
                        } else {
                            mOnItemSelectedListener.onSelectionChanged(null, 0);
                        }
                        super.onSelectionRestored();
                    }
                });
            }

            if (mRequestManager != null) {
                // precarico le immagini dei comics
                final FixedPreloadSizeProvider<CmkWebComics> sizeProvider =
                        new FixedPreloadSizeProvider<>(ImageHelper.getDefaultSize(), ImageHelper.getDefaultSize());
                final ComicsPreloadModelProvider modelProvider =
                        new ComicsPreloadModelProvider(adapter, mRequestManager);
                final RecyclerViewPreloader<CmkWebComics> preloader =
                        new RecyclerViewPreloader<>(mRequestManager, modelProvider, sizeProvider, 10);

                adapter.mRequestManager = mRequestManager;
                mRecyclerView.addOnScrollListener(preloader);
            }

            return adapter;
        }
    }

    private static class MyItemKeProvider extends ItemKeyProvider<String> {
        private final PagedListCmkWebComicsAdapter mAdapter;

        public MyItemKeProvider(@NonNull RecyclerView recyclerView, int scope) {
            super(scope);
            mAdapter = (PagedListCmkWebComicsAdapter) recyclerView.getAdapter();
        }

        @Nullable
        @Override
        public String getKey(int position) {
            return mAdapter == null ? null : mAdapter.getSelectionKey(position);
        }

        @Override
        public int getPosition(@NonNull String key) {
            return mAdapter == null ? RecyclerView.NO_POSITION : mAdapter.getPosition(key);
        }
    }

    private static final DiffUtil.ItemCallback<CmkWebComics> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CmkWebComics>() {
                // ComicsWithReleases details may have changed if reloaded from the database,
                // but ID is fixed.
                @Override
                public boolean areItemsTheSame(@NonNull CmkWebComics oldCmkWebComics,
                                               @NonNull CmkWebComics newCmkWebComics) {
                    return oldCmkWebComics.id.equals(newCmkWebComics.id);
                }

                @Override
                public boolean areContentsTheSame(@NonNull CmkWebComics oldCmkWebComics,
                                                  @NonNull CmkWebComics newCmkWebComics) {
                    return oldCmkWebComics.equals(newCmkWebComics);
                }
            };

    private static class ComicsPreloadModelProvider implements ListPreloader.PreloadModelProvider<CmkWebComics> {

        private final PagedListCmkWebComicsAdapter mAdapter;
        private final RequestManager mRequestManager;

        ComicsPreloadModelProvider(@NonNull PagedListCmkWebComicsAdapter adapter, @NonNull RequestManager requestManager) {
            mAdapter = adapter;
            mRequestManager = requestManager;
        }

        @NonNull
        @Override
        public List<CmkWebComics> getPreloadItems(int position) {
            final CmkWebComics item = mAdapter.getItem(position);
            if (item == null) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(item);
            }
        }

        @Nullable
        @Override
        public RequestBuilder<?> getPreloadRequestBuilder(@NonNull CmkWebComics item) {
//            if (item.comics.hasImage()) {
//                return mRequestManager
//                        .load(Uri.parse(item.comics.image))
//                        .listener(ImageHelper.drawableRequestListener)
//                        .apply(ImageHelper.getGlideCircleOptions());
//
//            } else {
            return null;
//            }
        }
    }

}