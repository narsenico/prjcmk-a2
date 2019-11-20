package it.amonshore.comikkua.ui;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.widget.TextView;

import com.bumptech.glide.request.target.CustomViewTarget;
import com.bumptech.glide.request.transition.Transition;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;

public class DrawableTextViewTarget extends CustomViewTarget<TextView, Drawable> {

    private ColorStateList mTint;

    public DrawableTextViewTarget(@NonNull TextView view) {
        super(view);
    }

    public DrawableTextViewTarget(@NonNull TextView view, @ColorInt int tintColor) {
        super(view);
        mTint = ColorStateList.valueOf(tintColor);
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
        if (mTint == null) {
            getView().setBackground(resource);
        } else {
            getView().setBackground(applyTint(resource, mTint));
        }
    }

    private Drawable applyTint(Drawable input, ColorStateList tint) {
        final Drawable wrappedDrawable = DrawableCompat.wrap(input);
        DrawableCompat.setTintList(wrappedDrawable, tint);
        DrawableCompat.setTintMode(wrappedDrawable, PorterDuff.Mode.MULTIPLY);
        return wrappedDrawable;
    }
}
