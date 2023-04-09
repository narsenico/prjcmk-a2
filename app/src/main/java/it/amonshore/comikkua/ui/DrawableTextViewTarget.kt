package it.amonshore.comikkua.ui

import android.graphics.drawable.Drawable
import android.widget.TextView
import com.bumptech.glide.request.target.CustomViewTarget
import com.bumptech.glide.request.transition.Transition

class DrawableTextViewTarget(view: TextView) : CustomViewTarget<TextView?, Drawable?>(view) {
    override fun onResourceCleared(placeholder: Drawable?) {
        getView().background = placeholder
    }

    override fun onLoadFailed(errorDrawable: Drawable?) {
        getView().background = errorDrawable
    }

    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable?>?) {
        getView().background = resource
    }
}