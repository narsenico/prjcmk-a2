package it.amonshore.comikkua.data;

import androidx.annotation.Nullable;
import androidx.room.Embedded;

public class ComicsRelease {
    public int type;
    @Embedded(prefix = "c")
    public Comics comics;
    @Embedded(prefix = "r")
    public Release release;

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) return true;

        if (obj instanceof ComicsRelease) {
            final ComicsRelease other = (ComicsRelease) obj;
            return other.comics.id == this.comics.id &&
                    other.release.id == this.release.id;
        } else {
            return false;
        }
    }
}
