package it.amonshore.comikkua.data;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;
import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.release.DatedRelease;
import it.amonshore.comikkua.data.release.LostRelease;
import it.amonshore.comikkua.data.release.MissingRelease;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseDao;

@Database(entities = {Comics.class, Release.class},
        views = {MissingRelease.class, LostRelease.class, DatedRelease.class},
        version = 3)
public abstract class ComikkuDatabase extends RoomDatabase {

    public abstract ComicsDao comicsDao();

    public abstract ReleaseDao releaseDao();

    private static volatile ComikkuDatabase INSTANCE;

    public static ComikkuDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (ComikkuDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ComikkuDatabase.class, "comikku_database")
                            .fallbackToDestructiveMigration() // in questo modo al cambio di vesione il vecchio DB viene semplicemente distrutto (con conseguente perdita di dati)
//                            .addMigrations(MIGRATION_1_2)
                            .addCallback(new DatabaseCallback(context))
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
//    private static final Migration MIGRATION_2_3 = new Migration(2, 3) {
//        @Override
//        public void migrate(@NonNull SupportSQLiteDatabase database) {
//            database.execSQL("CREATE UNIQUE INDEX tComics_name_unique_index ON tComics(name)");
//        }
//    };

    private static class DatabaseCallback extends RoomDatabase.Callback {

        Context mContext;

        DatabaseCallback(Context context) {
            mContext = context;
        }

        @Override
        public void onOpen(@NonNull SupportSQLiteDatabase db) {
            super.onOpen(db);
            if (BuildConfig.DEBUG) {
//                new PopulateDbWithTestAsync(INSTANCE).execute();
//                new ClearDbWithTestAsync(INSTANCE).execute();
//                new ImportAsyncTask(mContext.getAssets(), INSTANCE).execute("backup.json");
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

    private static class ImportAsyncTask extends AsyncTask<String, Void, Integer> {

        private final AssetManager mAssetManager;
        private final ComicsDao mComicsDao;
        private final ReleaseDao mReleaseDao;

        ImportAsyncTask(AssetManager assetManager, ComikkuDatabase db) {
            mAssetManager = assetManager;
            mComicsDao = db.comicsDao();
            mReleaseDao = db.releaseDao();
        }

        @Override
        protected Integer doInBackground(final String... params) {
            mReleaseDao.deleteAll();
            mComicsDao.deleteAll();

            final BackupImporter importer = new BackupImporter(mComicsDao, mReleaseDao);
            return importer.importFromAssets(mAssetManager, params[0]);
        }
    }
}
