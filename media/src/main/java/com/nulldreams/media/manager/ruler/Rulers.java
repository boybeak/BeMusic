package com.nulldreams.media.manager.ruler;

import com.nulldreams.media.model.Song;

import java.util.List;
import java.util.Random;
import java.util.Stack;

/**
 * Created by gaoyunfei on 2016/12/28.
 * Some rules often use
 */

public class Rulers {

    public static final Rule RULER_SINGLE_LOOP = new SingleLoopRuler(),
            RULER_LIST_LOOP = new ListLoopRuler(), RULER_RANDOM = new RandomRuler();

    public static class SingleLoopRuler implements Rule {

        private SingleLoopRuler () {

        }

        @Override
        public Song previous(Song song, List<Song> songList, boolean isUserAction) {
            if (isUserAction) {
                return RULER_LIST_LOOP.previous(song, songList, isUserAction);
            }
            return song;
        }

        @Override
        public Song next(Song song, List<Song> songList, boolean isUserAction) {
            if (isUserAction) {
                return RULER_LIST_LOOP.next(song, songList, isUserAction);
            }
            return song;
        }

        @Override
        public void clear() {

        }
    }

    public static class ListLoopRuler implements Rule {

        private ListLoopRuler () {

        }

        @Override
        public Song previous(Song song, List<Song> songList, boolean isUserAction) {
            if (songList != null && !songList.isEmpty()) {
                if (song == null) {
                    return songList.get(0);
                }
                int index = songList.indexOf(song);
                if (index < 0) {
                    return songList.get(0);
                } else if (index == 0) {
                    index = songList.size();
                }
                return songList.get(index - 1);
            }
            return song;
        }

        @Override
        public Song next(Song song, List<Song> songList, boolean isUserAction) {
            if (songList != null && !songList.isEmpty()) {
                if (song == null) {
                    return songList.get(0);
                }
                int index = songList.indexOf(song);
                if (index < 0) {
                    return songList.get(0);
                }
                return songList.get((index + 1) % songList.size());
            }
            return song;
        }

        @Override
        public void clear() {

        }
    }

    public static class RandomRuler implements Rule {

        private Random mRandom;
        private Stack<Song> mSongStack;

        private RandomRuler () {
            mRandom = new Random();
            mSongStack = new Stack<>();
        }

        @Override
        public Song previous(Song song, List<Song> songList, boolean isUserAction) {
            if (songList == null || songList.isEmpty()) {
                return song;
            }
            if (!mSongStack.isEmpty()) {
                return mSongStack.pop();
            }
            int index = mRandom.nextInt(songList.size());

            return songList.get(index);
        }

        @Override
        public Song next(Song song, List<Song> songList, boolean isUserAction) {
            if (songList != null && songList.size() > 1) {
                Song forwardSong;
                if (!mSongStack.isEmpty()) {
                    Song lastSong = mSongStack.get(mSongStack.size() - 1);

                    do {
                        int index = mRandom.nextInt(songList.size());
                        forwardSong = songList.get(index);
                    } while (lastSong.equals(forwardSong));
                } else {
                    int index = mRandom.nextInt(songList.size());
                    forwardSong = songList.get(index);
                }

                mSongStack.push(forwardSong);
                return forwardSong;
            }
            return song;
        }

        @Override
        public void clear() {
            mSongStack.clear();
        }
    }
}
