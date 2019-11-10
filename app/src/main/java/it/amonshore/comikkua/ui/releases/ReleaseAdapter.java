package it.amonshore.comikkua.ui.releases;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.util.Iterator;
import java.util.Objects;

import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.OnItemActivatedListener;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.IReleaseViewModelItem;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseHeader;
import it.amonshore.comikkua.ui.CustomItemKeyProvider;

public class ReleaseAdapter extends ListAdapter<IReleaseViewModelItem, AReleaseViewModelItemViewHolder> {
    private SelectionTracker<Long> mSelectionTracker;
    private ReleaseViewHolder.Callback mReleaseViewHolderCallback;
//    @MenuRes
//    private int mReleaseMenuRes;
//    private ReleaseCallback mRelaseCallback;

    private ReleaseAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public void onBindViewHolder(@NonNull AReleaseViewModelItemViewHolder holder, int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item != null) {
            if (item.getItemType() == ReleaseHeader.ITEM_TYPE) {
                holder.bind(item, false);
            } else {
                final ComicsRelease release = (ComicsRelease)item;
                holder.bind(item, mSelectionTracker.isSelected(release.release.id));
            }
        } else {
            holder.clear();
        }
    }

    @NonNull
    @Override
    public AReleaseViewModelItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ReleaseHeader.ITEM_TYPE) {
            return ReleaseHeaderViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
        } else {
            return ReleaseViewHolder.create(LayoutInflater.from(parent.getContext()), parent, mReleaseViewHolderCallback);
        }
    }

    @Override
    public long getItemId(int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item == null) {
            return RecyclerView.NO_ID;
        } else {
            return item.getId();
        }
    }

    @Override
    public int getItemViewType(int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item == null) {
            return RecyclerView.INVALID_TYPE;
        } else {
            return item.getItemType();
        }
    }

    @Nullable
    public IReleaseViewModelItem getItemAt(int position) {
        return getItem(position);
    }

    SelectionTracker<Long> getSelectionTracker() {
        return mSelectionTracker;
    }

    interface OnItemSelectedListener {

        void onSelectionChanged(@Nullable Iterator<Long> keys, int size);
    }

    interface ReleaseCallback {

        void onReleasePurchased(@NonNull ComicsRelease release, boolean toBePurchased);

        void onReleaseMenuItemSelected(@NonNull MenuItem item, @NonNull ComicsRelease release);
    }

    static class Builder {
        private final RecyclerView mRecyclerView;
//        private OnItemActivatedListener<Long> mOnItemActivatedListener;
        private OnItemSelectedListener mOnItemSelectedListener;
        @MenuRes
        private int releaseMenuRes;
        private ReleaseCallback releaseCallback;

        Builder(@NonNull RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

//        Builder withOnItemActivatedListener(OnItemActivatedListener<Long> listener) {
//            mOnItemActivatedListener = listener;
//            return this;
//        }

        Builder withOnItemSelectedListener(OnItemSelectedListener listener) {
            mOnItemSelectedListener = listener;
            return this;
        }

        Builder withReleaseCallback(@MenuRes int menuRes, @NonNull ReleaseCallback callback) {
            releaseMenuRes = menuRes;
            releaseCallback = callback;
            return this;
        }

        ReleaseAdapter build() {
            final ReleaseAdapter adapter = new ReleaseAdapter();
            adapter.mReleaseViewHolderCallback = new ReleaseViewHolder.Callback(releaseMenuRes) {
                @Override
                void onReleaseActivated(long comicsId, long id, int position) {
                    if (releaseCallback != null) {
                        final IReleaseViewModelItem item = adapter.getItemAt(position);
                        if (item != null) {
                            final ComicsRelease release = (ComicsRelease) item;
                            releaseCallback.onReleasePurchased(release, !release.release.purchased);
                        }
                    }
                }

                @Override
                void onReleaseMenuSelected(@NonNull MenuItem menuItem, long comicsId, long id, int position) {
                    if (releaseCallback != null) {
                        final IReleaseViewModelItem item = adapter.getItemAt(position);
                        if (item != null) {
                            final ComicsRelease release = (ComicsRelease) item;
                            releaseCallback.onReleaseMenuItemSelected(menuItem, release);
                        }
                    }
                }
            };
            // questo è necessario insieme all'override di getItemId() per funzionare con SelectionTracker
            adapter.setHasStableIds(true);
            mRecyclerView.setAdapter(adapter);

            final SelectionTracker.Builder<Long> builder = new SelectionTracker.Builder<>(
                    "comics-selection",
                    mRecyclerView,
                    new CustomItemKeyProvider(mRecyclerView, ItemKeyProvider.SCOPE_MAPPED),
                    new ReleaseItemDetailsLookup(mRecyclerView),
                    StorageStrategy.createLongStorage())
                    // escludo dalla selezione gli header
                    .withSelectionPredicate(new SelectionTracker.SelectionPredicate<Long>() {
                        @Override
                        public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
                            return key < ReleaseHeader.BASE_ID;
                        }

                        @Override
                        public boolean canSetStateAtPosition(int position, boolean nextState) {
                            return true;
                        }

                        @Override
                        public boolean canSelectMultiple() {
                            return true;
                        }
                    });

//            if (mOnItemActivatedListener != null) {
//                builder.withOnItemActivatedListener(mOnItemActivatedListener);
//            }

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

            // TODO: il purchase deve essere gestito tramite swipe
            // TODO: il click apre la release in edit, oppure se multi apre il dettaglio comics
//            // TEST
//            final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
//
//                @Override
//                public boolean isLongPressDragEnabled() {
//                    return false;
//                }
//
//                @Override
//                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
//                    return false;
//                }
//
//                @Override
//                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
//                    LogHelper.d("Release swiped");
//                    final int position = viewHolder.getAdapterPosition();
//                    adapter.notifyItemChanged(position);
//                }
//            });
//            itemTouchHelper.attachToRecyclerView(mRecyclerView);
//            // TEST

            return adapter;
        }
    }

    private static DiffUtil.ItemCallback<IReleaseViewModelItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<IReleaseViewModelItem>() {
                // ComicsRelease details may have changed if reloaded from the database,
                // but ID is fixed.
                @Override
                public boolean areItemsTheSame(@NonNull IReleaseViewModelItem oldItem,
                                               @NonNull IReleaseViewModelItem newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull IReleaseViewModelItem oldItem,
                                                  @NonNull IReleaseViewModelItem newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };
}
