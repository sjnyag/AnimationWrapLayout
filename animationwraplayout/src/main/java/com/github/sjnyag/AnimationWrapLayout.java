package com.github.sjnyag;

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
    private AddedViewContainer mAddedViewContainer;

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
        final int rowMaxWidth = View.resolveSize(getSuggestedMinimumWidth(), widthMeasureSpec);
        int height = View.resolveSize(getSuggestedMinimumHeight(), heightMeasureSpec);

        int childWidthSpec = MeasureSpec.makeMeasureSpec(rowMaxWidth, MeasureSpec.UNSPECIFIED);
        int childHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.UNSPECIFIED);

        int count = this.getChildCount();
        for (int i = 0; i < count; i++) {
            getChildAt(i).measure(childWidthSpec, childHeightSpec);
        }
        int totalHeight = mChildrenMeasure.init().rowMaxWidth(rowMaxWidth).measure().getTotalHeight();
        if (mAddedViewContainer != null && getMeasuredHeight() > totalHeight) {
            totalHeight = getMeasuredHeight();
        }
        this.setMeasuredDimension(rowMaxWidth, totalHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        SparseArray<Layout> layoutSet = mChildrenMeasure.init().ignoreAddedView().measure().getLayoutSet();
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
        if (mAddedViewContainer != null) {
            return false;
        }
        mAddedViewContainer = new AddedViewContainer(view, position);
        final float alpha = view.getAlpha();
        final List<View> animatedViewList = new ArrayList<>();
        view.measure(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        view.setAlpha(0.0f);
        SparseArray<Layout> layoutSet = mChildrenMeasure.init().measure().getLayoutSet();
        requestLayout();
        int count = this.getChildCount();
        if (count == 0) {
            addView(view, position);
            addAnimation(view, alpha, new AnimationCallback() {
                @Override
                public void onEnd() {
                    mAddedViewContainer = null;
                }
            });
            return true;
        }
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
                        mAddedViewContainer = null;
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
        SparseArray<Layout> layoutSet = mChildrenMeasure.init().without(view).measure().getLayoutSet();
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
        int mRowMaxWidth = getWidth();
        int mTop = 0;
        int mHeight = 0;
        int mCurrentRowWidth = 0;
        int mCurrentRowHeight = 0;
        boolean mIsIgnoreAddedView = false;
        View mRemoveView;
        SparseArray<Layout> mMeasuredLayoutSet = new SparseArray<>();

        ChildrenMeasure() {
        }

        ChildrenMeasure init() {
            mRowCount = 0;
            mColumnCount = 0;
            mTop = getPaddingTop();
            mHeight = getPaddingTop();
            mCurrentRowWidth = 0;
            mCurrentRowHeight = 0;
            mRowMaxWidth = getWidth();
            mIsIgnoreAddedView = false;
            mRemoveView = null;
            mMeasuredLayoutSet.clear();
            return this;
        }

        ChildrenMeasure rowMaxWidth(int rowMaxWidth) {
            this.mRowMaxWidth = rowMaxWidth;
            return this;
        }

        ChildrenMeasure ignoreAddedView() {
            this.mIsIgnoreAddedView = true;
            return this;
        }

        ChildrenMeasure without(View removeView) {
            this.mRemoveView = removeView;
            return this;
        }

        ChildrenMeasure measure() {
            int count = getChildCount();
            boolean hasAddedView = mAddedViewContainer != null && !mIsIgnoreAddedView;
            for (int i = 0; i < count; i++) {
                if (hasAddedView && i >= mAddedViewContainer.position) {
                    hasAddedView = false;
                    measureView(mAddedViewContainer.view).applyTo(mAddedViewContainer.view);
                    i--;
                } else {
                    View child = getChildAt(i);
                    if (shouldLayout(child) && child != mRemoveView) {
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
            return mHeight + getPaddingBottom();
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

            if (mRowMaxWidth < mCurrentRowWidth + childWidth + mEachMarginWidth + rightMargin + getPaddingRight() || (mColumnCount == 0 && mRowCount == 0)) {
                mRowCount++;
                mColumnCount = 1;
                if (mRowCount > 1) {
                    mTop = mHeight + mEachMarginHeight;
                }
                mCurrentRowHeight = 0;
                mCurrentRowWidth = getPaddingLeft();
            } else {
                mColumnCount++;
            }
            if (mHeight < mTop + childTotalHeight) {
                mHeight = mTop + childTotalHeight;
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
    }

    class AddedViewContainer {
        final View view;
        final int position;

        AddedViewContainer(View view, int position) {
            this.view = view;
            this.position = position;
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