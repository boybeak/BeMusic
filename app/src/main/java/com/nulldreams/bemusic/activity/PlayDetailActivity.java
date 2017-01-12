package com.nulldreams.bemusic.activity;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialog;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.DelegateImpl;
import com.nulldreams.bemusic.Intents;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class PlayDetailActivity extends AppCompatActivity implements PlayManager.Callback, PlayManager.ProgressCallback {

    private static final String TAG = PlayDetailActivity.class.getSimpleName();

    private TextView mTitleTv, mArtistTv, mAlbumTv, mPositionTv, mDurationTv;
    private ImageView mThumbIv, mPlayPauseIv, mPreviousIv, mNextIv, mRuleIv, mPlayListIv;
    private View mPanel;
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
                showQuickList();
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

        final int width = getResources().getDisplayMetrics().widthPixels;
        final int height = getResources().getDisplayMetrics().heightPixels;
        final int size = Math.min(width, height);
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mThumbIv.getLayoutParams();
        if (params == null) {
            params = new RelativeLayout.LayoutParams(size, size);
        } else {
            params.width = size;
            params.height = size;
        }
        mThumbIv.setLayoutParams(params);

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
                Log.v(TAG, "onContextItemSelected action_save_cover");
                if (PackageManager.PERMISSION_GRANTED !=
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.v(TAG, "onContextItemSelected action_save_cover check permission");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                } else {
                    Log.v(TAG, "onContextItemSelected action_save_cover doSave");
                    saveAlbumCover();
                }
                break;
            case R.id.action_share_cover:
                Song song = PlayManager.getInstance(this).getCurrentSong();
                if (song != null) {
                    Album album = song.getAlbumObj();
                    if (album == null) {
                        return true;
                    }
                    Intents.shareImage(PlayDetailActivity.this, getResources().getString(R.string.title_dialog_send_to), album.getAlbumArt());
                }
                break;
            case R.id.action_set_as_wallpaper:
                setWallpaper();
                break;
            case R.id.action_set_as_ringtone:
                setRingtone();
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.v(TAG, "onRequestPermissionsResult " + grantResults[0]);
        if (requestCode == 100) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                saveAlbumCover();
            }
        }
    }

    private void saveAlbumCover() {
        Song song = PlayManager.getInstance(this).getCurrentSong();
        if (song != null) {
            Album album = song.getAlbumObj();
            if (album == null) {
                return;
            }
            File source = new File(album.getAlbumArt());
            if (source.exists()) {
                try {
                    File dest = new File (
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                            song.getTitle() + "-" + song.getArtist() + "-" + song.getArtist() + ".jpg");
                    FileInputStream inputStream = new FileInputStream(source);
                    FileOutputStream outputStream = new FileOutputStream(dest);
                    byte[] buffer = new byte[1024];
                    while (inputStream.read(buffer) != -1) {
                        outputStream.write(buffer);
                    }
                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();
                    Toast.makeText(this, getString(R.string.text_album_cover_saved_at, dest.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(this, R.string.text_album_cover_saved_failed, Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void setRingtone () {
        Song song = PlayManager.getInstance(this).getCurrentSong();
        if (song != null) {
            RingtoneManager.setActualDefaultRingtoneUri(
                    this,
                    RingtoneManager.TYPE_RINGTONE,
                    Uri.fromFile(new File(song.getPath())));
        }
    }

    private void setWallpaper () {
        WallpaperManager manager = WallpaperManager.getInstance(this);
        boolean canSetWallpaper = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canSetWallpaper &= manager.isWallpaperSupported();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            canSetWallpaper &= manager.isSetWallpaperAllowed();
        }
        if (canSetWallpaper) {
            Song song = PlayManager.getInstance(this).getCurrentSong();
            if (song != null) {
                Album album = song.getAlbumObj();
                if (album == null) {
                    return;
                }
                File source = new File(album.getAlbumArt());
                if (source.exists()) {
                    Bitmap bmp = BitmapFactory.decodeFile(source.getAbsolutePath());
                    try {
                        manager.setBitmap(bmp);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        bmp.recycle();
                    }
                }
            }
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
    public void onAlbumListPrepared(List<Album> albums) {

    }

    @Override
    public void onPlayStateChanged(@PlayService.State int state, Song song) {
        switch (state) {
            case PlayService.STATE_INITIALIZED:
                closeContextMenu();
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
    public void onShutdown() {

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
            mSeekBar.setEnabled(false);
            Glide.with(this).load(R.drawable.avatar).animate(android.R.anim.fade_in).into(mThumbIv);
            unregisterForContextMenu(mThumbIv);
        } else {
            mTitleTv.setText(song.getTitle());
            mArtistTv.setText(song.getArtist());
            mAlbumTv.setText(song.getAlbum());
            mSeekBar.setEnabled(true);
            Album album = song.getAlbumObj();
            if (album == null) {
                album = PlayManager.getInstance(this).getAlbum(song.getAlbumId());
            }
            if (album != null) {
                String albumArt = album.getAlbumArt();
                if (!TextUtils.isEmpty(albumArt)) {
                    File file = new File(albumArt);
                    if (!TextUtils.isEmpty(albumArt) && file.exists()) {
                        registerForContextMenu(mThumbIv);
                    } else {
                        unregisterForContextMenu(mThumbIv);
                    }
                    Glide.with(this).load(albumArt).asBitmap().placeholder(R.mipmap.ic_launcher).animate(android.R.anim.fade_in)
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
            } else {
                Glide.with(this).load(R.drawable.avatar).animate(android.R.anim.fade_in).into(mThumbIv);
                unregisterForContextMenu(mThumbIv);
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
