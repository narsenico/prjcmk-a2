package it.amonshore.comikkua;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class LiveDataEx<T> extends LiveData<T> {

    public LiveDataEx() {
        super();
    }

    public LiveDataEx(T value) {
        super(value);
    }

    public void observeOnce(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        observeOnce(this, owner, observer);
    }

    public static <R> void observeOnce(@NonNull LiveData<R> liveData,
                                       @NonNull LifecycleOwner owner,
                                       @NonNull Observer<? super R> observer) {
        liveData.observe(owner, new Observer<R>() {
            @Override
            public void onChanged(R value) {
                liveData.removeObserver(this);
                observer.onChanged(value);
            }
        });
    }
}
