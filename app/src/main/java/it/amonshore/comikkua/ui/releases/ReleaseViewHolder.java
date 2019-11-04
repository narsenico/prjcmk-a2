package it.amonshore.comikkua.ui.releases;

import android.graphics.Rect;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.ComicsRelease;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.LostRelease;
import it.amonshore.comikkua.data.MissingRelease;
import it.amonshore.comikkua.ui.IViewHolderWithDetails;

import static it.amonshore.comikkua.data.Release.*;

public class ReleaseViewHolder extends IViewHolderWithDetails<Long> {
    private final TextView mHeader, mNumbers, mDate, mTitle, mInfo, mNotes;
    private final View mPurchased, mOrdered, mMenu, mMainCard, mBackground;

    private long mComicsId;
    private long mId;

    private float mMainCardElevationPx;

    private ReleaseViewHolder(View itemView) {
        super(itemView);
        mHeader = itemView.findViewById(R.id.txt_header);
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

    void bind(@NonNull ComicsRelease release, boolean selected, boolean showHeader, boolean isFirst) {
        itemView.setActivated(selected);
        mComicsId = release.comics.id;
        mId = release.release.id;
        mNumbers.setText(String.format(Locale.getDefault(), "%d", release.release.number));
        mDate.setText(release.release.date); // TODO: formattare data relase (oggi, domani, etc.)
        mTitle.setText(release.comics.name);
        mInfo.setText(TextUtils.join(",", new String[] { release.comics.publisher, release.comics.authors }));
        mNotes.setText(TextUtils.isEmpty(release.release.notes) ? release.comics.notes : release.release.notes);
        mOrdered.setVisibility(release.release.ordered ? View.VISIBLE : View.GONE);
        if (release.release.purchased) {
            mPurchased.setVisibility(View.VISIBLE);
            mMainCard.setElevation(0);
            mBackground.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorItemBackgroundLighter));
        } else {
            mPurchased.setVisibility(View.GONE);
            mMainCard.setElevation(mMainCardElevationPx);
            mBackground.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.colorItemBackgroundLight));
        }

        mHeader.setOnClickListener(v -> {
            LogHelper.d(("HEADER CLICK"));
        });
        mHeader.setOnLongClickListener(v -> {
            LogHelper.d(("HEADER LONG CLICK"));
            return true;
        });

        if (showHeader) {
            if (release.type == LostRelease.TYPE) {
                mHeader.setText("Lost");
            } else if (release.type == MissingRelease.TYPE) {
                mHeader.setText("Missing");
            } else {
                mHeader.setText("Other");
            }
            if (isFirst) {
                mHeader.setBackground(null);
            } else {
                mHeader.setBackgroundResource(R.drawable.background_border_up);
            }
            mHeader.setVisibility(View.VISIBLE);
        } else {
            mHeader.setVisibility(View.GONE);
        }

        // TODO: gestire il menu
    }

    void clear() {
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
        mHeader.setVisibility(View.GONE);
    }

    static ReleaseViewHolder create(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent) {
        return new ReleaseViewHolder(inflater.inflate(R.layout.listitem_release, parent, false));
    }

}
