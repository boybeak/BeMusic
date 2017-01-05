package com.nulldreams.bemusic.adapter;

import com.nulldreams.adapter.annotation.AnnotationDelegate;
import com.nulldreams.adapter.annotation.DelegateInfo;
import com.nulldreams.bemusic.R;
import com.nulldreams.media.model.Song;

/**
 * Created by boybe on 2016/12/27.
 */
@DelegateInfo(layoutID = R.layout.layout_song, holderClass = SongHolder.class)
public class SongDelegate extends AnnotationDelegate<Song> {
    public SongDelegate(Song song) {
        super(song);
    }

}
