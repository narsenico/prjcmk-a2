package it.amonshore.comikkua.data.web;

import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.paging.PagingSource;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.Resource;

public class CmkWebRepository {

    public static String getProjectId() {
        return FirebaseFirestore.getInstance().getApp().getOptions().getProjectId();
    }

    private final FirebaseFirestore mFirestore;
    private final SimpleDateFormat mReleaseDateFormat;
    private final Executor mExecutor;
    private final CmkWebDao mCmkWebDao;
//    private CmkWebComicsPagingSource mComicsPagingSource;

    public CmkWebRepository(@NonNull Context context) {
        mFirestore = FirebaseFirestore.getInstance();
        mReleaseDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        mExecutor = Executors.newCachedThreadPool();

        final ComikkuDatabase db = ComikkuDatabase.getDatabase(context);
        mCmkWebDao = db.cmkWebDao();

        LogHelper.d("CMKWEB/Firestore using projectId " + mFirestore.getApp().getOptions().getProjectId());
    }

    PagingSource<Integer, AvailableComics> getAvailableComicsPagingSource() {
        return mCmkWebDao.getAvailableComicsPagingSource();
    }

    PagingSource<Integer, AvailableComics> getAvailableComicsPagingSource(String likeName) {
        return mCmkWebDao.getAvailableComicsPagingSource(likeName);
    }

    public void refreshAvailableComics(AvailableComics... comics) {
        mCmkWebDao.refresh(comics);
    }

    public void refreshAvailableComics(List<AvailableComics> lstComics) {
        mCmkWebDao.refresh(lstComics.toArray(new AvailableComics[0]));
    }

    /**
     * Richiede solo l'elenco dei titoli, senza altre informazioni.
     *
     * @return elenco di titoli
     */
    public CustomData<List<String>> getComicNames() {
        LogHelper.d("CMKWEB/Firestore read all comic names");

        final CustomData<List<String>> liveData = new CustomData<>();
        liveData.postValue(Resource.loading(null));
        // estraggo tutti i documenti dalla collection "comics"
        // ogni documento è un comics, che possiede la proprietà "name"
        mFirestore.collection("comics")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        final QuerySnapshot result = task.getResult();
                        if (result == null) {
                            // nessun risultato, ritorno una lista vuota
                            liveData.postValue(Resource.success(Collections.emptyList()));
                        } else {
                            // salvo i titoli prima in un HashSet a causa dei doppioni
                            // (visto che possono esistere diverse "reissue" per ogni comics)
                            final HashSet<String> titles = new HashSet<>();
                            for (QueryDocumentSnapshot document : result) {
                                titles.add(document.getString("name"));
                            }
                            liveData.postValue(Resource.success(new ArrayList<>(titles)));
                        }
                    } else {
                        final Exception error = task.getException();
                        LogHelper.e("Error loading titles", error);
                        if (error == null) {
                            liveData.postValue(Resource.error(null, "Unknown error"));
                        } else {
                            liveData.postValue(Resource.error(null, error.getMessage()));
                        }
                    }
                });

        return liveData;
    }

