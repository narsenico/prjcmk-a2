package it.amonshore.comikkua.data.comics;

import android.app.Application;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import it.amonshore.comikkua.data.CustomData;
import it.amonshore.comikkua.data.GsonRequest;

class CmkWebRepository {

    private final static String GET_COMICS_URL = "https://cmkweb.herokuapp.com/comics";
    private final RequestQueue mRequestQueue;

    CmkWebRepository(Application application) {
        mRequestQueue = Volley.newRequestQueue(application);
    }

    CustomData<List<Comics>> getComics() {
        // GSon non riesce a deserializzare una generica List<?>
        // quindi non posso passargli un Class<List<Comics>>
        // ma con uno specifito tipo Type invece ci riesce
        // https://sites.google.com/site/gson/gson-user-guide#TOC-Serializing-and-Deserializing-Generic-Types
        final Type type = new TypeToken<List<Comics>>() {}.getType();

        final CustomData<List<Comics>> liveData = new CustomData<>();
        final GsonRequest<List<Comics>> request = new GsonRequest<>(GsonRequest.Method.GET,
                GET_COMICS_URL,
                type,
                liveData);

        mRequestQueue.add(request);
        return liveData;
    }
}
