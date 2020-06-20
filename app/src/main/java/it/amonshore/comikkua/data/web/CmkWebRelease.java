package it.amonshore.comikkua.data.web;

import com.google.gson.annotations.SerializedName;

/**
 * Rappresenta una release letto dalla rete
 */
public class CmkWebRelease {
//     "release_date": "2020-06-10",
//    "number": 46,
//    "title": "Fairy Tail New Edition",
//    "editor": "Star Comics",
//    "reissue": 0,
//    "last": false,
//    "ref_id": "9870"

    @SerializedName("title")
    public String title;
    @SerializedName("editor")
    public String editor;
    @SerializedName("number")
    public int number;
    @SerializedName("release_date")
    public String date;

}
