package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.web.CmkWebComics;
import it.amonshore.comikkua.ui.IViewHolderWithDetails;

class CmkWebComicsViewHolder extends IViewHolderWithDetails<String> {
    private final TextView mInitial;
    private final TextView mName;
    private final TextView mPublisher;
    private final TextView mAuthors;
    private final TextView mNotes;
    private final TextView mLast;
    private final TextView mNext;
    private final TextView mMissing;
    private String mId;

    private CmkWebComicsViewHolder(View itemView, final IComicsViewHolderCallback<String> callback) {
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
            itemView.setOnClickListener(v -> callback.onComicsClick(mId, getLayoutPosition()));
            menu.setVisibility(View.VISIBLE);
            menu.setOnClickListener(v -> callback.onComicsMenuSelected(mId, getLayoutPosition()));
        } else {
            itemView.setOnClickListener(null);
            menu.setVisibility(View.INVISIBLE);
            menu.setOnClickListener(null);
        }
    }

    @Override
    public ItemDetailsLookup.ItemDetails<String> getItemDetails() {
        return new ComicsItemDetails<>(getLayoutPosition(), mId);
    }

    void bind(@NonNull CmkWebComics comics, boolean selected, RequestManager requestManager) {
        itemView.setActivated(selected);
        mId = comics.id;
        mName.setText(comics.name);
        mPublisher.setText(comics.publisher);
//        mAuthors.setText(comics.authors);
//        mNotes.setText(comics.notes);
        mNotes.setText(comics.selected ? "SELECTED" : "");

        final Context context = itemView.getContext();

//        final Release lastRelease = comics.getLastPurchasedRelease();
//        mLast.setText(lastRelease == null ? context.getString(R.string.release_last_none) :
//                context.getString(R.string.release_last, lastRelease.number));

//        final Release nextRelease = comics.getNextToPurchaseRelease();
//        if (nextRelease != null) {
//            if (nextRelease.date != null) {
//                // TODO: non mi piace, dovrei mostrare la data solo se futura e nel formato ddd dd MMM
//                mNext.setText(context.getString(R.string.release_next_dated, nextRelease.number,
//                        DateFormatterHelper.toHumanReadable(itemView.getContext(), nextRelease.date, DateFormatterHelper.STYLE_SHORT)));
//            } else {
//                mNext.setText(context.getString(R.string.release_next, nextRelease.number));
//            }
//        } else {
//            mNext.setText(context.getString(R.string.release_next_none));
//        }

//        final int missingCount = comics.getNotPurchasedReleaseCount();
//        mMissing.setText(context.getString(R.string.release_missing, missingCount));

//        // cambio il colore dello sfondo dell'iniziale in base al suo valore
//        final LayerDrawable layerDrawable = (LayerDrawable) ContextCompat.getDrawable(itemView.getContext(), R.drawable.background_comics_initial);
//        if (layerDrawable != null) {
//            final GradientDrawable drawable = (GradientDrawable) layerDrawable.findDrawableByLayerId(R.id.background);
//            if (drawable != null) {
//                drawable.setColor(ContextCompat.getColor(itemView.getContext(),
//                        getInitialColor(comics.comics.getInitial().toUpperCase().charAt(0))));
//            }
//        }

//        if (requestManager != null && comics.comics.hasImage()) {
//            mInitial.setText("");
//            requestManager
//                    .load(Uri.parse(comics.comics.image))
//                    .apply(ImageHelper.getGlideCircleOptions())
//                    .into(new DrawableTextViewTarget(mInitial));
//        } else {
        mInitial.setText(comics.getInitial());
        mInitial.setBackgroundResource(R.drawable.background_comics_initial_noborder);
//        }
    }

    void clear() {
        itemView.setActivated(false);
        mId = null;
        mInitial.setText("");
        mName.setText("");
        mPublisher.setText("");
        mAuthors.setText("");
        mNotes.setText("");
        mLast.setText("");
        mNext.setText("");
        mMissing.setText("");
    }

    static CmkWebComicsViewHolder create(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, IComicsViewHolderCallback<String> callback) {
        return new CmkWebComicsViewHolder(inflater.inflate(R.layout.listitem_comics, parent, false), callback);
    }

}