//    public PagingSource<String, AvailableComics> getComicsPagingSource() {
//        LogHelper.d("CMKWEB/Firestore read all comic");
//        if (mComicsPagingSource == null) {
//            mComicsPagingSource = new CmkWebComicsPagingSource();
//        }
//        return mComicsPagingSource;
//    }

    public CustomData<List<AvailableComics>> getComics() {
        LogHelper.d("CMKWEB/Firestore read comics");

        final CustomData<List<AvailableComics>> liveData = new CustomData<>();
        liveData.postValue(Resource.loading(null));
        // estraggo tutti i documenti dalla collection "comics"
        mFirestore.collection("comics")
                .orderBy("searchableName")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        final QuerySnapshot result = task.getResult();
                        if (result == null) {
                            // nessun risultato, ritorno una lista vuota
                            liveData.postValue(Resource.success(Collections.emptyList()));
                        } else {
                            // possono esserci comics con lo stesso nome, ma per publisher divefsi e/o ristampe diverse
                            final ArrayList<AvailableComics> lstComics = new ArrayList<>();
                            for (QueryDocumentSnapshot document : result) {
                                lstComics.add(document.toObject(AvailableComics.class));
                            }
                            liveData.postValue(Resource.success(lstComics));
                        }
                    } else {
                        final Exception error = task.getException();
                        LogHelper.e("Error loading titles", error);
                        if (error == null) {
                            liveData.postValue(Resource.error(null, "Unknown error"));
                        } else {
                            liveData.postValue(Resource.error(null, error.getMessage()));
                        }
                    }
                });

        return liveData;
    }

    public CustomData<Integer> refreshComics() {
        LogHelper.d("CMKWEB/Firestore refresh comics");

        final CustomData<Integer> liveData = new CustomData<>();
        liveData.postValue(Resource.loading(null));

        // estraggo tutti i documenti dalla collection "comics"
        mFirestore.collection("comics")
                .get()
                // completo in un thread separato per via delle operazioni su DB
                .addOnCompleteListener(mExecutor, task -> {
                    try {
                        if (task.isSuccessful()) {
                            final QuerySnapshot result = task.getResult();
                            if (result == null) {
                                // nessun risultato, ritorno una lista vuota
                                liveData.postValue(Resource.success(0));
                            } else {
                                // leggo i comics da Firestore e li salvo nel DB locale
                                final ArrayList<AvailableComics> lstComics = new ArrayList<>();
                                for (QueryDocumentSnapshot document : result) {
                                    lstComics.add(document.toObject(AvailableComics.class).withSourceId(document.getId()));
                                }
                                int size = lstComics.size();
                                // pulisco la tabella e poi inserisco i nuovi comics
                                // tutto sotto transazione
                                mCmkWebDao.refresh(lstComics.toArray(new AvailableComics[0]));
                                LogHelper.d("CMKWEB/Firestore refresh with %s comics", size);
                                liveData.postValue(Resource.success(size));
                            }
                        } else {
                            final Exception error = task.getException();
                            LogHelper.e("Error refreshing comics", error);
                            if (error == null) {
                                liveData.postValue(Resource.error(null, "Unknown error"));
                            } else {
                                liveData.postValue(Resource.error(null, error.getMessage()));
                            }
                        }
                    } catch (Exception ex) {
                        LogHelper.e("CMKWEB/Firestore result: refresh error");
                        liveData.postValue(Resource.error(null, ex.getMessage()));
                    }
                });

        return liveData;
    }

//    private Comics updateOrCreateComics(@NonNull AvailableComics availableComics, Comics comics) {
//        if (comics == null) {
//            comics = new Comics();
//        }
//        comics.name = availableComics.name;
//        comics.sourceId = availableComics.sourceId;
//        comics.publisher = availableComics.publisher;
//        comics.version = availableComics.version;
//        return comics;
//    }

    /**
     * Cerca la nuova release di un comics a partire da un dato numero (compreso).
     *
     * @param comicsName titolo del comics per cui cercare la nuova release
     * @param numberFrom numero da cui iniziare la ricerca (compreso)
     * @return nuova release
     * @deprecated Usare getReleases passando anche il numero di ristampa
     */
    @Deprecated
    public CustomData<List<CmkWebRelease>> getReleases(String comicsName, int numberFrom) {
        return getReleases(comicsName, 0, numberFrom);
    }

    /**
     * Cerca la nuova release di un comics a partire da un dato numero (compreso).
     *
     * @param comicsName titolo del comics per cui cercare la nuova release
     * @param reissue    numero di ristampa (0 per la prima edizione)
     * @param numberFrom numero da cui iniziare la ricerca (compreso)
     * @return nuova release
     */
    public CustomData<List<CmkWebRelease>> getReleases(String comicsName, int reissue, int numberFrom) {
        final CustomData<List<CmkWebRelease>> liveData = new CustomData<>();
        liveData.postValue(Resource.loading(null));

        final String searchableName = formatComicsId(comicsName, reissue);

        LogHelper.d("CMKWEB/Firestore read '%s' with release >= %s",
                searchableName, numberFrom);

        // eseguo una query sul nome
        // in teoria dovrebbe ritornare un solo comics, ma mi tocca gestire risultati multipli
        // per ogni comcis estraggo le release
        mFirestore.collection("comics")
                .whereEqualTo("searchableName", searchableName)
                .get()
                .addOnCompleteListener(task -> {
                    try {
                        List<Task<QuerySnapshot>> tasks = new ArrayList<>();
                        for (DocumentSnapshot ds : task.getResult()) {
                            LogHelper.d("CMKWEB/Firestore found '%s'", ds.getId());
                            tasks.add(ds.getReference().collection("releases")
                                    .whereGreaterThanOrEqualTo("releaseNumber", numberFrom)
                                    .get());
                        }

                        Tasks.whenAllSuccess(tasks).addOnCompleteListener(task1 -> {
                            final List<CmkWebRelease> releases = new ArrayList<>();
                            for (Object o : task1.getResult()) {
                                final QuerySnapshot qs = (QuerySnapshot) o;
                                for (QueryDocumentSnapshot qds : qs) {
                                    releases.add(fromDocument(comicsName, qds));
                                }
                            }
                            liveData.postValue(Resource.success(releases));
                        });
                    } catch (Exception ex) {
                        LogHelper.e("CMKWEB/Firestore result: read error");
                        liveData.postValue(Resource.error(null, ex.getMessage()));
                    }
                });

        return liveData;
    }

    private String formatComicsId(@NonNull String comicsName, int reissue) {
        return String.format("%s_%s",
                comicsName.replaceAll("/", "_").toUpperCase(),
                reissue);
    }

    private CmkWebRelease fromDocument(@NonNull String comicsName, @Nonnull QueryDocumentSnapshot
            document) {
        final CmkWebRelease release = new CmkWebRelease();
        release.comicsName = comicsName;
        release.number = Integer.parseInt(document.getId());
        release.date = mReleaseDateFormat.format(document.getTimestamp("releaseDate").toDate());
        return release;
    }

