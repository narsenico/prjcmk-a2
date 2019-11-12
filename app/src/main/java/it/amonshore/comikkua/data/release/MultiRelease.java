package it.amonshore.comikkua.data.release;

import java.util.List;

public class MultiRelease extends ComicsRelease {
    public final static int ITEM_TYPE = 3;

    @Override
    public int getItemType() {
        return ITEM_TYPE;
    }

    /**
     * Contiene le restanti release che fanno gruppo con con {@link ComicsRelease#release}
     * (esclusa da questo elenco).
     */
    public List<Release> otherReleases;

}
