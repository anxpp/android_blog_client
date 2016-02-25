package com.anxpp.blog.stickylistheaders;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SectionIndexer;

import com.anxpp.blog.R;

import com.anxpp.blog.stickylistheaders.WrapperViewList.LifeCycleListener;

/**
 * Even though this is a FrameLayout subclass we still consider it a ListView.
 * This is because of 2 reasons:
 *   1. It acts like as ListView.
 *   2. It used to be a ListView subclass and refactoring the name would cause compatibility errors.
 *
 * @author Emil Sj枚lander
 */
public class StickyListHeadersListView extends FrameLayout {

    public interface OnHeaderClickListener {
        void onHeaderClick(StickyListHeadersListView l, View header, int itemPosition, long headerId, boolean currentlySticky);
    }

    /**
     * 偏移量改变时的监听器
     */
    public interface OnStickyHeaderOffsetChangedListener {
        /**
         * @param l      父布局
         * @param header 当前的Header会被偏移
         *               Header没有保证它的测量设置，但视图是保证测量了的
         *               因此，你应该使用getMeasured*方法代替get*确定视图的尺寸。
         * @param offset 偏移量，从顶部偏移的
         */
        void onStickyHeaderOffsetChanged(StickyListHeadersListView l, View header, int offset);
    }

    /**
     * Header更新时的监听器
     */
    public interface OnStickyHeaderChangedListener {
        /**
         * @param l             父布局
         * @param header        新的header.
         * @param itemPosition  The position of the item within the adapter's data set of
         *                      the item whose header is now sticky.
         * @param headerId      id.
         */
        void onStickyHeaderChanged(StickyListHeadersListView l, View header, int itemPosition, long headerId);

    }

    /* 子视图 */
    //带标题的列表
    private WrapperViewList wrapperViewList;
    //头布局
    private View mHeader;

    /* --- Header 的一些参数 --- */
    //id
    private Long mHeaderId;
    // used to not have to call getHeaderId() all the time
    //位置
    private Integer mHeaderPosition;
    //偏移量
    private Integer mHeaderOffset;

    /* --- 成员 --- */
    private OnScrollListener mOnScrollListenerDelegate;
    private AdapterWrapper mAdapter;

    /* --- 设置 --- */
    //是否固定到顶部
    private boolean mAreHeadersSticky = true;
    private boolean mClippingToPadding = true;
    private boolean mIsDrawingListUnderStickyHeader = true;
    private int mStickyHeaderTopOffset = 0;
    private int mPaddingLeft = 0;
    private int mPaddingTop = 0;
    private int mPaddingRight = 0;
    private int mPaddingBottom = 0;

    /* --- 触摸处理 --- */
    private float mDownY;
    private boolean mHeaderOwnsTouch;
    //滑动距离
    private float mTouchSlop;

    /* --- 其他 --- */
    private OnHeaderClickListener mOnHeaderClickListener;
    private OnStickyHeaderOffsetChangedListener mOnStickyHeaderOffsetChangedListener;
    private OnStickyHeaderChangedListener mOnStickyHeaderChangedListener;
    private AdapterWrapperDataSetObserver mDataSetObserver;
    private Drawable mDivider;
    private int mDividerHeight;

    public StickyListHeadersListView(Context context) {
        this(context, null);
    }

