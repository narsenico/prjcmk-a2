package it.amonshore.comikkua.data.web;

import com.google.gson.annotations.SerializedName;

/**
 * Rappresenta una release letto dalla rete
 */
public class CmkWebRelease {
    @SerializedName("title")
    public String title;
    @SerializedName("editor")
    public String editor;
    @SerializedName("number")
    public int number;
    @SerializedName("release_date")
    public String date;

}
