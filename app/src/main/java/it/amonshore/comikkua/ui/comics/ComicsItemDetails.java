package it.amonshore.comikkua.ui.comics;

import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;

public class ComicsItemDetails<T> extends ItemDetailsLookup.ItemDetails<T> {

    private final int mPosition;
    private final T mSelectionKey;

    public ComicsItemDetails(int position, T selectionKey) {
        mPosition = position;
        mSelectionKey = selectionKey;
    }

    @Override
    public int getPosition() {
        return mPosition;
    }

    @Nullable
    @Override
    public T getSelectionKey() {
        return mSelectionKey;
    }
}
