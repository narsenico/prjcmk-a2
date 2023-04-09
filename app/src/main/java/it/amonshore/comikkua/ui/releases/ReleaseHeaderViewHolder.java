package it.amonshore.comikkua.ui.releases;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;

import org.jetbrains.annotations.Nullable;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.Constants;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.release.DatedRelease;
import it.amonshore.comikkua.data.release.IReleaseViewModelItem;
import it.amonshore.comikkua.data.release.LostRelease;
import it.amonshore.comikkua.data.release.MissingRelease;
import it.amonshore.comikkua.data.release.NotPurchasedRelease;
import it.amonshore.comikkua.data.release.PurchasedRelease;
import it.amonshore.comikkua.data.release.ReleaseHeader;
import it.amonshore.comikkua.databinding.ListitemReleaseHeaderBinding;

public class ReleaseHeaderViewHolder extends AReleaseViewModelItemViewHolder {

    private final ListitemReleaseHeaderBinding _binding;
    private IReleaseViewModelItem _item;

    private ReleaseHeaderViewHolder(View itemView) {
        super(itemView);
        _binding = ListitemReleaseHeaderBinding.bind(itemView);
    }

    @Override
    public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
        return new ReleaseItemDetails(getLayoutPosition(), _item.getId());
    }

    @Override
    public void bind(@NonNull IReleaseViewModelItem item,
                     boolean _selected,
                     @Nullable RequestManager _requestManager,
                     @Nullable IReleaseViewHolderCallback _callback) {
        _item = item;

        final ReleaseHeader header = (ReleaseHeader) item;
        switch (header.getItemType()) {
            case LostRelease.TYPE:
                _binding.txtTitle.setText(R.string.header_lost);
                break;
            case MissingRelease.TYPE:
                _binding.txtTitle.setText(R.string.header_missing);
                break;
            case DatedRelease.TYPE:
                _binding.txtTitle.setText(R.string.header_current_period);
                break;
            case DatedRelease.TYPE_NEXT:
                _binding.txtTitle.setText(R.string.header_next_period);
                break;
            case NotPurchasedRelease.TYPE:
                _binding.txtTitle.setText(R.string.header_not_purchased);
                break;
            case PurchasedRelease.TYPE:
                _binding.txtTitle.setText(R.string.header_purchased);
                break;
            case Constants.RELEASE_NEW:
                _binding.txtTitle.setText(R.string.header_new_releases);
                break;
            case DatedRelease.TYPE_OTHER:
            default:
                _binding.txtTitle.setText(R.string.header_other);
                break;
        }
        _binding.txtInfo.setText(itemView.getResources().getString(R.string.header_count, header.purchasedCount, header.totalCount));
        _binding.separator.setVisibility(getLayoutPosition() == 0 ? View.GONE : View.VISIBLE);
    }

    @Override
    public void clear() {
        _binding.txtTitle.setText("");
        _binding.txtInfo.setText("");
    }

    static ReleaseHeaderViewHolder create(@NonNull LayoutInflater inflater,
                                          @NonNull ViewGroup parent) {
        return new ReleaseHeaderViewHolder(inflater.inflate(R.layout.listitem_release_header, parent, false));
    }
}
