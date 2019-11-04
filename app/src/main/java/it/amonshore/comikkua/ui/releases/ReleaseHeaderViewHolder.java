package it.amonshore.comikkua.ui.releases;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.release.DatedRelease;
import it.amonshore.comikkua.data.release.IReleaseViewModelItem;
import it.amonshore.comikkua.data.release.LostRelease;
import it.amonshore.comikkua.data.release.MissingRelease;
import it.amonshore.comikkua.data.release.ReleaseHeader;

public class ReleaseHeaderViewHolder extends AReleaseViewModelItemViewHolder {
    private final TextView mTitle, mInfo;
    private final View mSeparator;
    private long mId;

    private ReleaseHeaderViewHolder(View itemView) {
        super(itemView);
        mTitle = itemView.findViewById(R.id.txt_title);
        mInfo = itemView.findViewById(R.id.txt_info);
        mSeparator = itemView.findViewById(R.id.separator);
    }

    @Override
    public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
        return new ReleaseItemDetails(getAdapterPosition(), mId);
    }

    @Override
    public void bind(@NonNull IReleaseViewModelItem item, boolean selected) {
        bind((ReleaseHeader) item);
    }

    public void bind(@NonNull ReleaseHeader item) {
        switch (item.getType()) {
            case LostRelease.TYPE:
                mTitle.setText(R.string.header_lost);
                break;
            case MissingRelease.TYPE:
                mTitle.setText(R.string.header_missing);
                break;
            case DatedRelease.TYPE:
                mTitle.setText(R.string.header_current_period);
                break;
            case DatedRelease.TYPE_NEXT:
                mTitle.setText(R.string.header_next_period);
                break;
            case DatedRelease.TYPE_OTHER:
            default:
                mTitle.setText(R.string.header_other);
                break;
        }
        mInfo.setText(itemView.getResources().getString(R.string.header_count, item.purchasedCount, item.totalCount));
        mSeparator.setVisibility(getAdapterPosition() == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void clear() {
        mTitle.setText("");
        mInfo.setText("");
    }

    static ReleaseHeaderViewHolder create(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return new ReleaseHeaderViewHolder(inflater.inflate(R.layout.listitem_release_header, parent, false));
    }
}
