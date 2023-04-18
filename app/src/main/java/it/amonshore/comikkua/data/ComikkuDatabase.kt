package it.amonshore.comikkua.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import it.amonshore.comikkua.data.comics.Comics
import it.amonshore.comikkua.data.comics.ComicsDao
import it.amonshore.comikkua.data.release.ComicsRelease
import it.amonshore.comikkua.data.release.DatedRelease
import it.amonshore.comikkua.data.release.LostRelease
import it.amonshore.comikkua.data.release.MissingRelease
import it.amonshore.comikkua.data.release.NotPurchasedRelease
import it.amonshore.comikkua.data.release.PurchasedRelease
import it.amonshore.comikkua.data.release.Release
import it.amonshore.comikkua.data.release.ReleaseDao
import it.amonshore.comikkua.data.web.AvailableComics
import it.amonshore.comikkua.data.web.CmkWebDao

@Database(
    entities = [Comics::class, Release::class, AvailableComics::class],
    views = [ComicsRelease::class, MissingRelease::class, LostRelease::class, DatedRelease::class, PurchasedRelease::class, NotPurchasedRelease::class],
    version = 16
)
abstract class ComikkuDatabase : RoomDatabase() {
    abstract fun comicsDao(): ComicsDao
    abstract fun releaseDao(): ReleaseDao
    abstract fun cmkWebDao(): CmkWebDao

    companion object {
        @Volatile
        private var INSTANCE: ComikkuDatabase? = null
        fun getDatabase(context: Context): ComikkuDatabase {
            // TODO: la vecchia applicazione ha un db chiamato comikkua.db alla versione 3
            //  questo ha un nome diverso quindi non dovrebbe fare casino (ho provato, mantiene i database con nomi diversi)
            //  OCCORRE importare i dati da vecchia version, anche da variante neon
            if (INSTANCE == null) {
                synchronized(ComikkuDatabase::class.java) {
                    if (INSTANCE == null) {
                        INSTANCE = databaseBuilder(
                            context.applicationContext,
                            ComikkuDatabase::class.java, "comikku_database"
                        )
                            .fallbackToDestructiveMigration() // in questo modo al cambio di vesione il vecchio DB viene semplicemente distrutto (con conseguente perdita di dati)
                            .build()
                    }
                }
            }
            return INSTANCE!!
        }
    }
}