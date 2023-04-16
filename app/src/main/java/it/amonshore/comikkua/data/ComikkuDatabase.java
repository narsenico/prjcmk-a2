package it.amonshore.comikkua.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.DatedRelease;
import it.amonshore.comikkua.data.release.LostRelease;
import it.amonshore.comikkua.data.release.MissingRelease;
import it.amonshore.comikkua.data.release.NotPurchasedRelease;
import it.amonshore.comikkua.data.release.PurchasedRelease;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseDao;
import it.amonshore.comikkua.data.web.AvailableComics;
import it.amonshore.comikkua.data.web.CmkWebDao;

@Database(entities = {Comics.class, Release.class, AvailableComics.class},
        views = {ComicsRelease.class,
                MissingRelease.class, LostRelease.class, DatedRelease.class,
                PurchasedRelease.class, NotPurchasedRelease.class},
        version = 14)
public abstract class ComikkuDatabase extends RoomDatabase {

    public abstract ComicsDao comicsDao();

    public abstract ReleaseDao releaseDao();

    public abstract CmkWebDao cmkWebDao();

    private static volatile ComikkuDatabase INSTANCE;

    public static ComikkuDatabase getDatabase(@NonNull final Context context) {
        // TODO: la vecchia applicazione ha un db chiamato comikkua.db alla versione 3
        //  questo ha un nome diverso quindi non dovrebbe fare casino (ho provato, mantiene i database con nomi diversi)
        //  OCCORRE importare i dati da vecchia version, anche da variante neon
        if (INSTANCE == null) {
            synchronized (ComikkuDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ComikkuDatabase.class, "comikku_database")
                            .fallbackToDestructiveMigration() // in questo modo al cambio di vesione il vecchio DB viene semplicemente distrutto (con conseguente perdita di dati)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
