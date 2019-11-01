package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.Comics;
import it.amonshore.comikkua.data.ComicsWithReleases;
import it.amonshore.comikkua.data.Release;
import it.amonshore.comikkua.data.XComics;
import it.amonshore.comikkua.ui.IViewHolderWithDetails;

class ComicsViewHolder extends IViewHolderWithDetails<Long> {
    private final TextView mInitial, mName, mPublisher, mAuthors, mNotes,
            mLast, mNext, mMissing;
    private long mId;

    ComicsViewHolder(View itemView) {
        super(itemView);
        mInitial = itemView.findViewById(R.id.txt_comics_initial);
        mName = itemView.findViewById(R.id.txt_comics_name);
        mPublisher = itemView.findViewById(R.id.txt_comics_publisher);
        mAuthors = itemView.findViewById(R.id.txt_comics_authors);
        mNotes = itemView.findViewById(R.id.txt_comics_notes);
        mLast = itemView.findViewById(R.id.txt_comics_release_last);
        mNext = itemView.findViewById(R.id.txt_comics_release_next);
        mMissing = itemView.findViewById(R.id.txt_comics_release_missing);
    }

    @Override
    public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
        return new ComicsItemDetails(getAdapterPosition(), mId);
    }

    void bind(@NonNull ComicsWithReleases comics, boolean selected) {
        itemView.setActivated(selected);
        mId = comics.comics.id;
        mInitial.setText(comics.comics.getInitial());
        mName.setText(comics.comics.name);
        mPublisher.setText(comics.comics.publisher);
        mAuthors.setText(comics.comics.authors);
        mNotes.setText(comics.comics.notes);

        final Context context = itemView.getContext();

        final Release lastRelease = comics.getLastPurchasedRelease();
        mLast.setText(lastRelease == null ? context.getString(R.string.release_last_none):
                context.getString(R.string.release_last, lastRelease.number));

        final Release nextRelease = comics.getNextToPurchaseRelease();
        mNext.setText(nextRelease == null ? context.getString(R.string.release_next_none) :
                context.getString(R.string.release_next, nextRelease.number));

        final int missingCount = comics.getMissingReleaseCount();
        mMissing.setText(context.getString(R.string.release_missing, missingCount));
    }

    void clear() {
        itemView.setActivated(false);
        mId = Comics.NO_COMICS_ID;
        mInitial.setText("");
        mName.setText("");
        mPublisher.setText("");
        mAuthors.setText("");
        mNotes.setText("");
        mLast.setText("");
        mNext.setText("");
        mMissing.setText("");
    }

}