    public StickyListHeadersListView(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.stickyListHeadersListViewStyle);
    }

    public StickyListHeadersListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // getScaledTouchSlop是一个距离，表示滑动的时候，手的移动要大于这个距离才开始移动控件。
        // 如果小于这个距离就不触发移动控件，如viewpager就是用这个距离来判断用户是否翻页
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();

        // 初始化封装的列表视图
        wrapperViewList = new WrapperViewList(context);

        // null out divider, dividers are handled by adapter so they look good with headers
        mDivider = wrapperViewList.getDivider();
        mDividerHeight = wrapperViewList.getDividerHeight();
        wrapperViewList.setDivider(null);
        wrapperViewList.setDividerHeight(0);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(attrs,R.styleable.StickyListHeadersListView, defStyle, 0);

            try {
                // -- 配置的视图属性 --
                int padding = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_padding, 0);
                mPaddingLeft = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingLeft, padding);
                mPaddingTop = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingTop, padding);
                mPaddingRight = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingRight, padding);
                mPaddingBottom = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_paddingBottom, padding);

                setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);

                // Set clip to padding on the list and reset value to default on
                // wrapper
                mClippingToPadding = a.getBoolean(R.styleable.StickyListHeadersListView_android_clipToPadding, true);
                super.setClipToPadding(true);
                wrapperViewList.setClipToPadding(mClippingToPadding);

                // 滚动条
                final int scrollBars = a.getInt(R.styleable.StickyListHeadersListView_android_scrollbars, 0x00000200);
                wrapperViewList.setVerticalScrollBarEnabled((scrollBars & 0x00000200) != 0);
                wrapperViewList.setHorizontalScrollBarEnabled((scrollBars & 0x00000100) != 0);

                // overscroll
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                    wrapperViewList.setOverScrollMode(a.getInt(R.styleable.StickyListHeadersListView_android_overScrollMode, 0));
                }

                // -- ListView的属性 --
                wrapperViewList.setFadingEdgeLength(a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_fadingEdgeLength,
                        wrapperViewList.getVerticalFadingEdgeLength()));
                final int fadingEdge = a.getInt(R.styleable.StickyListHeadersListView_android_requiresFadingEdge, 0);
                if (fadingEdge == 0x00001000) {
                    wrapperViewList.setVerticalFadingEdgeEnabled(false);
                    wrapperViewList.setHorizontalFadingEdgeEnabled(true);
                } else if (fadingEdge == 0x00002000) {
                    wrapperViewList.setVerticalFadingEdgeEnabled(true);
                    wrapperViewList.setHorizontalFadingEdgeEnabled(false);
                } else {
                    wrapperViewList.setVerticalFadingEdgeEnabled(false);
                    wrapperViewList.setHorizontalFadingEdgeEnabled(false);
                }
                wrapperViewList.setCacheColorHint(a
                        .getColor(R.styleable.StickyListHeadersListView_android_cacheColorHint, wrapperViewList.getCacheColorHint()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    wrapperViewList.setChoiceMode(a.getInt(R.styleable.StickyListHeadersListView_android_choiceMode,
                            wrapperViewList.getChoiceMode()));
                }
                wrapperViewList.setDrawSelectorOnTop(a.getBoolean(R.styleable.StickyListHeadersListView_android_drawSelectorOnTop, false));
                wrapperViewList.setFastScrollEnabled(a.getBoolean(R.styleable.StickyListHeadersListView_android_fastScrollEnabled,
                        wrapperViewList.isFastScrollEnabled()));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                    wrapperViewList.setFastScrollAlwaysVisible(a.getBoolean(
                            R.styleable.StickyListHeadersListView_android_fastScrollAlwaysVisible,
                            wrapperViewList.isFastScrollAlwaysVisible()));
                }

                //设置滚动条无边框
