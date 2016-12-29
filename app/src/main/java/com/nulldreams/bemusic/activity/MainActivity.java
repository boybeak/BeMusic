package com.nulldreams.bemusic.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
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
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.adapter.SongDelegate;
import com.nulldreams.bemusic.fragment.PlayDetailFragment;
import com.nulldreams.bemusic.manager.PlayManager;
import com.nulldreams.bemusic.manager.ruler.Rule;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;

import java.io.File;
import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;

public class MainActivity extends AppCompatActivity implements PlayManager.Callback{

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = MainActivity.class.getSimpleName();

    private DelegateAdapter mAdapter;
    private RecyclerView mRv;
    private View mMiniPanel;
    private ImageView mMiniPanelBgIv, mThumbIv, mControlBtn, mNextBtn;
    private TextView mTitleTv, mArtistAlbumTv;

    PlayDetailFragment fragment = PlayDetailFragment.newInstance();

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == mControlBtn.getId()) {
                PlayManager.getInstance(v.getContext()).dispatch();
            } else if (id == mMiniPanel.getId()) {
                showPlayDetail();
            } else if (id == mNextBtn.getId()) {
                PlayManager.getInstance(v.getContext()).next();
            }
        }
    };

    private void showPlayDetail () {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.activity_main, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private void hidePlayDetail () {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.remove(fragment);
        manager.popBackStack();
        transaction.commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMiniPanel = findViewById(R.id.main_mini_panel);
        mMiniPanelBgIv = (ImageView)findViewById(R.id.main_mini_panel_bg);
        mThumbIv = (ImageView)findViewById(R.id.main_mini_thumb);
        mControlBtn = (ImageView)findViewById(R.id.main_mini_control_btn);
        mNextBtn = (ImageView)findViewById(R.id.main_mini_control_next);
        mTitleTv = (TextView)findViewById(R.id.main_mini_title);
        mArtistAlbumTv = (TextView)findViewById(R.id.main_mini_artist_album);

        mRv = (RecyclerView)findViewById(R.id.main_rv);
        mRv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DelegateAdapter(this);
        mRv.setAdapter(mAdapter);

        mControlBtn.setOnClickListener(mClickListener);
        mMiniPanel.setOnClickListener(mClickListener);
        mNextBtn.setOnClickListener(mClickListener);
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

                mTitleTv.setText(song.getTitle());
                mArtistAlbumTv.setText(song.getArtistAlbum());
                File file = song.getCoverFile(this);
                if (file.exists()) {
                    Glide.with(this).load(file).placeholder(R.mipmap.ic_launcher).into(mThumbIv);
                    Glide.with(this).load(file).asBitmap().animate(android.R.anim.fade_in)
                            .transform(new BlurTransformation(this)).into(mMiniPanelBgIv);
                }
                animationShowMiniPanel();
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

    @Override
    public void onPlayRuleChanged(Rule rule) {

    }

    private void animationShowMiniPanel() {
        if (mMiniPanel.getVisibility() == View.VISIBLE) {
            return;
        }
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
