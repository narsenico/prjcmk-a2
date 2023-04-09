package it.amonshore.comikkua.ui.releases;

import android.net.Uri;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.RequestManager;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.IReleaseViewModelItem;
import it.amonshore.comikkua.data.release.MultiRelease;
import it.amonshore.comikkua.databinding.ListitemReleaseLiteBinding;
import it.amonshore.comikkua.ui.DrawableTextViewTarget;
import it.amonshore.comikkua.ui.ImageHelperKt;

public class ReleaseLiteViewHolder extends AReleaseViewModelItemViewHolder {

    private final ListitemReleaseLiteBinding _binding;
    private final float _mainCardElevation;
    private IReleaseViewModelItem _item;

    private ReleaseLiteViewHolder(View itemView) {
        super(itemView);
        _binding = ListitemReleaseLiteBinding.bind(itemView);
        _mainCardElevation = _binding.releaseMainCard.getElevation();
    }

    @Override
    public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
        return new ReleaseItemDetails(getLayoutPosition(), _item.getId());
    }

    @Override
    public void bind(@NonNull IReleaseViewModelItem item,
                     boolean selected,
                     @Nullable RequestManager requestManager,
                     @Nullable IReleaseViewHolderCallback callback) {
        _item = item;

        if (callback != null) {
            itemView.setOnClickListener(v -> callback.onReleaseClick(item, getLayoutPosition()));
        } else {
            itemView.setOnClickListener(null);
        }

        itemView.setActivated(selected);
        final ComicsRelease release = (ComicsRelease) item;
        if (release instanceof MultiRelease) {
            _binding.txtReleaseNumbers.setText(extractNumbers((MultiRelease) release));
        } else {
            _binding.txtReleaseNumbers.setText(String.format(Locale.getDefault(), "%d", release.release.number));
        }
        _binding.txtReleaseDate.setText(TextUtils.isEmpty(release.release.date) ?
                null :
                DateFormatterHelper.toHumanReadable(itemView.getContext(), release.release.date, DateFormatterHelper.STYLE_FULL));
        _binding.txtReleaseNotes.setText(release.release.notes);
        _binding.imgReleaseOrdered.setVisibility(release.release.ordered ? View.VISIBLE : View.INVISIBLE);
        if (release.release.purchased) {
            _binding.imgReleasePurchased.setVisibility(View.VISIBLE);
            _binding.releaseMainCard.setElevation(0);
            _binding.releaseBackground.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorItemPurchased));
        } else {
            _binding.imgReleasePurchased.setVisibility(View.INVISIBLE);
            _binding.releaseMainCard.setElevation(_mainCardElevation);
            _binding.releaseBackground.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorItemNotPurchased));
        }

        if (requestManager != null && release.comics.hasImage()) {
            requestManager
                    .load(Uri.parse(release.comics.image))
                    .apply(ImageHelperKt.getInstance(itemView.getContext()).getSquareOptions())
                    .into(new DrawableTextViewTarget(_binding.txtReleaseNumbers));
        } else {
            _binding.txtReleaseNumbers.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.colorItemBackgroundAlt));
        }
    }

    @Override
    public void clear() {
        itemView.setActivated(false);
        _item = null;
        _binding.txtReleaseNumbers.setText("");
        _binding.txtReleaseDate.setText("");
        _binding.txtReleaseNotes.setText("");
        _binding.imgReleasePurchased.setVisibility(View.INVISIBLE);
        _binding.imgReleaseOrdered.setVisibility(View.INVISIBLE);
    }

    @NonNull
    private String extractNumbers(@NonNull MultiRelease multiRelease) {
        final int[] numbers = new int[1 + multiRelease.otherReleases.size()];
        numbers[0] = multiRelease.release.number;
        for (int ii = 0; ii < multiRelease.otherReleases.size(); ii++) {
            numbers[ii + 1] = multiRelease.otherReleases.get(ii).number;
        }
        return Utility.formatInterval(null, ",", "~", numbers).toString();
    }

    @NonNull
    @Contract("_, _ -> new")
    static ReleaseLiteViewHolder create(@NonNull LayoutInflater inflater,
                                        @NonNull ViewGroup parent) {
        return new ReleaseLiteViewHolder(inflater.inflate(R.layout.listitem_release_lite, parent, false));
    }
}
