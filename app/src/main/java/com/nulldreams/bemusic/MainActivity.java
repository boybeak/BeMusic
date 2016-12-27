package com.nulldreams.bemusic;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.DelegateImpl;
import com.nulldreams.bemusic.adapter.SongDelegate;
import com.nulldreams.bemusic.manager.PlayManager;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;

import java.util.List;

public class MainActivity extends AppCompatActivity implements PlayManager.Callback{

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = MainActivity.class.getSimpleName();

    private DelegateAdapter mAdapter;
    private RecyclerView mRv;
    private View mMiniPanel;
    private ImageView mThumbIv, mControlBtn;
    private TextView mTitleTv, mArtistAlbumTv;

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == mControlBtn.getId()) {
                PlayManager.getInstance(v.getContext()).dispatch();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMiniPanel = findViewById(R.id.main_mini_panel);
        mThumbIv = (ImageView)findViewById(R.id.main_mini_thumb);
        mControlBtn = (ImageView)findViewById(R.id.main_mini_control_btn);
        mTitleTv = (TextView)findViewById(R.id.main_mini_title);
        mArtistAlbumTv = (TextView)findViewById(R.id.main_mini_artist_album);

        mRv = (RecyclerView)findViewById(R.id.main_rv);
        mRv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DelegateAdapter(this);
        mRv.setAdapter(mAdapter);

        mControlBtn.setOnClickListener(mClickListener);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        List<Song> songs = PlayManager.getInstance(this).getTotalList();
        if (songs != null) {
            setupPlayList(songs);
        }

        mControlBtn.setSelected(PlayManager.getInstance(this).isPlaying());
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayManager.getInstance(this).registerCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayManager.getInstance(this).unregisterCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onPlayListPrepared(List<Song> songs) {
        setupPlayList(songs);
    }

    private void setupPlayList (List<Song> songs) {
        mAdapter.clear();
        mAdapter.addAll(songs, new DelegateParser<Song>() {
            @Override
            public DelegateImpl parse(Song data) {
                return new SongDelegate(data);
            }
        });
        mAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPlayStateChanged(@PlayService.State int state, Song song) {
        switch (state) {
            case PlayService.STATE_INITIALIZED:
                Glide.with(this).load(song.getCoverFile(this)).placeholder(R.mipmap.ic_launcher).into(mThumbIv);
                mTitleTv.setText(song.getTitle());
                mArtistAlbumTv.setText(song.getArtist() + " - " + song.getAlbum());
                annimtionShowMiniPanel();
                break;
            case PlayService.STATE_STARTED:
                mControlBtn.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_PAUSED:
                mControlBtn.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_STOPPED:
                mControlBtn.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_COMPLETED:
                mControlBtn.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
        }
    }

    private void annimtionShowMiniPanel () {
        ObjectAnimator animator = ObjectAnimator.ofFloat(mMiniPanel, "translationY", mMiniPanel.getHeight(), 0);
        animator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                mMiniPanel.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                animation.removeAllListeners();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animation.removeAllListeners();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        animator.start();
    }
}
