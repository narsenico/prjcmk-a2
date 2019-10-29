package it.amonshore.comikkua.data;

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

    public static Comics create(@NonNull String name) {
        final Comics comics = new Comics();
        comics.name = name;
        return comics;
    }
}
