package it.amonshore.comikkua.data.web;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Rappresenta un comics letto dalla rete.
 */
public class CmkWebComics {

    /**
     * Id del comics così come indentificato in rete.
     */
    @SerializedName("id")
    public String id;

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

    public boolean selected;

    CmkWebComics withId(String id) {
        this.id = id;
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

        if (obj instanceof CmkWebComics) {
            final CmkWebComics other = (CmkWebComics) obj;
            return other.id.equals(this.id);
        } else {
            return false;
        }
    }
}
