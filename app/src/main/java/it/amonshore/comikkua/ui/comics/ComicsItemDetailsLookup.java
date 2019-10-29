package it.amonshore.comikkua.ui.comics;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

public class ComicsItemDetailsLookup extends ItemDetailsLookup<Long> {

    private RecyclerView mRecyclerView;

    public ComicsItemDetailsLookup(RecyclerView recyclerView) {
        mRecyclerView = recyclerView;
    }

    @Nullable
    @Override
    public ItemDetails<Long> getItemDetails(@NonNull MotionEvent e) {
        final View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
            if (holder instanceof ComicsViewHolder) {
                return ((ComicsViewHolder) holder).getItemDetails();
            }
        }
        return null;
    }
}