//                wrapperViewList.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
//                wrapperViewList.setSmoothScrollbarEnabled(false);

                if (a.hasValue(R.styleable.StickyListHeadersListView_android_listSelector)) {
                    wrapperViewList.setSelector(a.getDrawable(R.styleable.StickyListHeadersListView_android_listSelector));
                }

                wrapperViewList.setScrollingCacheEnabled(a.getBoolean(R.styleable.StickyListHeadersListView_android_scrollingCache,
                        wrapperViewList.isScrollingCacheEnabled()));

                if (a.hasValue(R.styleable.StickyListHeadersListView_android_divider)) {
                    mDivider = a.getDrawable(R.styleable.StickyListHeadersListView_android_divider);
                }
                
                wrapperViewList.setStackFromBottom(a.getBoolean(R.styleable.StickyListHeadersListView_android_stackFromBottom, false));

                mDividerHeight = a.getDimensionPixelSize(R.styleable.StickyListHeadersListView_android_dividerHeight,
                        mDividerHeight);

                wrapperViewList.setTranscriptMode(a.getInt(R.styleable.StickyListHeadersListView_android_transcriptMode,
                        ListView.TRANSCRIPT_MODE_DISABLED));

                // -- StickyListHeaders attributes --
                mAreHeadersSticky = a.getBoolean(R.styleable.StickyListHeadersListView_hasStickyHeaders, true);
                mIsDrawingListUnderStickyHeader = a.getBoolean(R.styleable.StickyListHeadersListView_isDrawingListUnderStickyHeader, true);
            } finally {
                a.recycle();
            }
        }

        // attach some listeners to the wrapped list
        wrapperViewList.setLifeCycleListener(new WrapperViewListLifeCycleListener());
        wrapperViewList.setOnScrollListener(new WrapperListScrollListener());

        addView(wrapperViewList);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureHeader(mHeader);
    }

    private void ensureHeaderHasCorrectLayoutParams(View header) {
        ViewGroup.LayoutParams lp = header.getLayoutParams();
        if (lp == null) {
            lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            header.setLayoutParams(lp);
        } else if (lp.height == LayoutParams.MATCH_PARENT || lp.width == LayoutParams.WRAP_CONTENT) {
            lp.height = LayoutParams.WRAP_CONTENT;
            lp.width = LayoutParams.MATCH_PARENT;
            header.setLayoutParams(lp);
        }
    }

    private void measureHeader(View header) {
        if (header != null) {
            final int width = getMeasuredWidth() - mPaddingLeft - mPaddingRight;
            final int parentWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                    width, MeasureSpec.EXACTLY);
            final int parentHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0,
                    MeasureSpec.UNSPECIFIED);
            measureChild(header, parentWidthMeasureSpec,
                    parentHeightMeasureSpec);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        wrapperViewList.layout(0, 0, wrapperViewList.getMeasuredWidth(), getHeight());
        if (mHeader != null) {
            MarginLayoutParams lp = (MarginLayoutParams) mHeader.getLayoutParams();
            int headerTop = lp.topMargin;
            mHeader.layout(mPaddingLeft, headerTop, mHeader.getMeasuredWidth()
                    + mPaddingLeft, headerTop + mHeader.getMeasuredHeight());
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        // Only draw the list here.
        // The header should be drawn right after the lists children are drawn.
        // This is done so that the header is above the list items
        // but below the list decorators (scroll bars etc).
        if (wrapperViewList.getVisibility() == VISIBLE || wrapperViewList.getAnimation() != null) {
            drawChild(canvas, wrapperViewList, 0);
        }
    }

    // Reset values tied the header. also remove header form layout
    // This is called in response to the data set or the adapter changing
    private void clearHeader() {
        if (mHeader != null) {
            removeView(mHeader);
            mHeader = null;
            mHeaderId = null;
            mHeaderPosition = null;
            mHeaderOffset = null;

            // reset the top clipping length
            wrapperViewList.setTopClippingLength(0);
            updateHeaderVisibilities();
        }
    }

    private void updateOrClearHeader(int firstVisiblePosition) {
        final int adapterCount = mAdapter == null ? 0 : mAdapter.getCount();
        if (adapterCount == 0 || !mAreHeadersSticky) {
            return;
        }

        final int headerViewCount = wrapperViewList.getHeaderViewsCount();
        int headerPosition = firstVisiblePosition - headerViewCount;
        if (wrapperViewList.getChildCount() > 0) {
            View firstItem = wrapperViewList.getChildAt(0);
            if (firstItem.getBottom() < stickyHeaderTop()) {
                headerPosition++;
            }
        }

        // It is not a mistake to call getFirstVisiblePosition() here.
        // Most of the time getFixedFirstVisibleItem() should be called
        // but that does not work great together with getChildAt()
        final boolean doesListHaveChildren = wrapperViewList.getChildCount() != 0;
        final boolean isFirstViewBelowTop = doesListHaveChildren
                && wrapperViewList.getFirstVisiblePosition() == 0
                && wrapperViewList.getChildAt(0).getTop() >= stickyHeaderTop();
        final boolean isHeaderPositionOutsideAdapterRange = headerPosition > adapterCount - 1
                || headerPosition < 0;
        if (!doesListHaveChildren || isHeaderPositionOutsideAdapterRange || isFirstViewBelowTop) {
            clearHeader();
            return;
        }

        updateHeader(headerPosition);
    }

    private void updateHeader(int headerPosition) {

        // check if there is a new header should be sticky
        if (mHeaderPosition == null || mHeaderPosition != headerPosition) {
            mHeaderPosition = headerPosition;
            final long headerId = mAdapter.getHeaderId(headerPosition);
            if (mHeaderId == null || mHeaderId != headerId) {
                mHeaderId = headerId;
                final View header = mAdapter.getHeaderView(mHeaderPosition, mHeader, this);
                if (mHeader != header) {
                    if (header == null) {
                        throw new NullPointerException("header may not be null");
                    }
                    swapHeader(header);
                }
                ensureHeaderHasCorrectLayoutParams(mHeader);
                measureHeader(mHeader);
                if(mOnStickyHeaderChangedListener != null) {
                    mOnStickyHeaderChangedListener.onStickyHeaderChanged(this, mHeader, headerPosition, mHeaderId);
                }
                // Reset mHeaderOffset to null ensuring
                // that it will be set on the header and
                // not skipped for performance reasons.
                mHeaderOffset = null;
            }
        }

        int headerOffset = stickyHeaderTop();

        // Calculate new header offset
        // Skip looking at the first view. it never matters because it always
        // results in a headerOffset = 0
        for (int i = 0; i < wrapperViewList.getChildCount(); i++) {
            final View child = wrapperViewList.getChildAt(i);
            final boolean doesChildHaveHeader = child instanceof WrapperView && ((WrapperView) child).hasHeader();
            final boolean isChildFooter = wrapperViewList.containsFooterView(child);
            if (child.getTop() >= stickyHeaderTop() && (doesChildHaveHeader || isChildFooter)) {
                headerOffset = Math.min(child.getTop() - mHeader.getMeasuredHeight(), headerOffset);
                break;
            }
        }

        setHeaderOffet(headerOffset);

        if (!mIsDrawingListUnderStickyHeader) {
            wrapperViewList.setTopClippingLength(mHeader.getMeasuredHeight()
                    + mHeaderOffset);
        }

        updateHeaderVisibilities();
    }

    private void swapHeader(View newHeader) {
        if (mHeader != null) {
            removeView(mHeader);
        }
        mHeader = newHeader;
        //设置透明度
        mHeader.setAlpha(0.8f);
        addView(mHeader);
        if (mOnHeaderClickListener != null) {
            mHeader.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnHeaderClickListener.onHeaderClick(
                            StickyListHeadersListView.this, mHeader,
                            mHeaderPosition, mHeaderId, true);
                }
            });
        }
        mHeader.setClickable(true);
    }

    // hides the headers in the list under the sticky header.
    // Makes sure the other ones are showing
    private void updateHeaderVisibilities() {
        int top = stickyHeaderTop();
        int childCount = wrapperViewList.getChildCount();
        for (int i = 0; i < childCount; i++) {

            // ensure child is a wrapper view
            View child = wrapperViewList.getChildAt(i);
            if (!(child instanceof WrapperView)) {
                continue;
            }

            // ensure wrapper view child has a header
            WrapperView wrapperViewChild = (WrapperView) child;
            if (!wrapperViewChild.hasHeader()) {
                continue;
            }

            // update header views visibility
            View childHeader = wrapperViewChild.mHeader;
            if (wrapperViewChild.getTop() < top) {
                if (childHeader.getVisibility() != View.INVISIBLE) {
                    childHeader.setVisibility(View.INVISIBLE);
                }
            } else {
                if (childHeader.getVisibility() != View.VISIBLE) {
                    childHeader.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    // Wrapper around setting the header offset in different ways depending on
    // the API version
    @SuppressLint("NewApi")
    private void setHeaderOffet(int offset) {
        if (mHeaderOffset == null || mHeaderOffset != offset) {
            mHeaderOffset = offset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                mHeader.setTranslationY(mHeaderOffset);
            } else {
                MarginLayoutParams params = (MarginLayoutParams) mHeader.getLayoutParams();
                params.topMargin = mHeaderOffset;
                mHeader.setLayoutParams(params);
            }
            if (mOnStickyHeaderOffsetChangedListener != null) {
                mOnStickyHeaderOffsetChangedListener.onStickyHeaderOffsetChanged(this, mHeader, -mHeaderOffset);
            }
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction() & MotionEvent.ACTION_MASK;
        if (action == MotionEvent.ACTION_DOWN) {
            mDownY = ev.getY();
            mHeaderOwnsTouch = mHeader != null && mDownY <= mHeader.getHeight() + mHeaderOffset;
        }

        boolean handled;
        if (mHeaderOwnsTouch) {
            if (mHeader != null && Math.abs(mDownY - ev.getY()) <= mTouchSlop) {
                handled = mHeader.dispatchTouchEvent(ev);
            } else {
                if (mHeader != null) {
                    MotionEvent cancelEvent = MotionEvent.obtain(ev);
                    cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
                    mHeader.dispatchTouchEvent(cancelEvent);
                    cancelEvent.recycle();
                }

                MotionEvent downEvent = MotionEvent.obtain(ev.getDownTime(), ev.getEventTime(), ev.getAction(), ev.getX(), mDownY, ev.getMetaState());
                downEvent.setAction(MotionEvent.ACTION_DOWN);
                handled = wrapperViewList.dispatchTouchEvent(downEvent);
                downEvent.recycle();
                mHeaderOwnsTouch = false;
            }
        } else {
            handled = wrapperViewList.dispatchTouchEvent(ev);
        }

        return handled;
    }

    private class AdapterWrapperDataSetObserver extends DataSetObserver {

        @Override
        public void onChanged() {
            clearHeader();
        }

        @Override
        public void onInvalidated() {
            clearHeader();
        }

    }

    //自定义滚动条
    private class WrapperListScrollListener implements OnScrollListener {
        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (mOnScrollListenerDelegate != null) {
                mOnScrollListenerDelegate.onScroll(view, firstVisibleItem,
                        visibleItemCount, totalItemCount);
            }
            updateOrClearHeader(wrapperViewList.getFixedFirstVisibleItem());
        }
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (mOnScrollListenerDelegate != null) {
                mOnScrollListenerDelegate.onScrollStateChanged(view, scrollState);
            }
        }
    }
    //生命周期监听
    private class WrapperViewListLifeCycleListener implements LifeCycleListener {
        @Override
        public void onDispatchDrawOccurred(Canvas canvas) {
            // onScroll is not called often at all before froyo
            // therefor we need to update the header here as well.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
                updateOrClearHeader(wrapperViewList.getFixedFirstVisibleItem());
            }
            if (mHeader != null) {
                if (mClippingToPadding) {
                    canvas.save();
                    canvas.clipRect(0, mPaddingTop, getRight(), getBottom());
                    drawChild(canvas, mHeader, 0);
                    canvas.restore();
                } else {
                    drawChild(canvas, mHeader, 0);
                }
            }
        }
    }

    private class AdapterWrapperHeaderClickHandler implements AdapterWrapper.OnHeaderClickListener {
        @Override
        public void onHeaderClick(View header, int itemPosition, long headerId) {
            mOnHeaderClickListener.onHeaderClick(
                    StickyListHeadersListView.this, header, itemPosition,
                    headerId, false);
        }
    }

    private boolean isStartOfSection(int position) {
        return position == 0 || mAdapter.getHeaderId(position) != mAdapter.getHeaderId(position - 1);
    }

    public int getHeaderOverlap(int position) {
        boolean isStartOfSection = isStartOfSection(Math.max(0, position - getHeaderViewsCount()));
        if (!isStartOfSection) {
            View header = mAdapter.getHeaderView(position, null, wrapperViewList);
            if (header == null) {
                throw new NullPointerException("header may not be null");
            }
            ensureHeaderHasCorrectLayoutParams(header);
            measureHeader(header);
            return header.getMeasuredHeight();
        }
        return 0;
    }

    private int stickyHeaderTop() {
        return mStickyHeaderTopOffset + (mClippingToPadding ? mPaddingTop : 0);
    }

    /* ---------- StickyListHeaders specific API ---------- */
    //设置ListView标题是否显示
    public void setAreHeadersSticky(boolean areHeadersSticky) {
        mAreHeadersSticky = areHeadersSticky;
        if (!areHeadersSticky) {
            clearHeader();
        } else {
            updateOrClearHeader(wrapperViewList.getFixedFirstVisibleItem());
        }
        // invalidating the list will trigger dispatchDraw()
        wrapperViewList.invalidate();
    }

//    //标题是否固定到顶端
//    public boolean isHeadersSticky() {
//        return mAreHeadersSticky;
//    }

    /**
     * @param stickyHeaderTopOffset 设置偏移量
     */
    public void setStickyHeaderTopOffset(int stickyHeaderTopOffset) {
        mStickyHeaderTopOffset = stickyHeaderTopOffset;
        updateOrClearHeader(wrapperViewList.getFixedFirstVisibleItem());
    }

//    public int getStickyHeaderTopOffset() {
//        return mStickyHeaderTopOffset;
//    }

    public void setDrawingListUnderStickyHeader(boolean drawingListUnderStickyHeader) {
        mIsDrawingListUnderStickyHeader = drawingListUnderStickyHeader;
        // reset the top clipping length
        wrapperViewList.setTopClippingLength(0);
    }

//    public boolean isDrawingListUnderStickyHeader() {
//        return mIsDrawingListUnderStickyHeader;
//    }

    public void setOnHeaderClickListener(OnHeaderClickListener listener) {
        mOnHeaderClickListener = listener;
        if (mAdapter != null) {
            if (mOnHeaderClickListener != null) {
                mAdapter.setOnHeaderClickListener(new AdapterWrapperHeaderClickHandler());
                if (mHeader != null) {
                    mHeader.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mOnHeaderClickListener.onHeaderClick(StickyListHeadersListView.this, mHeader, mHeaderPosition, mHeaderId, true);
                        }
                    });
                }
            } else {
                mAdapter.setOnHeaderClickListener(null);
            }
        }
    }

    public void setOnStickyHeaderOffsetChangedListener(OnStickyHeaderOffsetChangedListener listener) {
        mOnStickyHeaderOffsetChangedListener = listener;
    }

    public void setOnStickyHeaderChangedListener(OnStickyHeaderChangedListener listener) {
        mOnStickyHeaderChangedListener = listener;
    }

