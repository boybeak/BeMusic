package com.nulldreams.bemusic.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.nulldreams.bemusic.R;

/**
 * Created by gaoyunfei on 16/7/23.
 */
public class ProgressBar extends View {

    private Paint mPaint = null;

    private int mProgress = 0, mMax = 100;

    public ProgressBar(Context context) {
        super(context);
        initThis(context, null);
    }

    public ProgressBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        initThis(context, attrs);
    }

    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initThis(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ProgressBar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initThis(context, attrs);
    }

    private void initThis (Context context, AttributeSet attrs) {
        mPaint = new Paint();
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressBar);
            try {
                final int color = a.getColor(R.styleable.ProgressBar_progressColor, Color.BLACK);
                mPaint.setColor(color);
            } finally {
                a.recycle();
            }
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        final int per = getWidth() / mMax;
        final int length = per * mProgress;
        canvas.drawRect(getLeft(), getTop(), getLeft() + length, getBottom(), mPaint);
    }

    public void setMax (int max) {
        mMax = max;
    }

    public void setProgress (int progress) {
        mProgress = progress;
        invalidate();
    }

    public void setProgressColor (int color) {
        mPaint.setColor(color);
        invalidate();
    }
}
