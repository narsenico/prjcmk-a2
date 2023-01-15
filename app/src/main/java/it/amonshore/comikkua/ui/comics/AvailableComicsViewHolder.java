package it.amonshore.comikkua.ui.comics;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.bumptech.glide.RequestManager;

import androidx.annotation.NonNull;
import androidx.recyclerview.selection.ItemDetailsLookup;
import it.amonshore.comikkua.Constants;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.web.AvailableComics;
import it.amonshore.comikkua.ui.IViewHolderWithDetails;

class AvailableComicsViewHolder extends IViewHolderWithDetails<String> {
    private final TextView mInitial;
    private final TextView mName;
    private final TextView mPublisher;
    private final TextView mAuthors;
    private final TextView mNotes;
    private final TextView mLast;
    private final Button mFollow;
    private String mId;

    private AvailableComicsViewHolder(View itemView, final IComicsViewHolderCallback<String> callback) {
        super(itemView);
        mInitial = itemView.findViewById(R.id.txt_comics_initial);
        mName = itemView.findViewById(R.id.txt_comics_name);
        mPublisher = itemView.findViewById(R.id.txt_comics_publisher);
        mAuthors = itemView.findViewById(R.id.txt_comics_authors);
        mNotes = itemView.findViewById(R.id.txt_comics_notes);
        mLast = itemView.findViewById(R.id.txt_comics_release_last);
        mFollow = itemView.findViewById(R.id.btn_follow);

        final View menu = itemView.findViewById(R.id.img_comics_menu);
        if (callback != null) {
            mFollow.setOnClickListener(v -> callback.onAction(mId, getLayoutPosition(),
                    Constants.VIEWHOLDER_ACTION_FOLLOW));
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
    public ItemDetailsLookup.ItemDetails<String> getItemDetails() {
        return new ComicsItemDetails<>(getLayoutPosition(), mId);
    }

    void bind(@NonNull AvailableComics comics, boolean selected, RequestManager requestManager) {
        itemView.setActivated(selected);
        mId = comics.sourceId;
        mName.setText(comics.name);
        mPublisher.setText(comics.publisher);
//        mAuthors.setText(comics.authors);
//        mNotes.setText(comics.notes);

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
        mFollow.setEnabled(true);
    }

    static AvailableComicsViewHolder create(@NonNull LayoutInflater inflater, @NonNull ViewGroup parent, IComicsViewHolderCallback<String> callback) {
        return new AvailableComicsViewHolder(inflater.inflate(R.layout.listitem_comics_available, parent, false), callback);
    }

}
