package it.amonshore.comikkua.data.web;

import android.app.Application;
import android.content.Context;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.common.util.concurrent.SettableFuture;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import androidx.paging.PagingSource;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.Utility;
import it.amonshore.comikkua.data.ComikkuDatabase;
import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.Resource;
import it.amonshore.comikkua.data.comics.Comics;
import it.amonshore.comikkua.data.comics.ComicsDao;
import it.amonshore.comikkua.data.comics.ComicsWithReleases;
import kotlin.coroutines.Continuation;

public class FirebaseRepository {

    public static String getProjectId() {
        return FirebaseFirestore.getInstance().getApp().getOptions().getProjectId();
    }

    private final FirebaseFirestore mFirestore;
    private final SimpleDateFormat mReleaseDateFormat;
    private final Executor mExecutor;
    private final ComicsDao mComicsDao;

    public FirebaseRepository(@NonNull Context context) {
        mFirestore = FirebaseFirestore.getInstance();
        mReleaseDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
        mExecutor = Executors.newCachedThreadPool();

        final ComikkuDatabase db = ComikkuDatabase.getDatabase(context);
        mComicsDao = db.comicsDao();

        LogHelper.d("CMKWEB/Firestore using projectId " + mFirestore.getApp().getOptions().getProjectId());
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

//    public PagingSource<String, CmkWebComics> getComicsPagingSource() {
//
//    }

    public CustomData<List<CmkWebComics>> getComics() {
        LogHelper.d("CMKWEB/Firestore read comics");

        final CustomData<List<CmkWebComics>> liveData = new CustomData<>();
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
                            final ArrayList<CmkWebComics> lstComics = new ArrayList<>();
                            for (QueryDocumentSnapshot document : result) {
                                lstComics.add(document.toObject(CmkWebComics.class));
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
        // (da eseguire in un thread che andrà eventualmente ad aggiornare la tabella)
        // 1. se ci sono novità remote
        // 2. aggiorno tComics aggiungendo i nuovi comics o aggiornando quelli esistenti (tComics.sourceId)

        final CustomData<Integer> liveData = new CustomData<>();
        liveData.postValue(Resource.loading(null));

        // estraggo tutti i documenti dalla collection "comics"
        mFirestore.collection("comics")
                .get()
                // completo in un thread separato per via delle operazioni su DB
                .addOnCompleteListener(mExecutor, task -> {
                    if (task.isSuccessful()) {
                        final QuerySnapshot result = task.getResult();
                        if (result == null) {
                            // nessun risultato, ritorno una lista vuota
                            liveData.postValue(Resource.success(0));
                        } else {
                            // possono esserci comics con lo stesso nome, ma per publisher divefsi e/o ristampe diverse
                            final ArrayList<Comics> lstNewComics = new ArrayList<>();
                            final ArrayList<Comics> lstUpdComics = new ArrayList<>();
                            CmkWebComics cmkWebComics;
                            Comics comics;
                            for (QueryDocumentSnapshot document : result) {
                                cmkWebComics = document.toObject(CmkWebComics.class);
                                cmkWebComics.id = document.getId(); // id non viene considerato da toObject!!!
                                // leggo il comics con sourceId=id dal db, se esiste aggiorno, altrimenti creo
                                comics = mComicsDao.getRawComicsBySourceId(cmkWebComics.id);
                                if (comics == null) {
                                    lstNewComics.add(updateOrCreateComics(cmkWebComics, comics));
                                } else {
                                    lstUpdComics.add(updateOrCreateComics(cmkWebComics, comics));
                                }
                            }
                            // controllo anche se inserisco e aggiorno tutti i comics
                            final int newSize = lstNewComics.size();
                            final int updSize = lstUpdComics.size();
                            int count = 0;
                            // inserisco i nuovi comics
                            if (newSize > 0) {
                                final int newCount = mComicsDao.insert(lstNewComics.toArray(new Comics[0])).length;
                                if (newCount != newSize) {
                                    LogHelper.w("CMKWEB/Firestore refresh insert problem: expected %s, inserted %s", newSize, newCount);
                                } else {
                                    LogHelper.i("CMKWEB/Firestore refresh comics: inserted %s comics", newCount);
                                }
                                count += newCount;
                            }
                            // aggiorno i comics già esistenti
                            if (updSize > 0) {
                                final int updCount = mComicsDao.update(lstUpdComics.toArray(new Comics[0]));
                                if (updCount != updSize) {
                                    LogHelper.w("CMKWEB/Firestore refresh update problem: expected %s, updated %s", updSize, updCount);
                                } else {
                                    LogHelper.i("CMKWEB/Firestore refresh comics: updated %s comics", updCount);
                                }
                                count += updCount;
                            }
                            liveData.postValue(Resource.success(count));
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
                });

        return liveData;
    }

    private Comics updateOrCreateComics(@NonNull CmkWebComics cmkWebComics, Comics comics) {
        if (comics == null) {
            comics = new Comics();
        }
        comics.name = cmkWebComics.name;
        comics.sourceId = cmkWebComics.id;
        comics.publisher = cmkWebComics.publisher;
        comics.version = cmkWebComics.version;
        return comics;
    }

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

    /**
     * PaginSource per la lettura dei comics dai Firestore asincrona.
     */
    private final class CmkWebComicsPagingSource extends PagingSource<String, CmkWebComics> {

        private Future<LoadResult<String, CmkWebComics>> loadFuture(@NotNull LoadParams<String> loadParams) {

            // tipologia di Future che può essere "settata" in un qualsiasi momento del futuro
            // in questo caso verrà "settata" appena ho caricato i dati da remoto
            final SettableFuture<LoadResult<String, CmkWebComics>> future = SettableFuture.create();
            // chiave da qui devo caricare (>=), corrisponde al searchableName, con qui è anche ordinata la query
            final String key = loadParams.getKey();
            final int limit = loadParams.getLoadSize();

            LogHelper.d("CMKWEB/Firestore read paginated comics fromKey='%s' limit=%s", key, limit);

            final Query query;
            if (key != null) {
                query = mFirestore.collection("comics")
                        .whereGreaterThan("searchableName", key);
            } else {
                query = mFirestore.collection("comics");
            }

            query.orderBy("searchableName")
                    .limit(limit)
                    .get()
                    // completo il caricamento in un thread separato
                    .addOnCompleteListener(mExecutor, task -> {
                        if (task.isSuccessful()) {
                            try {
                                final QuerySnapshot result = task.getResult();
                                if (result != null) {
                                    LogHelper.d("Parsing %s comics", result.size());
                                    final List<CmkWebComics> lstComics = new ArrayList<>();
                                    for (QueryDocumentSnapshot document : result) {
                                        lstComics.add(document.toObject(CmkWebComics.class));
                                    }

                                    final int size = lstComics.size();
                                    // l'ultimo elemento conterrà la chiave per la prossima chiamata
                                    final String nextKey = size > 0 ? lstComics.get(size - 1).searchableName : null;
                                    LogHelper.d(String.format("Result size=%s nextKey='%s'", size, nextKey));

                                    future.set(new LoadResult.Page<>(lstComics, null, nextKey));
                                } else {
                                    LogHelper.w("Result: no data");
                                    future.set(new LoadResult.Page<>(Collections.emptyList(), null, null));
                                }
                            } catch (Exception ex) {
                                LogHelper.e("Result: read error", ex);
                                future.set(new LoadResult.Error<>(ex));
                            }
                        } else {
                            final Exception error = task.getException();
                            if (error != null) {
                                LogHelper.e("Result: read error", error);
                                future.set(new LoadResult.Error<>(error));
                            } else {
                                LogHelper.e("Result: read error");
                                future.set(new LoadResult.Error<>(new UnknownError()));
                            }
                        }
                    });

            return future;
        }

        @Nullable
        @Override
        public LoadResult<String, CmkWebComics> load(@NotNull LoadParams<String> loadParams,
                                                     @NotNull Continuation<? super LoadResult<String, CmkWebComics>> continuation) {
            try {
                // attendo il caricamento dei dati da remoto
                return loadFuture(loadParams).get();
            } catch (ExecutionException | InterruptedException ex) {
                LogHelper.e("Result: load error", ex);
                return new LoadResult.Error<>(ex);
            }
        }
    }
}