//    public View getListChildAt(int index) {
//        return wrapperViewList.getChildAt(index);
//    }
//    public int getListChildCount() {
//        return wrapperViewList.getChildCount();
//    }

    /**
     * Use the method with extreme caution!! Changing any values on the
     * underlying ListView might break everything.
     *
     * @return the ListView backing this view.
     */
//    public ListView getWrappedList() {
//        return wrapperViewList;
//    }

    private boolean requireSdkVersion(int versionCode) {
        if (Build.VERSION.SDK_INT < versionCode) {
            Log.e("StickyListHeaders", "Api lvl must be at least "+versionCode+" to call this method");
            return false;
        }
        return true;
    }

	/* ---------- 下面是ListView的代理方法 ---------- */
    //没有用到的已经注释掉了，若要使用，取消注释即可！
    public void setAdapter(StickyListHeadersAdapter adapter) {
        if (adapter == null) {
            if (mAdapter instanceof SectionIndexerAdapterWrapper) {
                ((SectionIndexerAdapterWrapper) mAdapter).mSectionIndexerDelegate = null;
            }
            if (mAdapter != null) {
                mAdapter.stickyListHeadersAdapter = null;
            }
            wrapperViewList.setAdapter(null);
            clearHeader();
            return;
        }
        if (mAdapter != null) {
            mAdapter.unregisterDataSetObserver(mDataSetObserver);
        }
        if (adapter instanceof SectionIndexer) {
            mAdapter = new SectionIndexerAdapterWrapper(getContext(), adapter);
        } else {
            mAdapter = new AdapterWrapper(getContext(), adapter);
        }
        mDataSetObserver = new AdapterWrapperDataSetObserver();
        mAdapter.registerDataSetObserver(mDataSetObserver);
        if (mOnHeaderClickListener != null) {
            mAdapter.setOnHeaderClickListener(new AdapterWrapperHeaderClickHandler());
        } else {
            mAdapter.setOnHeaderClickListener(null);
        }
        mAdapter.setDivider(mDivider, mDividerHeight);
        wrapperViewList.setAdapter(mAdapter);
        clearHeader();
    }

