package it.amonshore.comikkua.data.web;

import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
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
        // non uso direttamente l'id del comics perché è composta da nome (normalizzato) e reissue
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

    /**
     * Cerca la nuova release di un comics a partire da un dato numero (compreso).
     *
     * TODO: considerare reissue
     *
     * @param title      titolo del comics per cui cercare la nuova release
     * @param numberFrom numero da cui iniziare la ricerca (compreso)
     * @return nuova release
     */
    public CustomData<List<CmkWebRelease>> getReleases(String title, int numberFrom) {
        LogHelper.d("CMKWEB/Firestore read '%s' with release >= %s", title, numberFrom);

        final CustomData<List<CmkWebRelease>> liveData = new CustomData<>();
        liveData.postValue(Resource.loading(null));

        // carico il documento relativo al comics
        // considerando che l'id è composto da <nome normalizzato>_<reissue>
        // inoltre eseguo un query sull'id della release, ricordando che è una stringa
        mFirestore.collection("comics")
                .document(formatComicsId(title, 0))
                .collection("releases")
                .whereGreaterThanOrEqualTo("releaseNumber", numberFrom)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        final QuerySnapshot result = task.getResult();
                        if (result == null) {
                            // nessun risultato, ritorno una lista vuota
                            liveData.postValue(Resource.success(Collections.emptyList()));
                        } else {
                            final List<CmkWebRelease> releases = new ArrayList<>();
                            for (QueryDocumentSnapshot document : result) {
                                releases.add(fromDocument(title, document));
                            }
                            liveData.postValue(Resource.success(releases));
                        }
                    } else {
                        final Exception error = task.getException();
                        LogHelper.e("Error loading releases", error);
                        if (error == null) {
                            liveData.postValue(Resource.error(null, "Unknown error"));
                        } else {
                            liveData.postValue(Resource.error(null, error.getMessage()));
                        }
                    }
                });

        return liveData;
    }

    private String formatComicsId(@NonNull String title, int reissue) {
        return String.format("%s_%s",
                title.replaceAll("/", "_").toUpperCase(),
                reissue);
    }

    private CmkWebRelease fromDocument(@NonNull String title, @Nonnull QueryDocumentSnapshot document) {
        final CmkWebRelease release = new CmkWebRelease();
        release.title = title;
        release.number = Integer.parseInt(document.getId());
        release.date = mReleaseDateFormat.format(document.getTimestamp("releaseDate").toDate());
        return release;
    }
}
