package it.amonshore.comikkua.data.web;

import com.google.gson.annotations.SerializedName;

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
}
