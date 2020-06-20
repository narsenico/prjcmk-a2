package it.amonshore.comikkua.data.web;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.GsonRequest;

public class CmkWebRepository {

    private final static String GET_COMICS_URL = BuildConfig.GET_COMICS_URL;
    private final static String GET_TITLES_URL = BuildConfig.GET_TITLES_URL;
    private final static String GET_RELEASES_TEMPLATE = BuildConfig.GET_RELEASES_TEMPLATE;
    private final RequestQueue mRequestQueue;

    public CmkWebRepository(Application application) {
        mRequestQueue = Volley.newRequestQueue(application);
    }

    public CustomData<List<CmkWebComics>> getComics() {
        // GSon non riesce a deserializzare una generica List<?>
        // quindi non posso passargli un Class<List<CmkWebComics>>
        // ma con uno specifito tipo Type invece ci riesce
        // https://sites.google.com/site/gson/gson-user-guide#TOC-Serializing-and-Deserializing-Generic-Types
        final Type type = new TypeToken<List<CmkWebComics>>() {}.getType();

        final CustomData<List<CmkWebComics>> liveData = new CustomData<>();
        final GsonRequest<List<CmkWebComics>> request = new GsonRequest<>(GsonRequest.Method.GET,
                GET_COMICS_URL,
                type,
                liveData);

        mRequestQueue.add(request);
        return liveData;
    }

    /**
     * Richiede solo l'elenco dei titoli, senza altre informazioni.
     *
     * @return elenco di titoli
     */
    public CustomData<List<String>> getTitles() {
        // la richiesta ritorna un array di stringhe
        final Type type = new TypeToken<List<String>>() {}.getType();

        final CustomData<List<String>> liveData = new CustomData<>();
        final GsonRequest<List<String>> request = new GsonRequest<>(GsonRequest.Method.GET,
                GET_TITLES_URL,
                type,
                liveData);

        mRequestQueue.add(request);
        return liveData;
    }

    public CustomData<List<CmkWebRelease>> getReleases(String title, int numberFrom) {
        final Type type = new TypeToken<List<CmkWebRelease>>() {}.getType();

        final String url = GET_RELEASES_TEMPLATE
                .replace(":title", title)
                .replace(":numberFrom", Integer.toString(numberFrom));

        final CustomData<List<CmkWebRelease>> liveData = new CustomData<>();
        final GsonRequest<List<CmkWebRelease>> request = new GsonRequest<>(GsonRequest.Method.GET,
                url,
                type,
                liveData);

        mRequestQueue.add(request);
        return liveData;
    }
}
