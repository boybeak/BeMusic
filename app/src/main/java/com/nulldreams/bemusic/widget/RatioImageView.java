package com.nulldreams.bemusic.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.nulldreams.bemusic.R;

/**
 * Created by boybe on 2016/9/3.
 */
public class RatioImageView extends ImageView {

    private int mWidthRatio, mHeightRatio;

    public RatioImageView(Context context) {
        this(context, null);
    }

    public RatioImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RatioImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initThis(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public RatioImageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initThis(context, attrs);
    }

    private void initThis (Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RatioImageView);
        try {
            mWidthRatio = array.getInt(R.styleable.RatioImageView_widthRatio, 1);
            mHeightRatio = array.getInt(R.styleable.RatioImageView_heightRatio, 1);
        } finally {
            array.recycle();
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        /*int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int size = Math.min(widthSize, heightSize);*/
        int heightSize = widthSize * mHeightRatio / mWidthRatio;
        setMeasuredDimension(widthSize, heightSize);
    }
}
