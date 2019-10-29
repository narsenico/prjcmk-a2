package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.XRelease;
import it.amonshore.comikkua.data.ReleaseHeader;
import it.amonshore.comikkua.data.ReleaseItem;

public class ReleasesRecyclerViewAdapter extends RecyclerView.Adapter<ReleasesRecyclerViewAdapter.ItemViewHolder> {

    private final ReleaseItem[] mItemList;
    private final LayoutInflater mLayoutInflater;

    public ReleasesRecyclerViewAdapter(Context context, ReleaseItem[] itemList) {
        mLayoutInflater = LayoutInflater.from(context);
        mItemList = itemList;
    }

    @Override
    public int getItemViewType(int position) {
        return mItemList[position].Type;
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        holder.bind(mItemList[position], position);
    }

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == 0) {
            return new HeaderViewHolder(mLayoutInflater.inflate(R.layout.listitem_release_header, parent, false));
        } else {
            return new ReleaseViewHolder(mLayoutInflater.inflate(R.layout.listitem_release, parent, false));
        }
    }

    @Override
    public int getItemCount() {
        return mItemList.length;
    }

    static abstract class ItemViewHolder extends RecyclerView.ViewHolder {
        ItemViewHolder(View itemView) {
            super(itemView);
        }

        abstract void bind(ReleaseItem item, int position);
    }

    static class ReleaseViewHolder extends ItemViewHolder {
        private final TextView
                mTitle,
                mNumbers,
                mDate,
                mInfo,
                mNotes;
        private final ImageView
                mPurchased,
                mOrdered;

        ReleaseViewHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.txt_release_title);
            mNumbers = itemView.findViewById(R.id.txt_release_numbers);
            mDate = itemView.findViewById(R.id.txt_release_date);
            mInfo = itemView.findViewById(R.id.txt_release_info);
            mNotes = itemView.findViewById(R.id.txt_release_notes);
            mPurchased = itemView.findViewById(R.id.img_release_purchased);
            mOrdered = itemView.findViewById(R.id.img_release_ordered);
        }

        @Override
        void bind(ReleaseItem item, int position) {
            bind((XRelease)item.Value, position);
        }

        void bind(XRelease release, int position) {
            mTitle.setText(release.Title);
            mNumbers.setText(release.Numbers);
            mDate.setText(release.Date);
            mInfo.setText(release.Info);
            mNotes.setText(release.Notes);
            mOrdered.setVisibility(release.Ordered.equals("S") ? View.VISIBLE : View.INVISIBLE);

            if (release.Purchased.equals("S")) {
                itemView.setElevation(0);
                ((CardView) itemView).setCardBackgroundColor(itemView.getResources().getColor(R.color.colorItemBackgroundLighter));
                mPurchased.setVisibility(View.VISIBLE);
                mNumbers.setBackgroundColor(itemView.getResources().getColor(R.color.colorItemBackgroundAlt));
            } else {
                itemView.setElevation(5.25f);
                ((CardView) itemView).setCardBackgroundColor(itemView.getResources().getColor(R.color.colorItemBackgroundLight));
                mPurchased.setVisibility(View.INVISIBLE);
                mNumbers.setBackgroundColor(itemView.getResources().getColor(R.color.colorItemBackgroundAltVivid));
            }
        }
    }

    static class HeaderViewHolder extends ItemViewHolder {
        private final TextView
                mTitle,
                mInfo;

        HeaderViewHolder(View itemView) {
            super(itemView);
            mTitle = itemView.findViewById(R.id.txt_title);
            mInfo = itemView.findViewById(R.id.txt_info);
        }

        @Override
        void bind(ReleaseItem item, int position) {
            bind((ReleaseHeader)item.Value, position);
        }

        void bind(ReleaseHeader header, int position) {
            mTitle.setText(header.Title);
            mInfo.setText(header.Info);

            if (position == 0) {
                itemView.findViewById(R.id.separator).setVisibility(View.GONE);
            } else {
                itemView.findViewById(R.id.separator).setVisibility(View.VISIBLE);
            }
        }
    }
}