//    /**
//     * PaginSource per la lettura dei comics dai Firestore asincrona.
//     */
//    private final class CmkWebComicsPagingSource extends PagingSource<String, AvailableComics> {
//
//        private Future<LoadResult<String, AvailableComics>> loadFuture(@NotNull LoadParams<String> loadParams) {
//            // tipologia di Future che può essere "settata" in un qualsiasi momento del futuro
//            // in questo caso verrà "settata" appena ho caricato i dati da remoto
//            final SettableFuture<LoadResult<String, AvailableComics>> future = SettableFuture.create();
//            // chiave da qui devo caricare (>=), corrisponde all'id, con qui è anche ordinata la query
//            final String key = loadParams.getKey();
//            final int limit = loadParams.getLoadSize();
//
//            LogHelper.d("CMKWEB/Firestore read paginated comics fromKey='%s' limit=%s", key, limit);
//
//            final Query query;
//            if (key != null) {
//                query = mFirestore.collection("comics")
//                        .whereGreaterThan(FieldPath.documentId(), key);
//            } else {
//                query = mFirestore.collection("comics");
//            }
//
//            // TODO: usare un altro campo per ordinare che prenda anche in considerazione il nome
//            //  ad esempio definire un nuovo campo che è la concetenazione di publisher + name + version
//
//            query.orderBy(FieldPath.documentId())
//                    .limit(limit)
//                    .get()
//                    // completo il caricamento in un thread separato
//                    .addOnCompleteListener(mExecutor, task -> {
//                        if (task.isSuccessful()) {
//                            try {
//                                final QuerySnapshot result = task.getResult();
//                                if (result != null) {
//                                    LogHelper.d("CMKWEB/Firestore parsing %s comics", result.size());
//                                    final List<AvailableComics> lstComics = new ArrayList<>();
//                                    for (QueryDocumentSnapshot document : result) {
//                                        lstComics.add(prepare(document));
//                                    }
//
//                                    final int size = lstComics.size();
//                                    // l'ultimo elemento conterrà la chiave per la prossima chiamata
//                                    final String nextKey = size > 0 ? lstComics.get(size - 1).id : null;
//                                    LogHelper.d(String.format("CMKWEB/Firestore result size=%s nextKey='%s'", size, nextKey));
//
//                                    future.set(new LoadResult.Page<>(lstComics, null, nextKey));
//                                } else {
//                                    LogHelper.w("CMKWEB/Firestore result: no data");
//                                    future.set(new LoadResult.Page<>(Collections.emptyList(), null, null));
//                                }
//                            } catch (Exception ex) {
//                                LogHelper.e("CMKWEB/Firestore result: read error", ex);
//                                future.set(new LoadResult.Error<>(ex));
//                            }
//                        } else {
//                            final Exception error = task.getException();
//                            if (error != null) {
//                                LogHelper.e("CMKWEB/Firestore result: read error", error);
//                                future.set(new LoadResult.Error<>(error));
//                            } else {
//                                LogHelper.e("CMKWEB/Firestore result: read error");
//                                future.set(new LoadResult.Error<>(new UnknownError()));
//                            }
//                        }
//                    });
//
//            return future;
//        }
//
//        private AvailableComics prepare(@NonNull QueryDocumentSnapshot document) {
//            final AvailableComics comics = document.toObject(AvailableComics.class)
//                    .withId(document.getId());
//            comics.selected = mCmkWebDao.existsSourceId(comics.id);
//            LogHelper.d("CMKWEB/Firestore prepare name='%s' selected=%s", comics.name, comics.selected);
//            return comics;
//        }
//
//        @Nullable
//        @Override
//        public LoadResult<String, AvailableComics> load(@NotNull LoadParams<String> loadParams,
//                                                        @NotNull Continuation<? super LoadResult<String, AvailableComics>> continuation) {
//            try {
//                // attendo il caricamento dei dati da remoto
//                LogHelper.d("CMKWEB/Firestore loading...");
//                return loadFuture(loadParams).get();
//            } catch (ExecutionException | InterruptedException ex) {
//                LogHelper.e("CMKWEB/Firestore result: load error", ex);
//                return new LoadResult.Error<>(ex);
//            }
//        }
//    }
}
