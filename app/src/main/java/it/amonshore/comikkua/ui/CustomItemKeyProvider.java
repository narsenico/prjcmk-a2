package it.amonshore.comikkua.ui;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Non posso usare {@link androidx.recyclerview.selection.StableIdKeyProvider} perché fa casino
 * se elimino elementi dall'adapter.
 */
public class CustomItemKeyProvider extends ItemKeyProvider<Long> {
    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;

    public CustomItemKeyProvider(@NonNull RecyclerView recyclerView, int scope) {
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
