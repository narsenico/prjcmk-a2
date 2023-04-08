package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.Constants;
import it.amonshore.comikkua.DateFormatterHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.ui.DrawableTextViewTarget;
import it.amonshore.comikkua.ui.IViewHolderWithDetails;
import it.amonshore.comikkua.ui.ImageHelperKt;

class ComicsViewHolder extends IViewHolderWithDetails<Long> {
    private final TextView mInitial;
    private final TextView mName;
    private final TextView mPublisher;
    private final TextView mAuthors;
    private final TextView mNotes;
    private final TextView mLast;
    private final TextView mNext;
    private final TextView mMissing;
    private long mId;

    private ComicsViewHolder(View itemView, final IComicsViewHolderCallback<Long> callback) {
        super(itemView);
        mInitial = itemView.findViewById(R.id.txt_comics_initial);
        mName = itemView.findViewById(R.id.txt_comics_name);
        mPublisher = itemView.findViewById(R.id.txt_comics_publisher);
        mAuthors = itemView.findViewById(R.id.txt_comics_authors);
        mNotes = itemView.findViewById(R.id.txt_comics_notes);
        mLast = itemView.findViewById(R.id.txt_comics_release_last);
        mNext = itemView.findViewById(R.id.txt_comics_release_next);
        mMissing = itemView.findViewById(R.id.txt_comics_release_missing);

        final View menu = itemView.findViewById(R.id.img_comics_menu);
        if (callback != null) {
            itemView.setOnClickListener(v -> callback.onAction(mId, getLayoutPosition(),
                    Constants.VIEWHOLDER_ACTION_CLICK));
            menu.setVisibility(View.VISIBLE);
            menu.setOnClickListener(v -> callback.onAction(mId, getLayoutPosition(),
                    Constants.VIEWHOLDER_ACTION_MENU));
        } else {
            itemView.setOnClickListener(null);
            menu.setVisibility(View.INVISIBLE);
            menu.setOnClickListener(null);
        }
    }

    @Override
    public ItemDetailsLookup.ItemDetails<Long> getItemDetails() {
        return new ComicsItemDetails<>(getLayoutPosition(), mId);
    }

    void bind(@NonNull ComicsWithReleases comics, boolean selected, RequestManager requestManager) {
        itemView.setActivated(selected);
        mId = comics.comics.id;
        mName.setText(comics.comics.name);
        mPublisher.setText(comics.comics.publisher);
        mAuthors.setText(comics.comics.authors);
        mNotes.setText(comics.comics.notes);

        final Context context = itemView.getContext();

        final Release lastRelease = comics.getLastPurchasedRelease();
        mLast.setText(lastRelease == null ? context.getString(R.string.release_last_none) :
                context.getString(R.string.release_last, lastRelease.number));

        final Release nextRelease = comics.getNextToPurchaseRelease();
        if (nextRelease != null) {
            if (nextRelease.date != null) {
                // TODO: non mi piace, dovrei mostrare la data solo se futura e nel formato ddd dd MMM
                mNext.setText(context.getString(R.string.release_next_dated, nextRelease.number,
                        DateFormatterHelper.toHumanReadable(itemView.getContext(), nextRelease.date, DateFormatterHelper.STYLE_SHORT)));
            } else {
                mNext.setText(context.getString(R.string.release_next, nextRelease.number));
            }
        } else {
            mNext.setText(context.getString(R.string.release_next_none));
        }

        final int missingCount = comics.getNotPurchasedReleaseCount();
        mMissing.setText(context.getString(R.string.release_missing, missingCount));

//        // cambio il colore dello sfondo dell'iniziale in base al suo valore
//        final LayerDrawable layerDrawable = (LayerDrawable) ContextCompat.getDrawable(itemView.getContext(), R.drawable.background_comics_initial);
//        if (layerDrawable != null) {
//            final GradientDrawable drawable = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.background);
//            if (drawable != null) {
//                drawable.setColor(ContextCompat.getColor(itemView.getContext(),
//                        getInitialColor(comics.comics.getInitial().toUpperCase().charAt(0))));
//            }
//        }

        if (requestManager != null && comics.comics.hasImage()) {
            mInitial.setText("");
            requestManager
                    .load(Uri.parse(comics.comics.image))
                    .apply(ImageHelperKt.getInstance(context).getCircleOptions())
                    .into(new DrawableTextViewTarget(mInitial));
        } else {
            mInitial.setText(comics.comics.getInitial());
            mInitial.setBackgroundResource(R.drawable.background_comics_initial_noborder);
        }
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

//    @ColorRes
//    int getInitialColor(char initial) {
//        switch (initial) {
//            case 'A':
//                return R.color.colorItemBackgroundA;
//            case 'B':
//                return R.color.colorItemBackgroundB;
//            case 'C':
//                return R.color.colorItemBackgroundC;
//            case 'D':
//                return R.color.colorItemBackgroundD;
//            case 'E':
//                return R.color.colorItemBackgroundE;
//            case 'F':
//                return R.color.colorItemBackgroundF;
//            case 'G':
//                return R.color.colorItemBackgroundG;
//            case 'H':
//                return R.color.colorItemBackgroundH;
//            case 'I':
//                return R.color.colorItemBackgroundI;
//            case 'J':
//                return R.color.colorItemBackgroundJ;
//            case 'K':
//                return R.color.colorItemBackgroundK;
//            case 'L':
//                return R.color.colorItemBackgroundL;
//            case 'M':
//                return R.color.colorItemBackgroundM;
//            case 'N':
//                return R.color.colorItemBackgroundN;
//            case 'O':
//                return R.color.colorItemBackgroundO;
//            case 'P':
//                return R.color.colorItemBackgroundP;
//            case 'Q':
//                return R.color.colorItemBackgroundQ;
//            case 'R':
//                return R.color.colorItemBackgroundR;
//            case 'S':
//                return R.color.colorItemBackgroundS;
//            case 'T':
//                return R.color.colorItemBackgroundT;
//            case 'U':
//                return R.color.colorItemBackgroundU;
//            case 'V':
//                return R.color.colorItemBackgroundV;
//            case 'W':
//                return R.color.colorItemBackgroundW;
//            case 'X':
//                return R.color.colorItemBackgroundX;
//            case 'Y':
//                return R.color.colorItemBackgroundY;
//            case 'Z':
//                return R.color.colorItemBackgroundZ;
//            case '_':
//                return R.color.colorItemBackground_;
//            default:
//                return R.color.colorItemBackgroundAlt;
//        }
//    }

    static ComicsViewHolder create(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, IComicsViewHolderCallback<Long> callback) {
        return new ComicsViewHolder(inflater.inflate(R.layout.listitem_comics, parent, false), callback);
    }

}