//    public StickyListHeadersAdapter getAdapter() {
//        return mAdapter == null ? null : mAdapter.stickyListHeadersAdapter;
//    }

    public void setDivider(Drawable divider) {
        mDivider = divider;
        if (mAdapter != null) {
            mAdapter.setDivider(mDivider, mDividerHeight);
        }
    }

//    public void setDividerHeight(int dividerHeight) {
//        mDividerHeight = dividerHeight;
//        if (mAdapter != null) {
//            mAdapter.setDivider(mDivider, mDividerHeight);
//        }
//    }

    public Drawable getDivider() {
        return mDivider;
    }

//    public int getDividerHeight() {
//        return mDividerHeight;
//    }
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        mOnScrollListenerDelegate = onScrollListener;
    }

    @Override
    public void setOnTouchListener(final OnTouchListener onTouchListener) {
        if (onTouchListener != null) {
            wrapperViewList.setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return onTouchListener.onTouch(StickyListHeadersListView.this, event);
                }
            });
        } else {
            wrapperViewList.setOnTouchListener(null);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        wrapperViewList.setOnItemClickListener(listener);
    }

//    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
//        wrapperViewList.setOnItemLongClickListener(listener);
//    }
//
//    public void addHeaderView(View v, Object data, boolean isSelectable) {
//        wrapperViewList.addHeaderView(v, data, isSelectable);
//    }
    //添加首布局
    public void addHeaderView(View v) {
        wrapperViewList.addHeaderView(v);
    }
