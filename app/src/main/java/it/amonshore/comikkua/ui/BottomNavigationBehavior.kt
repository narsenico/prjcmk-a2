package it.amonshore.comikkua.ui

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar.SnackbarLayout
import kotlin.math.max
import kotlin.math.min

/**
 * Code from [Valentin Hinov converted from Kotlin](https://android.jlelse.eu/scroll-your-bottom-navigation-view-away-with-10-lines-of-code-346f1ed40e9e)
 */
class BottomNavigationBehavior<V>(
    context: Context,
    attrs: AttributeSet
) : CoordinatorLayout.Behavior<V>(context, attrs) where V : View {

    private var _lastStartedType = 0
    private var _offsetAnimator: ValueAnimator? = null

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        if (axes != ViewCompat.SCROLL_AXIS_VERTICAL) return false

        _lastStartedType = type
        _offsetAnimator?.cancel()

        return true
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        type: Int
    ) {
        if (_lastStartedType == ViewCompat.TYPE_TOUCH ||
            type == ViewCompat.TYPE_NON_TOUCH
        ) {
            val currTranslation = child.translationY
            val childHalfHeight = child.height * 0.5f
            val visible = currTranslation < childHalfHeight
            animateBarVisibility(child, visible)
        }
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        super.onNestedPreScroll(coordinatorLayout, child, target, dx, dy, consumed, type)

        child.translationY = max(
            0f,
            min(child.height.toFloat(), child.translationY + dy)
        )
    }

    override fun onApplyWindowInsets(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        insets: WindowInsetsCompat
    ): WindowInsetsCompat {
        val isVisible = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom == 0
        animateBarVisibility(child, isVisible)
        return super.onApplyWindowInsets(coordinatorLayout, child, insets)
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: V,
        dependency: View
    ): Boolean {
        if (dependency is SnackbarLayout) {
            updateSnackbar(child, dependency)
        }

        return super.layoutDependsOn(parent, child, dependency)
    }

    private fun updateSnackbar(
        child: View,
        snackbarLayout: View
    ) {
        val params = snackbarLayout.layoutParams
        if (params is CoordinatorLayout.LayoutParams) {
            with(params) {
                anchorId = child.id
                anchorGravity = Gravity.CENTER_HORIZONTAL
                gravity = Gravity.CENTER_HORIZONTAL
            }
            snackbarLayout.layoutParams = params
        }
    }

    private fun animateBarVisibility(child: View, isVisible: Boolean) {
        if (_offsetAnimator == null) {
            _offsetAnimator = ValueAnimator().apply {
                interpolator = DecelerateInterpolator()
                duration = 150L
                addUpdateListener { animation ->
                    child.translationY = animation.animatedValue as Float
                }
            }
        } else {
            _offsetAnimator?.cancel()
        }

        _offsetAnimator?.run {
            val targetTranslation = if (isVisible) 0f else child.height.toFloat()
            setFloatValues(child.translationY, targetTranslation)
            start()
        }
    }
}