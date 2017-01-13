package com.nulldreams.bemusic.activity;

import android.app.ActivityOptions;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Pair;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nulldreams.bemusic.Intents;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.fragment.AlbumListFragment;
import com.nulldreams.bemusic.fragment.RvFragment;
import com.nulldreams.bemusic.fragment.SongListFragment;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.ruler.Rule;
import com.nulldreams.media.model.Album;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.service.PlayService;
import com.nulldreams.bemusic.widget.ProgressBar;
import com.nulldreams.bemusic.widget.RatioImageView;
import com.nulldreams.media.utils.MediaUtils;

import java.io.File;
import java.util.Collections;
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
    private View mMiniPanel, mSongInfoLayout;
    private ImageView mMiniThumbIv, mPlayPauseIv, mPreviousIv, mNextIv, mAvatarIv;
    private TextView mMiniTitleTv, mMiniArtistAlbumTv;
    private ProgressBar mMiniPb;
    private NavigationView mNavView;
    private RatioImageView mHeaderCover;

    private int mLength = 2;
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
            } else if (id == mHeaderCover.getId()) {
                Intents.openUrl(MainActivity.this, "https://github.com/boybeak");
            }
        }
    };

    private NavigationView.OnNavigationItemSelectedListener mNavListener = new NavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            final int id = item.getItemId();
            if (id == R.id.action_github) {
                Intents.openUrl(MainActivity.this, "https://github.com/boybeak/BeMusic");
            } else if (id == R.id.action_star_me) {
                Intents.viewMyAppOnStore(MainActivity.this);
            } else if (id == R.id.action_help) {
                final String message = getString(R.string.text_help);
                TextView messageTv = new TextView(MainActivity.this);
                final int padding = (int)(getResources().getDisplayMetrics().density * 24);
                messageTv.setPadding(padding, padding, padding, padding);
                messageTv.setAutoLinkMask(Linkify.WEB_URLS);
                messageTv.setText(message);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(R.string.title_menu_help)
                        .setView(messageTv)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
            }
            mDrawerLayout.closeDrawers();
            return true;
        }
    };
//    private PlayDetailFragment mDetailFragment = PlayDetailFragment.newInstance();
    private void showPlayDetail () {
        Intent it = new Intent(this, PlayDetailActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Pair<View, String> thumb = new Pair<View, String>(mMiniThumbIv, getString(R.string.translation_thumb));
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(this, thumb);
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
        mFragmentArray[1] = new AlbumListFragment();

        mDrawerLayout = (DrawerLayout)findViewById(R.id.main_drawer);
        mCoorLayout = (CoordinatorLayout)findViewById(R.id.main_coordinator_layout);
        mTb = (Toolbar)findViewById(R.id.main_tool_bar);
        mVp = (ViewPager)findViewById(R.id.main_view_pager);
        mTl = (TabLayout)findViewById(R.id.main_tab_layout);

        mMiniPanel = findViewById(R.id.main_mini_panel);
        mMiniThumbIv = (ImageView)findViewById(R.id.main_mini_thumb);
        mSongInfoLayout = findViewById(R.id.main_mini_song_info_layout);
        mMiniTitleTv = (TextView)findViewById(R.id.main_mini_title);
        mMiniArtistAlbumTv = (TextView)findViewById(R.id.main_mini_artist_album);

        mPlayPauseIv = (ImageView)findViewById(R.id.main_mini_action_play_pause);
        mPreviousIv = (ImageView)findViewById(R.id.main_mini_action_previous);
        mNextIv = (ImageView)findViewById(R.id.main_mini_action_next);

        mMiniPb = (ProgressBar)findViewById(R.id.main_mini_progress_bar);

        mNavView = (NavigationView)findViewById(R.id.main_nav);
        mHeaderCover = (RatioImageView)mNavView.getHeaderView(0).findViewById(R.id.header_cover);
        mAvatarIv = (ImageView)mNavView.getHeaderView(0).findViewById(R.id.header_avatar);
        mHeaderCover.setOnClickListener(mClickListener);

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

        MediaUtils.getAlbumList(this);
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
        if (mDrawerLayout.isDrawerOpen(mNavView)) {
            mDrawerLayout.closeDrawers();
            return;
        }
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
    public void onAlbumListPrepared(List<Album> albums) {

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
    public void onShutdown() {

    }

    @Override
    public void onPlayRuleChanged(Rule rule) {

    }

    private void showSong(Song song) {
        if (song != null) {
            mMiniTitleTv.setText(song.getTitle());
            mMiniArtistAlbumTv.setText(song.getArtistAlbum());
            Album album = song.getAlbumObj();
            if (album == null) {
                album = PlayManager.getInstance(this).getAlbum(song.getAlbumId());
            }
            if (album != null) {
                Glide.with(this).load(album.getAlbumArt()).asBitmap().placeholder(R.mipmap.ic_launcher).animate(android.R.anim.fade_in).into(mMiniThumbIv);
                Glide.with(this).load(album.getAlbumArt()).asBitmap().animate(android.R.anim.fade_in).transform(new BlurTransformation(this))
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
                case 1:
                    if (mFragmentArray[1] == null) {
                        mFragmentArray[1] = new AlbumListFragment();
                    }
                    return mFragmentArray[1];
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
