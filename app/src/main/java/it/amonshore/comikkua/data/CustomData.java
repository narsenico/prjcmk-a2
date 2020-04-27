package it.amonshore.comikkua.data;

import com.android.volley.Response;
import com.android.volley.VolleyError;

import androidx.lifecycle.MutableLiveData;

public class CustomData<T> extends MutableLiveData<Resource<T>>
        implements Response.Listener<T>, Response.ErrorListener {

    @Override
    public void onErrorResponse(VolleyError error) {
        postValue(Resource.error(null, error.getMessage()));
    }

    @Override
    public void onResponse(T response) {
        postValue(Resource.success(response));
    }
}
