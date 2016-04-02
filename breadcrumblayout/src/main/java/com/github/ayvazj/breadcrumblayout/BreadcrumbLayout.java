package com.github.ayvazj.breadcrumblayout;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.Resources;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v4.widget.TextViewCompat;
import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class BreadcrumbLayout extends HorizontalScrollView {

    private static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();
    private static final Interpolator ADD_RESIZE_INTERPOLATOR = FAST_OUT_SLOW_IN_INTERPOLATOR;
    private static final Interpolator ADD_SLIDEIN_INTERPOLATOR = FAST_OUT_SLOW_IN_INTERPOLATOR;

    private static final int ANIMATION_DURATION = 200;
    private static final long ADD_RESIZE_DURATION = ANIMATION_DURATION;
    private static final long ADD_SLIDEIN_DURATION = ANIMATION_DURATION;

    private static final long REMOVE_SIDEOUT_DURATION = ANIMATION_DURATION;
    private static final TimeInterpolator REMOVE_SLIDEOUT_INTERPOLATOR = FAST_OUT_SLOW_IN_INTERPOLATOR;
    private static final long REMOVE_RESIZE_DURATION = ANIMATION_DURATION;
    private static final TimeInterpolator REMOVE_RESIZE_INTERPOLATOR = FAST_OUT_SLOW_IN_INTERPOLATOR;

    private final SlidingCrumbStrip crumbStrip;

    List<Breadcrumb> crumbs;
    private Breadcrumb selectedCrumb;
    private OnBreadcrumbSelectedListener onBreadcrumbSelectedListener;
    private View.OnClickListener mCrumbClickListener;
    private int crumbMaxWidth = Integer.MAX_VALUE;
    private ValueAnimator mScrollAnimator;
    private boolean animating = false;

    private int crumbLayoutRes = R.layout.breadcrumb_item;
    private int textViewResourceId = R.id.breadcrumb_label;
    private int separatorViewResourceId = R.id.breadcrumb_separator;

    public static final int MODE_SCROLLABLE = 0;
    public static final int MODE_FIXED = 1;

    private int mode;


    /**
     * Callback interface invoked when a crumb's selection state changes.
     */
    public interface OnBreadcrumbSelectedListener {

        /**
         * Called when a crumb enters the selected state.
         *
         * @param crumb The crumb that was selected
         */
        void onBreadcrumbSelected(Breadcrumb crumb);

        /**
         * Called when a crumb exits the selected state.
         *
         * @param crumb The crumb that was unselected
         */
        void onBreadcrumbUnselected(Breadcrumb crumb);

        /**
         * Called when a crumb that is already selected is chosen again by the user. Some applications
         * may use this action to return to the top level of a category.
         *
         * @param crumb The crumb that was reselected.
         */
        void onBreadcrumbReselected(Breadcrumb crumb);
    }

    public BreadcrumbLayout(Context context) {
        this(context, null);
    }

    public BreadcrumbLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }


    public BreadcrumbLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        crumbs = new ArrayList<>();


        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);

        setFillViewport(false);
        setSmoothScrollingEnabled(true);

        // Add the CrumStrip
        crumbStrip = new SlidingCrumbStrip(context);
        crumbStrip.setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));


        FrameLayout.LayoutParams crumbStripLp = new FrameLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        crumbStripLp.gravity = Gravity.CENTER;

        addView(crumbStrip, crumbStripLp);

        updateCrumbViews(true);
    }

    // center the content
    // https://code.google.com/p/android/issues/detail?id=20088
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if (getChildCount() <= 0 || getChildAt(0) instanceof ViewGroup != true)
            return;
        final ViewGroup view = (ViewGroup) getChildAt(0);
        if (view.getMeasuredWidth() <= getMeasuredWidth()) {
            final LayoutParams lp = (LayoutParams) view.getLayoutParams();
            view.setLayoutParams(new LayoutParams(lp.width, lp.height, lp.gravity | Gravity.CENTER_HORIZONTAL));
            return;
        }

        final LayoutParams params = (LayoutParams) view.getLayoutParams();
        view.setLayoutParams(new LayoutParams(params.width, params.height, params.gravity & ~Gravity.CENTER_HORIZONTAL));
    }



    private void updateCrumbViews(final boolean requestLayout) {
        for (int i = 0; i < crumbStrip.getChildCount(); i++) {
            View child = crumbStrip.getChildAt(i);
            child.setMinimumWidth(getCrumbMinWidth());
            updateCrumbViewLayoutParams((FrameLayout.LayoutParams) child.getLayoutParams());
            if (requestLayout) {
                child.requestLayout();
            }
        }
    }

    private int getCrumbMinWidth() {
        return 0;
    }


    void selectCrumb(Breadcrumb crumb) {
        if (animating) return;
        if (selectedCrumb == crumb) {
            if (selectedCrumb != null) {
                if (onBreadcrumbSelectedListener != null) {
                    onBreadcrumbSelectedListener.onBreadcrumbReselected(selectedCrumb);
                }
                if (crumb.getPosition() < crumbs.size() - 1) {
                    removeCrumbs(crumb.getPosition() + 1, crumbs.size() - 1);
                }
                animateToCrumb(crumb.getPosition());
            }
        } else {
            if (selectedCrumb != null && onBreadcrumbSelectedListener != null) {
                onBreadcrumbSelectedListener.onBreadcrumbUnselected(selectedCrumb);
            }
            selectedCrumb = crumb;
            if (selectedCrumb != null && onBreadcrumbSelectedListener != null) {
                onBreadcrumbSelectedListener.onBreadcrumbSelected(selectedCrumb);
            }

            if (crumb.getPosition() < crumbs.size() - 1) {
                removeCrumbs(crumb.getPosition() + 1, crumbs.size() - 1);
            }
        }
    }

    /**
     * setBreadCrumbLayout
     *
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     */
    public void setBreadCrumbLayout(@LayoutRes int resource) {
        this.crumbLayoutRes = resource;
    }

    /**
     * setTextViewResourceId
     *
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     */
    public void setTextViewResourceId(@IdRes int textViewResourceId) {
        this.textViewResourceId = textViewResourceId;
    }

    /**
     * separatorViewResourceId
     *
     * @param separatorViewResourceId The id of the separator view within the layout resource to be populated
     */
    public void setSeparatorViewResourceId(@IdRes int separatorViewResourceId) {
        this.separatorViewResourceId = separatorViewResourceId;
    }

    /**
     * Add a crumb to this layout. The crumb will be added at the end of the list.
     * If this is the first crumb to be added it will become the selected crumb.
     *
     * @param crumb Breadcrumb to add
     */
    public void addCrumb(@NonNull Breadcrumb crumb) {
        addCrumb(crumb, crumbs.isEmpty());
    }

    /**
     * Add a crumb to this layout. The crumb will be added at the end of the list.
     *
     * @param crumb       crumb to add
     * @param setSelected True if the added crumb should become the selected crumb.
     */
    public void addCrumb(@NonNull Breadcrumb crumb, boolean setSelected) {
        addCrumb(crumb, crumbs.size(), setSelected);
    }

    /**
     * Add a crumb to this layout. The crumb will be inserted at <code>position</code>.
     *
     * @param crumb       The crumb to add
     * @param position    The new position of the crumb
     * @param setSelected True if the added crumb should become the selected crumb.
     */
    private void addCrumb(@NonNull Breadcrumb crumb, int position, boolean setSelected) {
        if (animating) return;
        if (crumb.parent != this) {
            throw new IllegalArgumentException("Breadcrumb belongs to a different BreadcrumbLayout.");
        }

        configureCrumb(crumb, position);
        addCrumbView(crumb, position, setSelected);

        if (setSelected) {
            crumb.select();
        }
    }

    private void configureCrumb(Breadcrumb crumb, int position) {
        crumb.setPosition(position);
        crumbs.add(position, crumb);

        final int count = crumbs.size();
        for (int i = position + 1; i < count; i++) {
            crumbs.get(i).setPosition(i);
        }
    }

    private void addCrumbView(Breadcrumb crumb, final int position, boolean setSelected) {
        final CrumbView crumbView = createCrumbView(crumb);
        int insertPos = 0;
        crumbStrip.addView(crumbView, insertPos, createLayoutParamsForCrumbs());

        if (position > 0) {
            Animator animator = addCrumbViewAnimated(crumbView);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    animateToCrumb(position);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }
        if (setSelected) {
            crumbView.setSelected(true);
        }

    }

    private Animator addCrumbViewAnimated(final CrumbView crumbView) {

        int widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        int heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

        crumbView.measure(widthMeasureSpec, heightMeasureSpec);
        int childCount = crumbStrip.getChildCount();

        AnimatorSet as = new AnimatorSet();

        if (childCount > 1) {
            final int crumbViewWidth = crumbView.getMeasuredWidth();
            final CrumbView prevCrumb = (CrumbView) crumbStrip.getChildAt(1);
            final int crumbStripPaddingEndBefore = crumbStrip.getPaddingEnd();
            final int crumbStripPaddingEndAfter = crumbStripPaddingEndBefore + crumbViewWidth;

            // initially the crumbView is behind previous crumb
            crumbView.setPadding(prevCrumb.getRight() - crumbViewWidth, 0, 0, 0);


            ValueAnimator containerAnimator = ValueAnimator.ofInt(0, crumbViewWidth);
            containerAnimator.setDuration(ADD_RESIZE_DURATION);
            containerAnimator.setInterpolator(ADD_RESIZE_INTERPOLATOR);
            containerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    crumbStrip.setPadding(0, 0, crumbStripPaddingEndBefore + (int) animation.getAnimatedValue(), 0);
                }
            });

            ValueAnimator va = ValueAnimator.ofInt(0, crumbViewWidth);
            va.setDuration(ADD_SLIDEIN_DURATION);
            va.setInterpolator(ADD_SLIDEIN_INTERPOLATOR);

            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    crumbView.setPadding(prevCrumb.getRight() - crumbViewWidth + (int) animation.getAnimatedValue(), 0, 0, 0);
                    crumbStrip.setPadding(0, 0, crumbStripPaddingEndAfter - (int) animation.getAnimatedValue(), 0);
                }
            });


            as.playSequentially(containerAnimator, va);
            as.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    animating = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    animating = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        return as;
    }

    private CrumbView createCrumbView(Breadcrumb crumb) {
        final CrumbView crumbView = new CrumbView(getContext());
        crumbView.crumb = crumb;
        crumbView.mTextView.setText(crumb.getText());
        crumbView.separatorView.setVisibility(crumb.getPosition() == 0 ? View.GONE : View.VISIBLE);
        crumbView.setLayoutParams(createLayoutParamsForCrumbs());
        crumbView.setFocusable(true);

        if (mCrumbClickListener == null) {
            mCrumbClickListener = new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CrumbView cv = (CrumbView) view;
                    cv.getCrumb().select();
                }
            };
        }
        crumbView.setOnClickListener(mCrumbClickListener);
        return crumbView;
    }

    private int dpToPx(int dps) {
        return Math.round(getResources().getDisplayMetrics().density * dps);
    }

    private FrameLayout.LayoutParams createLayoutParamsForCrumbs() {
        final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.MATCH_PARENT);
        updateCrumbViewLayoutParams(lp);
        return lp;
    }

    private void updateCrumbViewLayoutParams(FrameLayout.LayoutParams lp) {
        lp.width = LinearLayout.LayoutParams.WRAP_CONTENT;
    }

    private void updateCrumb(int position) {
        final CrumbView view = getCrumbView(position);
        if (view != null) {
            view.update();
        }
    }

    private CrumbView getCrumbView(int position) {
        return (CrumbView) crumbStrip.getChildAt(crumbStrip.getChildCount() - 1 - position);
    }

    /**
     * Set the {@link BreadcrumbLayout.OnBreadcrumbSelectedListener} that will
     * handle switching to and from bread crumb.
     *
     * @param onBreadcrumbSelectedListener Listener to handle crumb selection events
     */
    public void setOnBreadcrumbSelectedListener(OnBreadcrumbSelectedListener onBreadcrumbSelectedListener) {
        this.onBreadcrumbSelectedListener = onBreadcrumbSelectedListener;
    }

    /**
     * Create and return a new {@link Breadcrumb}. You need to manually add this using
     * {@link #addCrumb(Breadcrumb)} or a related method.
     *
     * @return A new Breadcrumb
     * @see #addCrumb(Breadcrumb)
     */
    @NonNull
    public Breadcrumb newCrumb() {
        return new Breadcrumb(this);
    }

    /**
     * Returns the number of crumbs currently registered with the action bar.
     *
     * @return Breadcrumb count
     */
    public int getCrumbCount() {
        return crumbs.size();
    }

    /**
     * Returns the crumb at the specified index.
     */
    @Nullable
    public Breadcrumb getCrumbAt(int index) {
        return crumbs.get(index);
    }

    /**
     * Returns the position of the current selected crumb.
     *
     * @return selected crumb position, or {@code -1} if there isn't a selected crumb.
     */
    public int getSelectedCrumbPosition() {
        return selectedCrumb != null ? selectedCrumb.getPosition() : -1;
    }

    /**
     * Remove a crumb from the layout. If the removed crumb was selected it will be deselected
     * and another crumb will be selected if present.
     *
     * @param crumb The crumb to remove
     */
    public void removeCrumb(Breadcrumb crumb) {
        if (animating) return;
        if (crumb.parent != this) {
            throw new IllegalArgumentException("Breadcrumb does not belong to this BreadcrumbLayout.");
        }

        removeCrumbAt(crumb.getPosition());
    }

    /**
     * Remove a crumb from the layout. If the removed crumb was selected it will be deselected
     * and another crumb will be selected if present.
     *
     * @param position Position of the crumb to remove
     */
    public void removeCrumbAt(final int position) {
        if (animating) return;
        final int selectedCrumbPosition = selectedCrumb != null ? selectedCrumb.getPosition() : 0;
        if (position > 0) {
            Animator animator = removeCrumbViewAnimated(position);
            animator.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    Breadcrumb removedCrumb = crumbs.remove(position);
                    if (removedCrumb != null) {
                        removedCrumb.setPosition(Breadcrumb.INVALID_POSITION);
                    }

                    final int newCrumbCount = crumbs.size();
                    for (int i = position; i < newCrumbCount; i++) {
                        crumbs.get(i).setPosition(i);
                    }

                    if (selectedCrumbPosition == position) {
                        selectCrumb(crumbs.isEmpty() ? null : crumbs.get(Math.max(0, position - 1)));
                    }
                    removeCrumbViewAt(position);
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            animator.start();
        }
    }


    public void removeCrumbs(final int startPos, final int endPos) {
        if (animating) return;
        final int selectedCrumbPosition = selectedCrumb != null ? selectedCrumb.getPosition() : 0;
        Animator animator = removeCrumbViewAnimated(startPos, endPos);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                for (int position = endPos; position >= startPos; position--) {
                    Breadcrumb removedCrumb = crumbs.remove(position);
                    if (removedCrumb != null) {
                        removedCrumb.setPosition(Breadcrumb.INVALID_POSITION);
                    }

                    final int newCrumbCount = crumbs.size();
                    for (int i = position; i < newCrumbCount; i++) {
                        crumbs.get(i).setPosition(i);
                    }

                    if (selectedCrumbPosition == position) {
                        selectCrumb(crumbs.isEmpty() ? null : crumbs.get(Math.max(0, position - 1)));
                    }
                    removeCrumbViewAt(position);
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }

    private Animator removeCrumbViewAnimated(int startPos, int endPos) {
        List<Animator> crumbAnimators = new ArrayList<>();
        List<Animator> resizeAnimators = new ArrayList<>();
        CrumbView prevCrumbView = (CrumbView) crumbStrip.getChildAt(getCrumbViewPosition(startPos - 1));
        final int translateValue = crumbStrip.getChildAt(getCrumbViewPosition(endPos)).getRight() - prevCrumbView.getRight();
        for (int i = startPos; i <= endPos; i++) {
            final CrumbView crumbView = (CrumbView) crumbStrip.getChildAt(getCrumbViewPosition(i));
            final int crumbViewWidth = crumbView.getMeasuredWidth() - crumbView.getPaddingStart();
            final int crumbViewPaddingStartBefore = crumbView.getPaddingStart();
            final int crumbStripPaddingEndBefore = crumbStrip.getPaddingEnd();
            final int crumbStripPaddingEndAfter = crumbStripPaddingEndBefore + translateValue;

            int childCount = crumbStrip.getChildCount();

            if (childCount > 1) {
                ValueAnimator va = ValueAnimator.ofInt(0, translateValue);
                va.setDuration(REMOVE_SIDEOUT_DURATION);
                va.setInterpolator(REMOVE_SLIDEOUT_INTERPOLATOR);
                va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        crumbView.setPadding(crumbViewPaddingStartBefore - (int) animation.getAnimatedValue(), 0, 0, 0);
                    }

                });
                crumbAnimators.add(va);

                if (i == endPos) {
                    ValueAnimator crumbStripSizeAnimator = ValueAnimator.ofInt(0, translateValue);
                    crumbStripSizeAnimator.setDuration(REMOVE_SIDEOUT_DURATION);
                    crumbStripSizeAnimator.setInterpolator(REMOVE_SLIDEOUT_INTERPOLATOR);
                    crumbStripSizeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            crumbStrip.setPadding(0, 0, crumbStripPaddingEndBefore + (int) animation.getAnimatedValue(), 0);
                        }
                    });
                    crumbAnimators.add(crumbStripSizeAnimator);

                    ValueAnimator containerAnimator = ValueAnimator.ofInt(0, translateValue);
                    containerAnimator.setDuration(REMOVE_RESIZE_DURATION);
                    containerAnimator.setInterpolator(REMOVE_RESIZE_INTERPOLATOR);
                    containerAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override
                        public void onAnimationUpdate(ValueAnimator animation) {
                            crumbStrip.setPadding(0, 0, crumbStripPaddingEndAfter - (int) animation.getAnimatedValue(), 0);
                        }

                    });
                    resizeAnimators.add(containerAnimator);
                }

            }
        }

        AnimatorSet together = new AnimatorSet();
        together.playTogether(crumbAnimators);

        AnimatorSet as = new AnimatorSet();
        resizeAnimators.add(0, together);
        as.playSequentially(resizeAnimators);
        as.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                animating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        return as;
    }

    /**
     * Remove all crumbs.
     */
    public void removeAllCrumbs() {
        // Remove all the views
        crumbStrip.removeAllViews();

        for (Iterator<Breadcrumb> i = crumbs.iterator(); i.hasNext(); ) {
            Breadcrumb crumb = i.next();
            crumb.setPosition(Breadcrumb.INVALID_POSITION);
            i.remove();
        }

        selectedCrumb = null;
    }

    private void removeCrumbViewAt(final int position) {
        crumbStrip.removeViewAt(getCrumbViewPosition(position));
    }

    private Animator removeCrumbViewAnimated(final int position) {
        return removeCrumbViewAnimated(position, position);
    }


    /**
     * Views are added in reverse order so that z-order is correct, so we have to translate them
     * when trying to reference them
     *
     * @param position
     * @return
     */
    private int getCrumbViewPosition(int position) {
        return crumbStrip.getChildCount() - 1 - position;
    }

    private void animateToCrumb(int newPosition) {
        if (newPosition == Breadcrumb.INVALID_POSITION) {
            return;
        }

        if (getWindowToken() == null || !ViewCompat.isLaidOut(this)
                || crumbStrip.childrenNeedLayout()) {
            // If we don't have a window token, or we haven't been laid out yet just draw the new
            // position now
            setScrollPosition(newPosition, 0f, true);
            return;
        }

        final int startScrollX = getScrollX();
        final int targetScrollX = calculateScrollXForCrumb(newPosition, 0);

        if (startScrollX != targetScrollX) {
            if (mScrollAnimator == null) {
                mScrollAnimator = new ValueAnimator();
                mScrollAnimator.setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR);
                mScrollAnimator.setDuration(ANIMATION_DURATION);
                mScrollAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        scrollTo((int) animation.getAnimatedValue(), 0);
                    }
                });
            }

            mScrollAnimator.setIntValues(startScrollX, targetScrollX);
            mScrollAnimator.start();
        }
    }

    public void setScrollPosition(int position, float positionOffset, boolean updateSelectedText) {
        if (position < 0 || position >= crumbStrip.getChildCount()) {
            return;
        }
        scrollTo(calculateScrollXForCrumb(position, positionOffset), 0);
    }

    private int calculateScrollXForCrumb(int position, float positionOffset) {

        final View selectedChild = crumbStrip.getChildAt(getCrumbViewPosition(position));
        final View nextChild = position + 1 < crumbStrip.getChildCount()
                ? crumbStrip.getChildAt(getCrumbViewPosition(position + 1))
                : null;
        final int selectedWidth = selectedChild != null ? (selectedChild.getWidth() - selectedChild.getPaddingStart()) : 0;
        final int nextWidth = nextChild != null ? (nextChild.getWidth() - nextChild.getPaddingStart()) : 0;

        return selectedChild.getPaddingStart()
//                + ((int) ((selectedWidth + nextWidth) * positionOffset * 0.5f))
//                + ((selectedChild.getWidth() - selectedChild.getPaddingStart()) / 2)
//                - (getWidth() / 2)
                ;
    }


    class CrumbView extends LinearLayout {
        Breadcrumb crumb;
        private TextView mTextView;
        private View separatorView;

        public CrumbView(Context context) {
            this(context, null);
        }

        public CrumbView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public CrumbView(Context context, AttributeSet attrs, int defStyleAttr) {
            super(context, attrs, defStyleAttr);
            LayoutInflater inflater = LayoutInflater.from(context);
            inflater.inflate(crumbLayoutRes, this, true);

            setBackgroundColor(ContextCompat.getColor(getContext(), android.R.color.white));

            mTextView = (TextView) findViewById(textViewResourceId);
            separatorView = findViewById(R.id.breadcrumb_separator);
            setWillNotDraw(false);
        }

        public Breadcrumb getCrumb() {
            return crumb;
        }

        private int getCrumbMaxWidth() {
            return crumbMaxWidth;
        }

        @Override
        public void onMeasure(final int origWidthMeasureSpec, final int origHeightMeasureSpec) {
            final int specWidthSize = MeasureSpec.getSize(origWidthMeasureSpec);
            final int specWidthMode = MeasureSpec.getMode(origWidthMeasureSpec);
            final int maxWidth = getCrumbMaxWidth();

            final int widthMeasureSpec;
            final int heightMeasureSpec = origHeightMeasureSpec;

            crumbMaxWidth = 256;

            if (maxWidth > 0 && (specWidthMode == MeasureSpec.UNSPECIFIED
                    || specWidthSize > maxWidth)) {
                // If we have a max width and a given spec which is either unspecified or
                // larger than the max width, update the width spec using the same mode
                widthMeasureSpec = MeasureSpec.makeMeasureSpec(crumbMaxWidth, specWidthMode);
            } else {
                // Else, use the original width spec
                widthMeasureSpec = origWidthMeasureSpec;
            }

            // Now lets measure
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            // We need to switch the text size based on whether the text is spanning 2 lines or not
            if (mTextView != null) {
                final Resources res = getResources();
                int crumbTextMultiLineSize = res.getDimensionPixelSize(R.dimen.crumb_text_size_2line);
                float textSize = 64f;
                float crumbTextSize = 64f;
                int maxLines = 1;

                if (mTextView != null && mTextView.getLineCount() > 1) {
                    // Otherwise when we have text which wraps we reduce the text size
                    textSize = crumbTextMultiLineSize;
                }

                final float curTextSize = mTextView.getTextSize();
                final int curLineCount = mTextView.getLineCount();
                final int curMaxLines = TextViewCompat.getMaxLines(mTextView);

                if (textSize != curTextSize || (curMaxLines >= 0 && maxLines != curMaxLines)) {
                    // We've got a new text size and/or max lines...
                    boolean updateTextView = true;

                    if (mode == MODE_FIXED && textSize > curTextSize && curLineCount == 1) {
                        // If we're in fixed mode, going up in text size and currently have 1 line
                        // then it's very easy to get into an infinite recursion.
                        // To combat that we check to see if the change in text size
                        // will cause a line count change. If so, abort the size change.
                        final Layout layout = mTextView.getLayout();
                        if (layout == null
                                || approximateLineWidth(layout, 0, textSize) > layout.getWidth()) {
                            updateTextView = false;
                        }
                    }

                    if (updateTextView) {
                        mTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
                        mTextView.setMaxLines(maxLines);
                        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
                    }
                }
            }
        }

        /**
         * Approximates a given lines width with the new provided text size.
         */
        private float approximateLineWidth(Layout layout, int line, float textSize) {
            return layout.getLineWidth(line) * (textSize / layout.getPaint().getTextSize());
        }

        final void update() {
            updateTextAndIcon(crumb, mTextView, separatorView);
        }

        private void updateTextAndIcon(Breadcrumb crumb, TextView textView, View separatorView) {
            final CharSequence text = crumb.getText();
            final boolean hasText = !TextUtils.isEmpty(text);
            if (textView != null) {
                if (hasText) {
                    textView.setText(text);
                    textView.setVisibility(VISIBLE);
                    setVisibility(VISIBLE);
                } else {
                    textView.setVisibility(GONE);
                    textView.setText(null);
                }
            }

            setOnLongClickListener(null);
            setLongClickable(false);
        }
    }

    public static final class Breadcrumb {
        /**
         * An invalid position for a Breadcrumb.
         *
         * @see #getPosition()
         */
        public static final int INVALID_POSITION = -1;

        private Object tag;
        private CharSequence text;
        private int position = INVALID_POSITION;
        private final BreadcrumbLayout parent;

        Breadcrumb(BreadcrumbLayout parent) {
            this.parent = parent;
        }

        /**
         * @return This crumb's tag object.
         */
        @Nullable
        public Object getTag() {
            return this.tag;
        }

        /**
         * Give this Breadcrumb an arbitrary object to hold for later use.
         *
         * @param tag Object to store
         * @return The current instance for call chaining
         */
        @NonNull
        public Breadcrumb setTag(@Nullable Object tag) {
            this.tag = tag;
            return this;
        }

        /**
         * Return the current position of this Breadcrumb.
         *
         * @return Current position, or {@link #INVALID_POSITION} if this Breadcrumb is not currently in
         * the layout.
         */
        public int getPosition() {
            return position;
        }

        void setPosition(int position) {
            this.position = position;
        }

        /**
         * Return the text of this Breadcrumb.
         *
         * @return The Breadcrumb's text
         */
        @Nullable
        public CharSequence getText() {
            return text;
        }

        /**
         * Set the text displayed on this Breadcrumb. Text may be truncated if there is not room to display
         * the entire string.
         *
         * @param text The text to display
         * @return The current instance for call chaining
         */
        @NonNull
        public Breadcrumb setText(@Nullable CharSequence text) {
            this.text = text;
            if (position >= 0) {
                parent.updateCrumb(position);
            }
            return this;
        }

        /**
         * Set the text displayed on this Breadcrumb. Text may be truncated if there is not room to display
         * the entire string.
         *
         * @param resId A resource ID referring to the text that should be displayed
         * @return The current instance for call chaining
         */
        @NonNull
        public Breadcrumb setText(@StringRes int resId) {
            return setText(parent.getResources().getText(resId));
        }

        /**
         * Select this Breadcrumb. Only valid if the Breadcrumb has been added to the layout.
         */
        public void select() {
            parent.selectCrumb(this);
        }

        /**
         * Returns true if this Breadcrumb is currently selected.
         */
        public boolean isSelected() {
            return parent.getSelectedCrumbPosition() == position;
        }
    }

    private class SlidingCrumbStrip extends FrameLayout {
        SlidingCrumbStrip(Context context) {
            super(context);
        }

        boolean childrenNeedLayout() {
            for (int i = 0, z = getChildCount(); i < z; i++) {
                final View child = getChildAt(i);
                if (child.getWidth() <= 0) {
                    return true;
                }
            }
            return false;
        }
    }
}
