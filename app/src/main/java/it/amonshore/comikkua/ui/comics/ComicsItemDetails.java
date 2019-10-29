package it.amonshore.comikkua.ui.comics;

import androidx.annotation.Nullable;
import androidx.recyclerview.selection.ItemDetailsLookup;

public class ComicsItemDetails extends ItemDetailsLookup.ItemDetails<Long> {

    private int mPosition;
    private Long mSelectionKey;

    public ComicsItemDetails(int position, Long selectionKey) {
        mPosition = position;
        mSelectionKey = selectionKey;
    }

    @Override
    public int getPosition() {
        return mPosition;
    }

    @Nullable
    @Override
    public Long getSelectionKey() {
        return mSelectionKey;
    }
}
