package it.amonshore.comikkua.data.release;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Embedded;
import it.amonshore.comikkua.data.comics.Comics;

public class ComicsRelease implements IReleaseViewModelItem {
    public final static int ITEM_TYPE = 2;

    /**
     * Indica la tipologia del dato estratto dalla query per un raggruppamento logico:
     * release persa, mancante, di questa settimana, etc.
     */
    public int type;

    @Embedded(prefix = "c")
    public Comics comics;

    @Embedded(prefix = "r")
    public Release release;

    @Override
    public long getId() {
        return this.release.id;
    }

    @Override
    public int getItemType() {
        return ITEM_TYPE;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;

        if (obj instanceof ComicsRelease) {
            final ComicsRelease other = (ComicsRelease) obj;
            return other.comics.id == this.comics.id &&
                    other.release.id == this.release.id &&
                    other.comics.lastUpdate == this.comics.lastUpdate &&
                    other.release.lastUpdate == this.release.lastUpdate;
        } else {
            return false;
        }
    }

    /**
     * Creo una nuova istanza di {@link ComicsRelease#release} inizializzato.
     *
     * @param comics comics a cui appartiene la release
     * @return nuova istanza
     */
    public static ComicsRelease createNew(@NonNull Comics comics) {
        final ComicsRelease cr = new ComicsRelease();
        cr.comics = comics;
        cr.release = Release.create(comics.id, 0);
        return cr;
    }
}
