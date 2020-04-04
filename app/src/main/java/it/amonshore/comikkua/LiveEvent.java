package it.amonshore.comikkua;

import java.util.Iterator;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.Observer;

/**
 *
 * @param <T>
 * @see {https://proandroiddev.com/livedata-with-single-events-2395dea972a8}
 */
public class LiveEvent<T> extends MediatorLiveData<T> {

    public static <T> LiveData<T> toSingleEvent(LiveData<T> liveData) {
        final LiveEvent<T> result = new LiveEvent<>();
        result.addSource(liveData, result::setValue);
        return result;
    }

    private ArraySet<ObserverWrapper<? super T>> mObservers = new ArraySet<>();

    @Override
    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        final ObserverWrapper<T> wrapper = new ObserverWrapper<T>(observer);
        mObservers.add(wrapper);
        super.observe(owner, wrapper);
    }

    @Override
    public void observeForever(@NonNull Observer<? super T> observer) {
        final ObserverWrapper<T> wrapper = new ObserverWrapper<T>(observer);
        mObservers.add(wrapper);
        super.observeForever(wrapper);
    }

    @Override
    public void removeObserver(@NonNull Observer<? super T> observer) {
        //noinspection SuspiciousMethodCalls
        if (mObservers.remove(observer)) {
            super.removeObserver(observer);
            return;
        }
        final Iterator<ObserverWrapper<? super T>> iterator = mObservers.iterator();
        while (iterator.hasNext()) {
            ObserverWrapper<? super T> wrapper = iterator.next();
            if (wrapper.mObserver == observer) {
                iterator.remove();
                super.removeObserver(wrapper);
                break;
            }
        }
    }

    @Override
    public void setValue(T value) {
        for (ObserverWrapper<? super T> observer : mObservers) {
            observer.newValue();
        }
        super.setValue(value);
    }

    private static class ObserverWrapper<T> implements Observer<T> {

        Observer<? super T> mObserver;
        boolean mPending = false;

        ObserverWrapper(Observer<? super T> observer) {
            mObserver = observer;
        }

        @Override
        public void onChanged(T t) {
            if (mPending) {
                mPending = false;
                mObserver.onChanged(t);
            }
        }

        void newValue() {
            mPending = true;
        }
    }

}
