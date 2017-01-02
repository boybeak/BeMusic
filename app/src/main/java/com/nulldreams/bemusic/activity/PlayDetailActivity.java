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
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;

import java.io.File;
import java.util.List;

public class PlayDetailActivity extends AppCompatActivity implements PlayManager.Callback, PlayManager.ProgressCallback {

    private TextView mTitleTv, mArtistTv, mAlbumTv;
    private ImageView mThumbIv, mPlayPauseIv, mPreviousIv, mNextIv;
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
            }
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
        mThumbIv = (ImageView)findViewById(R.id.play_detail_thumb);
        mSeekBar = (SeekBar)findViewById(R.id.play_detail_seek_bar);
        mPlayPauseIv = (ImageView)findViewById(R.id.play_detail_play_pause);
        mPreviousIv = (ImageView)findViewById(R.id.play_detail_previous);
        mNextIv = (ImageView)findViewById(R.id.play_detail_next);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(null);

        mPlayPauseIv.setOnClickListener(mClickListener);
        mPreviousIv.setOnClickListener(mClickListener);
        mNextIv.setOnClickListener(mClickListener);

        Song song = PlayManager.getInstance(this).getCurrentSong();
        if (song != null) {
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

    }

    @Override
    public void onProgress(int progress, int duration) {
        if (mSeekBar.getMax() != duration) {
            mSeekBar.setMax(duration);
        }
        mSeekBar.setProgress(progress);
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
