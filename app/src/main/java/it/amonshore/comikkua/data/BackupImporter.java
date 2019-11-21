package it.amonshore.comikkua.data;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.comics.ComicsRepository;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseDao;

public class BackupImporter {

    private final static String TRUE = "T";
    private final static String FALSE = "F";

    private final static String FIELD_ID = "id";
    private final static String FIELD_NAME = "name";
    private final static String FIELD_SERIES = "series";
    private final static String FIELD_PUBLISHER = "publisher";
    private final static String FIELD_AUTHORS = "authors";
    private final static String FIELD_PRICE = "price";
    private final static String FIELD_PERIODICITY = "periodicity";
    private final static String FIELD_RESERVED = "reserved";
    private final static String FIELD_NOTES = "notes";
    private final static String FIELD_RELEASES = "releases";
    private final static String FIELD_NUMBER = "number";
    private final static String FIELD_DATE = "date";
    private final static String FIELD_REMINDER = "reminder";
    private final static String FIELD_PURCHASED = "purchased";
    private final static String FIELD_ORDERED = "ordered";

    private final ComicsDao mComicsDao;
    private final ReleaseDao mReleaseDao;

    public BackupImporter(Application application) {
        final ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mComicsDao = db.comicsDao();
        mReleaseDao = db.releaseDao();
    }

    public BackupImporter(ComicsDao comicsRepository, ReleaseDao releaseRepository) {
        mComicsDao = comicsRepository;
        mReleaseDao = releaseRepository;
    }

    public int importFromAssets(@NonNull AssetManager assetManager, @NonNull String fileName, boolean clearData) {
        InputStream stream = null;
        try {
            stream = assetManager.open(fileName, AssetManager.ACCESS_STREAMING);
            return importFromStream(stream, clearData);
        } catch (Exception ex) {
            LogHelper.e("Importing backup from assets", ex);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ioex) {
                    LogHelper.e("Closing stream", ioex);
                }
            }
            return 0;
        }
    }

    public int importFromFile(@NonNull File file, boolean clearData) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return importFromStream(stream, clearData);
        } catch (Exception ex) {
            LogHelper.e("Importing backup from file", ex);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ioex) {
                    LogHelper.e("Closing stream", ioex);
                }
            }
            return 0;
        }
    }

    public int importFromStream(@NonNull InputStream stream, boolean clearData) throws IOException, JSONException {
        final int size = stream.available();
        final byte[] buffer = new byte[size];
        final int len = stream.read(buffer);

        if (clearData) {
            mReleaseDao.deleteAll();
            mComicsDao.deleteAll();
        }

        if (len > 0) {
            final String data = new String(buffer);
            final JSONArray arr = (JSONArray) new JSONTokener(data).nextValue();
            for (int ii = 0; ii < arr.length(); ii++) {
                ComicsWithReleases cwr = parseComics(arr.getJSONObject(ii));
                mComicsDao.insert(cwr.comics);
                Comics comics = mComicsDao.getRawComicsByRefJsonId(cwr.comics.refJsonId);
                if (comics.refJsonId == cwr.comics.refJsonId) {
                    for (Release release : cwr.releases) {
                        release.comicsId = comics.id;
                    }
                    mReleaseDao.insert(cwr.releases.toArray(new Release[0]));
                }
            }
            return arr.length();
        } else {
            return 0;
        }
    }

    private ComicsWithReleases parseComics(JSONObject object) throws JSONException {
        final ComicsWithReleases cwr = new ComicsWithReleases();
        final Comics comics = new Comics();
        comics.refJsonId = object.getLong(FIELD_ID);
        comics.name = object.getString(FIELD_NAME);
        comics.series = tryGetString(object, FIELD_SERIES);
        comics.publisher = tryGetString(object, FIELD_PUBLISHER);
        comics.authors = tryGetString(object, FIELD_AUTHORS);
        comics.price = object.getLong(FIELD_PRICE);
        comics.periodicity = tryGetString(object, FIELD_PERIODICITY);
        comics.reserved = Objects.equals(TRUE, object.getString(FIELD_RESERVED));
        comics.notes = tryGetString(object, FIELD_NOTES);
        cwr.comics = comics;

        final JSONArray arrReleases = object.getJSONArray(FIELD_RELEASES);
        final ArrayList<Release> list = new ArrayList<>();
        for (int ii = 0; ii < arrReleases.length(); ii++) {
            list.add(parseRelease(arrReleases.getJSONObject(ii)));
        }
        cwr.releases = list;

        return cwr;
    }

    private Release parseRelease(JSONObject object) throws JSONException {
        final Release release = new Release();
        release.number = object.getInt(FIELD_NUMBER);
        release.date = tryGetDate(object, FIELD_DATE);
        release.price = object.getLong(FIELD_PRICE);
        release.ordered = Objects.equals(TRUE, object.getString(FIELD_ORDERED));
        release.purchased = Objects.equals(TRUE, object.getString(FIELD_PURCHASED));
        release.notes = tryGetString(object, FIELD_NOTES);
        return release;
    }

    private String tryGetString(JSONObject obj, String field) throws JSONException {
        return obj.isNull(field) ? null : obj.getString(field);
    }

    private String tryGetDate(JSONObject obj, String field) throws JSONException {
        return obj.isNull(field) ? null : obj.getString(field).replaceAll("\\-", "");
    }
}
