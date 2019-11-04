package it.amonshore.comikkua.ui.releases;

import android.view.View;

import androidx.annotation.NonNull;
import it.amonshore.comikkua.data.release.IReleaseViewModelItem;
import it.amonshore.comikkua.ui.IViewHolderWithDetails;

public abstract class AReleaseViewModelItemViewHolder extends IViewHolderWithDetails<Long> {

    public AReleaseViewModelItemViewHolder(View itemView) {
        super(itemView);
    }

    public abstract void bind(@NonNull IReleaseViewModelItem item, boolean selected);

    public abstract void clear();
}
