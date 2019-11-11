package it.amonshore.comikkua.ui.releases;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
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

        void onReleaseSwipe(@NonNull ComicsRelease release);

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
                    // escludo dalla selezione gli header
                    .withSelectionPredicate(new SelectionTracker.SelectionPredicate<Long>() {
                        @Override
                        public boolean canSetStateForKey(@NonNull Long key, boolean nextState) {
                            // TODO: se sto eseguendo lo swipe dovrei prevenire la selezione
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
                    if (viewHolder instanceof ReleaseHeaderViewHolder) return 0;
                    return super.getSwipeDirs(recyclerView, viewHolder);
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    // TODO: potrei gestire "ordered" con uno swipe inverso

                    LogHelper.d("Release swiped");
                    final int position = viewHolder.getAdapterPosition();
//                    adapter.notifyItemChanged(position);
                    final IReleaseViewModelItem item = adapter.getItemAt(position);
                    if (item != null) {
                        final ComicsRelease release = (ComicsRelease) item;
                        releaseCallback.onReleaseSwipe(release);
                    }
                }
            };
            new ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(mRecyclerView);

            mRecyclerView.addItemDecoration(new MyItemDecoration(mRecyclerView.getContext(),
                    R.drawable.ic_purchased,
                    R.drawable.ic_ordered,
                    R.color.colorItemBackgroundLight));

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

    private static class MyItemDecoration extends RecyclerView.ItemDecoration {

        private Drawable mDecorationLeft, mDecorationRight;
        private final Rect mBounds = new Rect();
        private final RectF mFBounds = new RectF();
        private final Paint mPaint = new Paint();

        MyItemDecoration(@NonNull Context context,
                         @DrawableRes int drawableLeft,
                         @DrawableRes int drawableRight,
                         @ColorRes int tint) {
            mDecorationLeft = context.getResources().getDrawable(drawableLeft);
            mDecorationRight = context.getResources().getDrawable(drawableRight);

            final int tintColor = context.getResources().getColor(tint);
            mDecorationLeft.setTint(tintColor);
            mDecorationRight.setTint(tintColor);

            mPaint.setStyle(Paint.Style.FILL);
            mPaint.setColor(Color.WHITE);
        }

        @Override
        public void onDraw(@NonNull Canvas canvas, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            canvas.save();
            final int left;
            final int right;
            if (parent.getClipToPadding()) {
                left = parent.getPaddingLeft() + 32;
                right = parent.getWidth() - parent.getPaddingRight() - 32;
            } else {
                left = 32;
                right = parent.getWidth() - 32;
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
                // deve essere grande 1/3 del child
                final int size = (bottom - top) / 3;
                // distanza dai bordi superiore e inferiore (centrato)
                final int py = (bottom - top) / 2 - size / 2;

//                final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
//                LogHelper.d("margin %s,%s - %s,%s",
//                        params.leftMargin, params.topMargin, params.rightMargin, params.bottomMargin);


//                mBounds.top += child.getPaddingTop();
//                mBounds.left += child.getPaddingLeft();
//                mBounds.bottom -= child.getPaddingBottom() + child.getPaddingTop();
//                mBounds.right -= child.getPaddingRight() + child.getPaddingLeft();
//                mFBounds.set(mBounds);

                // disegno il drawable a sinistra se lo swipe è verso destra
                // disegno il drawable a destra se lo swipe è verso sinistra
                if (tx > 0) {
                    canvas.drawRect(mBounds, mPaint);
                    mDecorationLeft.setBounds(left,
                            top + py,
                            left + size,
                            top + py + size);
                    mDecorationLeft.draw(canvas);
                } else {
                    canvas.drawRect(mBounds, mPaint);
                    mDecorationRight.setBounds(right - size,
                            top + py,
                            right,
                            top + py + size);
                    mDecorationRight.draw(canvas);
                }
            }
            canvas.restore();
        }
    }
}
