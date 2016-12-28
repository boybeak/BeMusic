package com.nulldreams.bemusic.manager;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.receiver.NoisyBroadcastReceiver;
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

    private NoisyBroadcastReceiver mNoisyReceiver = new NoisyBroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
                pause();
            }
        }

    };

    private AudioManager.OnAudioFocusChangeListener mAfListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                pause();
            }
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

    public List<Song> getTotalList () {
        return mTotalList;
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

    public void dispatch () {
        dispatch(mSong);
    }

    public void dispatch(final Song song) {

        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
            if (mService != null) {
                if (song.equals(mSong)) {
                    if (mService.isStarted()) {
                        pause();
                    } else {
                        resume();
                    }
                } else {
                    mSong = song;
                    mService.startPlayer(song.getPath());
                }
            }
        }

    }

    public void forward () {

    }

    public void previous () {

    }

    public void resume () {
        mService.resumePlayer();
    }

    public void pause () {
        mService.pausePlayer();
    }

    public boolean isPlaying () {
        return mService != null && mService.isStarted();
    }

    public Song getCurrentSong () {
        return mSong;
    }

    private int requestAudioFocus () {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.requestAudioFocus(
                mAfListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private int releaseAudioFocus () {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.abandonAudioFocus(mAfListener);
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

    private void registerNoisyReceiver () {
        mNoisyReceiver.register(mContext, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
        /*if (!mNoisyReceiver.isRegistered()) {
            mContext.registerReceiver(mNoisyReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            mNoisyReceiver.setRegistered(true);
        }*/
    }

    private void unregisterNoisyReceiver () {
        mNoisyReceiver.unregister(mContext);
        /*if (mNoisyReceiver.isRegistered()) {
            mContext.unregisterReceiver(mNoisyReceiver);
            mNoisyReceiver.setRegistered(false);
        }*/
    }

    @Override
    public void onStateChanged(@PlayService.State int state) {

        switch (state) {
            case PlayService.STATE_STARTED:
                registerNoisyReceiver();
                break;
            case PlayService.STATE_PAUSED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                break;
            case PlayService.STATE_ERROR:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                break;
            case PlayService.STATE_STOPPED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                break;
            case PlayService.STATE_COMPLETED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                break;
            case PlayService.STATE_RELEASED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                break;
        }
        for (Callback callback : mCallbacks) {
            callback.onPlayStateChanged(state, mSong);
        }
        notification(state);
    }

    private void notification (@PlayService.State int state) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.addAction(new android.support.v4.app.NotificationCompat.Action(R.drawable.ic_play_pause_sel, "play", PendingIntent.getBroadcast(
                mContext, 100, new Intent("play_pause"), PendingIntent.FLAG_ONE_SHOT
        )));
        builder.setContentTitle(mSong.getTitle());
        builder.setContentText(mSong.getArtist() + " - " + mSong.getAlbum());
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);
        boolean onGoing = isPlaying();
        builder.setOngoing(onGoing);
        builder.setAutoCancel(!onGoing);
        final Notification notification = builder.build();
        Glide.with(mContext).load(mSong.getCoverFile(mContext)).asBitmap().into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                builder.setLargeIcon(resource);
                mService.startForeground(1, notification);
            }
        });

    }

    public interface Callback {
        void onPlayListPrepared (List<Song> songs);
        void onPlayStateChanged (@PlayService.State int state, Song song);
    }

}
