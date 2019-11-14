package it.amonshore.comikkua.ui.releases;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

public class ReleaseItemDetailsLookup extends ItemDetailsLookup<Long> {

    private RecyclerView mRecyclerView;

    public ReleaseItemDetailsLookup(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Nullable
    @Override
    public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
        final View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
            if (holder instanceof AReleaseViewModelItemViewHolder) {
                return ((AReleaseViewModelItemViewHolder) holder).getItemDetails();
            }
        }
        return null;
    }
}
