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
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import it.amonshore.comikkua.Constants;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.web.AvailableComics;
import it.amonshore.comikkua.ui.ImageHelper;

public class AvailableComicsAdapter extends ListAdapter<AvailableComics, AvailableComicsViewHolder> {

    private SelectionTracker<String> mSelectionTracker;
    private IComicsViewHolderCallback<String> mComicsViewHolderCallback;
    private RequestManager mRequestManager;

    private AvailableComicsAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public void onBindViewHolder(@NonNull AvailableComicsViewHolder holder, int position) {
        final AvailableComics item = getItem(position);
        if (item != null) {
            holder.bind(item, mSelectionTracker.isSelected(item.sourceId), mRequestManager);
        } else {
            holder.clear();
        }
    }

    @NonNull
    @Override
    public AvailableComicsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return AvailableComicsViewHolder.create(LayoutInflater.from(parent.getContext()), parent, mComicsViewHolderCallback);
    }

    public String getSelectionKey(int position) {
        final AvailableComics item = getItem(position);
        if (item == null) {
            return null;
        } else {
            return item.sourceId;
        }
    }

    public int getPosition(String selectionKey) {
        long nn = SystemClock.elapsedRealtimeNanos();
        try {
            for (int ii = 0; ; ii++) {
                final AvailableComics item = getItem(ii);
                if (item == null) {
                    return RecyclerView.NO_POSITION;
                } else if (item.sourceId.equals(selectionKey)) {
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

        void onComicsFollowed(@NonNull AvailableComics comics);

        void onComicsMenuSelected(@NonNull AvailableComics comics);
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

        AvailableComicsAdapter build() {
            final AvailableComicsAdapter adapter = new AvailableComicsAdapter();
            adapter.mComicsViewHolderCallback = (comicsId, position, action) -> {
                if (action == Constants.VIEWHOLDER_ACTION_MENU) {
                    if (comicsCallback != null) {
                        final AvailableComics comics = adapter.getItem(position);
                        if (comics != null) {
                            comicsCallback.onComicsMenuSelected(comics);
                        }
                    }
                } else if (action == Constants.VIEWHOLDER_ACTION_FOLLOW) {
                    // se capita che venga scatenato il click anche se è in corso una selezione devo skippare
                    if (comicsCallback != null && !adapter.mSelectionTracker.hasSelection()) {
                        final AvailableComics comics = adapter.getItem(position);
                        if (comics != null) {
                            comicsCallback.onComicsFollowed(comics);
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
                    new ComicsItemDetailsLookup<>(mRecyclerView, AvailableComicsViewHolder.class),
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
                final FixedPreloadSizeProvider<AvailableComics> sizeProvider =
                        new FixedPreloadSizeProvider<>(ImageHelper.getDefaultSize(), ImageHelper.getDefaultSize());
                final ComicsPreloadModelProvider modelProvider =
                        new ComicsPreloadModelProvider(adapter, mRequestManager);
                final RecyclerViewPreloader<AvailableComics> preloader =
                        new RecyclerViewPreloader<>(mRequestManager, modelProvider, sizeProvider, 10);

                adapter.mRequestManager = mRequestManager;
                mRecyclerView.addOnScrollListener(preloader);
            }

            return adapter;
        }
    }

    private static class MyItemKeProvider extends ItemKeyProvider<String> {
        private final AvailableComicsAdapter mAdapter;

        public MyItemKeProvider(@NonNull RecyclerView recyclerView, int scope) {
            super(scope);
            mAdapter = (AvailableComicsAdapter) recyclerView.getAdapter();
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

    private static final DiffUtil.ItemCallback<AvailableComics> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<AvailableComics>() {
                // ComicsWithReleases details may have changed if reloaded from the database,
                // but ID is fixed.
                @Override
                public boolean areItemsTheSame(@NonNull AvailableComics oldAvailableComics,
                                               @NonNull AvailableComics newAvailableComics) {
                    return oldAvailableComics.sourceId.equals(newAvailableComics.sourceId);
                }

                @Override
                public boolean areContentsTheSame(@NonNull AvailableComics oldAvailableComics,
                                                  @NonNull AvailableComics newAvailableComics) {
                    return oldAvailableComics.equals(newAvailableComics);
                }
            };

    private static class ComicsPreloadModelProvider implements ListPreloader.PreloadModelProvider<AvailableComics> {

        private final AvailableComicsAdapter mAdapter;
        private final RequestManager mRequestManager;

        ComicsPreloadModelProvider(@NonNull AvailableComicsAdapter adapter, @NonNull RequestManager requestManager) {
            mAdapter = adapter;
            mRequestManager = requestManager;
        }

        @NonNull
        @Override
        public List<AvailableComics> getPreloadItems(int position) {
            final AvailableComics item = mAdapter.getItem(position);
            if (item == null) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(item);
            }
        }

        @Nullable
        @Override
        public RequestBuilder<?> getPreloadRequestBuilder(@NonNull AvailableComics item) {
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