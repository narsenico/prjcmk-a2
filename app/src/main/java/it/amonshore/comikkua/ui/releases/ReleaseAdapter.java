package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.ListPreloader;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.RequestManager;
import com.bumptech.glide.integration.recyclerview.RecyclerViewPreloader;
import com.bumptech.glide.util.FixedPreloadSizeProvider;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.res.ResourcesCompat;
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
import it.amonshore.comikkua.ui.ImageHelperKt;

public class ReleaseAdapter extends ListAdapter<IReleaseViewModelItem, AReleaseViewModelItemViewHolder> {

    private final boolean _useLite;
    private final RequestManager _requestManager;
    private SelectionTracker<Long> _selectionTracker;
    private IReleaseViewHolderCallback _releaseViewHolderCallback;

    private ReleaseAdapter(boolean useLite,
                           @Nullable RequestManager requestManager) {
        super(DIFF_CALLBACK);
        _useLite = useLite;
        _requestManager = requestManager;
    }

    @Override
    public void onBindViewHolder(@NonNull AReleaseViewModelItemViewHolder holder, int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item == null) {
            holder.clear();
            return;
        }

        if (item.getItemType() == ReleaseHeader.ITEM_TYPE) {
            holder.bind(item, false, null, null);
        } else {
            holder.bind(item, _selectionTracker.isSelected(item.getId()), _requestManager, _releaseViewHolderCallback);
        }
    }

    @NonNull
    @Override
    public AReleaseViewModelItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ReleaseHeader.ITEM_TYPE) {
            return ReleaseHeaderViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
        }

        if (_useLite) {
            return ReleaseLiteViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
        }

        return ReleaseViewHolder.create(LayoutInflater.from(parent.getContext()), parent);
    }

    public long getSelectionKey(int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item == null) {
            return RecyclerView.NO_ID;
        }

        return item.getId();
    }

    public int getPosition(long selectionKey) {
        long nn = SystemClock.elapsedRealtimeNanos();
        final int count = getItemCount();
        try {
            for (int ii = 0; ii < count; ii++) {
                final IReleaseViewModelItem item = getItem(ii);
                if (item == null) {
                    break;
                } else if (item.getId() == selectionKey) {
                    return ii;
                }
            }

            return RecyclerView.NO_POSITION;
        } finally {
            LogHelper.d("getPosition of key=%s in %s items in %sns",
                    selectionKey, count, SystemClock.elapsedRealtimeNanos() - nn);
        }
    }

    @Override
    public int getItemViewType(int position) {
        final IReleaseViewModelItem item = getItem(position);
        if (item == null) {
            return RecyclerView.INVALID_TYPE;
        }

        return item.getItemType();

    }

    public SelectionTracker<Long> getSelectionTracker() {
        return _selectionTracker;
    }

    public interface OnItemSelectedListener {

        void onSelectionChanged(@Nullable Iterator<Long> keys, int size);
    }

    public interface ReleaseCallback {

        void onReleaseClick(@NonNull ComicsRelease release);

        void onReleaseTogglePurchase(@NonNull ComicsRelease release);

        void onReleaseToggleOrder(@NonNull ComicsRelease release);

        void onReleaseMenuSelected(@NonNull ComicsRelease release);
    }

    @NonNull
    private static SelectionTracker<Long> createSelectionTracker(@NonNull RecyclerView recyclerView,
                                                                 @Nullable OnItemSelectedListener onItemSelectedListener) {
        final MyItemKeProvider itemKeyProvider = new MyItemKeProvider(recyclerView, ItemKeyProvider.SCOPE_MAPPED);
        final SelectionTracker.SelectionPredicate<Long> selectionPredicate = new SelectionTracker.SelectionPredicate<Long>() {
            @Override
            public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
                // escludo dalla selezione gli header e le multi
                final int pos = itemKeyProvider.getPosition(key);
                if (pos != RecyclerView.NO_POSITION) {
                    return recyclerView.getAdapter().getItemViewType(pos) == ComicsRelease.ITEM_TYPE;
                }

                return key < ReleaseHeader.BASE_ID;
            }

            @Override
            public boolean canSetStateAtPosition(int position, boolean nextState) {
                return true;
            }

            @Override
            public boolean canSelectMultiple() {
                return true;
            }
        };

        final SelectionTracker<Long> selectionTracker = new SelectionTracker.Builder<>(
                "release-selection",
                recyclerView,
                itemKeyProvider,
                new ReleaseItemDetailsLookup(recyclerView),
                StorageStrategy.createLongStorage())
                .withSelectionPredicate(selectionPredicate)
                .build();

        if (onItemSelectedListener != null) {
            selectionTracker.addObserver(new SelectionTracker.SelectionObserver<Long>() {
                @Override
                public void onSelectionChanged() {
                    if (selectionTracker.hasSelection()) {
                        final Selection<Long> selection = selectionTracker.getSelection();
                        onItemSelectedListener.onSelectionChanged(selection.iterator(), selection.size());
                    } else {
                        onItemSelectedListener.onSelectionChanged(null, 0);
                    }
                }

                @Override
                public void onSelectionRestored() {
                    LogHelper.d("fire selection changed onSelectionRestored");
                    if (selectionTracker.hasSelection()) {
                        final Selection<Long> selection = selectionTracker.getSelection();
                        onItemSelectedListener.onSelectionChanged(selection.iterator(), selection.size());
                    } else {
                        onItemSelectedListener.onSelectionChanged(null, 0);
                    }
                    super.onSelectionRestored();
                }
            });
        }

        return selectionTracker;
    }

    private static IReleaseViewHolderCallback createReleaseViewHolderCallback(@NonNull SelectionTracker<Long> selectionTracker,
                                                                              @Nullable ReleaseCallback releaseCallback) {
        if (releaseCallback == null) {
            return null;
        }

        return new IReleaseViewHolderCallback() {
            @Override
            public void onReleaseClick(@NonNull IReleaseViewModelItem item, int position) {
                // se capita che venga scatenato il click anche se è in corso una selezione devo skippare
                if (!selectionTracker.hasSelection()) {
                    if (item.getItemType() != ReleaseHeader.ITEM_TYPE) {
                        final ComicsRelease release = (ComicsRelease) item;
                        releaseCallback.onReleaseClick(release);
                    }
                }
            }

            @Override
            public void onReleaseMenuSelected(@NonNull IReleaseViewModelItem item, int position) {
                if (item.getItemType() != ReleaseHeader.ITEM_TYPE) {
                    final ComicsRelease release = (ComicsRelease) item;
                    releaseCallback.onReleaseMenuSelected(release);
                }
            }
        };
    }

    private static void setupSwipe(@NonNull RecyclerView recyclerView,
                                   @NonNull ReleaseAdapter releaseAdapter,
                                   @Nullable ReleaseCallback releaseCallback) {

        // senza callback non gestisco lo swipe, ne tantomeno l'ItemDecoration
        if (releaseCallback == null) {
            return;
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
                if (releaseAdapter.getSelectionTracker().hasSelection()) return 0;
                if (releaseAdapter.getItemViewType(viewHolder.getLayoutPosition()) != ComicsRelease.ITEM_TYPE)
                    return 0;
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final IReleaseViewModelItem item = releaseAdapter.getItem(viewHolder.getLayoutPosition());
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
        new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(recyclerView);

        recyclerView.addItemDecoration(new SwappableItemDecoration(recyclerView.getContext(),
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
    }

    private static void setupImagePreloader(@NonNull RecyclerView recyclerView,
                                            @NonNull ReleaseAdapter releaseAdapter,
                                            @Nullable RequestManager requestManager) {
        if (requestManager == null) {
            return;
        }

        final Context context = recyclerView.getContext();
        final int defaultSize = ImageHelperKt.getInstance(context).getDefaultSize();
        final FixedPreloadSizeProvider<IReleaseViewModelItem> sizeProvider =
                new FixedPreloadSizeProvider<>(defaultSize, defaultSize);
        final ReleasePreloadModelProvider modelProvider =
                new ReleasePreloadModelProvider(context, releaseAdapter, requestManager);
        final RecyclerViewPreloader<IReleaseViewModelItem> preloader =
                new RecyclerViewPreloader<>(requestManager, modelProvider, sizeProvider, 10);

        recyclerView.addOnScrollListener(preloader);
    }

    public static class Builder {
        private final RecyclerView mRecyclerView;
        private OnItemSelectedListener mOnItemSelectedListener;
        private boolean useLite;
        private ReleaseCallback releaseCallback;
        private RequestManager mRequestManager;

        public Builder(@NonNull RecyclerView recyclerView) {
            mRecyclerView = recyclerView;
        }

        public Builder withOnItemSelectedListener(OnItemSelectedListener listener) {
            mOnItemSelectedListener = listener;
            return this;
        }

        public Builder withReleaseCallback(@NonNull ReleaseCallback callback) {
            releaseCallback = callback;
            return this;
        }

        public Builder useLite() {
            useLite = true;
            return this;
        }

        Builder withGlide(RequestManager requestManager) {
            mRequestManager = requestManager;
            return this;
        }

        public ReleaseAdapter build() {
            final ReleaseAdapter adapter = new ReleaseAdapter(useLite, mRequestManager);
            mRecyclerView.setAdapter(adapter);

            final SelectionTracker<Long> selectionTracker = createSelectionTracker(mRecyclerView, mOnItemSelectedListener);
            final IReleaseViewHolderCallback releaseViewHolderCallback = createReleaseViewHolderCallback(selectionTracker, releaseCallback);
            adapter._selectionTracker = selectionTracker;
            adapter._releaseViewHolderCallback = releaseViewHolderCallback;

            setupSwipe(mRecyclerView, adapter, releaseCallback);
            setupImagePreloader(mRecyclerView, adapter, mRequestManager);

            return adapter;
        }
    }

    private static class MyItemKeProvider extends ItemKeyProvider<Long> {
        private final ReleaseAdapter mAdapter;

        public MyItemKeProvider(@NonNull RecyclerView recyclerView, int scope) {
            super(scope);
            mAdapter = (ReleaseAdapter) recyclerView.getAdapter();
        }

        @Nullable
        @Override
        public Long getKey(int position) {
            return mAdapter == null ? RecyclerView.NO_ID : mAdapter.getSelectionKey(position);
        }

        @Override
        public int getPosition(@NonNull Long key) {
            return mAdapter == null ? RecyclerView.NO_POSITION : mAdapter.getPosition(key);
        }
    }

    private static final DiffUtil.ItemCallback<IReleaseViewModelItem> DIFF_CALLBACK =
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
            mDrawableLeft = ResourcesCompat.getDrawable(context.getResources(), drawableLeft, null).mutate();
            mDrawableLeft.setTint(ciDrawableColor);

            // uso mutate() in modo che le modifiche vengono apportate solo a questa istanza di drawable
            mDrawableRight = ResourcesCompat.getDrawable(context.getResources(), drawableRight, null).mutate();
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

    private static class ReleasePreloadModelProvider implements ListPreloader.PreloadModelProvider<IReleaseViewModelItem> {

        private final ImageHelperKt mImageHelperKt;
        private final ReleaseAdapter mAdapter;
        private final RequestManager mRequestManager;

        ReleasePreloadModelProvider(@NonNull Context context,
                                    @NonNull ReleaseAdapter adapter,
                                    @NonNull RequestManager requestManager) {
            mImageHelperKt = ImageHelperKt.getInstance(context);
            mAdapter = adapter;
            mRequestManager = requestManager;
        }

        @NonNull
        @Override
        public List<IReleaseViewModelItem> getPreloadItems(int position) {
            if (position >= mAdapter.getItemCount()) {
                return Collections.emptyList();
            }

            final IReleaseViewModelItem item = mAdapter.getItem(position);
            if (item == null || item.getItemType() != ComicsRelease.ITEM_TYPE) {
                return Collections.emptyList();
            } else {
                return Collections.singletonList(item);
            }
        }

        @Nullable
        @Override
        public RequestBuilder<?> getPreloadRequestBuilder(@NonNull IReleaseViewModelItem item) {
            final ComicsRelease comicsRelease = (ComicsRelease) item;
            if (comicsRelease.comics.hasImage()) {
                return mRequestManager
                        .load(Uri.parse(comicsRelease.comics.image))
                        .listener(mImageHelperKt.getDrawableRequestListener())
                        .apply(mImageHelperKt.getSquareOptions());

            } else {
                return null;
            }
        }
    }
}
