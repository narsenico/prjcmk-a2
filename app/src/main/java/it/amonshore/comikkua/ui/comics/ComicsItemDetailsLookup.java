package it.amonshore.comikkua.ui.comics;

import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;
import it.amonshore.comikkua.ui.IViewHolderWithDetails;

/**
 * Sorgente del dettaglio sull'item della RecyclerView.
 *
 * @param <T>   Indica il tipo della chiave dell'item
 * @param <VH>  Indica il tipo del ViewHolder usato per gli item
 */
public class ComicsItemDetailsLookup<T, VH extends IViewHolderWithDetails<T>> extends ItemDetailsLookup<T> {

    private final RecyclerView mRecyclerView;
    private final Class<VH> mKlass;

    public ComicsItemDetailsLookup(@NonNull RecyclerView recyclerView,
                                   @NonNull Class<VH> klass) {
        mRecyclerView = recyclerView;
        mKlass = klass;
    }

    @Nullable
    @Override
    public ItemDetails<T> getItemDetails(@NonNull MotionEvent e) {
        final View view = mRecyclerView.findChildViewUnder(e.getX(), e.getY());
        if (view != null) {
            final RecyclerView.ViewHolder holder = mRecyclerView.getChildViewHolder(view);
            if (mKlass.isAssignableFrom(holder.getClass())) {
                return mKlass.cast(holder).getItemDetails();
            }
        }
        return null;
    }
}
