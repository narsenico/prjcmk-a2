package it.amonshore.comikkua.data;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.JsonWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import it.amonshore.comikkua.data.release.Release;
import it.amonshore.comikkua.data.release.ReleaseDao;
import it.amonshore.comikkua.ui.ImageHelper;

/**
 * TODO: da testare BENE import/export con nuovi campi tComics (sourceId, etc.)
 */
public class BackupHelper {

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
    private final static String FIELD_IMAGE_DATA = "image_data";
    private final static String FIELD_IMAGE_URI = "image_uri";
    private final static String FIELD_SOURCE_ID = "source_id";
    private final static String FIELD_VERSION = "version";
    private final static String FIELD_SELECTED = "selected";
    private final static String FIELD_RELEASES = "releases";
    private final static String FIELD_NUMBER = "number";
    private final static String FIELD_DATE = "date";
    private final static String FIELD_REMINDER = "reminder";
    private final static String FIELD_PURCHASED = "purchased";
    private final static String FIELD_ORDERED = "ordered";

    private final ComicsDao mComicsDao;
    private final ReleaseDao mReleaseDao;

    public final static int RETURN_ERR = -1;

    public BackupHelper(Application application) {
        final ComikkuDatabase db = ComikkuDatabase.getDatabase(application);
        mComicsDao = db.comicsDao();
        mReleaseDao = db.releaseDao();
    }

//    public int importFromAssets(@NonNull Context context, @NonNull String fileName, boolean clearData) {
//        final AssetManager assetManager = context.getAssets();
//        InputStream stream = null;
//        try {
//            stream = assetManager.open(fileName, AssetManager.ACCESS_STREAMING);
//            final int count = importFromStream(context, stream, clearData);
//            if (clearData) {
//                clearCachedData(context);
//            }
//            return count;
//        } catch (Exception ex) {
//            LogHelper.e("Importing backup from assets", ex);
//            if (stream != null) {
//                try {
//                    stream.close();
//                } catch (IOException ioex) {
//                    LogHelper.e("Closing stream", ioex);
//                }
//            }
//            return RETURN_ERR;
//        }
//    }

