package it.amonshore.comikkua.ui.releases.adapter

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import it.amonshore.comikkua.data.release.ReleaseHeader
import it.amonshore.comikkua.ui.getDrawableFromResource
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal class SwappableItemDecoration(
    context: Context,
    @DrawableRes drawableLeft: Int,
    @DrawableRes drawableRight: Int,
    @ColorRes drawableColor: Int,
    @ColorRes lineColor: Int,
    @StringRes leftText: Int,
    @StringRes rightText: Int,
    private val drawableLeftPadding: Int,
    private val drawableRightPadding: Int,
    private val lineHeight: Float,
    private val drawableSpeed: Float,
) : ItemDecoration() {

    private val _ciDrawableColor = context.getColor(drawableColor)
    private val _linePaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = context.getColor(lineColor)
    }
    private val _textPaint: Paint = Paint().apply {
        color = _ciDrawableColor
        isFakeBoldText = true
        textSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            16f, context.resources.displayMetrics
        )
    }
    private val _drawableLeft: Drawable =
        context.getDrawableFromResource(drawableLeft, _ciDrawableColor)
    private val _drawableRight: Drawable =
        context.getDrawableFromResource(drawableRight, _ciDrawableColor)
    private val _leftText: String = context.getString(leftText)
    private val _rightText: String = context.getString(rightText)
    private val _leftTextWidth: Int = _textPaint.measureText(_leftText).toInt()
    private val _rightTextWidth: Int = _textPaint.measureText(_rightText).toInt()
    private val _textHeight: Int = _textPaint.getFontMetricsInt(null)
    private val _bounds = Rect()

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        canvas.save()

        val left: Float
        val right: Float
        if (parent.clipToPadding) {
            left = parent.paddingLeft + BORDER_SIZE
            right = parent.width - parent.paddingRight - BORDER_SIZE
        } else {
            left = BORDER_SIZE
            right = parent.width - BORDER_SIZE
        }

        for (child in parent.children) {
            drawChild(child, parent, canvas, left, right)
        }

        canvas.restore()
    }

    private fun drawChild(
        child: View,
        parent: RecyclerView,
        canvas: Canvas,
        left: Float,
        right: Float
    ) {
        val tx = child.translationX.roundToInt()
        // se non sto eseguendo uno swipe non disegno nulla
        if (tx == 0) return
        // anche in caso di header non disegno nulla
        if (parent.getChildItemId(child) >= ReleaseHeader.BASE_ID) return
        parent.getDecoratedBoundsWithMargins(child, _bounds)
        val top = _bounds.top.toFloat()
        val bottom = _bounds.bottom.toFloat()
        // il drawable deve essere grande 1/3 del child
        val size = (bottom - top) / 3
        // distanza dai bordi superiore e inferiore (centrato)
        val py = (bottom - top) / 2 - size / 2
        // dimensione della linea (calcolata come frazione della grandezza del drawable)
        val lineSize = size * lineHeight
        // distanza della liena dai bordi (centrato)
        val ly = (bottom - top) / 2f - lineSize / 2f
        // distanza del testo dai bordi (centrato)
        val ty = (bottom - top) / 2f - _textHeight / 2f + 10f
        canvas.drawRoundRect(
            left,
            top + ly,
            right,
            top + ly + lineSize,
            50f,
            50f,
            _linePaint
        )
        if (tx > 0) {
            drawLeft(right, size, left, tx, canvas, bottom, ty, top, py)
        } else {
            drawRight(left, right, size, tx, canvas, bottom, ty, top, py)
        }
    }

    private fun drawRight(
        left: Float,
        right: Float,
        size: Float,
        tx: Int,
        canvas: Canvas,
        bottom: Float,
        ty: Float,
        top: Float,
        py: Float
    ) {
        // il drawable segue la view durante lo swipe
        val start = max(
            left + drawableRightPadding,
            min(
                right - size - drawableRightPadding,
                _bounds.right + (tx * drawableSpeed)
            )
        )
        // se ci sta disegno il testo
        if (right - start - size > _rightTextWidth + 20f) {
            canvas.drawText(
                _rightText,
                start + size + 20f,
                bottom - ty,
                _textPaint
            )
        }
        _drawableRight.setBounds(
            start.toInt(),
            (top + py).toInt(),
            (start + size).toInt(),
            (top + py + size).toInt()
        )
        _drawableRight.draw(canvas)
    }

    private fun drawLeft(
        right: Float,
        size: Float,
        left: Float,
        tx: Int,
        canvas: Canvas,
        bottom: Float,
        ty: Float,
        top: Float,
        py: Float
    ) {
        // il drawable segue la view durante lo swipe
        val start = min(
            right - size - drawableLeftPadding,
            max(
                left + drawableLeftPadding,
                (tx * drawableSpeed) - size + left
            )
        )
        // se ci sta disegno il testo
        if (start - left > _leftTextWidth + 20f) {
            canvas.drawText(
                _leftText,
                start - _leftTextWidth - 20f,
                bottom - ty,
                _textPaint
            )
        }
        _drawableLeft.setBounds(
            start.toInt(),
            (top + py).toInt(),
            (start + size).toInt(),
            (top + py + size).toInt()
        )
        _drawableLeft.draw(canvas)
    }

    companion object {
        private const val BORDER_SIZE = 32F
    }
}