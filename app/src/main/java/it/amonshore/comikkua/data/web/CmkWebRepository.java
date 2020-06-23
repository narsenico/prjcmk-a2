package it.amonshore.comikkua.data.web;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.amonshore.comikkua.BuildConfig;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.GsonRequest;

public class CmkWebRepository {

    private final static String GET_COMICS_URL;
    private final static String GET_TITLES_URL;
    private final static String GET_RELEASES_TEMPLATE;
    private final static Map<String, String> HEADERS;
    private final RequestQueue mRequestQueue;

    static {
        if (BuildConfig.CMKWEB.equals("LOCAL")) {
            GET_COMICS_URL = "http://192.168.0.4:5000/v1/comics";
            GET_TITLES_URL = "http://192.168.0.4:5000/v1/comics?mode=array-of-title";
            GET_RELEASES_TEMPLATE = "http://192.168.0.4:5000/v1/title/:title/releases?numberFrom=:numberFrom";
            HEADERS = null;
        } else if (BuildConfig.CMKWEB.equals("RAPIDAPI")) {
            GET_COMICS_URL = "https://cmkweb1.p.rapidapi.com/v1/comics";
            GET_TITLES_URL = "https://cmkweb1.p.rapidapi.com/v1/comics?mode=array-of-title";
            GET_RELEASES_TEMPLATE = "https://cmkweb1.p.rapidapi.com/v1/title/:title/releases?numberFrom=:numberFrom";
            HEADERS = new HashMap<>();
            HEADERS.put("x-rapidapi-host", "cmkweb1.p.rapidapi.com");
            HEADERS.put("x-rapidapi-key", "d46dfdacccmsh3bc31c7c1970f40p12801cjsnec30d44d3008");
        } else {
            GET_COMICS_URL = "https://cmkweb.herokuapp.com/v1/comics";
            GET_TITLES_URL = "https://cmkweb.herokuapp.com/v1/comics?mode=array-of-title";
            GET_RELEASES_TEMPLATE = "https://cmkweb.herokuapp.com/v1/title/:title/releases?numberFrom=:numberFrom";
            HEADERS = null;
        }
    }

    public CmkWebRepository(Application application) {
        // TODO: per implementare un uso corretto della cache con etag
        //  potrebbe essere necessario scrivere una impl di HttpStack
        //  che esegua prima una HEAD
        mRequestQueue = Volley.newRequestQueue(application);
    }

    public CustomData<List<CmkWebComics>> getComics() {
        // GSon non riesce a deserializzare una generica List<?>
        // quindi non posso passargli un Class<List<CmkWebComics>>
        // ma con uno specifito tipo Type invece ci riesce
        // https://sites.google.com/site/gson/gson-user-guide#TOC-Serializing-and-Deserializing-Generic-Types
        final Type type = new TypeToken<List<CmkWebComics>>() {}.getType();

        LogHelper.d("CMKWEB " + GET_COMICS_URL);

        final CustomData<List<CmkWebComics>> liveData = new CustomData<>();
        final GsonRequest<List<CmkWebComics>> request = new GsonRequest<>(GsonRequest.Method.GET,
                GET_COMICS_URL,
                type,
                liveData);
        request.setHeaders(HEADERS);

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

        LogHelper.d("CMKWEB " + GET_TITLES_URL);

        final CustomData<List<String>> liveData = new CustomData<>();
        final GsonRequest<List<String>> request = new GsonRequest<>(GsonRequest.Method.GET,
                GET_TITLES_URL,
                type,
                liveData);
        request.setHeaders(HEADERS);

        mRequestQueue.add(request);
        return liveData;
    }

    public CustomData<List<CmkWebRelease>> getReleases(String title, int numberFrom) {
        final Type type = new TypeToken<List<CmkWebRelease>>() {}.getType();

        final String url = GET_RELEASES_TEMPLATE
                .replace(":title", title)
                .replace(":numberFrom", Integer.toString(numberFrom));

        LogHelper.d("CMKWEB " + url);

        final CustomData<List<CmkWebRelease>> liveData = new CustomData<>();
        final GsonRequest<List<CmkWebRelease>> request = new GsonRequest<>(GsonRequest.Method.GET,
                url,
                type,
                liveData);
        request.setHeaders(HEADERS);

        mRequestQueue.add(request);
        return liveData;
    }
}
