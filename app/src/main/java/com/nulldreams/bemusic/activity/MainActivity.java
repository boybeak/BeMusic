package com.nulldreams.bemusic.activity;

import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateFilter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.DelegateImpl;
import com.nulldreams.adapter.impl.LayoutImpl;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.adapter.SongDelegate;
import com.nulldreams.bemusic.fragment.PlayDetailFragment;
import com.nulldreams.bemusic.fragment.RvFragment;
import com.nulldreams.bemusic.fragment.SongListFragment;
import com.nulldreams.bemusic.manager.PlayManager;
import com.nulldreams.bemusic.manager.ruler.Rule;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;

import java.io.File;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements PlayManager.Callback, PlayManager.ProgressCallback{

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = MainActivity.class.getSimpleName();

    private DrawerLayout mDrawerLayout;
    private CoordinatorLayout mCoorLayout;
    private Toolbar mTb;
    private ViewPager mVp;
    private TabLayout mTl;
    private View mMiniPanel;
    private ImageView mMiniThumbIv, mPlayPauseIv, mPreviousIv, mNextIv;
    private TextView mMiniTitleTv, mMiniArtistAlbumTv;

    private int mLength = 1;
    private RvFragment[] mFragmentArray = null;

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == mPlayPauseIv.getId()) {
                PlayManager.getInstance(v.getContext()).dispatch();
            } else if (id == mMiniPanel.getId()) {
                showPlayDetail();
            } else if (id == mPreviousIv.getId()) {
                PlayManager.getInstance(v.getContext()).previous();
            } else if (id == mNextIv.getId()) {
                PlayManager.getInstance(v.getContext()).next();
            }
        }
    };
    private PlayDetailFragment mDetailFragment = PlayDetailFragment.newInstance();
    private void showPlayDetail () {
        FragmentManager manager = getSupportFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.add(R.id.main_content, mDetailFragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }

    private boolean isResumed;

    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFragmentArray = new RvFragment[mLength];
        mFragmentArray[0] = new SongListFragment();

        mDrawerLayout = (DrawerLayout)findViewById(R.id.main_drawer);
        mCoorLayout = (CoordinatorLayout)findViewById(R.id.main_coordinator_layout);
        mTb = (Toolbar)findViewById(R.id.main_tool_bar);
        mVp = (ViewPager)findViewById(R.id.main_view_pager);
        mTl = (TabLayout)findViewById(R.id.main_tab_layout);

        mMiniPanel = findViewById(R.id.main_mini_panel);
        mMiniThumbIv = (ImageView)findViewById(R.id.main_mini_thumb);
        mMiniTitleTv = (TextView)findViewById(R.id.main_mini_title);
        mMiniArtistAlbumTv = (TextView)findViewById(R.id.main_mini_artist_album);

        mPlayPauseIv = (ImageView)findViewById(R.id.main_mini_action_play_pause);
        mPreviousIv = (ImageView)findViewById(R.id.main_mini_action_previouse);
        mNextIv = (ImageView)findViewById(R.id.main_mini_action_next);

        mVp.setAdapter(new VpAdapter(getSupportFragmentManager()));
        mTl.setupWithViewPager(mVp);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            /*getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );*/
            getWindow().getDecorView().setFitsSystemWindows(true);
            mDrawerLayout.setFitsSystemWindows(true);
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        setSupportActionBar(mTb);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mTb, R.string.app_name, R.string.title_song_list);

        mMiniPanel.setOnClickListener(mClickListener);
        mPlayPauseIv.setOnClickListener(mClickListener);
        mPreviousIv.setOnClickListener(mClickListener);
        mNextIv.setOnClickListener(mClickListener);

    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
        mDrawerLayout.addDrawerListener(mDrawerToggle);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
        Song song = PlayManager.getInstance(this).getCurrentSong();
        if (song != null) {
            showSong(song);
        }

        PlayManager.getInstance(this).registerCallback(this);
        PlayManager.getInstance(this).registerProgressCallback(this);
        PlayManager.getInstance(this).unlockScreenControls();
        isResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayManager.getInstance(this).unregisterCallback(this);
        PlayManager.getInstance(this).unregisterProgressCallback(this);
        PlayManager.getInstance(this).lockScreenControls();
        isResumed = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDrawerLayout.removeDrawerListener(mDrawerToggle);
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
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_PAUSED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_STOPPED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_COMPLETED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
        }
    }

    @Override
    public void onPlayRuleChanged(Rule rule) {

    }

    private void showSong(Song song) {
        mMiniTitleTv.setText(song.getTitle());
        mMiniArtistAlbumTv.setText(song.getArtistAlbum());
        File file = song.getCoverFile(this);
        if (file.exists()) {
            Glide.with(this).load(file).asBitmap().animate(android.R.anim.fade_in).into(mMiniThumbIv);
        } else {

        }
    }

    @Override
    public void onProgress(int progress, int duration) {
        //mProgressDurationTv.setText(MediaUtils.formatTime(progress) + "/" + MediaUtils.formatTime(duration));
        //mProgressBar.setProgress(progress);
        //Log.v(TAG, "onProgress progress=" + progress + " duration=" + duration);
    }

    private class VpAdapter extends FragmentStatePagerAdapter {

        public VpAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if (mFragmentArray[0] == null) {
                        mFragmentArray[0] = new SongListFragment();
                    }
                    return mFragmentArray[0];
            }
            return null;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            RvFragment fragment = (RvFragment)super.instantiateItem(container, position);
            mFragmentArray[position] = fragment;
            return fragment;
        }

        @Override
        public int getCount() {
            return mFragmentArray.length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentArray[position].getTitle(MainActivity.this);
        }
    }
}
