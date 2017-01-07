package com.nulldreams.bemusic.adapter;

import android.content.Context;
import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.nulldreams.bemusic.R;

/**
 * Created by boybe on 2017/1/7.
 */

public class AlbumDecoration extends RecyclerView.ItemDecoration {

    private int offset = 0;
    private int columnCount = 2;

    public AlbumDecoration (Context context){
        offset = context.getResources().getDimensionPixelOffset(R.dimen.album_item_offset);
        columnCount = context.getResources().getInteger(R.integer.album_column_count);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        final int position = parent.getChildAdapterPosition(view);
        final int row = position / columnCount;
        final int column = position % columnCount;
        int left;
        int right;
        int top;
        int bottom;
        if (row == 0) {
            top = offset;
            bottom = offset;
        } else {
            top = 0;
            bottom = offset;
        }
        if (column == 0) {
            left = offset;
            right = offset;
        } else {
            left = 0;
            right = offset;
        }
        outRect.set(left, top, right, bottom);
    }
}
