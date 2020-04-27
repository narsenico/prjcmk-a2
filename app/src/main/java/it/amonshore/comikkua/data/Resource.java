package it.amonshore.comikkua.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public final class Resource<T> {

    public enum Status {
        SUCCESS,
        ERROR,
        LOADING
    }

    @NonNull
    public Status status;
    @Nullable
    public T data;
    @Nullable
    public String message;

    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data    = data;
        this.message = message;
    }

    static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    static <T> Resource<T> error(@Nullable T data, @Nullable String message) {
        return new Resource<>(Status.ERROR, data, message);
    }

    static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }
}
