package it.amonshore.comikkua.ui.comics;

import android.net.Uri;
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
import it.amonshore.comikkua.Constants;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.ui.ImageHelper;

public class PagedListComicsAdapter extends PagingDataAdapter<ComicsWithReleases, ComicsViewHolder> {

    private SelectionTracker<Long> mSelectionTracker;
    private IComicsViewHolderCallback<Long> mComicsViewHolderCallback;
    private RequestManager mRequestManager;

    private PagedListComicsAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicsViewHolder holder, int position) {
        final ComicsWithReleases item = getItem(position);
        if (item != null) {
            holder.bind(item, mSelectionTracker.isSelected(item.comics.id), mRequestManager);
        } else {
            holder.clear();
        }
    }

    @NonNull
    @Override
    public ComicsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ComicsViewHolder.create(LayoutInflater.from(parent.getContext()), parent, mComicsViewHolderCallback);
    }

    public long getSelectionKey(int position) {
        final ComicsWithReleases item = getItem(position);
        if (item == null) {
            return RecyclerView.NO_ID;
        } else {
            return item.comics.id;
        }
    }

    public int getPosition(long selectionKey) {
        long nn = SystemClock.elapsedRealtimeNanos();
        try {
            for (int ii = 0; ; ii++) {
                final ComicsWithReleases item = getItem(ii);
                if (item == null) {
                    return RecyclerView.NO_POSITION;
                } else if (item.comics.id == selectionKey) {
                    return ii;
                }
            }
        } finally {
            LogHelper.d("getPosition of %s out of %s in %sns",
                    selectionKey, getItemCount(), SystemClock.elapsedRealtimeNanos() - nn);
        }
    }

    SelectionTracker<Long> getSelectionTracker() {
        return mSelectionTracker;
    }

    interface OnItemSelectedListener {

        void onSelectionChanged(@Nullable Iterator<Long> keys, int size);
    }

    public interface ComicsCallback {

        void onComicsClick(@NonNull ComicsWithReleases comics);

        void onComicsMenuSelected(@NonNull ComicsWithReleases comics);
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

        PagedListComicsAdapter build() {
            final PagedListComicsAdapter adapter = new PagedListComicsAdapter();
            adapter.mComicsViewHolderCallback = (comicsId, position, action) -> {
                if (action == Constants.VIEWHOLDER_ACTION_MENU) {
                    if (comicsCallback != null) {
                        final ComicsWithReleases comics = adapter.getItem(position);
                        if (comics != null) {
                            comicsCallback.onComicsMenuSelected(comics);
                        }
                    }
                } else if (action == Constants.VIEWHOLDER_ACTION_CLICK) {
                    // se capita che venga scatenato il click anche se è in corso una selezione devo skippare
                    if (comicsCallback != null && !adapter.mSelectionTracker.hasSelection()) {
                        final ComicsWithReleases comics = adapter.getItem(position);
                        if (comics != null) {
                            comicsCallback.onComicsClick(comics);
                        }
                    }
                }
            };
//            // questo è necessario insieme all'override di getItemId() per funzionare con SelectionTracker
//            adapter.setHasStableIds(true);
            mRecyclerView.setAdapter(adapter);

            final MyItemKeProvider itemKeyProvider = new MyItemKeProvider(mRecyclerView, ItemKeyProvider.SCOPE_MAPPED);
            final SelectionTracker.Builder<Long> builder = new SelectionTracker.Builder<>(
                    "comics-selection",
                    mRecyclerView,
                    itemKeyProvider,
                    new ComicsItemDetailsLookup<>(mRecyclerView, ComicsViewHolder.class),
                    StorageStrategy.createLongStorage());

//            if (mOnItemActivatedListener != null) {
//                builder.withOnItemActivatedListener(mOnItemActivatedListener);
//            }

            adapter.mSelectionTracker = builder.build();

            if (mOnItemSelectedListener != null) {
                adapter.mSelectionTracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
                    @Override
                    public void onSelectionChanged() {
                        if (adapter.mSelectionTracker.hasSelection()) {
                            final Selection<Long> selection = adapter.getSelectionTracker().getSelection();
                            mOnItemSelectedListener.onSelectionChanged(selection.iterator(), selection.size());
                        } else {
                            mOnItemSelectedListener.onSelectionChanged(null, 0);
                        }
                    }

                    @Override
                    public void onSelectionRestored() {
                        LogHelper.d("fire selection changed onSelectionRestored");
                        if (adapter.mSelectionTracker.hasSelection()) {
                            final Selection<Long> selection = adapter.getSelectionTracker().getSelection();
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
                final FixedPreloadSizeProvider<ComicsWithReleases> sizeProvider =
                        new FixedPreloadSizeProvider<>(ImageHelper.getDefaultSize(), ImageHelper.getDefaultSize());
                final ComicsPreloadModelProvider modelProvider =
                        new ComicsPreloadModelProvider(adapter, mRequestManager);
                final RecyclerViewPreloader<ComicsWithReleases> preloader =
                        new RecyclerViewPreloader<>(mRequestManager, modelProvider, sizeProvider, 10);

                adapter.mRequestManager = mRequestManager;
                mRecyclerView.addOnScrollListener(preloader);
            }

            return adapter;
        }
    }

    private static class MyItemKeProvider extends ItemKeyProvider<Long> {
        private final PagedListComicsAdapter mAdapter;

        public MyItemKeProvider(@NonNull RecyclerView recyclerView, int scope) {
            super(scope);
            mAdapter = (PagedListComicsAdapter) recyclerView.getAdapter();
        }

        @Nullable
        @Override
        public Long getKey(int position) {
            return mAdapter == null ? RecyclerView.NO_ID : mAdapter.getSelectionKey(position);
        }

        @Override
        public int getPosition(@NonNull Long key) {
            return mAdapter == null ? RecyclerView.NO_POSITION : mAdapter.getPosition(key);
        }
    }

    private static final DiffUtil.ItemCallback<ComicsWithReleases> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ComicsWithReleases>() {
                // ComicsWithReleases details may have changed if reloaded from the database,
                // but ID is fixed.
                @Override
                public boolean areItemsTheSame(@NonNull ComicsWithReleases oldComicsWithReleases,
                                               @NonNull ComicsWithReleases newComicsWithReleases) {
                    return oldComicsWithReleases.comics.id == newComicsWithReleases.comics.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ComicsWithReleases oldComicsWithReleases,
                                                  @NonNull ComicsWithReleases newComicsWithReleases) {
                    return oldComicsWithReleases.equals(newComicsWithReleases);
                }
            };

    private static class ComicsPreloadModelProvider implements ListPreloader.PreloadModelProvider<ComicsWithReleases> {

        private final PagedListComicsAdapter mAdapter;
        private final RequestManager mRequestManager;

        ComicsPreloadModelProvider(@NonNull PagedListComicsAdapter adapter, @NonNull RequestManager requestManager) {
            mAdapter = adapter;
            mRequestManager = requestManager;
        }

        @NonNull
        @Override
        public List<ComicsWithReleases> getPreloadItems(int position) {
            if (position >= mAdapter.getItemCount()) {
                return Collections.emptyList();
            }

            final ComicsWithReleases item = mAdapter.getItem(position);
            if (item == null) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(item);
            }
        }

        @Nullable
        @Override
        public RequestBuilder<?> getPreloadRequestBuilder(@NonNull ComicsWithReleases item) {
            if (item.comics.hasImage()) {
                return mRequestManager
                        .load(Uri.parse(item.comics.image))
                        .listener(ImageHelper.drawableRequestListener)
                        .apply(ImageHelper.getGlideCircleOptions());

            } else {
                return null;
            }
        }
    }

}