package it.amonshore.comikkua.data.release;

import java.util.Objects;

import androidx.annotation.Nullable;

public class ReleaseHeader implements IReleaseViewModelItem {
    public final static int ITEM_TYPE = 1;
    public final static long BASE_ID = 90000000L;

    public int totalCount;
    public int purchasedCount;

    private long mId;
    private int mType;

    /**
     * @param relativeId questo valore verrà sommato a BASE_ID per ricavare l'id dell'item
     * @param type       tipo dei dati che fanno riferimento a questo header
     */
    public ReleaseHeader(long relativeId, int type) {
        mId = BASE_ID + relativeId;
        mType = type;
    }

    public int getType() {
        return mType;
    }

    @Override
    public long getId() {
        return mId;
    }

    @Override
    public int getItemType() {
        return ITEM_TYPE;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;

        if (obj instanceof ReleaseHeader) {
            final ReleaseHeader other = (ReleaseHeader) obj;
            return other.getId() == this.getId() &&
                    other.totalCount == this.totalCount &&
                    other.purchasedCount == this.purchasedCount;
        } else {
            return false;
        }
    }
}
