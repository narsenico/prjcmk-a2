package it.amonshore.comikkua.ui;

import android.view.View;

import androidx.recyclerview.selection.ItemDetailsLookup;
import androidx.recyclerview.widget.RecyclerView;

public abstract class IViewHolderWithDetails<K> extends RecyclerView.ViewHolder {

    public IViewHolderWithDetails(View itemView) {
        super(itemView);
    }

    public abstract ItemDetailsLookup.ItemDetails<K> getItemDetails();

}
