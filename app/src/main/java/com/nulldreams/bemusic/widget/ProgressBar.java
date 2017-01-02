package com.nulldreams.bemusic.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.nulldreams.bemusic.R;

/**
 * Created by gaoyunfei on 16/7/23.
 */
public class ProgressBar extends View {

    private static final String TAG = ProgressBar.class.getSimpleName();

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
        mPaint.setStyle(Paint.Style.FILL);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ProgressBar);
            try {
                final int color = a.getColor(R.styleable.ProgressBar_progressColor, Color.BLACK);
                mPaint.setColor(color);
                Log.v(TAG, "color=" + Integer.toHexString(color));
            } finally {
                a.recycle();
            }
        }
    }


    @Override
    protected void onDraw(Canvas canvas) {
        final int length = (int)((float)getWidth() * mProgress / mMax);
        final int left = 0;
        final int top = getTop();
        final int right = left + length;
        final int bottom = getBottom();
        //Log.v(TAG, "onDraw left=" + left + " top=" + top + " right=" + right + " bottom=" + bottom);
        canvas.drawRect(left, top, right, bottom, mPaint);
        super.onDraw(canvas);
    }

    public void setMax (int max) {
        mMax = max;
    }

    public int getMax() {
        return mMax;
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
