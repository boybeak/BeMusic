package com.nulldreams.bemusic.adapter;

import com.nulldreams.adapter.annotation.AnnotationDelegate;
import com.nulldreams.adapter.annotation.DelegateInfo;
import com.nulldreams.bemusic.R;
import com.nulldreams.media.model.Album;

/**
 * Created by boybe on 2017/1/6.
 */
@DelegateInfo(layoutID = R.layout.layout_album, holderClass = AlbumHolder.class)
public class AlbumDelegate extends AnnotationDelegate<Album> {

    private int rgb;

    public AlbumDelegate(Album album) {
        super(album);
    }

    public int getRgb() {
        return rgb;
    }

    public void setRgb(int rgb) {
        this.rgb = rgb;
    }
}
