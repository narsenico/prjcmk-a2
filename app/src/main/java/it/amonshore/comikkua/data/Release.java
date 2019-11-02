package it.amonshore.comikkua.data;

import java.lang.annotation.Retention;

import androidx.annotation.IntDef;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import static java.lang.annotation.RetentionPolicy.SOURCE;

@Entity(tableName = "tReleases",
        foreignKeys = @ForeignKey(entity = Comics.class,
                parentColumns = "id", childColumns = "comicsId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"comicsId", "number"})})
public class Release {

    public final static long NO_RELEASE_ID = -1;

    @Retention(SOURCE)
    @IntDef({FLAG_NONE, FLAG_ORDERED, FLAG_PURCHASED})
    public @interface ReleaseFlag {}
    public final static int FLAG_NONE = 0;
    public final static int FLAG_ORDERED = 2;
    public final static int FLAG_PURCHASED = 4;

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long comicsId;
    public int number;
    public String date;
    public double price;
    @ReleaseFlag
    public int flags;
    public String notes;
    public long lastUpdate;

    public static Release create(long comicsId, int number, String date) {
        final Release release = new Release();
        release.comicsId = comicsId;
        release.number = number;
        release.date = date;
        return release;
    }

    public static Release create(long comicsId, int number) {
        return create(comicsId, number, null);
    }
}
