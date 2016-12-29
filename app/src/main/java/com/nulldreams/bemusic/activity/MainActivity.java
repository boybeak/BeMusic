package com.nulldreams.bemusic.activity;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateFilter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.DelegateImpl;
import com.nulldreams.adapter.impl.LayoutImpl;
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
import jp.wasabeef.glide.transformations.CropCircleTransformation;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;

public class MainActivity extends AppCompatActivity
        implements PlayManager.Callback, PlayManager.ProgressCallback{

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = MainActivity.class.getSimpleName();

    private DelegateAdapter mAdapter;
    private RecyclerView mRv;
    private CollapsingToolbarLayout mToolbarLayout;
    private Toolbar mToolbar;
    private ImageView mCoverIv, mThumbView;
    private FloatingActionButton mFab;

    PlayDetailFragment fragment = PlayDetailFragment.newInstance();

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == mFab.getId()) {
                PlayManager.getInstance(v.getContext()).dispatch();
            }/* else if (id == mMiniPanel.getId()) {
                showPlayDetail();
            } else if (id == mNextBtn.getId()) {
                PlayManager.getInstance(v.getContext()).next();
            }*/
        }
    };
    private boolean isIdle = true, isResumed = false;
    private RecyclerView.OnScrollListener mScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            isIdle = newState == RecyclerView.SCROLL_STATE_IDLE;
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

    private DelegateFilter mFilter = new DelegateFilter() {
        @Override
        public boolean accept(LayoutImpl impl) {
            if (impl instanceof SongDelegate) {
                SongDelegate songDelegate = (SongDelegate)impl;
                return songDelegate.getSource().equals(PlayManager.getInstance(MainActivity.this).getCurrentSong());
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbarLayout = (CollapsingToolbarLayout)findViewById(R.id.main_toolbar_layout);
        mToolbar = (Toolbar)findViewById(R.id.main_toolbar);
        mThumbView = (ImageView)findViewById(R.id.main_current_thumb);
        mCoverIv = (ImageView)findViewById(R.id.main_current_cover);
        mFab = (FloatingActionButton)findViewById(R.id.main_fab);

        mRv = (RecyclerView)findViewById(R.id.main_rv);
        mRv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DelegateAdapter(this);
        mRv.setAdapter(mAdapter);
        mRv.addOnScrollListener(mScrollListener);

        mFab.setOnClickListener(mClickListener);

        mToolbarLayout.setExpandedTitleColor(Color.WHITE);
        mToolbarLayout.setCollapsedTitleTextColor(Color.WHITE);

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        List<Song> songs = PlayManager.getInstance(this).getTotalList();
        if (songs != null) {
            setupPlayList(songs);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFab.setSelected(PlayManager.getInstance(this).isPlaying());
        Song song = PlayManager.getInstance(this).getCurrentSong();

        if (song != null) {
            int index = mAdapter.firstIndexOf(mFilter);
            mRv.getLayoutManager().scrollToPosition(index);
            showSong(song);
        }
        PlayManager.getInstance(this).registerCallback(this);
        PlayManager.getInstance(this).registerProgressCallback(this);
        isResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayManager.getInstance(this).unregisterCallback(this);
        PlayManager.getInstance(this).unregisterProgressCallback(this);
        isResumed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRv.removeOnScrollListener(mScrollListener);
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
                if (isIdle && isResumed) {
                    mRv.scrollToPosition(mAdapter.firstIndexOf(mFilter));
                }
                showSong(song);
                break;
            case PlayService.STATE_STARTED:
                mFab.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_PAUSED:
                mFab.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_STOPPED:
                mFab.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_COMPLETED:
                mFab.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
        }
    }

    @Override
    public void onPlayRuleChanged(Rule rule) {

    }

    private void showSong(Song song) {
        mToolbarLayout.setTitle(song.getTitle());
        mToolbar.setTitle(song.getTitle());
        mToolbar.setSubtitle(song.getArtistAlbum());
        File file = song.getCoverFile(this);
        if (file.exists()) {
            final int radius = (int)(getResources().getDisplayMetrics().density * 96);
            Glide.with(this).load(file).asBitmap().transform(new RoundedCornersTransformation(this, radius, 0))
                    .placeholder(R.mipmap.ic_launcher).into(new SimpleTarget<Bitmap>() {
                @Override
                public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                    mThumbView.setImageBitmap(resource);
                    if (resource != null) {
                        Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                            @Override
                            public void onGenerated(Palette palette) {
                                Palette.Swatch swatch = palette.getLightMutedSwatch();
                                if (swatch != null) {
                                    mToolbarLayout.setExpandedTitleColor(swatch.getTitleTextColor());
                                }
                            }
                        });
                    }
                }
            });
            Glide.with(this).load(file).asBitmap().animate(android.R.anim.fade_in)
                    .transform(new BlurTransformation(this)).into(mCoverIv);
        } else {
            mThumbView.setImageDrawable(null);
            mCoverIv.setImageDrawable(null);
        }
    }

    @Override
    public void onProgress(int progress, int duration) {
        //Log.v(TAG, "onProgress progress=" + progress + " duration=" + duration);
    }
}
