package it.amonshore.comikkua.data.release;

import java.util.List;

public class MultiRelease extends ComicsRelease {

    /**
     * Contiene le restanti release che fanno gruppo con con {@link ComicsRelease#release}
     * (esclusa da questo elenco).
     */
    public List<Release> otherReleases;

}
