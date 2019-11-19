package it.amonshore.comikkua.ui.releases;

import android.net.Uri;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.IReleaseViewModelItem;
import it.amonshore.comikkua.data.release.MultiRelease;
import it.amonshore.comikkua.ui.DrawableTextViewTarget;
import it.amonshore.comikkua.ui.GlideHelper;

import static it.amonshore.comikkua.data.release.Release.NO_RELEASE_ID;

public class ReleaseViewHolder extends AReleaseViewModelItemViewHolder {

    private final TextView mNumbers, mDate, mTitle, mInfo, mNotes;
    private final View mPurchased, mOrdered, mMenu, mMainCard, mBackground;

    private long mComicsId;
    private long mId;

    private float mMainCardElevationPx;

    private ReleaseViewHolder(View itemView, final ReleaseViewHolderCallback callback) {
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

        if (callback != null) {
            itemView.setOnClickListener(v -> {
                callback.onReleaseClick(mComicsId, mId, getAdapterPosition());
            });

            if (callback.mMenuRes != 0) {
                final PopupMenu popupMenu = new PopupMenu(itemView.getContext(), mMenu);
                popupMenu.inflate(callback.mMenuRes);
                popupMenu.setOnMenuItemClickListener(item -> {
                    callback.onReleaseMenuSelected(item, mComicsId, mId, getAdapterPosition());
                    return true;
                });
                mMenu.setVisibility(View.VISIBLE);
                mMenu.setOnClickListener(v -> {
                    popupMenu.show();
                });
            }
        } else {
            itemView.setOnClickListener(null);
            mMenu.setVisibility(View.INVISIBLE);
            mMenu.setOnClickListener(null);
        }
    }

    @Override
    public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
        return new ReleaseItemDetails(getAdapterPosition(), mId);
    }

    @Override
    public void bind(@NonNull IReleaseViewModelItem item, boolean selected, RequestManager requestManager) {
        bind((ComicsRelease) item, selected, requestManager);
    }

    private void bind(@NonNull ComicsRelease item, boolean selected, RequestManager requestManager) {
        itemView.setActivated(selected);
        mComicsId = item.comics.id;
        mId = item.release.id;
        if (item instanceof MultiRelease) {
            mNumbers.setText(extractNumbers((MultiRelease) item));
        } else {
            mNumbers.setText(String.format(Locale.getDefault(), "%d", item.release.number));
        }
        mDate.setText(TextUtils.isEmpty(item.release.date) ?
                null :
                DateFormatterHelper.toHumanReadable(itemView.getContext(), item.release.date, DateFormatterHelper.STYLE_FULL));
        mTitle.setText(item.comics.name);
        mInfo.setText(Utility.join(", ", true, item.comics.publisher, item.comics.authors));
        mNotes.setText(TextUtils.isEmpty(item.release.notes) ? item.comics.notes : item.release.notes);
        mOrdered.setVisibility(item.release.ordered ? View.VISIBLE : View.INVISIBLE);
        if (item.release.purchased) {
            mPurchased.setVisibility(View.VISIBLE);
            mMainCard.setElevation(0);
            mBackground.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorItemBackgroundLighter));
        } else {
            mPurchased.setVisibility(View.INVISIBLE);
            mMainCard.setElevation(mMainCardElevationPx);
            mBackground.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorItemBackgroundLight));
        }

        if (requestManager != null && item.comics.image != null) {
            requestManager
                    .load(Uri.parse(item.comics.image))
                    .apply(GlideHelper.getSquareOptions())
                    .into(new DrawableTextViewTarget(mNumbers));
        } else {
            mNumbers.setBackgroundColor(itemView.getContext().getResources().getColor(R.color.colorItemBackgroundAlt));
        }
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
        mPurchased.setVisibility(View.INVISIBLE);
        mOrdered.setVisibility(View.INVISIBLE);
    }

    private String extractNumbers(@NonNull MultiRelease multiRelease) {
        final int[] numbers = new int[1 + multiRelease.otherReleases.size()];
        numbers[0] = multiRelease.release.number;
        for (int ii = 0; ii < multiRelease.otherReleases.size(); ii++) {
            numbers[ii + 1] = multiRelease.otherReleases.get(ii).number;
        }
        return Utility.formatInterval(null, ",", "~", numbers).toString();
    }

    static ReleaseViewHolder create(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, ReleaseViewHolderCallback callback) {
        return new ReleaseViewHolder(inflater.inflate(R.layout.listitem_release, parent, false), callback);
    }

}
