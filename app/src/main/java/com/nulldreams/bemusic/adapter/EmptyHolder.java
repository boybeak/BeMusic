package com.nulldreams.bemusic.adapter;

import android.content.Context;
import android.support.v7.widget.GridLayoutManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nulldreams.adapter.AbsViewHolder;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.bemusic.R;

/**
 * Created by gaoyunfei on 2017/1/14.
 */

public class EmptyHolder extends AbsViewHolder<EmptyDelegate>{

    private TextView mMessageTv;

    public EmptyHolder(View itemView) {
        super(itemView);
        mMessageTv = (TextView)itemView.findViewById(R.id.empty_message);
    }

    @Override
    public void onBindView(Context context, EmptyDelegate emptyDelegate, int position, DelegateAdapter adapter) {
        String message = emptyDelegate.getSource();
        mMessageTv.setText(message);

    }
}
