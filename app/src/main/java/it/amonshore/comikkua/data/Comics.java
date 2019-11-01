package it.amonshore.comikkua.data;

import android.text.TextUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.Relation;

@Entity(tableName = "tComics",
        indices = {@Index("name")})
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
    public String periodicity;
    public String reserved;
    public String notes;
    public String image;
    public long lastUpdate;

    public String getInitial() {
        if (TextUtils.isEmpty(name)) {
            return "";
        } else {
            return name.substring(0, 1);
        }
    }

    public static Comics create(@NonNull String name) {
        final Comics comics = new Comics();
        comics.name = name;
        return comics;
    }
}
