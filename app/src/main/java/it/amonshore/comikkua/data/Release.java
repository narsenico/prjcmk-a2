package it.amonshore.comikkua.data;

import androidx.annotation.IntDef;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "tReleases",
        foreignKeys = @ForeignKey(entity = Comics.class,
                parentColumns = "id", childColumns = "comicsId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"comicsId", "number"})})
public class Release {

    @IntDef({FLAG_NONE, FLAG_ORDERED, FLAG_PURCHASED})
    public @interface ReleaseFlag {}
    final static int FLAG_NONE = 0;
    final static int FLAG_ORDERED = 2;
    final static int FLAG_PURCHASED = 4;

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long comicsId;
    public int number;
    public String date;
    public double price;
    @ReleaseFlag
    public int flags;
    public String notes;

    public static Release create(long comicsId, int number) {
        final Release release = new Release();
        release.comicsId = comicsId;
        release.number = number;
        return release;
    }
}
