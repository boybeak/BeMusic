package com.nulldreams.bemusic.adapter;

import android.content.Context;
import android.support.annotation.StringRes;

import com.nulldreams.adapter.annotation.AnnotationDelegate;
import com.nulldreams.adapter.annotation.DelegateInfo;
import com.nulldreams.bemusic.R;

/**
 * Created by gaoyunfei on 2017/1/14.
 */
@DelegateInfo(layoutID = R.layout.layout_empty, holderClass = EmptyHolder.class)
public class EmptyDelegate extends AnnotationDelegate<String> {

    public EmptyDelegate(String s) {
        super(s);
    }

    public EmptyDelegate (Context context, @StringRes int stringRes) {
        this (context.getString(stringRes));
    }
}
