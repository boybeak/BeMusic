package com.nulldreams.bemusic.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateFilter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.LayoutImpl;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.adapter.EmptyDelegate;
import com.nulldreams.bemusic.adapter.SongDecoration;
import com.nulldreams.bemusic.adapter.SongDelegate;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.ruler.Rule;
import com.nulldreams.media.model.Album;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.service.PlayService;

import java.util.List;

/**
 * Created by gaoyunfei on 2017/1/1.
 */

public class SongListFragment extends RvFragment
        implements PlayManager.Callback{

    private boolean isIdle = true, isResumed = false;
    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            isIdle = newState == RecyclerView.SCROLL_STATE_IDLE;
        }
    };

    private SongDelegate mLastSongDelegate = null;

    private DelegateFilter mFilter = new DelegateFilter() {

        @Override
        public boolean accept(DelegateAdapter adapter, LayoutImpl impl) {
            if (impl instanceof SongDelegate) {
                SongDelegate songDelegate = (SongDelegate)impl;
                boolean result = songDelegate.getSource().equals(PlayManager.getInstance(getContext()).getCurrentSong());
                if (result) {
                    if (mLastSongDelegate != null) {
                        mLastSongDelegate.setSelected(false);
                    }
                    songDelegate.setSelected(true);
                    mLastSongDelegate = songDelegate;
                }
                return result;
            }
            return false;
        }

    };

    private SongDecoration mSongDecoration;

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mSongDecoration = new SongDecoration(getContext());
        getRecyclerView().addItemDecoration(mSongDecoration);
        PlayManager.getInstance(getContext()).getTotalListAsync(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        Song song = PlayManager.getInstance(getContext()).getCurrentSong();

        if (song != null) {
            int index = getAdapter().firstIndexOf(mFilter);
            getRecyclerView().getLayoutManager().scrollToPosition(index);
            //showSong(song);
        }

        PlayManager.getInstance(getContext()).registerCallback(this);
        getRecyclerView().addOnScrollListener(mScrollListener);
        isResumed = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        PlayManager.getInstance(getContext()).unregisterCallback(this);
        getRecyclerView().removeOnScrollListener(mScrollListener);
        isResumed = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getRecyclerView().removeItemDecoration(mSongDecoration);
    }

    @Override
    public RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @Override
    public CharSequence getTitle(Context context, Object... params) {
        return context.getString(R.string.title_song_list);
    }

    public void setSongList (List<Song> songList) {
        getAdapter().clear();
        if (songList != null && !songList.isEmpty()) {
            getAdapter().addAll(songList, new DelegateParser<Song>() {
                @Override
                public LayoutImpl parse(DelegateAdapter adapter, Song data) {
                    return new SongDelegate(data);
                }

            });
        } else {
            getAdapter().add(new EmptyDelegate(getContext(), R.string.text_empty_msg_songs));
        }

        getAdapter().notifyDataSetChanged();

    }

    @Override
    public void onPlayListPrepared(List<Song> songs) {
        setSongList(songs);
    }

    @Override
    public void onAlbumListPrepared(List<Album> albums) {

    }

    @Override
    public void onPlayStateChanged(@PlayService.State int state, Song song) {
        switch (state) {
            case PlayService.STATE_INITIALIZED:
                if (isIdle && isResumed) {
                    getRecyclerView().scrollToPosition(getAdapter().firstIndexOf(mFilter));
                }
                break;
            case PlayService.STATE_STARTED:
//                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_PAUSED:
//                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_STOPPED:
//                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_COMPLETED:
//                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
        }
    }

    @Override
    public void onShutdown() {

    }

    @Override
    public void onPlayRuleChanged(Rule rule) {

    }
}
