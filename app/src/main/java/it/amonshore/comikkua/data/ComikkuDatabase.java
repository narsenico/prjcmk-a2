package it.amonshore.comikkua.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;

import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.loader.content.AsyncTaskLoader;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.comics.ComicsDaoKt;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.DatedRelease;
import it.amonshore.comikkua.data.release.LostRelease;
import it.amonshore.comikkua.data.release.MissingRelease;
import it.amonshore.comikkua.data.release.NotPurchasedRelease;
import it.amonshore.comikkua.data.release.PurchasedRelease;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseDao;
import it.amonshore.comikkua.data.release.ReleaseDaoKt;
import it.amonshore.comikkua.data.web.AvailableComics;
import it.amonshore.comikkua.data.web.CmkWebDao;
import it.amonshore.comikkua.data.web.CmkWebDaoKt;

@Database(entities = {Comics.class, Release.class, AvailableComics.class},
        views = {ComicsRelease.class,
                MissingRelease.class, LostRelease.class, DatedRelease.class,
                PurchasedRelease.class, NotPurchasedRelease.class},
        version = 8)
public abstract class ComikkuDatabase extends RoomDatabase {

    public abstract ComicsDao comicsDao();

    public abstract ComicsDaoKt comicsDaoKt();

    public abstract ReleaseDao releaseDao();

    public abstract ReleaseDaoKt releaseDaoKt();

    public abstract CmkWebDao cmkWebDao();

    public abstract CmkWebDaoKt cmkWebDaoKt();

    private static volatile ComikkuDatabase INSTANCE;

    public static ComikkuDatabase getDatabase(@NonNull final Context context) {
        // NB: la vecchia applicazione ha un db chiamato comikkua.db alla versione 3
        // questo ha un nome diverso quindi non dovrebbe fare casino (ho provato, mantiene i database con nomi diversi)
        if (INSTANCE == null) {
            synchronized (ComikkuDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ComikkuDatabase.class, "comikku_database")
//                            .fallbackToDestructiveMigration() // in questo modo al cambio di vesione il vecchio DB viene semplicemente distrutto (con conseguente perdita di dati)
//                            .addMigrations(new FakeMigration(1, 2))
//                            .addMigrations(MIGRATION_2_3)
//                            .addMigrations(MIGRATION_3_4)
//                            .addMigrations(MIGRATION_4_5)
//                            .addMigrations(MIGRATION_5_6)
//                            .addMigrations(MIGRATION_6_7)
//                            .addMigrations(MIGRATION_7_8)
//                            .addCallback(new DatabaseCallback())
                            .build();
                }
            }
        }
        return INSTANCE;
    }

