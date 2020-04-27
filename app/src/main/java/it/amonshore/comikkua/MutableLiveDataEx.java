package it.amonshore.comikkua;

public class MutableLiveDataEx<T> extends LiveDataEx<T> {

    public MutableLiveDataEx(T value) {
        super(value);
    }

    public MutableLiveDataEx() {
        super();
    }

    @Override
    public void postValue(T value) {
        super.postValue(value);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }

}
