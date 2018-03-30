package com.github.sjnyag.animationwraplayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;

import java.util.ArrayList;
import java.util.List;

public class AnimationWrapLayout extends ViewGroup {

    private int mEachMarginWidth;
    private int mEachMarginHeight;
    private ChildrenMeasure mChildrenMeasure = new ChildrenMeasure();

    interface AnimationCallback {
        void onEnd();
    }

    public AnimationWrapLayout(Context context) {
        super(context);
    }

    public AnimationWrapLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        readAttr(context, attrs);
    }

    public AnimationWrapLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        readAttr(context, attrs);
    }

    @Override
    public AnimationWrapLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new AnimationWrapLayout.LayoutParams(getContext(), attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = View.resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = View.resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        int childWidthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.UNSPECIFIED);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED);

        int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec);
        }

        height = mChildrenMeasure.measureByTotalWidth(width).getTotalHeight();
        if (mChildrenMeasure.hasPreLayoutView() && getMeasuredHeight() > height) {
            height = getMeasuredHeight();
        }
        this.setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        SparseArray<Layout> layoutSet = mChildrenMeasure.measureLayoutOnly().getLayoutSet();
        int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = this.getChildAt(i);
            if (!shouldLayout(child)) {
                continue;
            }
            Layout layout = layoutSet.valueAt(i);
            if (layout == null) {
                continue;
            }
            layout.applyTo(child);
        }
    }

    @Override
    protected AnimationWrapLayout.LayoutParams generateDefaultLayoutParams() {
        return new AnimationWrapLayout.LayoutParams(AnimationWrapLayout.LayoutParams.WRAP_CONTENT, AnimationWrapLayout.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof AnimationWrapLayout.LayoutParams;
    }

    @Override
    protected AnimationWrapLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new AnimationWrapLayout.LayoutParams(p);
    }

    synchronized public boolean addViewWithAnimation(final View view, final int position) {
        if (mChildrenMeasure.hasPreLayoutView()) {
            return false;
        }
        mChildrenMeasure.addViewStart(view, position);
        SparseArray<Layout> layoutSet = mChildrenMeasure.measure().getLayoutSet();

        final float alpha = view.getAlpha();
        view.setAlpha(0.0f);
        requestLayout();

        int count = this.getChildCount();
        if (count == 0) {
            addView(view, position);
            addAnimation(view, alpha, new AnimationCallback() {
                @Override
                public void onEnd() {
                    mChildrenMeasure.addedViewComplete();
                }
            });
            return true;
        }

        final List<View> animatedViewList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final View child = this.getChildAt(i);
            if (!shouldLayout(child)) {
                continue;
            }
            final Layout layout = layoutSet.valueAt(i);
            if (layout == null) {
                continue;
            }
            boolean isAnimated = translateAnimation(child, layout.l, layout.t, new AnimationCallback() {
                @Override
                public void onEnd() {
                    layout.applyTo(child);
                    child.setAnimation(null);
                    if (animatedViewList.contains(child)) {
                        animatedViewList.remove(child);
                    }
                    if (animatedViewList.isEmpty()) {
                        mChildrenMeasure.addedViewComplete();
                        addView(view, position);
                        addAnimation(view, alpha, new AnimationCallback() {
                            @Override
                            public void onEnd() {
                            }
                        });
                    }
                }
            });
            if (isAnimated) {
                animatedViewList.add(child);
            }
        }
        return true;
    }

    synchronized public boolean removeViewWithAnimation(final View view) {
        if (mChildrenMeasure.hasPreLayoutView()) {
            return false;
        }
        mChildrenMeasure.removeViewStart(view);
        SparseArray<Layout> layoutSet = mChildrenMeasure.measure().getLayoutSet();
        int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = this.getChildAt(i);
            if (!shouldLayout(child)) {
                continue;
            }
            final Layout layout = layoutSet.valueAt(i);
            if (layout == null) {
                continue;
            }
            translateAnimation(child, layout.l, layout.t, new AnimationCallback() {
                @Override
                public void onEnd() {
                    layout.applyTo(child);
                    child.setAnimation(null);
                }
            });
        }
        removeAnimation(view, new AnimationCallback() {
            @Override
            public void onEnd() {
                mChildrenMeasure.removeViewComplete();
                removeView(view);
            }
        });
        return true;
    }

    protected boolean addAnimation(final View view, final float alpha, final AnimationCallback callback) {
        view.animate()
                .alpha(alpha)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        callback.onEnd();
                    }
                });
        return true;
    }

    protected boolean removeAnimation(final View view, final AnimationCallback callback) {
        view.animate()
                .alpha(0.0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        callback.onEnd();
                    }
                });
        return true;
    }

    protected boolean translateAnimation(final View view, final int l, final int t, final AnimationCallback callback) {
        Animation animation = new TranslateAnimation(0.0f, l - view.getLeft(), 0.0f, t - view.getTop());
        animation.setDuration(500);
        animation.setRepeatCount(0);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                callback.onEnd();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.startAnimation(animation);
        return true;
    }

    private void readAttr(Context context, AttributeSet attrs) {
        TypedArray parameters = context.obtainStyledAttributes(attrs, R.styleable.animation_wrap_layout);
        mEachMarginWidth = parameters.getLayoutDimension(R.styleable.animation_wrap_layout_each_margin_width, 4);
        mEachMarginHeight = parameters.getLayoutDimension(R.styleable.animation_wrap_layout_each_margin_height, 4);
        parameters.recycle();
    }

    protected boolean shouldLayout(View view) {
        return view.getVisibility() != View.GONE;
    }

    class ChildrenMeasure {
        int mRowCount = 0;
        int mColumnCount = 0;
        int mTotalWidth = 0;
        int mTop = 0;
        int mTotalHeight = 0;
        int mCurrentRowWidth = 0;
        int mCurrentRowHeight = 0;
        boolean mIsLayoutOnly = false;
        SparseArray<Layout> mMeasuredLayoutSet = new SparseArray<>();
        AddedViewContainer mAddedViewContainer;
        RemovedViewContainer mRemovedViewContainer;

        ChildrenMeasure() {
        }

        ChildrenMeasure measure() {
            return measure(false, getWidth());
        }

        ChildrenMeasure measureLayoutOnly() {
            return measure(true, getWidth());
        }

        ChildrenMeasure measureByTotalWidth(int width) {
            return measure(false, width);
        }

        ChildrenMeasure measure(boolean isLayoutOnly, int width) {
            mRowCount = 0;
            mColumnCount = 0;
            mTotalWidth = width;
            mTop = getPaddingTop();
            mTotalHeight = getPaddingTop();
            mCurrentRowWidth = 0;
            mCurrentRowHeight = 0;
            mIsLayoutOnly = isLayoutOnly;
            mMeasuredLayoutSet.clear();
            int count = getChildCount();
            boolean hasAddedView = mAddedViewContainer != null && !mIsLayoutOnly;
            for (int i = 0; i < count; i++) {
                if (hasAddedView && i >= mAddedViewContainer.position) {
                    hasAddedView = false;
                    measureView(mAddedViewContainer.view).applyTo(mAddedViewContainer.view);
                    i--;
                } else {
                    View child = getChildAt(i);
                    if (shouldLayout(child) && (mRemovedViewContainer == null || child != mRemovedViewContainer.view)) {
                        mMeasuredLayoutSet.put(i, measureView(child));
                    } else {
                        mMeasuredLayoutSet.put(i, null);
                    }
                }
            }
            return this;
        }

        int getTotalHeight() {
            if (mRowCount == 0) {
                return 0;
            }
            return mTotalHeight + getPaddingBottom();
        }

        SparseArray<Layout> getLayoutSet() {
            return mMeasuredLayoutSet;
        }

        Layout measureView(View view) {
            AnimationWrapLayout.LayoutParams lp = (AnimationWrapLayout.LayoutParams) view.getLayoutParams();
            int rightMargin = lp == null ? 0 : lp.rightMargin;
            int leftMargin = lp == null ? 0 : lp.leftMargin;
            int topMargin = lp == null ? 0 : lp.topMargin;
            int bottomMargin = lp == null ? 0 : lp.bottomMargin;
            int childWidth = view.getMeasuredWidth();
            int childHeight = view.getMeasuredHeight();
            int childTotalWidth = childWidth + rightMargin + leftMargin;
            int childTotalHeight = childHeight + topMargin + bottomMargin;

            if (mTotalWidth < mCurrentRowWidth + childWidth + mEachMarginWidth + rightMargin + getPaddingRight() || (mColumnCount == 0 && mRowCount == 0)) {
                mRowCount++;
                mColumnCount = 1;
                if (mRowCount > 1) {
                    mTop = mTotalHeight + mEachMarginHeight;
                }
                mCurrentRowHeight = 0;
                mCurrentRowWidth = getPaddingLeft();
            } else {
                mColumnCount++;
            }
            if (mTotalHeight < mTop + childTotalHeight) {
                mTotalHeight = mTop + childTotalHeight;
            }
            if (mColumnCount > 1) {
                mCurrentRowWidth += mEachMarginWidth;
            }
            int l = mCurrentRowWidth + leftMargin;
            int t = mTop + topMargin;
            int r = mCurrentRowWidth + childWidth + rightMargin;
            int b = mTop + childHeight + bottomMargin;
            mCurrentRowHeight = mCurrentRowHeight < childTotalHeight ? childTotalHeight : mCurrentRowHeight;
            mCurrentRowWidth += childTotalWidth;
            return new Layout(l, t, r, b);
        }

        boolean hasPreLayoutView() {
            return mAddedViewContainer != null || mRemovedViewContainer != null;
        }

        void addViewStart(View view, int position) {
            view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            mAddedViewContainer = new AddedViewContainer(view, position);
        }

        void addedViewComplete() {
            mAddedViewContainer = null;
        }

        void removeViewStart(View view) {
            mRemovedViewContainer = new RemovedViewContainer(view);
        }

        void removeViewComplete() {
            mRemovedViewContainer = null;
        }

        class AddedViewContainer {
            final View view;
            final int position;

            AddedViewContainer(View view, int position) {
                this.view = view;
                this.position = position;
            }
        }

        class RemovedViewContainer {
            final View view;

            RemovedViewContainer(View view) {
                this.view = view;
            }
        }
    }

    class Layout {
        final int l, t, r, b;

        Layout(int l, int t, int r, int b) {
            this.l = l;
            this.t = t;
            this.r = r;
            this.b = b;
        }

        void applyTo(View view) {
            view.layout(this.l, this.t, this.r, this.b);
        }
    }

    class LayoutParams extends MarginLayoutParams {

        LayoutParams(Context context, AttributeSet attr) {
            super(context, attr);
        }

        LayoutParams(int width, int height) {
            super(width, height);
        }

        LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}