//    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            database.execSQL("ALTER TABLE tComics ADD COLUMN lastUpdate INTEGER NOT NULL DEFAULT 0");
//        }
//    };
//
    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE VIEW `vComicsReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId");
        }
    };

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tComics ADD COLUMN removed INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tReleases ADD COLUMN removed INTEGER NOT NULL DEFAULT 0");

            database.execSQL("DROP VIEW IF EXISTS `vComicsReleases`");
            database.execSQL("CREATE VIEW `vComicsReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0");

            database.execSQL("DROP VIEW IF EXISTS `vMissingReleases`");
            database.execSQL("CREATE VIEW `vMissingReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND (date is null or date = '')");

            database.execSQL("DROP VIEW IF EXISTS `vLostReleases`");
            database.execSQL("CREATE VIEW `vLostReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND (date is not null and date <> '')");

            database.execSQL("DROP VIEW IF EXISTS `vDatedReleases`");
            database.execSQL("CREATE VIEW `vDatedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND (date is not null and date <> '')");

            database.execSQL("DROP VIEW IF EXISTS `vPurchasedReleases`");
            database.execSQL("CREATE VIEW `vPurchasedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND purchased = 1");

            database.execSQL("DROP VIEW IF EXISTS `vNotPurchasedReleases`");
            database.execSQL("CREATE VIEW `vNotPurchasedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND purchased = 0");
        }
    };

    private static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tReleases ADD COLUMN tag TEXT");

            database.execSQL("DROP VIEW IF EXISTS `vComicsReleases`");
            database.execSQL("CREATE VIEW `vComicsReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0");

            database.execSQL("DROP VIEW IF EXISTS `vMissingReleases`");
            database.execSQL("CREATE VIEW `vMissingReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND (date is null or date = '')");

            database.execSQL("DROP VIEW IF EXISTS `vLostReleases`");
            database.execSQL("CREATE VIEW `vLostReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND (date is not null and date <> '')");

            database.execSQL("DROP VIEW IF EXISTS `vDatedReleases`");
            database.execSQL("CREATE VIEW `vDatedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND (date is not null and date <> '')");

            database.execSQL("DROP VIEW IF EXISTS `vPurchasedReleases`");
            database.execSQL("CREATE VIEW `vPurchasedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND purchased = 1");

            database.execSQL("DROP VIEW IF EXISTS `vNotPurchasedReleases`");
            database.execSQL("CREATE VIEW `vNotPurchasedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND purchased = 0");
        }
    };

    private static final Migration MIGRATION_5_6 = new Migration(5, 6) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // nuovo campo tComics.sourceId indicizzato
            database.execSQL("ALTER TABLE tComics ADD COLUMN sourceId TEXT");
            database.execSQL("CREATE INDEX index_tComics_sourceid ON tComics(sourceId)");

            // nuovi campi tComics.version e tComics.selected
            database.execSQL("ALTER TABLE tComics ADD COLUMN version INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tComics ADD COLUMN selected INTEGER NOT NULL DEFAULT 1");

            // aggiungo nuovi campi a tutte le viste
            database.execSQL("DROP VIEW IF EXISTS `vComicsReleases`");
            database.execSQL("CREATE VIEW `vComicsReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tComics.sourceId as csourceId, tComics.selected as cselected, tComics.version as cversion, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND tComics.selected = 1");

            database.execSQL("DROP VIEW IF EXISTS `vMissingReleases`");
            database.execSQL("CREATE VIEW `vMissingReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tComics.sourceId as csourceId, tComics.selected as cselected, tComics.version as cversion, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND tComics.selected = 1 AND (date is null or date = '')");

            database.execSQL("DROP VIEW IF EXISTS `vLostReleases`");
            database.execSQL("CREATE VIEW `vLostReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tComics.sourceId as csourceId, tComics.selected as cselected, tComics.version as cversion, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND tComics.selected = 1 AND (date is not null and date <> '')");

            database.execSQL("DROP VIEW IF EXISTS `vDatedReleases`");
            database.execSQL("CREATE VIEW `vDatedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tComics.sourceId as csourceId, tComics.selected as cselected, tComics.version as cversion, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND tComics.selected = 1 AND (date is not null and date <> '')");

            database.execSQL("DROP VIEW IF EXISTS `vPurchasedReleases`");
            database.execSQL("CREATE VIEW `vPurchasedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tComics.sourceId as csourceId, tComics.selected as cselected, tComics.version as cversion, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND tComics.selected = 1 AND purchased = 1");

            database.execSQL("DROP VIEW IF EXISTS `vNotPurchasedReleases`");
            database.execSQL("CREATE VIEW `vNotPurchasedReleases` AS SELECT tComics.id as cid, tComics.name as cname, tComics.series as cseries, tComics.publisher as cpublisher, tComics.authors as cauthors, tComics.price as cprice, tComics.periodicity as cperiodicity, tComics.reserved as creserved, tComics.notes as cnotes, tComics.image as cimage, tComics.lastUpdate as clastUpdate, tComics.refJsonId as crefJsonId, tComics.sourceId as csourceId, tComics.selected as cselected, tComics.version as cversion, tReleases.id as rid, tReleases.comicsId as rcomicsId, tReleases.number as rnumber, tReleases.date as rdate, tReleases.price as rprice, tReleases.purchased as rpurchased, tReleases.ordered as rordered, tReleases.notes as rnotes, tReleases.lastUpdate as rlastUpdate, tReleases.tag as rtag FROM tComics INNER JOIN tReleases ON tComics.id = tReleases.comicsId WHERE tComics.removed = 0 AND tReleases.removed = 0 AND tComics.selected = 1 AND purchased = 0");
        }
    };

    private static final Migration MIGRATION_6_7 = new Migration(6, 7) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // creo nuova tabella tAvailableComics con indici
            database.execSQL("CREATE TABLE IF NOT EXISTS `tAvailableComics` (`sourceId` TEXT NOT NULL, `name` TEXT NOT NULL, `searchableName` TEXT NOT NULL, `publisher` TEXT NOT NULL, `version` INTEGER NOT NULL, `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL)");
            database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_tAvailableComics_sourceId` ON `tAvailableComics` (`sourceId`)");
            database.execSQL("CREATE INDEX IF NOT EXISTS `index_tAvailableComics_name_publisher_version` ON `tAvailableComics` (`name`, `publisher`, `version`)");
        }
    };

    private static final class FakeMigration extends Migration {

        FakeMigration(int startVersion, int endVersion) {
            super(startVersion, endVersion);
        }

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // non faccio nulla
        }
    }

    private static class DatabaseCallback extends RoomDatabase.Callback {

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            if (BuildConfig.DEBUG) {
//                new PopulateDbWithTestAsync(INSTANCE).execute();
//                new ClearDbWithTestAsync(INSTANCE).execute();
//                new ClearAvailableComicsAsync(INSTANCE).execute();
            }
        }
    }

    private static class PopulateDbWithTestAsync extends AsyncTask<Void, Void, Void> {

        private final ComicsDao mComicsDao;
        private final ReleaseDao mReleaseDao;

        PopulateDbWithTestAsync(ComikkuDatabase db) {
            mComicsDao = db.comicsDao();
            mReleaseDao = db.releaseDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mReleaseDao.deleteAll();
            mComicsDao.deleteAll();
            Comics[] list = new Comics[20];
            for (int ii=0; ii<list.length; ii++) {
                list[ii] = Comics.create(String.format("Comics #%s", ii+1));
            }
            mComicsDao.insert(list);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, -3);

            List<Comics> comics = mComicsDao.getRawComics();
            Release[] releases = new Release[comics.size() * 2];
            for (int ii=0, jj=0; ii<comics.size(); ii++) {
                // senza data (missing)
                releases[jj++] = Release.create(comics.get(ii).id, (int) (Math.random() * 10));
                // con data (lost)
                releases[jj++] = Release.create(comics.get(ii).id, (int) (Math.random() * 10), sdf.format(calendar.getTime()));

                calendar.add(Calendar.DAY_OF_MONTH, 1);
            }
            mReleaseDao.insert(releases);
            return null;
        }
    }

    private static class ClearDbWithTestAsync extends AsyncTask<Void, Void, Void> {

        private final ComicsDao mComicsDao;
        private final ReleaseDao mReleaseDao;

        ClearDbWithTestAsync(ComikkuDatabase db) {
            mComicsDao = db.comicsDao();
            mReleaseDao = db.releaseDao();
        }

        @Override
        protected Void doInBackground(final Void... params) {
            mReleaseDao.deleteAll();
            mComicsDao.deleteAll();
            return null;
        }
    }

    private static class ClearAvailableComicsAsync extends AsyncTask<Void, Void, Void> {

        private final CmkWebDao _cmkWebDao;

        ClearAvailableComicsAsync(@NonNull ComikkuDatabase db) {
            _cmkWebDao = db.cmkWebDao();
        }

        @Nullable
        @Override
        protected Void doInBackground(Void... voids) {
            _cmkWebDao.deleteAll();
            return null;
        }
    }
}
