package com.nulldreams.bemusic.service;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.IntDef;
import android.util.Log;

import com.nulldreams.bemusic.model.Song;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class PlayService extends Service implements MediaPlayer.OnInfoListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnSeekCompleteListener{

    private static final String TAG = PlayService.class.getSimpleName();

    public static final int STATE_IDLE = 0, STATE_INITIALIZED = 1, STATE_PREPARING = 2,
            STATE_PREPARED = 3, STATE_STARTED = 4, STATE_PAUSED = 5, STATE_STOPPED = 6,
            STATE_COMPLETED = 7, STATE_RELEASED = 8, STATE_ERROR = -1;

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        setPlayerState(STATE_PREPARED);
        doStartPlayer();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {

    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {

    }

    @IntDef({STATE_IDLE, STATE_INITIALIZED, STATE_PREPARING,
            STATE_PREPARED, STATE_STARTED, STATE_PAUSED,
            STATE_STOPPED, STATE_COMPLETED, STATE_RELEASED,
            STATE_ERROR})
    @Retention(RetentionPolicy.SOURCE)
    public @interface State {}

    private @State int mState = STATE_IDLE;

    private MediaPlayer mPlayer = null;
    //private Song mCurrentSong;

    private PlayBinder mBinder = null;

    private PlayStateChangeListener mStateListener;

    @Override
    public IBinder onBind(Intent intent) {
        if (mBinder == null) {
            mBinder = new PlayBinder();
        }
        Log.v(TAG, "onBind");
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.v(TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v(TAG, "onStartCommand flags=" + flags + " startId=" + startId);
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy");
    }

    private void setPlayerState (@State int state) {
        if (mState == state) {
            return;
        }
        mState = state;
        if (mStateListener != null) {
            mStateListener.onStateChanged(mState);
        }
    }

    private void ensurePlayer () {
        if (mPlayer == null) {
            mPlayer = new MediaPlayer();
        }
        setPlayerState(STATE_IDLE);
        mPlayer.setOnInfoListener(this);
        mPlayer.setOnPreparedListener(this);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setOnErrorListener(this);
        mPlayer.setOnSeekCompleteListener(this);
    }

    public void startPlayer (String path) {
        releasePlayer();
        ensurePlayer();
        try {
            mPlayer.setDataSource(path);
            setPlayerState(STATE_INITIALIZED);
            mPlayer.prepareAsync();
            setPlayerState(STATE_PREPARING);
        } catch (IOException e) {
            e.printStackTrace();
            releasePlayer();
        }
    }

    private void doStartPlayer () {
        mPlayer.start();
        setPlayerState(STATE_STARTED);
    }

    public void resumePlayer () {
        if (isPaused()) {
            doStartPlayer();
        }
    }

    public void pausePlayer () {
        if (isStarted()) {
            mPlayer.pause();
            setPlayerState(STATE_PAUSED);
        }
    }

    public void releasePlayer () {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            setPlayerState(STATE_RELEASED);
        }
    }

    public boolean isStarted () {
        return mState == STATE_STARTED;
    }

    public boolean isPaused () {
        return mState == STATE_PAUSED;
    }

    public boolean isReleased () {
        return mState == STATE_RELEASED;
    }

    public @State int getState () {
        return mState;
    }

    private void showNotify (@State int state) {

    }

    public void setPlayStateChangeListener (PlayStateChangeListener listener) {
        mStateListener = listener;
    }

    public interface PlayStateChangeListener {
        void onStateChanged (@State int state);
    }

    public class PlayBinder extends Binder {
        public PlayService getService () {
            return PlayService.this;
        }
    }
}
