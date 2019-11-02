package it.amonshore.comikkua.ui.releases;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.util.Iterator;

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
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.ComicsRelease;
import it.amonshore.comikkua.ui.ActionModeController;
import it.amonshore.comikkua.ui.CustomItemKeyProvider;

public class ReleasesRecyclerViewAdapter extends PagedListAdapter<ComicsRelease, ReleaseViewHolder> {
    private SelectionTracker<Long> mSelectionTracker;

    private ReleasesRecyclerViewAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public void onBindViewHolder(@NonNull ReleaseViewHolder holder, int position) {
        final ComicsRelease item = getItem(position);
        if (item != null) {
            // mostro l'instestazione (che fa parte del ViewHolder) solo se l'elemento precedente è "tipo" diverso
            //  cioè è stato estratto con una query diversa
            boolean showHeader;
            if (position == 0) {
                showHeader = true;
            } else {
                final ComicsRelease previous = getItem(position - 1);
                if (previous == null) {
                    showHeader = true;
                } else {
                    showHeader = previous.type != item.type;
                }
            }
            holder.bind(item, mSelectionTracker.isSelected(item.release.id), showHeader, position == 0);
        } else {
            holder.clear();
        }
    }

    @NonNull
    @Override
    public ReleaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ReleaseViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
    }

    @Override
    public long getItemId(int position) {
        final ComicsRelease item = getItem(position);
        if (item == null) {
            return RecyclerView.NO_ID;
        } else {
            return item.release.id;
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

        ReleasesRecyclerViewAdapter build() {
            final ReleasesRecyclerViewAdapter adapter = new ReleasesRecyclerViewAdapter();
            // questo è necessario insieme all'override di getItemId() per funzionare con SelectionTracker
            adapter.setHasStableIds(true);
            mRecyclerView.setAdapter(adapter);

            final SelectionTracker.Builder<Long> builder = new SelectionTracker.Builder<>(
                    "comics-selection",
                    mRecyclerView,
                    new CustomItemKeyProvider(mRecyclerView, ItemKeyProvider.SCOPE_MAPPED),
                    new ReleaseItemDetailsLookup(mRecyclerView),
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

//            // appena un item viene inserito mi sposto sulla sua posizione
//            adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
//                @Override
//                public void onItemRangeInserted(int positionStart, int itemCount) {
//                    mRecyclerView.scrollToPosition(positionStart);
//                }
//            });

            return adapter;
        }
    }

    private static DiffUtil.ItemCallback<ComicsRelease> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<ComicsRelease>() {
                // ComicsWithReleases details may have changed if reloaded from the database,
                // but ID is fixed.
                @Override
                public boolean areItemsTheSame(@NonNull ComicsRelease oldComicsRelease,
                                               @NonNull ComicsRelease newComicsRelease) {
                    return oldComicsRelease.release.id == newComicsRelease.release.id;
                }

                @Override
                public boolean areContentsTheSame(@NonNull ComicsRelease oldComicsRelease,
                                                  @NonNull ComicsRelease newComicsRelease) {
                    return oldComicsRelease.equals(newComicsRelease) &&
                            oldComicsRelease.comics.lastUpdate == newComicsRelease.comics.lastUpdate &&
                            oldComicsRelease.release.lastUpdate == newComicsRelease.release.lastUpdate;
                }
            };
}
