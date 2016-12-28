package com.nulldreams.bemusic.manager.ruler;

import com.nulldreams.bemusic.model.Song;

import java.util.List;

/**
 * Created by gaoyunfei on 2016/12/28.
 */

public interface Rule {
    Song previous (Song song, List<Song> songList, boolean isUserAction);
    Song forward (Song song, List<Song> songList, boolean isUserAction);
    void clear ();
}
