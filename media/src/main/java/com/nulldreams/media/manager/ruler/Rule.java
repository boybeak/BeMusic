package com.nulldreams.media.manager.ruler;

import com.nulldreams.media.model.Song;

import java.util.List;

/**
 * Created by gaoyunfei on 2016/12/28.
 */
public interface Rule {
    Song previous (Song song, List<Song> songList, boolean isUserAction);
    Song next(Song song, List<Song> songList, boolean isUserAction);
    void clear ();
}
