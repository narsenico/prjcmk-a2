package it.amonshore.comikkua.data;

import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;

import androidx.lifecycle.MutableLiveData;
import it.amonshore.comikkua.Utility;

public class CustomData<T> extends MutableLiveData<Resource<T>>
        implements Response.Listener<T>, Response.ErrorListener {

    @Override
    public void onErrorResponse(VolleyError error) {
        onErrorResponse((Exception) error);
    }

    public void onErrorResponse(Exception error) {
        final String message = error.getMessage();
        if (Utility.isNullOrEmpty(message)) {
            if (error instanceof TimeoutError) {
                postValue(Resource.error(null, "Timeout"));
            } else {
                postValue(Resource.error(null, String.format("Unknown error: '%s'", error.getClass().getName())));
            }
        } else {
            postValue(Resource.error(null, error.getMessage()));
        }
    }

    @Override
    public void onResponse(T response) {
        postValue(Resource.success(response));
    }
}
