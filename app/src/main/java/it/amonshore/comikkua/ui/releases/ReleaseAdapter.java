package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Iterator;
import java.util.Objects;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.recyclerview.selection.ItemKeyProvider;
import androidx.recyclerview.selection.Selection;
import androidx.recyclerview.selection.SelectionTracker;
import androidx.recyclerview.selection.StorageStrategy;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import it.amonshore.comikkua.LogHelper;
import it.amonshore.comikkua.R;
import it.amonshore.comikkua.data.release.ComicsRelease;
import it.amonshore.comikkua.data.release.IReleaseViewModelItem;
import it.amonshore.comikkua.data.release.ReleaseHeader;
import it.amonshore.comikkua.ui.CustomItemKeyProvider;

public class ReleaseAdapter extends ListAdapter<IReleaseViewModelItem, AReleaseViewModelItemViewHolder> {
    private SelectionTracker<Long> mSelectionTracker;
    private ReleaseViewHolder.Callback mReleaseViewHolderCallback;
//    @MenuRes
//    private int mReleaseMenuRes;
//    private ReleaseCallback mRelaseCallback;

    private ReleaseAdapter() {
        super(DIFF_CALLBACK);
    }

    @Override
    public void onBindViewHolder(@NonNull AReleaseViewModelItemViewHolder holder, int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item != null) {
            if (item.getItemType() == ReleaseHeader.ITEM_TYPE) {
                holder.bind(item, false);
            } else {
                final ComicsRelease release = (ComicsRelease) item;
                holder.bind(item, mSelectionTracker.isSelected(release.release.id));
            }
        } else {
            holder.clear();
        }
    }

    @NonNull
    @Override
    public AReleaseViewModelItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ReleaseHeader.ITEM_TYPE) {
            return ReleaseHeaderViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
        } else {
            return ReleaseViewHolder.create(LayoutInflater.from(parent.getContext()), parent, mReleaseViewHolderCallback);
        }
    }

    @Override
    public long getItemId(int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item == null) {
            return RecyclerView.NO_ID;
        } else {
            return item.getId();
        }
    }

    @Override
    public int getItemViewType(int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item == null) {
            return RecyclerView.INVALID_TYPE;
        } else {
            return item.getItemType();
        }
    }

    @Nullable
    public IReleaseViewModelItem getItemAt(int position) {
        return getItem(position);
    }

    SelectionTracker<Long> getSelectionTracker() {
        return mSelectionTracker;
    }

    interface OnItemSelectedListener {

        void onSelectionChanged(@Nullable Iterator<Long> keys, int size);
    }

    interface ReleaseCallback {

        void onReleaseClick(@NonNull ComicsRelease release);

        void onReleaseTogglePurchase(@NonNull ComicsRelease release);

        void onReleaseToggleOrder(@NonNull ComicsRelease release);

        void onReleaseMenuItemSelected(@NonNull MenuItem item, @NonNull ComicsRelease release);
    }

    static class Builder {
        private final RecyclerView mRecyclerView;
        //        private OnItemActivatedListener<Long> mOnItemActivatedListener;
        private OnItemSelectedListener mOnItemSelectedListener;
        @MenuRes
        private int releaseMenuRes;
        private ReleaseCallback releaseCallback;

        Builder(@NonNull RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

//        Builder withOnItemActivatedListener(OnItemActivatedListener<Long> listener) {
//            mOnItemActivatedListener = listener;
//            return this;
//        }

        Builder withOnItemSelectedListener(OnItemSelectedListener listener) {
            mOnItemSelectedListener = listener;
            return this;
        }

        Builder withReleaseCallback(@MenuRes int menuRes, @NonNull ReleaseCallback callback) {
            releaseMenuRes = menuRes;
            releaseCallback = callback;
            return this;
        }

        ReleaseAdapter build() {
            final ReleaseAdapter adapter = new ReleaseAdapter();
            adapter.mReleaseViewHolderCallback = new ReleaseViewHolder.Callback(releaseMenuRes) {
                @Override
                void onReleaseClick(long comicsId, long id, int position) {
                    if (releaseCallback != null) {
                        final IReleaseViewModelItem item = adapter.getItemAt(position);
                        if (item != null) {
                            final ComicsRelease release = (ComicsRelease) item;
                            releaseCallback.onReleaseClick(release);
                        }
                    }
                }

                @Override
                void onReleaseMenuSelected(@NonNull MenuItem menuItem, long comicsId, long id, int position) {
                    if (releaseCallback != null) {
                        final IReleaseViewModelItem item = adapter.getItemAt(position);
                        if (item != null) {
                            final ComicsRelease release = (ComicsRelease) item;
                            releaseCallback.onReleaseMenuItemSelected(menuItem, release);
                        }
                    }
                }
            };
            // questo è necessario insieme all'override di getItemId() per funzionare con SelectionTracker
            adapter.setHasStableIds(true);
            mRecyclerView.setAdapter(adapter);

            final SelectionTracker.Builder<Long> builder = new SelectionTracker.Builder<>(
                    "comics-selection",
                    mRecyclerView,
                    new CustomItemKeyProvider(mRecyclerView, ItemKeyProvider.SCOPE_MAPPED),
                    new ReleaseItemDetailsLookup(mRecyclerView),
                    StorageStrategy.createLongStorage())
                    .withSelectionPredicate(new SelectionTracker.SelectionPredicate<Long>() {
                        @Override
                        public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
                            // escludo dalla selezione gli header
                            return key < ReleaseHeader.BASE_ID;
                        }

                        @Override
                        public boolean canSetStateAtPosition(int position, boolean nextState) {
                            // TODO: non viene mai chiamato! non riesco ad escludere le multi release
//                            LogHelper.d("canSetStateAtPosition position=%s nextState=%s", position, nextState);
//                            final IReleaseViewModelItem item = adapter.getItemAt(position);
//                            return item != null && item.getItemType() == ComicsRelease.ITEM_TYPE;
                            return true;
                        }

                        @Override
                        public boolean canSelectMultiple() {
                            return true;
                        }
                    });

//            if (mOnItemActivatedListener != null) {
//                builder.withOnItemActivatedListener(mOnItemActivatedListener);
//            }

            adapter.mSelectionTracker = builder.build();

            if (mOnItemSelectedListener != null) {
                adapter.mSelectionTracker.addObserver(new SelectionTracker.SelectionObserver() {
                    @Override
                    public void onSelectionChanged() {
                        if (adapter.mSelectionTracker.hasSelection()) {
                            final Selection<Long> selection = adapter.getSelectionTracker().getSelection();
                            mOnItemSelectedListener.onSelectionChanged(selection.iterator(), selection.size());
                        } else {
                            mOnItemSelectedListener.onSelectionChanged(null, 0);
                        }
                    }

                    @Override
                    public void onSelectionRestored() {
                        LogHelper.d("fire selection changed onSelectionRestored");
                        if (adapter.mSelectionTracker.hasSelection()) {
                            final Selection<Long> selection = adapter.getSelectionTracker().getSelection();
                            mOnItemSelectedListener.onSelectionChanged(selection.iterator(), selection.size());
                        } else {
                            mOnItemSelectedListener.onSelectionChanged(null, 0);
                        }
                        super.onSelectionRestored();
                    }
                });
            }

            // gestisco con uno swipe il purchase
            final ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT | ItemTouchHelper.LEFT) {

                @Override
                public boolean isLongPressDragEnabled() {
                    return false;
                }

                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                    // inibisco lo swipe per gli header e le multi release e se è in corso una selezione
//                    if (viewHolder instanceof ReleaseHeaderViewHolder) return 0;
                    if (adapter.mSelectionTracker.hasSelection()) return 0;
                    final IReleaseViewModelItem item = adapter.getItemAt(viewHolder.getAdapterPosition());
                    if (item == null || item.getItemType() != ComicsRelease.ITEM_TYPE) return 0;
                    return super.getSwipeDirs(recyclerView, viewHolder);
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
//                    LogHelper.d("Release swiped direction=%s", direction);
                    final IReleaseViewModelItem item = adapter.getItemAt(viewHolder.getAdapterPosition());
                    if (item != null) {
                        final ComicsRelease release = (ComicsRelease) item;
                        if (direction == ItemTouchHelper.RIGHT) {
                            releaseCallback.onReleaseTogglePurchase(release);
                        } else if (direction == ItemTouchHelper.LEFT) {
                            releaseCallback.onReleaseToggleOrder(release);
                        }
                    }
                }
            };
            new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);

            mRecyclerView.addItemDecoration(new SwappableItemDecoration(mRecyclerView.getContext(),
                    R.drawable.ic_purchased,
                    R.drawable.ic_ordered,
                    R.color.colorPrimary,
                    R.color.colorItemBackgroundLighterX2,
                    0,
                    10,
                    1f,
                    .85f,
                    R.string.swipe_purchase,
                    R.string.swipe_order));

            return adapter;
        }
    }

    private static DiffUtil.ItemCallback<IReleaseViewModelItem> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<IReleaseViewModelItem>() {
                // ComicsRelease details may have changed if reloaded from the database,
                // but ID is fixed.
                @Override
                public boolean areItemsTheSame(@NonNull IReleaseViewModelItem oldItem,
                                               @NonNull IReleaseViewModelItem newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull IReleaseViewModelItem oldItem,
                                                  @NonNull IReleaseViewModelItem newItem) {
                    return Objects.equals(oldItem, newItem);
                }
            };

    private static class SwappableItemDecoration extends RecyclerView.ItemDecoration {

        private final Rect mBounds = new Rect();
        private final Paint mLinePaint, mTextPaint;
        private final Drawable mDrawableLeft, mDrawableRight;
        private final int mDrawableLeftPadding;
        private final int mDrawableRightPadding;
        private final float mLineHeight;
        private final float mDrawableSpeed;
        private final int mLeftTextWidth, mRightTextWidth,
                mTextHeight;
        private final String mLeftText, mRightText;

        private final static int mBorderSize = 32;

        SwappableItemDecoration(@NonNull Context context,
                                @DrawableRes int drawableLeft,
                                @DrawableRes int drawableRight,
                                @ColorRes int drawableColor,
                                @ColorRes int lineColor,
                                int drawableLeftPadding,
                                int drawableRightPadding,
                                float lineHeight,
                                float drawableSpeed,
                                @StringRes int leftText,
                                @StringRes int rightText) {
            final int ciDrawableColor = context.getResources().getColor(drawableColor);

            mLinePaint = new Paint();
            mLinePaint.setStyle(Paint.Style.FILL);
            mLinePaint.setColor(context.getResources().getColor(lineColor));

            // uso mutate() in modo che le modifiche vengono apportate solo a questa istanza di drawable
            mDrawableLeft = context.getResources().getDrawable(drawableLeft).mutate();
            mDrawableLeft.setTint(ciDrawableColor);

            // uso mutate() in modo che le modifiche vengono apportate solo a questa istanza di drawable
            mDrawableRight = context.getResources().getDrawable(drawableRight).mutate();
            mDrawableRight.setTint(ciDrawableColor);

            mDrawableLeftPadding = drawableLeftPadding;
            mDrawableRightPadding = drawableRightPadding;

            mLineHeight = Math.max(0.2f, lineHeight);
            mDrawableSpeed = Math.min(1f, drawableSpeed);

            mLeftText = context.getString(leftText);
            mRightText = context.getString(rightText);

            mTextPaint = new Paint();
            mTextPaint.setColor(ciDrawableColor);
            mTextPaint.setFakeBoldText(true);
            mTextPaint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                    16, context.getResources().getDisplayMetrics()));
            mLeftTextWidth = (int) mTextPaint.measureText(mLeftText);
            mRightTextWidth = (int) mTextPaint.measureText(mRightText);
            mTextHeight = mTextPaint.getFontMetricsInt(null);
        }

        @Override
        public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            canvas.save();
            final int left;
            final int right;
            if (parent.getClipToPadding()) {
                left = parent.getPaddingLeft() + mBorderSize;
                right = parent.getWidth() - parent.getPaddingRight() - mBorderSize;
            } else {
                left = mBorderSize;
                right = parent.getWidth() - mBorderSize;
            }

            final int childCount = parent.getChildCount();
            for (int ii = 0; ii < childCount; ii++) {
                final View child = parent.getChildAt(ii);
                final int tx = Math.round(child.getTranslationX());

                // se non sto eseguendo uno swipe non disegno nulla
                if (tx == 0) continue;
                // anche in caso di header non disegno nulla
                if (parent.getChildItemId(child) >= ReleaseHeader.BASE_ID) continue;

                parent.getDecoratedBoundsWithMargins(child, mBounds);

                final int top = mBounds.top;
                final int bottom = mBounds.bottom;
                // il drawable deve essere grande 1/3 del child
                final int size = (bottom - top) / 3;
                // distanza dai bordi superiore e inferiore (centrato)
                final int py = (bottom - top) / 2 - size / 2;

                // dimensione della linea (calcolata come frazione della grandezza del drawable)
                final int lineSize = (int) (size * mLineHeight);
                // distanza della liena dai bordi (centrato)
                final int ly = (bottom - top) / 2 - lineSize / 2;

                // distanza del testo dai bordi (centrato)
                final float ty = (bottom - top) / 2f - mTextHeight / 2f + 10f;

                canvas.drawRoundRect(left,
                        top + ly,
                        right,
                        top + ly + lineSize,
                        50f, 50f,
                        mLinePaint);

                if (tx > 0) {
                    // il drawable segue la view durante lo swipe
                    final int start = Math.min(right - size - mDrawableLeftPadding,
                            Math.max(left + mDrawableLeftPadding, (int) (tx * mDrawableSpeed) - size + left));
                    // se ci sta disegno il testo
                    if (start - left > mLeftTextWidth + 20) {
                        canvas.drawText(mLeftText, start - mLeftTextWidth - 20, bottom - ty, mTextPaint);
                    }

                    mDrawableLeft.setBounds(start,
                            top + py,
                            start + size,
                            top + py + size);
                    mDrawableLeft.draw(canvas);
                } else {
                    // il drawable segue la view durante lo swipe
                    final int start = Math.max(left + mDrawableRightPadding,
                            Math.min(right - size - mDrawableRightPadding, mBounds.right + (int) (tx * mDrawableSpeed)));
                    // se ci sta disegno il testo
                    if (right - start - size > mRightTextWidth + 20) {
                        canvas.drawText(mRightText, start + size + 20, bottom - ty, mTextPaint);
                    }

                    mDrawableRight.setBounds(start,
                            top + py,
                            start + size,
                            top + py + size);
                    mDrawableRight.draw(canvas);
                }
            }
            canvas.restore();
        }
    }
}