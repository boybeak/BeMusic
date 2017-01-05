package com.nulldreams.bemusic.activity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.fragment.RvFragment;
import com.nulldreams.bemusic.fragment.SongListFragment;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.ruler.Rule;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.service.PlayService;
import com.nulldreams.bemusic.widget.ProgressBar;
import com.nulldreams.bemusic.widget.RatioImageView;

import java.io.File;
import java.util.List;

import jp.wasabeef.glide.transformations.BlurTransformation;
import jp.wasabeef.glide.transformations.CropCircleTransformation;

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
    private ImageView mMiniThumbIv, mPlayPauseIv, mPreviousIv, mNextIv, mAvatarIv;
    private TextView mMiniTitleTv, mMiniArtistAlbumTv;
    private ProgressBar mMiniPb;
    private NavigationView mNavView;
    private RatioImageView mHeaderCover;

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

    private NavigationView.OnNavigationItemSelectedListener mNavListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            final int id = item.getItemId();
            if (id == R.id.action_github) {
                Intent it = new Intent(Intent.ACTION_VIEW);
                it.setData(Uri.parse("https://github.com/boybeak/BeMusic"));
                startActivity(it);
            }
            return false;
        }
    };
//    private PlayDetailFragment mDetailFragment = PlayDetailFragment.newInstance();
    private void showPlayDetail () {
        Intent it = new Intent(this, PlayDetailActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this, mMiniThumbIv, "thumb");
            startActivity(it, options.toBundle());
        } else {
            startActivity(it);
        }
        overridePendingTransition(R.anim.anim_bottom_in, 0);
    }

    private boolean isResumed;

    private ActionBarDrawerToggle mDrawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }

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
        mPreviousIv = (ImageView)findViewById(R.id.main_mini_action_previous);
        mNextIv = (ImageView)findViewById(R.id.main_mini_action_next);

        mMiniPb = (ProgressBar)findViewById(R.id.main_mini_progress_bar);

        mNavView = (NavigationView)findViewById(R.id.main_nav);
        mHeaderCover = (RatioImageView)mNavView.getHeaderView(0).findViewById(R.id.header_cover);
        mAvatarIv = (ImageView)mNavView.getHeaderView(0).findViewById(R.id.header_avatar);

        mVp.setAdapter(new VpAdapter(getSupportFragmentManager()));
        mTl.setupWithViewPager(mVp);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }*/

        setSupportActionBar(mTb);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, mTb, R.string.app_name, R.string.title_song_list);

        mMiniPanel.setOnClickListener(mClickListener);
        mPlayPauseIv.setOnClickListener(mClickListener);
        mPreviousIv.setOnClickListener(mClickListener);
        mNextIv.setOnClickListener(mClickListener);

        mNavView.setNavigationItemSelectedListener(mNavListener);

        Glide.with(this).load(R.drawable.avatar).asBitmap()
                .transform(new CropCircleTransformation(this)).into(mAvatarIv);
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
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            moveTaskToBack(true);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
        Song song = PlayManager.getInstance(this).getCurrentSong();
        showSong(song);

        PlayManager.getInstance(this).registerCallback(this);
        PlayManager.getInstance(this).registerProgressCallback(this);
        //PlayManager.getInstance(this).unlockScreenControls();
        isResumed = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayManager.getInstance(this).unregisterCallback(this);
        PlayManager.getInstance(this).unregisterProgressCallback(this);
        //PlayManager.getInstance(this).lockScreenControls();
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
            case PlayService.STATE_RELEASED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                mMiniPb.setProgress(0);
                break;
            case PlayService.STATE_ERROR:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                mMiniPb.setProgress(0);
                break;
        }
    }

    @Override
    public void onPlayRuleChanged(Rule rule) {

    }

    private void showSong(Song song) {
        if (song != null) {
            mMiniTitleTv.setText(song.getTitle());
            mMiniArtistAlbumTv.setText(song.getArtistAlbum());
            File file = song.getCoverFile(this);
            if (file.exists()) {
                Glide.with(this).load(file).asBitmap().animate(android.R.anim.fade_in).into(mMiniThumbIv);
                Glide.with(this).load(file).asBitmap().animate(android.R.anim.fade_in).transform(new BlurTransformation(this))
                        .into(mHeaderCover);
            }
        } else {
            mMiniTitleTv.setText(R.string.app_name);
            mMiniArtistAlbumTv.setText(R.string.text_github_name);
            Glide.with(this).load(R.drawable.avatar).asBitmap().animate(android.R.anim.fade_in).into(mMiniThumbIv);
            Glide.with(this).load(R.drawable.avatar).asBitmap().animate(android.R.anim.fade_in).transform(new BlurTransformation(this))
                    .into(mHeaderCover);
        }

    }

    @Override
    public void onProgress(int progress, int duration) {
        if (mMiniPb.getMax() != duration) {
            mMiniPb.setMax(duration);
        }
        mMiniPb.setProgress(progress);
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
