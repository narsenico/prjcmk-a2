package it.amonshore.comikkua.data.release;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import it.amonshore.comikkua.data.comics.Comics;

@Entity(tableName = "tReleases",
        foreignKeys = @ForeignKey(entity = Comics.class,
                parentColumns = "id", childColumns = "comicsId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = {"comicsId", "number"})})
public class Release {

    public final static long NO_RELEASE_ID = -1;

    @PrimaryKey(autoGenerate = true)
    public long id;
    public long comicsId;
    public int number;
    public String date;
    public double price;
    public boolean purchased;
    public boolean ordered;
    public String notes;
    public long lastUpdate;

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;

        if (obj instanceof Release) {
            final Release other = (Release) obj;
            return other.comicsId == this.comicsId &&
                    other.id == this.id &&
                    other.number == this.number &&
                    other.lastUpdate == this.lastUpdate;
        } else {
            return false;
        }
    }

    public static Release create(long comicsId, int number, String date) {
        final Release release = new Release();
        release.comicsId = comicsId;
        release.number = number;
        release.date = date;
        release.purchased = false;
        release.ordered = false;
        return release;
    }

    public static Release create(long comicsId, int number) {
        return create(comicsId, number, null);
    }

    public static Release create(@NonNull Release release) {
        final Release clone = new Release();
        clone.id = release.id;
        clone.comicsId = release.comicsId;
        clone.number = release.number;
        clone.date = release.date;
        clone.price = release.price;
        clone.purchased = release.purchased;
        clone.ordered = release.ordered;
        clone.notes = release.notes;
        clone.lastUpdate = release.lastUpdate;
        return clone;
    }
}
