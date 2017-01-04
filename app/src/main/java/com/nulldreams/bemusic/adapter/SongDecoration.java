package com.nulldreams.bemusic.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.nulldreams.bemusic.R;

/**
 * Created by gaoyunfei on 2017/1/4.
 */

public class SongDecoration extends RecyclerView.ItemDecoration {

    private int offset;

    public SongDecoration (Context context) {
        offset = context.getResources().getDimensionPixelOffset(R.dimen.song_item_divider_height);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        final int position = parent.getChildAdapterPosition(view);
        if (position == 0) {
            outRect.set(0, offset, 0, offset);
        } else {
            outRect.set(0, 0, 0, offset);
        }
    }
}
