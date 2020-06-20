package it.amonshore.comikkua.data.comics;

import com.google.gson.annotations.SerializedName;

import androidx.annotation.NonNull;

/**
 * Rappresenta un comics letto dalla rete
 */
public class CmkWebComics {
    @SerializedName("title")
    public String name;
    @SerializedName("editor")
    public String publisher;

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
