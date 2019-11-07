package it.amonshore.comikkua.ui.comics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.OnItemActivatedListener;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.util.Iterator;

import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.CustomItemKeyProvider;

public class PagedListComicsAdapter extends PagedListAdapter<ComicsWithReleases, ComicsViewHolder> {

    private SelectionTracker<Long> mSelectionTracker;

    private PagedListComicsAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicsViewHolder holder, int position) {
        final ComicsWithReleases item = getItem(position);
        if (item != null) {
            holder.bind(item, mSelectionTracker.isSelected(item.comics.id));
        } else {
            holder.clear();
        }
    }

    @NonNull
    @Override
    public ComicsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ComicsViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public long getItemId(int position) {
        final ComicsWithReleases item = getItem(position);
        if (item == null) {
            return RecyclerView.NO_ID;
        } else {
            return item.comics.id;
        }
    }

    SelectionTracker<Long> getSelectionTracker() {
        return mSelectionTracker;
    }

    interface OnItemSelectedListener {

        void onSelectionChanged(@Nullable Iterator<Long> keys, int size);
    }

    interface ActionModeControllerCallback {

        void onActionModeControllerCreated(ActionModeController controller);

        void onActionModeMenuItemSelected(MenuItem item);
    }

    static class Builder {
        private final RecyclerView mRecyclerView;
        private OnItemActivatedListener<Long> mOnItemActivatedListener;
        private OnItemSelectedListener mOnItemSelectedListener;
        private ActionModeControllerCallback mActionModeControllerCallback;
        private int actionModeMenuRes;

        Builder(@NonNull RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

        Builder withOnItemActivatedListener(OnItemActivatedListener<Long> listener) {
            mOnItemActivatedListener = listener;
            return this;
        }

        Builder withOnItemSelectedListener(OnItemSelectedListener listener) {
            mOnItemSelectedListener = listener;
            return this;
        }

        Builder withActionModeControllerCallback(int menuRes, ActionModeControllerCallback callback) {
            // TODO: dovrebbe essere usato con withOnItemSelectedListener... altrimenti non funziona
            //  quindi gestire da qua la creazione di ActionMode.Callback,
            //  e al chiamante si passa la notifica della creazione e la selezione di un tasto del menu
            //  in modo che possa aggiornare la toolbar dell'activity
            actionModeMenuRes = menuRes;
            mActionModeControllerCallback = callback;
            return this;
        }

        PagedListComicsAdapter build() {
            final PagedListComicsAdapter adapter = new PagedListComicsAdapter();
            // questo è necessario insieme all'override di getItemId() per funzionare con SelectionTracker
            adapter.setHasStableIds(true);
            mRecyclerView.setAdapter(adapter);

            final SelectionTracker.Builder<Long> builder = new SelectionTracker.Builder<>(
                    "comics-selection",
                    mRecyclerView,
                    new CustomItemKeyProvider(mRecyclerView, ItemKeyProvider.SCOPE_MAPPED),
                    new ComicsItemDetailsLookup(mRecyclerView),
                    StorageStrategy.createLongStorage());

            if (mOnItemActivatedListener != null) {
                builder.withOnItemActivatedListener(mOnItemActivatedListener);
            }

            adapter.mSelectionTracker = builder.build();

            if (mOnItemSelectedListener != null) {
                adapter.mSelectionTracker.addObserver(new SelectionTracker.SelectionObserver() {
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

            return adapter;
        }
    }

    private static DiffUtil.ItemCallback<ComicsWithReleases> DIFF_CALLBACK =
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

}