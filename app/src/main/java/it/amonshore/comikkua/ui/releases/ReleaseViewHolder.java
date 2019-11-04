package it.amonshore.comikkua.ui.releases;

import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.IReleaseViewModelItem;

import static it.amonshore.comikkua.data.release.Release.NO_RELEASE_ID;

public class ReleaseViewHolder extends AReleaseViewModelItemViewHolder {
    private final TextView mNumbers, mDate, mTitle, mInfo, mNotes;
    private final View mPurchased, mOrdered, mMenu, mMainCard, mBackground;

    private long mComicsId;
    private long mId;

    private float mMainCardElevationPx;

    private ReleaseViewHolder(View itemView) {
        super(itemView);
        mNumbers = itemView.findViewById(R.id.txt_release_numbers);
        mDate = itemView.findViewById(R.id.txt_release_date);
        mTitle = itemView.findViewById(R.id.txt_release_title);
        mInfo = itemView.findViewById(R.id.txt_release_info);
        mNotes = itemView.findViewById(R.id.txt_release_notes);
        mPurchased = itemView.findViewById(R.id.img_release_purchased);
        mOrdered = itemView.findViewById(R.id.img_release_ordered);
        mMenu = itemView.findViewById(R.id.img_release_menu);
        mMainCard = itemView.findViewById(R.id.release_main_card);
        mBackground = itemView.findViewById(R.id.release_background);

        mMainCardElevationPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                2f,
                itemView.getResources().getDisplayMetrics());
    }

    @Override
    public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
        return new ReleaseItemDetails(getAdapterPosition(), mId);
    }

    @Override
    public void bind(@NonNull IReleaseViewModelItem item, boolean selected) {
        bind((ComicsRelease) item, selected);
    }

    public void bind(@NonNull ComicsRelease item, boolean selected) {
        itemView.setActivated(selected);
        mComicsId = item.comics.id;
        mId = item.release.id;
        mNumbers.setText(String.format(Locale.getDefault(), "%d", item.release.number));
        mDate.setText(item.release.date); // TODO: formattare data relase (oggi, domani, etc.)
        mTitle.setText(item.comics.name);
        mInfo.setText(TextUtils.join(",", new String[] { item.comics.publisher, item.comics.authors }));
        mNotes.setText(TextUtils.isEmpty(item.release.notes) ? item.comics.notes : item.release.notes);
        mOrdered.setVisibility(item.release.ordered ? View.VISIBLE : View.GONE);
        if (item.release.purchased) {
            mPurchased.setVisibility(View.VISIBLE);
            mMainCard.setElevation(0);
            mBackground.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorItemBackgroundLighter));
        } else {
            mPurchased.setVisibility(View.GONE);
            mMainCard.setElevation(mMainCardElevationPx);
            mBackground.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorItemBackgroundLight));
        }

        // TODO: gestire il menu
    }

    @Override
    public void clear() {
        itemView.setActivated(false);
        mComicsId = Comics.NO_COMICS_ID;
        mId = NO_RELEASE_ID;
        mNumbers.setText("");
        mDate.setText("");
        mTitle.setText("");
        mInfo.setText("");
        mNotes.setText("");
        mPurchased.setVisibility(View.GONE);
        mOrdered.setVisibility(View.GONE);
    }

    static ReleaseViewHolder create(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return new ReleaseViewHolder(inflater.inflate(R.layout.listitem_release, parent, false));
    }

}
