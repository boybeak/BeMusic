package com.nulldreams.bemusic.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.DelegateImpl;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.adapter.SongDelegate;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.ruler.Rule;
import com.nulldreams.media.manager.ruler.Rulers;
import com.nulldreams.media.model.Album;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.service.PlayService;
import com.nulldreams.media.utils.MediaUtils;

import java.io.File;
import java.util.List;

public class PlayDetailActivity extends AppCompatActivity implements PlayManager.Callback, PlayManager.ProgressCallback {

    private static final String TAG = PlayDetailActivity.class.getSimpleName();

    private TextView mTitleTv, mArtistTv, mAlbumTv, mPositionTv, mDurationTv;
    private ImageView mThumbIv, mPlayPauseIv, mPreviousIv, mNextIv, mRuleIv, mPlayListIv;
    private View mPanel;
    private SeekBar mSeekBar;
    private Toolbar mToolbar;
    /*private RecyclerView mQuickRv;
    private DelegateAdapter mAdapter;*/

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
            } else if (id == mRuleIv.getId()) {
                PlayManager manager = PlayManager.getInstance(v.getContext());
                Rule rule = manager.getRule();
                if (rule == Rulers.RULER_LIST_LOOP) {
                    manager.setRule(Rulers.RULER_SINGLE_LOOP);
                } else if (rule == Rulers.RULER_SINGLE_LOOP) {
                    manager.setRule(Rulers.RULER_RANDOM);
                } else if (rule == Rulers.RULER_RANDOM) {
                    manager.setRule(Rulers.RULER_LIST_LOOP);
                }
            } else if (id == mPlayListIv.getId()) {
                /*if (mQuickRv.getVisibility() == View.VISIBLE) {
                    hideQuickList();
                } else {*/
                    showQuickList();
//                }
            }
        }
    };

    private void showQuickList () {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        RecyclerView rv = new RecyclerView(this);
        rv.setLayoutManager(new LinearLayoutManager(this));
        DelegateAdapter adapter = new DelegateAdapter(this);
        adapter.addAll(PlayManager.getInstance(this).getTotalList(), new DelegateParser<Song>() {
            @Override
            public DelegateImpl parse(Song data) {
                return new SongDelegate(data);
            }
        });
        rv.setAdapter(adapter);
        dialog.setContentView(rv);
        dialog.show();
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // get the center for the clipping circle
            int cx = (mQuickRv.getLeft() + mQuickRv.getRight()) / 2;
            int cy = (mQuickRv.getTop() + mQuickRv.getBottom()) / 2;

            // get the final radius for the clipping circle
            int finalRadius = Math.max(mQuickRv.getWidth(), mQuickRv.getHeight());

            // create the animator for this view (the start radius is zero)
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(mQuickRv, cx, cy, 0, finalRadius);

            // make the view visible and start the animation
            mQuickRv.setVisibility(View.VISIBLE);
            anim.start();
        } else {
            mQuickRv.setVisibility(View.VISIBLE);
        }*/
    }

    private void hideQuickList () {
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // get the center for the clipping circle
            int cx = (mQuickRv.getLeft() + mQuickRv.getRight()) / 2;
            int cy = (mQuickRv.getTop() + mQuickRv.getBottom()) / 2;

            Log.v(TAG, "hideQuickList cx=" + cx + " cy=" + cy);

            // get the initial radius for the clipping circle
            int initialRadius = mQuickRv.getWidth();

            // create the animation (the final radius is zero)
            Animator anim =
                    ViewAnimationUtils.createCircularReveal(mQuickRv, cx, cy, initialRadius, 0);

            // make the view invisible when the animation is done
            anim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    mQuickRv.setVisibility(View.INVISIBLE);
                }
            });

            // start the animation
            anim.start();
        } else {
            mQuickRv.setVisibility(View.GONE);
        }*/
    }

    private boolean isSeeking = false;
    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            mPositionTv.setText(MediaUtils.formatTime(progress));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            isSeeking = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            isSeeking = false;
            PlayManager.getInstance(seekBar.getContext()).seekTo(seekBar.getProgress());
        }
    };

    private int mLastColor = 0x00000000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_detail);

        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }*/

        mToolbar = (Toolbar)findViewById(R.id.play_detail_tool_bar);

        mTitleTv = (TextView)findViewById(R.id.play_detail_title);
        mArtistTv = (TextView)findViewById(R.id.play_detail_artist);
        mAlbumTv = (TextView)findViewById(R.id.play_detail_album);
        mPositionTv = (TextView)findViewById(R.id.play_detail_position);
        mDurationTv = (TextView)findViewById(R.id.play_detail_duration);

        mPanel = findViewById(R.id.play_detail_panel);

        mThumbIv = (ImageView)findViewById(R.id.play_detail_thumb);
        mSeekBar = (SeekBar)findViewById(R.id.play_detail_seek_bar);
        mPlayPauseIv = (ImageView)findViewById(R.id.play_detail_play_pause);
        mPreviousIv = (ImageView)findViewById(R.id.play_detail_previous);
        mNextIv = (ImageView)findViewById(R.id.play_detail_next);
        mRuleIv = (ImageView)findViewById(R.id.play_detail_rule_change);
        mPlayListIv = (ImageView)findViewById(R.id.play_detail_play_list);

        /*mQuickRv = (RecyclerView)findViewById(R.id.play_detail_quick_list);
        mQuickRv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DelegateAdapter(this);
        List<Song> songs = PlayManager.getInstance(this).getTotalList();
        mAdapter.addAll(songs, new DelegateParser<Song>() {
            @Override
            public DelegateImpl parse(Song data) {
                return new SongDelegate(data);
            }
        });
        mQuickRv.setAdapter(mAdapter);*/

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);

        mPlayPauseIv.setOnClickListener(mClickListener);
        mPreviousIv.setOnClickListener(mClickListener);
        mNextIv.setOnClickListener(mClickListener);
        mRuleIv.setOnClickListener(mClickListener);
        mPlayListIv.setOnClickListener(mClickListener);
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);

        Song song = PlayManager.getInstance(this).getCurrentSong();
        mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
        onPlayRuleChanged(PlayManager.getInstance(this).getRule());
        showSong(song);
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    @Override
    public void onBackPressed() {
        /*if (mQuickRv.getVisibility() == View.VISIBLE) {
            hideQuickList();
            return;
        }*/
        super.onBackPressed();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.menu_context_play_detail, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save_cover:
                break;
            case R.id.action_share_cover:
                //Intent it = new Intent(Intent.ACTION_)
                break;
            case R.id.action_set_as_wallpaper:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        PlayManager.getInstance(this).registerCallback(this);
        PlayManager.getInstance(this).registerProgressCallback(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PlayManager.getInstance(this).unregisterCallback(this);
        PlayManager.getInstance(this).unregisterProgressCallback(this);
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
            case PlayService.STATE_COMPLETED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_STOPPED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
            case PlayService.STATE_RELEASED:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                mSeekBar.setProgress(0);
                break;
            case PlayService.STATE_ERROR:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                mSeekBar.setProgress(0);
                break;
        }
    }

    @Override
    public void onPlayRuleChanged(Rule rule) {
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int id = 0;
        if (rule == Rulers.RULER_LIST_LOOP) {
            mRuleIv.setImageResource(R.drawable.ic_repeat);
            id = 0;
        } else if (rule == Rulers.RULER_SINGLE_LOOP) {
            mRuleIv.setImageResource(R.drawable.ic_repeat_once);
            id = 1;
        } else if (rule == Rulers.RULER_RANDOM) {
            mRuleIv.setImageResource(R.drawable.ic_shuffle);
            id = 2;
        }
        editor.putInt("rule", id);
        editor.commit();
    }

    @Override
    public void onProgress(int progress, int duration) {
        if (isSeeking) {
            return;
        }
        if (mSeekBar.getMax() != duration) {
            mSeekBar.setMax(duration);
            mDurationTv.setText(MediaUtils.formatTime(duration));
        }
        mSeekBar.setProgress(progress);
        mPositionTv.setText(MediaUtils.formatTime(progress));
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(0, R.anim.anim_bottom_out);
    }

    private void showSong (Song song) {
        if (song == null) {
            mTitleTv.setText(R.string.app_name);
            mArtistTv.setText(R.string.text_github_name);
            mAlbumTv.setText(R.string.text_github_name);
            Glide.with(this).load(R.drawable.avatar).animate(android.R.anim.fade_in).into(mThumbIv);
            unregisterForContextMenu(mThumbIv);
        } else {
            mTitleTv.setText(song.getTitle());
            mArtistTv.setText(song.getArtist());
            mAlbumTv.setText(song.getAlbum());
            File file = song.getCoverFile(this);
            if (file.exists()) {
                registerForContextMenu(mThumbIv);
                Glide.with(this).load(file).asBitmap().animate(android.R.anim.fade_in)
                        .placeholder(R.mipmap.ic_launcher).into(new SimpleTarget<Bitmap>() {
                            @Override
                            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                                mThumbIv.setImageBitmap(resource);
                                Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                                    @Override
                                    public void onGenerated(Palette palette) {
                                        Palette.Swatch swatch = palette.getDarkMutedSwatch();
                                        if (swatch != null) {
                                            animColor(swatch.getRgb());
                                        }
                                    }
                                });
                            }
                        });
            }
        }

    }

    private void animColor (int newColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ObjectAnimator animator = ObjectAnimator.ofArgb(mPanel, "backgroundColor", mLastColor, newColor);
            animator.start();
        } else {
            mPanel.setBackgroundColor (newColor);
        }
        mLastColor = newColor;
    }
}
