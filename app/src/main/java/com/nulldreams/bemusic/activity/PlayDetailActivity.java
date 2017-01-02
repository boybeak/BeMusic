package com.nulldreams.bemusic.activity;

import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.manager.PlayManager;
import com.nulldreams.bemusic.manager.ruler.Rule;
import com.nulldreams.bemusic.manager.ruler.Rulers;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;
import com.nulldreams.bemusic.utils.MediaUtils;

import java.io.File;
import java.util.List;

public class PlayDetailActivity extends AppCompatActivity implements PlayManager.Callback, PlayManager.ProgressCallback {

    private TextView mTitleTv, mArtistTv, mAlbumTv, mPositionTv, mDurationTv;
    private ImageView mThumbIv, mPlayPauseIv, mPreviousIv, mNextIv, mRuleIv, mPlayListIv;
    private SeekBar mSeekBar;
    private Toolbar mToolbar;

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

            }
        }
    };

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().requestFeature(Window.FEATURE_CONTENT_TRANSITIONS);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_play_detail);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
            );
        }

        mToolbar = (Toolbar)findViewById(R.id.play_detail_tool_bar);

        mTitleTv = (TextView)findViewById(R.id.play_detail_title);
        mArtistTv = (TextView)findViewById(R.id.play_detail_artist);
        mAlbumTv = (TextView)findViewById(R.id.play_detail_album);
        mPositionTv = (TextView)findViewById(R.id.play_detail_position);
        mDurationTv = (TextView)findViewById(R.id.play_detail_duration);

        mThumbIv = (ImageView)findViewById(R.id.play_detail_thumb);
        mSeekBar = (SeekBar)findViewById(R.id.play_detail_seek_bar);
        mPlayPauseIv = (ImageView)findViewById(R.id.play_detail_play_pause);
        mPreviousIv = (ImageView)findViewById(R.id.play_detail_previous);
        mNextIv = (ImageView)findViewById(R.id.play_detail_next);
        mRuleIv = (ImageView)findViewById(R.id.play_detail_rule_change);
        mPlayListIv = (ImageView)findViewById(R.id.play_detail_play_list);

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
        if (song != null) {
            mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
            onPlayRuleChanged(PlayManager.getInstance(this).getRule());
            showSong(song);
        }
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
                break;
            case PlayService.STATE_ERROR:
                mPlayPauseIv.setSelected(PlayManager.getInstance(this).isPlaying());
                break;
        }
    }

    @Override
    public void onPlayRuleChanged(Rule rule) {
        if (rule == Rulers.RULER_LIST_LOOP) {
            mRuleIv.setImageResource(R.drawable.ic_repeat);
        } else if (rule == Rulers.RULER_SINGLE_LOOP) {
            mRuleIv.setImageResource(R.drawable.ic_repeat_once);
        } else if (rule == Rulers.RULER_RANDOM) {
            mRuleIv.setImageResource(R.drawable.ic_shuffle);
        }
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
        mTitleTv.setText(song.getTitle());
        mArtistTv.setText(song.getArtist());
        mAlbumTv.setText(song.getAlbum());
        File file = song.getCoverFile(this);
        if (file.exists()) {
            Glide.with(this).load(file).into(mThumbIv);
        }
    }
}
