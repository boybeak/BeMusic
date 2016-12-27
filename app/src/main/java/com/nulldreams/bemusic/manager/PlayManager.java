package com.nulldreams.bemusic.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;

import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;
import com.nulldreams.bemusic.utils.MediaUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by boybe on 2016/12/27.
 */

public class PlayManager implements PlayService.PlayStateChangeListener{

    private static PlayManager sManager = null;

    public synchronized static PlayManager getInstance (Context context) {
        if (sManager == null) {
            sManager = new PlayManager(context.getApplicationContext());
        }
        return sManager;
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((PlayService.PlayBinder)service).getService();
            mService.setPlayStateChangeListener(PlayManager.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService.setPlayStateChangeListener(null);
            mService = null;
        }
    };

    private List<Callback> mCallbacks = new ArrayList<>();

    private Context mContext;

    private List<Song> mTotalList = null;
    private Song mSong = null;
    private PlayService mService;

    private PlayManager (Context context) {
        mContext = context;
        new AsyncTask<Context, Integer, List<Song>>() {

            @Override
            protected List<Song> doInBackground(Context... params) {
                Context context = params[0];
                List<Song> songs = MediaUtils.getAudioList(context);
                for (Song song : songs) {
                    File file = song.getCoverFile(context);
                    if (file.exists()) {
                        continue;
                    }
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    byte[] rawArt;
                    Bitmap art;
                    BitmapFactory.Options bfo=new BitmapFactory.Options();

                    mmr.setDataSource(mContext, Uri.parse(song.getPath()));
                    rawArt = mmr.getEmbeddedPicture();

                    if (null != rawArt) {
                        art = BitmapFactory.decodeByteArray(rawArt, 0, rawArt.length, bfo);
                        if (art != null) {
                            if (!file.getParentFile().exists()) {
                                file.getParentFile().mkdirs();
                            }
                            try {
                                FileOutputStream outputStream = new FileOutputStream(file);
                                art.compress(Bitmap.CompressFormat.JPEG, 75, outputStream);
                                outputStream.flush();
                                outputStream.close();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                return songs;
            }

            @Override
            protected void onPostExecute(List<Song> songs) {
                mTotalList = songs;
                bindPlayService();
                startPlayService();
                for (Callback callback : mCallbacks) {
                    callback.onPlayListPrepared(songs);
                }
                /*mAdapter.addAll(songs, new DelegateParser<Song>() {
                    @Override
                    public DelegateImpl parse(Song data) {
                        return new SongDelegate(data);
                    }
                });
                mAdapter.notifyDataSetChanged();*/
            }
        }.execute(mContext);
    }

    private void bindPlayService () {
        mContext.bindService(new Intent(mContext, PlayService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    private void unbindPlayService () {
        mContext.unbindService(mConnection);
    }
    private void startPlayService () {
        mContext.startService(new Intent(mContext, PlayService.class));
    }
    private void stopPlayService () {
        mContext.stopService(new Intent(mContext, PlayService.class));
    }

    public void dispatch(Song song) {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {

            }
        }, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_LOSS);
        if (mService != null) {
            if (song.equals(mSong)) {
                if (mService.isStarted()) {
                    mService.pausePlayer();
                } else {
                    mService.resumePlayer();
                }
            } else {
                mService.startPlayer(song.getPath());
                mSong = song;
            }
        }

    }

    public void registerCallback (Callback callback) {
        if (mCallbacks.contains(callback)) {
            return;
        }
        mCallbacks.add(callback);
    }

    public void unregisterCallback (Callback callback) {
        if (mCallbacks.contains(callback)) {
            mCallbacks.remove(callback);
        }
    }

    @Override
    public void onStateChanged(@PlayService.State int state) {
        for (Callback callback : mCallbacks) {
            callback.onPlayStateChanged(state, mSong);
        }
    }

    public interface Callback {
        void onPlayListPrepared (List<Song> songs);
        void onPlayStateChanged (@PlayService.State int state, Song song);
    }

}
