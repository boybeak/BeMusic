package com.nulldreams.bemusic.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.manager.PlayManager;
import com.nulldreams.bemusic.manager.ruler.Rule;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;

import java.io.File;
import java.util.List;

/**
 * Created by boybe on 2016/12/28.
 */

public class PlayDetailFragment extends Fragment implements PlayManager.Callback, PlayManager.ProgressCallback{

    private static final String TAG = PlayDetailFragment.class.getSimpleName();

    public static PlayDetailFragment newInstance () {
        return new PlayDetailFragment();
    }

    private TextView mTitleTv, mArtistTv, mAlbumTv;
    private ImageView mThumbIv, mPlayPauseIv, mPreviousIv, mNextIv;
    private SeekBar mSeekBar;

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == mPlayPauseIv.getId()) {
                PlayManager.getInstance(v.getContext()).dispatch();
            } else if (id == mPreviousIv.getId()) {
                PlayManager.getInstance(v.getContext()).previous();
            } else if (id == mNextIv.getId()) {
                PlayManager.getInstance(v.getContext()).next();
            }
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        PlayManager.getInstance(context).registerCallback(this);
        PlayManager.getInstance(context).registerProgressCallback(this);
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = null;
        if (enter) {
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.anim_bottom_in);
        } else {
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.anim_bottom_out);
        }
        return animation;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.activity_play_detail, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mTitleTv = (TextView)view.findViewById(R.id.play_detail_title);
        mArtistTv = (TextView)view.findViewById(R.id.play_detail_artist);
        mAlbumTv = (TextView)view.findViewById(R.id.play_detail_album);
        mThumbIv = (ImageView)view.findViewById(R.id.play_detail_thumb);
        mSeekBar = (SeekBar)view.findViewById(R.id.play_detail_seek_bar);
        mPlayPauseIv = (ImageView)view.findViewById(R.id.play_detail_play_pause);
        mPreviousIv = (ImageView)view.findViewById(R.id.play_detail_previous);
        mNextIv = (ImageView)view.findViewById(R.id.play_detail_next);

        mPlayPauseIv.setOnClickListener(mClickListener);
        mPreviousIv.setOnClickListener(mClickListener);
        mNextIv.setOnClickListener(mClickListener);

        Song song = PlayManager.getInstance(getContext()).getCurrentSong();
        if (song != null) {
            showSong(song);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        PlayManager.getInstance(getContext()).registerCallback(this);
        PlayManager.getInstance(getContext()).registerProgressCallback(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PlayManager.getInstance(getContext()).unregisterCallback(this);
        PlayManager.getInstance(getContext()).unregisterProgressCallback(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        PlayManager.getInstance(getContext()).unregisterCallback(this);
        PlayManager.getInstance(getContext()).unregisterProgressCallback(this);
        Log.v(TAG, "onDetach");
    }

    @Override
    public void onPlayListPrepared(List<Song> songs) {

    }

    @Override
    public void onPlayStateChanged(@PlayService.State int state, Song song) {
        switch (state) {
            case PlayService.STATE_INITIALIZED:
                showSong(song);
                break;
            case PlayService.STATE_STARTED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(getContext()).isPlaying());
                break;
            case PlayService.STATE_PAUSED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(getContext()).isPlaying());
                break;
            case PlayService.STATE_COMPLETED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(getContext()).isPlaying());
                break;
            case PlayService.STATE_STOPPED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(getContext()).isPlaying());
                break;
            case PlayService.STATE_RELEASED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(getContext()).isPlaying());
                break;
            case PlayService.STATE_ERROR:
                mPlayPauseIv.setSelected(PlayManager.getInstance(getContext()).isPlaying());
                break;
        }
    }

    @Override
    public void onPlayRuleChanged(Rule rule) {

    }

    private void showSong (Song song) {
        mTitleTv.setText(song.getTitle());
        mArtistTv.setText(song.getArtist());
        mAlbumTv.setText(song.getAlbum());
        File file = song.getCoverFile(getContext());
        if (file.exists()) {
            Glide.with(getContext()).load(file).into(mThumbIv);
        }
    }

    @Override
    public void onProgress(int progress, int duration) {
        if (mSeekBar.getMax() != duration) {
            mSeekBar.setMax(duration);
        }
        mSeekBar.setProgress(progress);
    }
}
