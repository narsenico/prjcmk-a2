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
        MultiRelease lastMulti = null;
        int lastType = 0;
        int totalCount = 0;
        int purchasedCount = 0;
        long headerCount = 0L;

        for (int ii = 0; ii < releases.size(); ii++) {
            final ComicsRelease cr = releases.get(ii);
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

            if (ii > 0 && canBeGrouped(releases.get(ii - 1), cr)) {
                // la release precedente e questa possono essere raggruppate
                if (lastMulti == null) {
                    // se non ho ancora creato un multi, lo creo adesso sostituendo in items l'ultimo valore
                    final ComicsRelease prev = releases.get(ii - 1);
                    lastMulti = new MultiRelease();
                    lastMulti.comics = prev.comics;
                    // la release principale è quella precedente (cioè la prima in ordine)
                    lastMulti.release = prev.release;
                    // poi vengono le altre
                    lastMulti.otherReleases = new ArrayList<>();
                    lastMulti.otherReleases.add(cr.release);
                    items.set(items.size() - 1, lastMulti);
                } else {
                    // il multi era già creato, aggiungo la release e basta
                    lastMulti.otherReleases.add(cr.release);
                }
            } else {
                // quella corrente non può essere raggruppata con la precedente
                // la inserisco così come è e annullo il multi in modo che venga ricreato il prossimo giro se necessario
                lastMulti = null;
                items.add(cr);
            }

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

    private boolean canBeGrouped(ComicsRelease cr1, ComicsRelease cr2) {
        return cr1.type == MissingRelease.TYPE &&
                cr2.type == MissingRelease.TYPE &&
                cr1.comics.id == cr2.comics.id;
    }

}
