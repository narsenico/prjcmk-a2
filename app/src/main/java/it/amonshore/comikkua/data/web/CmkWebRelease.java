package it.amonshore.comikkua.data.web;

import com.google.gson.annotations.SerializedName;

/**
 * Rappresenta una release letto dalla rete
 */
public class CmkWebRelease {
    @SerializedName("name")
    public String comicsName;
    @SerializedName("number")
    public int number;
    @SerializedName("release_date")
    public String date;
}
