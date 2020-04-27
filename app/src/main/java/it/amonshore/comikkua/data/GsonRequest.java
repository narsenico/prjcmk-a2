package it.amonshore.comikkua.data;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.google.gson.GsonBuilder;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.util.Map;

import androidx.annotation.NonNull;

public class GsonRequest<T> extends JsonRequest<T> {
    private final Class<T> clazz;
    private final Type type;

    public GsonRequest(int method, @NonNull String url, @NonNull Class<T> clazz,
                       @NonNull CustomData<T> customData) {
        super(method, url, null, customData, customData);
        this.clazz = clazz;
        this.type = null;
        customData.postValue(Resource.loading(null));
    }

    /**
     * Usare questo costruttore quando type si riferisce, ad esempio, a List<?>.
     *
     * @param method     metodo HTTP
     * @param url        url da cui prelevare il json
     * @param type       tipo da usare durante la deserializzazione del json
     * @param customData {@link androidx.lifecycle.LiveData} che andrà a contenere lo stato della richiesta
     *                                                      e il json deserializzato
     */
    public GsonRequest(int method, @NonNull String url, @NonNull Type type,
                       @NonNull CustomData<T> customData) {
        super(method, url, null, customData, customData);
        this.clazz = null;
        this.type = type;
        customData.postValue(Resource.loading(null));
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return super.getParams();
    }

    @Override
    protected Response<T> parseNetworkResponse(NetworkResponse response) {
        try {
            final String json = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
            T result;
            if (clazz == null) {
                result = new GsonBuilder().create().fromJson(json, type);
            } else {
                result = new GsonBuilder().create().fromJson(json, clazz);
            }
            return Response.success(result,
                    HttpHeaderParser.parseCacheHeaders(response));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }
    }
}
