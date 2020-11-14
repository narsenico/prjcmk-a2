package it.amonshore.comikkua.data.web;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;

import androidx.annotation.NonNull;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.Resource;

public class FirebaseRepository {

    private final FirebaseFirestore mFirestore;
    private final SimpleDateFormat mReleaseDateFormat;

    public FirebaseRepository() {
        mFirestore = FirebaseFirestore.getInstance();
        mReleaseDateFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        LogHelper.d("CMKWEB/Firestore using projectId " + mFirestore.getApp().getOptions().getProjectId());
    }

    /**
     * Richiede solo l'elenco dei titoli, senza altre informazioni.
     *
     * @return elenco di titoli
     */
    public CustomData<List<String>> getTitles() {
        LogHelper.d("CMKWEB/Firestore read all comics");

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

    public CustomData<List<CmkWebComics>> getComics() {
        // TODO: da implementare
        throw new UnsupportedOperationException("Not yet implemented");
    }

    /**
     * Cerca la nuova release di un comics a partire da un dato numero (compreso).
     *
     * @param title      titolo del comics per cui cercare la nuova release
     * @param numberFrom numero da cui iniziare la ricerca (compreso)
     * @return nuova release
     * @deprecated Usare getReleases passando anche il numero di ristampa
     */
    @Deprecated
    public CustomData<List<CmkWebRelease>> getReleases(String title, int numberFrom) {
        return getReleases(title, 0, numberFrom);
    }

    /**
     * Cerca la nuova release di un comics a partire da un dato numero (compreso).
     *
     * @param title      titolo del comics per cui cercare la nuova release
     * @param reissue    numero di ristampa (0 per la prima edizione)
     * @param numberFrom numero da cui iniziare la ricerca (compreso)
     * @return nuova release
     */
    public CustomData<List<CmkWebRelease>> getReleases(String title, int reissue, int numberFrom) {
        final CustomData<List<CmkWebRelease>> liveData = new CustomData<>();
        liveData.postValue(Resource.loading(null));

        final String searchableName = formatComicsId(title, reissue);

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
                            final QuerySnapshot qs = (QuerySnapshot)o;
                            for (QueryDocumentSnapshot qds : qs) {
                                releases.add(fromDocument(title, qds));
                            }
                        }
                        liveData.postValue(Resource.success(releases));
                    });
                });

        return liveData;
    }

    private String formatComicsId(@NonNull String title, int reissue) {
        return String.format("%s_%s",
                title.replaceAll("/", "_").toUpperCase(),
                reissue);
    }

    private CmkWebRelease fromDocument(@NonNull String title, @Nonnull QueryDocumentSnapshot
            document) {
        final CmkWebRelease release = new CmkWebRelease();
        release.title = title;
        release.number = Integer.parseInt(document.getId());
        release.date = mReleaseDateFormat.format(document.getTimestamp("releaseDate").toDate());
        return release;
    }
}
