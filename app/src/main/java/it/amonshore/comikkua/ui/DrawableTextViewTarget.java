package it.amonshore.comikkua.ui;

import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DrawableTextViewTarget extends CustomViewTarget<TextView, Drawable> {

    public DrawableTextViewTarget(@NonNull TextView view) {
        super(view);
    }

    @Override
    protected void onResourceCleared(@Nullable Drawable placeholder) {
        getView().setBackground(placeholder);
    }

    @Override
    public void onLoadFailed(@Nullable Drawable errorDrawable) {
        getView().setBackground(errorDrawable);
    }

    @Override
    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
        getView().setBackground(resource);
    }
}
