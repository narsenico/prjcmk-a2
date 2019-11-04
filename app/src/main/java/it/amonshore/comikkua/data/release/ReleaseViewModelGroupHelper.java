package it.amonshore.comikkua.data.release;

import java.util.ArrayList;
import java.util.List;

public class ReleaseViewModelGroupHelper {

    /**
     * Si presuppone che le release siano ordinate per {@link ComicsRelease#type}.
     * La procedura scorre le release, quando incontra una release il cui tipo
     * {@link ComicsRelease#type} è diverso da quella precedente, inserisce un header {@link ReleaseHeader},
     * con relativi contatori del numero totale di release di quel tipo, e di quelle acquistate.
     * <p>
     * Inoltre le release senza data, cioè del tipo {@link MissingRelease}, verranno raggruppate per comics.
     *
     * @param releases elenco di release ordinate per {@link ComicsRelease#type}.
     * @return elenco di item comprensivi di header, release e release raggruppate.
     */
    public List<IReleaseViewModelItem> createViewModelItems(List<ComicsRelease> releases) {
        // scorro tutte le release
        // - se la release fa parte di un gruppo diverso da quella precedente, creo un nuovo header
        // - se la release non ha data, e così anche la precedente, se si riferiscono alla stesso comics, le raggruppo

        final ArrayList<IReleaseViewModelItem> items = new ArrayList<>();
        ReleaseHeader lastHeader = null;
        int lastType = 0;
        int totalCount = 0;
        int purchasedCount = 0;
        long headerCount = 0L;

        for (ComicsRelease cr : releases) {
            if (lastHeader == null || lastType != cr.type) {
                // la tipologia del dato è cambiata, creo un nuovo header
                final ReleaseHeader header = new ReleaseHeader(++headerCount, cr.type);
                items.add(header);

                if (lastHeader != null) {
                    lastHeader.totalCount = totalCount;
                    lastHeader.purchasedCount = purchasedCount;
                }

                lastHeader = header;
                lastType = cr.type;
                totalCount = 0;
                purchasedCount = 0;
            }

            // TODO: multi relesase
            items.add(cr);
            ++totalCount;
            if (cr.release.purchased) {
                ++purchasedCount;
            }
        }

        if (lastHeader != null) {
            lastHeader.totalCount = totalCount;
            lastHeader.purchasedCount = purchasedCount;
        }

        return items;
    }

}
