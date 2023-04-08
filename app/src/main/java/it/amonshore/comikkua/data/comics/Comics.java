package it.amonshore.comikkua.data.comics;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import it.amonshore.comikkua.Exclude;

@Entity(tableName = "tComics",
        indices = {@Index("name"), @Index("sourceId")})
public class Comics {

    public final static long NO_COMICS_ID = -1;
    public final static long NEW_COMICS_ID = 0;

    @PrimaryKey(autoGenerate = true)
    public long id;
    @NonNull
    public String name;
    public String series;
    public String publisher;
    public String authors;
    public double price;
    // Wn, Mn, Yn
    public String periodicity;
    public boolean reserved;
    public String notes;
    @Exclude
    public String image;
    public long lastUpdate;
    public long refJsonId;
    public boolean removed;
    @Exclude
    public String tag;

    /**
     * Id della sorgente da cui è stato generato il comics.
     * Solitamente l'id dell'elemento letto da una sorgente remota.
     */
    public String sourceId;

    /**
     * Indica se il comics è stato selezionato dall'utente,
     * quindi visibile nel suo elenco dei comics.
     */
    public boolean selected;

    /**
     * Versione del comics, cioè il numero di ristampa.
     * 0=nessuna ristampa, 1=prima ristampa, etc.
     */
    public int version;

    @NonNull
    public String getInitial() {
        if (TextUtils.isEmpty(name)) {
            return "";
        } else {
            return name.substring(0, 1);
        }
    }

    public boolean hasImage() {
        return !TextUtils.isEmpty(this.image);
    }

    /**
     * @return true se proviene da una sorgente remota, false se è stato creato manualmente
     */
    public boolean isSourced() {
        return !TextUtils.isEmpty(this.sourceId);
    }

    /**
     * Crea un nuovo comics con il nome specificato.
     * Il comics è selezionato (select = true)
     *
     * @param name il nome del comics
     * @return il comics
     */
    @NonNull
    public static Comics create(@NonNull String name) {
        final Comics comics = new Comics();
        comics.name = name;
        comics.selected = true;
        return comics;
    }
}
