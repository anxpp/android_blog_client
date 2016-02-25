package com.anxpp.blog.stickylistheaders;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * 带下划线的TextView
 * @author http://www.anxpp.com/
 */
public class UnderlineTextView  extends TextView {
    //Paint即画笔，在绘图过程中起到了极其重要的作用，画笔主要保存了颜色，
    //样式等绘制信息，指定了如何绘制文本和图形，画笔对象有很多设置方法，
    //大体上可以分为两类，一类与图形绘制相关，一类与文本绘制相关
    private final Paint mPaint = new Paint();
    //下划线高度
    private int mUnderlineHeight = 0;

    public UnderlineTextView(Context context) {
        this(context, null);
    }

    public UnderlineTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UnderlineTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mUnderlineHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, getResources().getDisplayMetrics());
    }

    @Override
    public void setPadding(int left, int top, int right, int bottom) {
        super.setPadding(left, top, right, bottom + mUnderlineHeight);
    }

    //绘制下划线
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //下划线与文字颜色相同
        mPaint.setColor(getTextColors().getDefaultColor());
        canvas.drawRect(0, getHeight() - mUnderlineHeight, getWidth(), getHeight(), mPaint);
    }
}