//    public void removeHeaderView(View v) {
//        wrapperViewList.removeHeaderView(v);
//    }
    public int getHeaderViewsCount() {
        return wrapperViewList.getHeaderViewsCount();
    }

    public void addFooterView(View v) {
        wrapperViewList.addFooterView(v);
    }
//    public void addFooterView(View v, Object data, boolean isSelectable) {
//        wrapperViewList.addFooterView(v, data, isSelectable);
//    }
//    public void removeFooterView(View v) {
//        wrapperViewList.removeFooterView(v);
//    }
//    public int getFooterViewsCount() {
//        return wrapperViewList.getFooterViewsCount();
//    }
    //列表为空时显示的内容
    public void setEmptyView(View v) {
        wrapperViewList.setEmptyView(v);
    }
//    public View getEmptyView() {
//        return wrapperViewList.getEmptyView();
//    }

    @Override
    public boolean isVerticalScrollBarEnabled() {
        return wrapperViewList.isVerticalScrollBarEnabled();
    }
    @Override
    public boolean isHorizontalScrollBarEnabled() {
        return wrapperViewList.isHorizontalScrollBarEnabled();
    }
    @Override
    public void setVerticalScrollBarEnabled(boolean verticalScrollBarEnabled) {
        wrapperViewList.setVerticalScrollBarEnabled(verticalScrollBarEnabled);
    }
    @Override
    public void setHorizontalScrollBarEnabled(boolean horizontalScrollBarEnabled) {
        wrapperViewList.setHorizontalScrollBarEnabled(horizontalScrollBarEnabled);
    }
    @Override
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public int getOverScrollMode() {
        if (requireSdkVersion(Build.VERSION_CODES.GINGERBREAD)) {
            return wrapperViewList.getOverScrollMode();
        }
        return 0;
    }
    @Override
    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    public void setOverScrollMode(int mode) {
        if (requireSdkVersion(Build.VERSION_CODES.GINGERBREAD)) {
            if (wrapperViewList != null) {
                wrapperViewList.setOverScrollMode(mode);
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.FROYO)
    public void smoothScrollBy(int distance, int duration) {
        if (requireSdkVersion(Build.VERSION_CODES.FROYO)) {
            wrapperViewList.smoothScrollBy(distance, duration);
        }
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void smoothScrollByOffset(int offset) {
        if (requireSdkVersion(Build.VERSION_CODES.HONEYCOMB)) {
            wrapperViewList.smoothScrollByOffset(offset);
        }
    }
    @SuppressLint("NewApi")
    @TargetApi(Build.VERSION_CODES.FROYO)
    public void smoothScrollToPosition(int position) {
        if (requireSdkVersion(Build.VERSION_CODES.FROYO)) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                wrapperViewList.smoothScrollToPosition(position);
            } else {
                int offset = mAdapter == null ? 0 : getHeaderOverlap(position);
                offset -= mClippingToPadding ? 0 : mPaddingTop;
                wrapperViewList.smoothScrollToPositionFromTop(position, offset);
            }
        }
    }
    @TargetApi(Build.VERSION_CODES.FROYO)
    public void smoothScrollToPosition(int position, int boundPosition) {
        if (requireSdkVersion(Build.VERSION_CODES.FROYO)) {
            wrapperViewList.smoothScrollToPosition(position, boundPosition);
        }
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void smoothScrollToPositionFromTop(int position, int offset) {
        if (requireSdkVersion(Build.VERSION_CODES.HONEYCOMB)) {
            offset += mAdapter == null ? 0 : getHeaderOverlap(position);
            offset -= mClippingToPadding ? 0 : mPaddingTop;
            wrapperViewList.smoothScrollToPositionFromTop(position, offset);
        }
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void smoothScrollToPositionFromTop(int position, int offset,
                                              int duration) {
        if (requireSdkVersion(Build.VERSION_CODES.HONEYCOMB)) {
            offset += mAdapter == null ? 0 : getHeaderOverlap(position);
            offset -= mClippingToPadding ? 0 : mPaddingTop;
            wrapperViewList.smoothScrollToPositionFromTop(position, offset, duration);
        }
    }

//    public void setSelection(int position) {
//        setSelectionFromTop(position, 0);
//    }
//    public void setSelectionAfterHeaderView() {
//        wrapperViewList.setSelectionAfterHeaderView();
//    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void setSelectionFromTop(int position, int y) {
        y += mAdapter == null ? 0 : getHeaderOverlap(position);
        y -= mClippingToPadding ? 0 : mPaddingTop;
        if (requireSdkVersion(Build.VERSION_CODES.LOLLIPOP))
            wrapperViewList.setSelectionFromTop(position, y);
    }

//    public void setSelector(Drawable sel) {
//        wrapperViewList.setSelector(sel);
//    }
//    public void setSelector(int resID) {
//        wrapperViewList.setSelector(resID);
//    }
//    public int getFirstVisiblePosition() {
//        return wrapperViewList.getFirstVisiblePosition();
//    }
//    public int getLastVisiblePosition() {
//        return wrapperViewList.getLastVisiblePosition();
//    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setChoiceMode(int choiceMode) {
        wrapperViewList.setChoiceMode(choiceMode);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setItemChecked(int position, boolean value) {
        wrapperViewList.setItemChecked(position, value);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public int getCheckedItemCount() {
        if (requireSdkVersion(Build.VERSION_CODES.HONEYCOMB)) {
            return wrapperViewList.getCheckedItemCount();
        }
        return 0;
    }
    @TargetApi(Build.VERSION_CODES.FROYO)
    public long[] getCheckedItemIds() {
        if (requireSdkVersion(Build.VERSION_CODES.FROYO)) {
            return wrapperViewList.getCheckedItemIds();
        }
        return null;
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public int getCheckedItemPosition() {
        return wrapperViewList.getCheckedItemPosition();
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public SparseBooleanArray getCheckedItemPositions() {
        return wrapperViewList.getCheckedItemPositions();
    }
//    public int getCount() {
//        return wrapperViewList.getCount();
//    }
//    public Object getItemAtPosition(int position) {
//        return wrapperViewList.getItemAtPosition(position);
//    }
//    public long getItemIdAtPosition(int position) {
//        return wrapperViewList.getItemIdAtPosition(position);
//    }
    @Override
    public void setOnCreateContextMenuListener(OnCreateContextMenuListener l) {
        wrapperViewList.setOnCreateContextMenuListener(l);
    }
    @Override
    public boolean showContextMenu() {
        return wrapperViewList.showContextMenu();
    }
//    public void invalidateViews() {
//        wrapperViewList.invalidateViews();
//    }
    @Override
    public void setClipToPadding(boolean clipToPadding) {
        if (wrapperViewList != null) {
            wrapperViewList.setClipToPadding(clipToPadding);
        }
        mClippingToPadding = clipToPadding;
    }
    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        mPaddingLeft = left;
        mPaddingTop = top;
        mPaddingRight = right;
        mPaddingBottom = bottom;

        if (wrapperViewList != null) {
            wrapperViewList.setPadding(left, top, right, bottom);
        }
        super.setPadding(0, 0, 0, 0);
        requestLayout();
    }
    /*
     * Overrides an @hide method in View
     */
//    protected void recomputePadding() {
//        setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);
//    }

    @Override
    public int getPaddingLeft() {
        return mPaddingLeft;
    }
    @Override
    public int getPaddingTop() {
        return mPaddingTop;
    }
    @Override
    public int getPaddingRight() {
        return mPaddingRight;
    }
    @Override
    public int getPaddingBottom() {
        return mPaddingBottom;
    }
    public void setFastScrollEnabled(boolean fastScrollEnabled) {
        wrapperViewList.setFastScrollEnabled(fastScrollEnabled);
    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setFastScrollAlwaysVisible(boolean alwaysVisible) {
        if (requireSdkVersion(Build.VERSION_CODES.HONEYCOMB)) {
            wrapperViewList.setFastScrollAlwaysVisible(alwaysVisible);
        }
    }
    /**
     * @return true if the fast scroller will always show. False on pre-Honeycomb devices.
     * @see AbsListView#isFastScrollAlwaysVisible()
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public boolean isFastScrollAlwaysVisible() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB && wrapperViewList.isFastScrollAlwaysVisible();
    }
    public void setScrollBarStyle(int style) {
        wrapperViewList.setScrollBarStyle(style);
    }
    public int getScrollBarStyle() {
        return wrapperViewList.getScrollBarStyle();
    }
//    public int getPositionForView(View view) {
//        return wrapperViewList.getPositionForView(view);
//    }
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void setMultiChoiceModeListener(MultiChoiceModeListener listener) {
        if (requireSdkVersion(Build.VERSION_CODES.HONEYCOMB)) {
            wrapperViewList.setMultiChoiceModeListener(listener);
        }
    }
    @Override
    public Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (superState != BaseSavedState.EMPTY_STATE) {
          throw new IllegalStateException("Handling non empty state of parent class is not implemented");
        }
        return wrapperViewList.onSaveInstanceState();
    }
    @Override
    public void onRestoreInstanceState(Parcelable state) {
        super.onRestoreInstanceState(BaseSavedState.EMPTY_STATE);
        wrapperViewList.onRestoreInstanceState(state);
    }
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean canScrollVertically(int direction) {
        return wrapperViewList.canScrollVertically(direction);
    }
//    public void setTranscriptMode (int mode) {
//        wrapperViewList.setTranscriptMode(mode);
//    }
//    public void setBlockLayoutChildren(boolean blockLayoutChildren) {
//        wrapperViewList.setBlockLayoutChildren(blockLayoutChildren);
//    }
//    public void setStackFromBottom(boolean stackFromBottom) {
//    	wrapperViewList.setStackFromBottom(stackFromBottom);
//    }
//    public boolean isStackFromBottom() {
//    	return wrapperViewList.isStackFromBottom();
//    }
}
