package it.amonshore.comikkua.data.web;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Rappresenta un comics disponibile per essere monitorato:
 * cioè le release possono essere aggiornate in automatico.
 * Una volta monitorato viene creato un record in tComcis con sourceId
 * valorizzato a tAvailableComics.id.
 */
@Entity(tableName = "tAvailableComics",
        indices = {@Index(value = "sourceId", unique = true),
                @Index({"name", "publisher", "version"})})
public class AvailableComics {

    public final static long NO_COMICS_ID = -1;

    /**
     * Id comics interno.
     * Room supporta solo id di tipo long.
     */
    @PrimaryKey(autoGenerate = true)
    public long id;

    /**
     * Id del comics così come indentificato in rete.
     */
    @SerializedName("id")
    public String sourceId;

    /**
     * Nome del cmomics.
     */
    @SerializedName("name")
    public String name;

    /**
     * Nome del cmomics usato per le ricerche.
     */
    @SerializedName("searchableName")
    public String searchableName;

    /**
     * Editore.
     */
    @SerializedName("publisher")
    public String publisher;

    /**
     * Versione del comics, cioè il numero di ristampa.
     * 0=nessuna ristampa, 1=prima ristampa, etc.
     */
    @SerializedName("version")
    public int version;

    public AvailableComics withSourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    @NonNull
    public String getInitial() {
        if (TextUtils.isEmpty(name)) {
            return "";
        } else {
            return name.substring(0, 1);
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;

        if (obj instanceof AvailableComics) {
            final AvailableComics other = (AvailableComics) obj;
            return other.sourceId.equals(this.sourceId);
        } else {
            return false;
        }
    }
}
