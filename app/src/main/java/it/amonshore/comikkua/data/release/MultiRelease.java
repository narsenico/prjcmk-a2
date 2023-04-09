package it.amonshore.comikkua.data.release;

import java.util.ArrayList;
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

    public int size() {
        return this.otherReleases.size() + 1;
    }

    public Long[] getAllReleaseId() {
        final Long[] ids = new Long[this.otherReleases.size() + 1];
        ids[0] = this.release.id;
        for (int ii = 0; ii < this.otherReleases.size(); ii++) {
            ids[ii + 1] = this.otherReleases.get(ii).id;
        }
        return ids;
    }

    public int[] getAllNumbers() {
        final int[] numbers = new int[otherReleases.size() + 1];
        numbers[0] = release.number;
        for (int ii = 0; ii < otherReleases.size(); ii++) {
            numbers[ii + 1] = this.otherReleases.get(ii).number;
        }
        return numbers;
    }
}