    /**
     * @param context   contesto
     * @param file      file da caricare, in formato JSON
     * @param clearData se true vengono eliminati sia i dati sul database, che la cache delle immagini;
     *                  la cancellazione avviene solo se ci sono dati da importare e non ci sono stati errori
     *                  durante la fase di parsing
     * @return numero di comics importati, oppure RETURN_ERR in caso di errore
     */
    @WorkerThread
    public int importFromFile(@NonNull Context context, @NonNull File file, boolean clearData) {
        FileInputStream stream = null;
        try {
            stream = new FileInputStream(file);
            return importFromStream(context, stream, clearData);
        } catch (Exception ex) {
            LogHelper.e("Importing backup from file", ex);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ioex) {
                    LogHelper.e("Closing stream", ioex);
                }
            }
            return RETURN_ERR;
        }
    }

    private int importFromStream(@NonNull Context context, @NonNull InputStream stream, boolean clearData)
            throws IOException, JSONException {
        final int size = stream.available();
        final byte[] buffer = new byte[size];
        final int len = stream.read(buffer);

        // prima leggo i dati dal file, e solo se non ci sono stati problemi intervengo sul DB
        if (len > 0) {
            final List<ComicsWithReleases> list = new ArrayList<>();

            final String data = new String(buffer);
            final JSONArray arr = (JSONArray) new JSONTokener(data).nextValue();
            for (int ii = 0; ii < arr.length(); ii++) {
                list.add(parseComics(context, arr.getJSONObject(ii)));
            }

            if (clearData) {
                mReleaseDao.deleteAll();
                mComicsDao.deleteAll();
                ImageHelper.deleteImageFiles(context, true);
            }

            for (ComicsWithReleases cwr : list) {
                final long id = mComicsDao.insert(cwr.comics);
                if (BuildConfig.DEBUG) {
                    if (id != cwr.comics.id) {
                        throw new AssertionError("ID returned by insert statement must be equals to comics ID");
                    }
                }
//                cwr.comics.id = id;
//                for (Release release : cwr.releases) {
//                    release.comicsId = id;
//                }
                mReleaseDao.insert(cwr.releases.toArray(new Release[0]));

                // sposto l'immagine dalla cache a files/
                for (File srcFile : context.getCacheDir().listFiles((dir, name) -> ImageHelper.isValidImageFileName(name, cwr.comics.id))) {
                    final File dstFile = new File(context.getFilesDir(), srcFile.getName());
                    if (!srcFile.renameTo(dstFile)) {
                        LogHelper.e("Error moving image temp file '%s' to files/", srcFile);
                    }
                }
            }

            return list.size();
        } else {
            return 0;
        }
    }

    private ComicsWithReleases parseComics(@NonNull Context context, JSONObject object)
            throws JSONException, IOException {
        final ComicsWithReleases cwr = new ComicsWithReleases();
        final Comics comics = new Comics();
        comics.id = object.getLong(FIELD_ID);
        comics.refJsonId = object.getLong(FIELD_ID);
        comics.name = object.getString(FIELD_NAME);
        comics.series = tryGetString(object, FIELD_SERIES);
        comics.publisher = tryGetString(object, FIELD_PUBLISHER);
        comics.authors = tryGetString(object, FIELD_AUTHORS);
        comics.price = object.getLong(FIELD_PRICE);
        comics.periodicity = tryGetString(object, FIELD_PERIODICITY);
        comics.reserved = Objects.equals(TRUE, object.getString(FIELD_RESERVED));
        comics.notes = tryGetString(object, FIELD_NOTES);
        comics.image = tryGetString(object, FIELD_IMAGE_URI);
        comics.sourceId = tryGetString(object, FIELD_SOURCE_ID);
        comics.version = tryGetInt(object, FIELD_VERSION, 0);
        // visto che si esportano solo i selezionati, presumo che quelli importati siano anch'essi selezionati
        comics.selected = tryGetBoolean(object, FIELD_SELECTED, true);

        cwr.comics = comics;

        final JSONArray arrReleases = object.getJSONArray(FIELD_RELEASES);
        final ArrayList<Release> list = new ArrayList<>();
        for (int ii = 0; ii < arrReleases.length(); ii++) {
            list.add(parseRelease(comics.id, arrReleases.getJSONObject(ii)));
        }
        cwr.releases = list;

        // salvo i file nella cache mantenendo il nome del file
        // verranno spostati nella cartella corretta solo se il parsing di tutto il JSON andrà a buon fine
        if (comics.hasImage()) {
            // per sicurezza aggiorno il percorso dell'immagine, perché potrebbe non essere compatibile con questo device
            final Uri imgUri = Uri.fromFile(new File(context.getFilesDir(), ImageHelper.newImageFileName(comics.id)));
            comics.image = imgUri.toString();

            final Uri uri = Uri.withAppendedPath(Uri.fromFile(context.getCacheDir()), imgUri.getLastPathSegment());
            // salvo l'immagine direttamente nel JSON in Base64
            final byte[] bytes = Base64.decode(tryGetString(object, FIELD_IMAGE_DATA), Base64.DEFAULT);
            OutputStream os = null;
            try {
                os = context.getContentResolver().openOutputStream(uri);
                if (os != null) {
                    os.write(bytes);
                }
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException ioex) {
                        LogHelper.e("Closing stream", ioex);
                    }
                }
            }
        }

        return cwr;
    }

    private Release parseRelease(long comicsId, JSONObject object) throws JSONException {
        final Release release = new Release();
        release.comicsId = comicsId;
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

    private int tryGetInt(JSONObject obj, String field, int def) throws JSONException {
        return obj.isNull(field) ? def : obj.getInt(field);
    }

    private boolean tryGetBoolean(JSONObject obj, String field, boolean def) throws JSONException {
        return obj.isNull(field) ? def : obj.getString(field).equals(TRUE);
    }

    private String tryGetDate(JSONObject obj, String field) throws JSONException {
        return obj.isNull(field) ? null : obj.getString(field).replaceAll("\\-", "");
    }

    /**
     * @param context contesto
     * @param file    file di destinazione, se già presente verrà sovrascritto
     * @return numero di comics esportati, oppure RETURN_ERR in caso di errore
     */
    @WorkerThread
    public int exportToFile(@NonNull Context context, @NonNull File file) {
        FileOutputStream stream = null;
        try {
            stream = new FileOutputStream(file);
            return exportToStream(context, stream);
        } catch (Exception ex) {
            LogHelper.e("Exporting backup to file", ex);
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException ioex) {
                    LogHelper.e("Closing stream", ioex);
                }
            }
            return RETURN_ERR;
        }
    }

    private int exportToStream(@NonNull Context context, @NonNull OutputStream stream)
            throws IOException {
        final ContentResolver contentResolver = context.getContentResolver();
        // estrae solo i comics selezionati
        final List<ComicsWithReleases> comicsList = mComicsDao.getRawComicsWithReleases();
        final JsonWriter writer = new JsonWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8));
        writer.beginArray();
        int count = 0;
        for (ComicsWithReleases cwr : comicsList) {
            writeComics(contentResolver, writer, cwr);
            ++count;
        }
        writer.endArray();
        writer.close();
        return count;
    }

    private void writeComics(@NonNull ContentResolver contentResolver, @NonNull JsonWriter writer, ComicsWithReleases cwr)
            throws IOException {
        final Comics comics = cwr.comics;

        writer.beginObject();
        writer.name(FIELD_ID).value(comics.id);
        writer.name(FIELD_NAME).value(comics.name);
        writer.name(FIELD_SERIES).value(comics.series);
        writer.name(FIELD_PUBLISHER).value(comics.publisher);
        writer.name(FIELD_AUTHORS).value(comics.authors);
        writer.name(FIELD_PRICE).value(comics.price);
        writer.name(FIELD_PERIODICITY).value(comics.periodicity);
        writer.name(FIELD_RESERVED).value(comics.reserved ? TRUE : FALSE);
        writer.name(FIELD_NOTES).value(comics.notes);
        writer.name(FIELD_SOURCE_ID).value(comics.sourceId);
        writer.name(FIELD_VERSION).value(comics.version);
        writer.name(FIELD_SELECTED).value(comics.selected ? TRUE : FALSE);

        if (comics.hasImage()) {
            final Uri uri = Uri.parse(comics.image);
            InputStream is = null;
            try {
                is = contentResolver.openInputStream(uri);
                if (is != null) {
                    final int len = is.available();
                    if (len > 0) {
                        final byte[] buff = new byte[len];
                        if (is.read(buff) > 0) {
                            writer.name(FIELD_IMAGE_DATA).value(Base64.encodeToString(buff, Base64.DEFAULT));
                            writer.name(FIELD_IMAGE_URI).value(comics.image);
                        }
                    }
                }
            } catch (Exception ex) {
                LogHelper.e("Error during backup of '%s'", comics.name);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioex) {
                        LogHelper.e("Closing stream", ioex);
                    }
                }
            }

        } else {
            writer.name(FIELD_IMAGE_DATA).nullValue();
            writer.name(FIELD_IMAGE_URI).nullValue();
        }

        writer.name(FIELD_RELEASES);
        writer.beginArray();
        for (Release release : cwr.releases) {
            writeRelease(writer, release);
        }
        writer.endArray();
        writer.endObject();
    }

    private void writeRelease(JsonWriter writer, Release release) throws IOException {
        writer.beginObject();
        writer.name(FIELD_NUMBER).value(release.number);
        writer.name(FIELD_DATE);
        writer.value(release.date);
        writer.name(FIELD_PRICE).value(release.price);
        writer.name(FIELD_ORDERED).value(release.ordered ? TRUE : FALSE);
        writer.name(FIELD_PURCHASED).value(release.purchased ? TRUE : FALSE);
        writer.name(FIELD_NOTES).value(release.notes);
        writer.endObject();
    }
}
