package it.amonshore.comikkua.ui.comics;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.OnItemActivatedListener;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import java.util.Iterator;
import java.util.List;

import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.ComicsWithReleases;
import it.amonshore.comikkua.ui.ActionModeController;

public class ComicsRecyclerViewAdapter extends RecyclerView.Adapter<ComicsViewHolder> {

    private final LayoutInflater mLayoutInflater;
    private List<ComicsWithReleases> mComicsList;
    private SelectionTracker<Long> mSelectionTracker;

    private ComicsRecyclerViewAdapter(@NonNull Context context) {
        mLayoutInflater = LayoutInflater.from(context);
    }

    @Override
    public void onBindViewHolder(@NonNull ComicsViewHolder holder, int position) {
        if (mComicsList != null) {
            final ComicsWithReleases comics = mComicsList.get(position);
            holder.bind(comics, mSelectionTracker.isSelected(comics.comics.id));
        } else {
            // TODO: bind empty comics
            LogHelper.w("comics list null!!!");
        }
    }

    @NonNull
    @Override
    public ComicsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ComicsViewHolder(mLayoutInflater.inflate(R.layout.listitem_comics, parent, false));
    }

    @Override
    public long getItemId(int position) {
        return mComicsList == null || mComicsList.size() < position ? RecyclerView.NO_ID : mComicsList.get(position).comics.id;
    }

    @Override
    public int getItemCount() {
        return mComicsList == null ? 0 : mComicsList.size();
    }

    public void setComics(List<ComicsWithReleases> comicsList) {
        mComicsList = comicsList;
        notifyDataSetChanged();
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
        private final Context mContext;
        private final RecyclerView mRecyclerView;
        private OnItemActivatedListener<Long> mOnItemActivatedListener;
        private OnItemSelectedListener mOnItemSelectedListener;
        private ActionModeControllerCallback mActionModeControllerCallback;
        private int actionModeMenuRes;

        Builder(@NonNull Context context, @NonNull RecyclerView recyclerView) {
            mContext = context;
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

        ComicsRecyclerViewAdapter build() {
            final ComicsRecyclerViewAdapter adapter = new ComicsRecyclerViewAdapter(mContext);
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

    /**
     * Non posso usare {@link androidx.recyclerview.selection.StableIdKeyProvider} perché fa casino
     * se elimino elementi dall'adapter.
     */
    static class CustomItemKeyProvider extends ItemKeyProvider<Long> {
        private RecyclerView mRecyclerView;
        private RecyclerView.Adapter mAdapter;

        CustomItemKeyProvider(@NonNull RecyclerView recyclerView, int scope) {
            super(scope);
            mRecyclerView = recyclerView;
            mAdapter = recyclerView.getAdapter();
        }

        @Nullable
        @Override
        public Long getKey(int position) {
            return mAdapter == null ? RecyclerView.NO_ID : mAdapter.getItemId(position);
        }

        @Override
        public int getPosition(@NonNull Long key) {
            final RecyclerView.ViewHolder viewHolder = mRecyclerView.findViewHolderForItemId(key);
            return viewHolder == null ? RecyclerView.NO_POSITION : viewHolder.getLayoutPosition();
        }
    }

}