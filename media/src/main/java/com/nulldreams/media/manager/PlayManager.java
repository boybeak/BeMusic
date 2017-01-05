package com.nulldreams.media.manager;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.RemoteControlClient;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.nulldreams.media.manager.notification.NotificationAgent;
import com.nulldreams.media.manager.ruler.Rule;
import com.nulldreams.media.manager.ruler.Rulers;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.receiver.LockControlReceiver;
import com.nulldreams.media.receiver.SimpleBroadcastReceiver;
import com.nulldreams.media.service.PlayService;
import com.nulldreams.media.utils.MediaUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by boybe on 2016/12/27.
 */

public class PlayManager implements PlayService.PlayStateChangeListener {

    private static final String TAG = PlayManager.class.getSimpleName();

    public static final String
            ACTION_NOTIFICATION_DELETE = "com.nulldreams.music.Action.ACTION_NOTIFICATION_DELETE",
            ACTION_REMOTE_PLAY_PAUSE = "com.nulldreams.music.Action.Remote.ACTION_REMOTE_PLAY_PAUSE",
            ACTION_REMOTE_PREVIOUS = "com.nulldreams.music.Action.Remote.ACTION_REMOTE_PREVIOUS",
            ACTION_REMOTE_NEXT = "com.nulldreams.music.Action.Remote.ACTION_REMOTE_NEXT";

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
            Log.v(TAG, "onServiceConnected");
            dispatch(mSong);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v(TAG, "onServiceDisconnected " + name);
            mService.setPlayStateChangeListener(null);
            mService = null;
        }
    };

    private SimpleBroadcastReceiver mNoisyReceiver = new SimpleBroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                // Pause the playback
                pause(false);
            }
        }

    };

    private SimpleBroadcastReceiver mNotifyDeleteReceiver = new SimpleBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_NOTIFICATION_DELETE.equals(intent.getAction())) {
                release();
                this.unregister(mContext);
                Log.v(TAG, "mNotifyDeleteReceiver onReceive " + intent.getAction());
            }
        }
    };

    private SimpleBroadcastReceiver mRemoteReceiver = new SimpleBroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            switch (action) {
                case ACTION_REMOTE_PREVIOUS:
                    PlayManager.getInstance(context).previous();
                    break;
                case ACTION_REMOTE_PLAY_PAUSE:
                    PlayManager.getInstance(context).dispatch();
                    break;
                case ACTION_REMOTE_NEXT:
                    PlayManager.getInstance(context).next();
                    break;
            }
        }
    };

    private AudioManager.OnAudioFocusChangeListener mAfListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.v(TAG, "onAudioFocusChange = " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                if (isPlaying()) {
                    pause(false);
                }
            } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                if (isPaused() && !isPausedByUser()) {
                    resume();
                }
            }
        }
    };

    private int mPeriod = 1000;
    private boolean isProgressUpdating = false;
    private Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mCallbacks != null && !mCallbacks.isEmpty()
                    && mService != null && mSong != null && mService.isStarted()) {
                for (ProgressCallback callback : mProgressCallbacks) {
                    callback.onProgress(mService.getPosition(), mSong.getDuration());
                }
                mHandler.postDelayed(this, mPeriod);
                isProgressUpdating = true;
            } else {
                isProgressUpdating = false;
            }
        }
    };

    private Handler mHandler = null;

    private List<Callback> mCallbacks;
    private List<ProgressCallback> mProgressCallbacks;

    private Context mContext;

    private List<Song> mTotalList = null;
    private Song mSong = null;
    private int mState = PlayService.STATE_IDLE;
    private PlayService mService;

    private Rule mPlayRule = Rulers.RULER_LIST_LOOP;

    private boolean isPausedByUser = false;

    private NotificationAgent mNotifyAgent = null;

    private PlayManager (Context context) {
        mContext = context;
        mCallbacks = new ArrayList<>();
        mProgressCallbacks = new ArrayList<>();
        mHandler = new Handler();
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
                /*bindPlayService();
                startPlayService();*/
                for (Callback callback : mCallbacks) {
                    callback.onPlayListPrepared(songs);
                }
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
        if (mService != null) {
            mContext.unbindService(mConnection);
        }
    }
    private void startPlayService () {
        mContext.startService(new Intent(mContext, PlayService.class));
    }
    private void stopPlayService () {
        mContext.stopService(new Intent(mContext, PlayService.class));
    }

    /**
     *  dispatch the current song
     */
    public void dispatch () {
        dispatch(mSong);
    }

    /**
     * dispatch a song.If the song is paused, then resume.If the song is not started, then start it.If the song is playing, then pause it.
     * {@link PlayService#STATE_COMPLETED}
     * @param song the song you want to dispatch, if null, dispatch a song from {@link Rule}.
     * @see Song;
     * @see com.nulldreams.media.manager.ruler.Rule#next(Song, List, boolean);
     */
    public void dispatch(final Song song) {
        Log.v(TAG, "dispatch song=" + song);
        Log.v(TAG, "dispatch getAudioFocus mService=" + mService);
        if (mService != null) {
            if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
                if (song == null && mSong == null) {
                    Song defaultSong = mPlayRule.next(song, mTotalList, false);
                    dispatch(defaultSong);
                } else if (song.equals(mSong)) {
                    if (mService.isStarted()) {
                        //Do really this action by user
                        pause();
                    } else if (mService.isPaused()){
                        resume();
                    } else {
                        mSong = song;
                        mService.startPlayer(song.getPath());
                    }
                } else {
                    mSong = song;
                    mService.startPlayer(song.getPath());
                }
            }
        } else {
            Log.v(TAG, "dispatch mService == null");
            mSong = song;
            bindPlayService();
            startPlayService();
        }

    }

    /**
     * you can set a custom {@link Rule} by this
     * @param rule
     */
    public void setRule (@NonNull Rule rule) {
        mPlayRule = rule;
        for (Callback callback : mCallbacks) {
            callback.onPlayRuleChanged(mPlayRule);
        }
    }

    /**
     *
     * @return the current {@link Rule}
     */
    public Rule getRule () {
        return mPlayRule;
    }

    /**
     * next song by user action
     */
    public void next() {
        next(true);
    }

    /**
     * next song triggered by {@link #onStateChanged(int)} and {@link PlayService#STATE_COMPLETED}
     * @param isUserAction
     */
    private void next(boolean isUserAction) {
        dispatch(mPlayRule.next(mSong, mTotalList, isUserAction));
    }

    /**
     * previous song by user action
     */
    public void previous () {
        previous(true);
    }

    private void previous (boolean isUserAction) {
        dispatch(mPlayRule.previous(mSong, mTotalList, isUserAction));
    }

    /**
     * resume play
     */
    public void resume () {
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
            mService.resumePlayer();
        }
    }

    /**
     * pause a playing song by user action
     */
    public void pause () {
        pause(true);
    }

    /**
     * pause a playing song
     * @param isPausedByUser false if triggered by {@link AudioManager#AUDIOFOCUS_LOSS} or
     *                       {@link AudioManager#AUDIOFOCUS_LOSS_TRANSIENT}
     */
    private void pause (boolean isPausedByUser) {
        mService.pausePlayer();
        this.isPausedByUser = isPausedByUser;
    }

    /**
     * release a playing song
     */
    public void release () {
        mService.releasePlayer();
        unbindPlayService();
        stopPlayService();

        mService.setPlayStateChangeListener(null);
        mService = null;
    }

    /**
     *
     * @return
     */
    public boolean isPlaying () {
        return mService != null && mService.isStarted();
    }

    public boolean isPaused () {
        return  mService != null && mService.isPaused();
    }

    public boolean isPausedByUser () {
        return isPausedByUser;
    }

    public void seekTo (int position) {
        if (mService != null) {
            mService.seekTo(position);
        }
    }

    /**
     *
     * @return a song current playing or paused, may be null
     */
    public Song getCurrentSong () {
        return mSong;
    }

    private ComponentName mEventReceiver = null;
    private RemoteControlClient mRemoteControlClient = null;

    private void lockScreenControls () {
        if (mService != null && (mService.isStarted() || mService.isPaused())) {
            mEventReceiver = new ComponentName(mContext, LockControlReceiver.class);
            AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
            audioManager.registerMediaButtonEventReceiver(mEventReceiver);
            Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            mediaButtonIntent.setComponent(mEventReceiver);
            PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(mContext.getApplicationContext(),
                    0, mediaButtonIntent, 0);

            mRemoteControlClient = new RemoteControlClient(mediaPendingIntent);

            mRemoteControlClient.editMetadata(true)
                    .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, mSong.getTitle())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, mSong.getArtist())
                    .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, mSong.getAlbum())
                    ;
            audioManager.registerRemoteControlClient(mRemoteControlClient);
        }
    }

    private void unlockScreenControls () {
        AudioManager audioManager = (AudioManager)mContext.getSystemService(Context.AUDIO_SERVICE);
        if (mEventReceiver != null) {
            audioManager.unregisterMediaButtonEventReceiver(mEventReceiver);
            mEventReceiver = null;
        }
        if (mRemoteControlClient != null) {
            audioManager.unregisterRemoteControlClient(mRemoteControlClient);
            mRemoteControlClient = null;
        }
    }

    private int requestAudioFocus () {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        Log.v(TAG, "requestAudioFocus");
        return audioManager.requestAudioFocus(
                mAfListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
    }

    private int releaseAudioFocus () {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        Log.v(TAG, "releaseAudioFocus");
        return audioManager.abandonAudioFocus(mAfListener);
    }

    public void registerCallback (Callback callback) {
        registerCallback(callback, false);
    }

    public void registerCallback (Callback callback, boolean updateOnceNow) {
        if (mCallbacks.contains(callback)) {
            return;
        }
        mCallbacks.add(callback);
        if (updateOnceNow) {
            callback.onPlayListPrepared(mTotalList);
            callback.onPlayRuleChanged(mPlayRule);
            callback.onPlayStateChanged(mState, mSong);
        }
    }

    public void unregisterCallback (Callback callback) {
        if (mCallbacks.contains(callback)) {
            mCallbacks.remove(callback);
        }
    }

    private void startUpdateProgressIfNeed () {
        if (!isProgressUpdating) {
            mHandler.post(mProgressRunnable);
        }
    }

    public void registerProgressCallback (ProgressCallback callback) {
        if (mProgressCallbacks.contains(callback)) {
            return;
        }
        mProgressCallbacks.add(callback);
        startUpdateProgressIfNeed();
    }

    public void unregisterProgressCallback (ProgressCallback callback) {
        if (mProgressCallbacks.contains(callback)) {
            mProgressCallbacks.remove(callback);
        }
    }

    private void registerNoisyReceiver () {
        mNoisyReceiver.register(mContext, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
    }

    private void unregisterNoisyReceiver () {
        mNoisyReceiver.unregister(mContext);
    }

    private void registerRemoteReceiver () {
        mRemoteReceiver.register(mContext, new IntentFilter(ACTION_REMOTE_PREVIOUS));
        mRemoteReceiver.register(mContext, new IntentFilter(ACTION_REMOTE_PLAY_PAUSE));
        mRemoteReceiver.register(mContext, new IntentFilter(ACTION_REMOTE_NEXT));
    }

    private void unregisterRemoteReceiver () {
        mRemoteReceiver.unregister(mContext);
    }

    @Override
    public void onStateChanged(@PlayService.State int state) {
        mState = state;
        switch (state) {
            case PlayService.STATE_IDLE:
                isPausedByUser = false;
                break;
            case PlayService.STATE_INITIALIZED:
                isPausedByUser = false;
                break;
            case PlayService.STATE_PREPARING:
                isPausedByUser = false;
                break;
            case PlayService.STATE_PREPARED:
                isPausedByUser = false;
                break;
            case PlayService.STATE_STARTED:
                registerNoisyReceiver();
                notification(state);
                startUpdateProgressIfNeed();
                registerRemoteReceiver();
                isPausedByUser = false;
                break;
            case PlayService.STATE_PAUSED:
                unregisterNoisyReceiver();
                //releaseAudioFocus();
                notification(state);
                break;
            case PlayService.STATE_ERROR:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                isPausedByUser = false;
                break;
            case PlayService.STATE_STOPPED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                isPausedByUser = false;
                break;
            case PlayService.STATE_COMPLETED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                isPausedByUser = false;
                next(false);
                break;
            case PlayService.STATE_RELEASED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                unregisterRemoteReceiver();
                isPausedByUser = false;
                break;
        }
        for (Callback callback : mCallbacks) {
            callback.onPlayStateChanged(state, mSong);
        }
    }

    /**
     * you can custom a {@link Notification} by the {@link NotificationAgent}
     * @param agent
     */
    public void setNotificationAgent (NotificationAgent agent) {
        this.mNotifyAgent = agent;
    }

    public NotificationAgent getNotificationAgent () {
        return mNotifyAgent;
    }

    private int mLastNotificationId;
    private void notification (@PlayService.State int state) {
        if (mNotifyAgent == null) {
            return;
        }
        NotificationManager manager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mLastNotificationId > 0) {
            mService.stopForeground(true);
            manager.cancel(mLastNotificationId);
        }

        boolean onGoing = isPlaying();
        NotificationCompat.Builder builder = mNotifyAgent.getBuilder(mContext, this, state, mSong);
        if (builder != null) {
            builder.setOngoing(onGoing);
            builder.setAutoCancel(!onGoing);

            final Notification notification = builder.build();

            int notificationId = mSong.getId();
            Log.v(TAG, "notification onGoing=" + onGoing + " notificationId=" + notificationId);
            if (onGoing) {
                mService.startForeground(notificationId, notification);
                mNotifyDeleteReceiver.unregister(mContext);
            } else {
                mService.stopForeground(true);
                manager.notify(notificationId, notification);
                mNotifyDeleteReceiver.register(mContext, new IntentFilter(ACTION_NOTIFICATION_DELETE));
            }
            mNotifyAgent.afterNotify();
            mLastNotificationId = notificationId;
        }

    }

    public interface Callback {
        void onPlayListPrepared (List<Song> songs);
        void onPlayStateChanged (@PlayService.State int state, Song song);
        void onPlayRuleChanged (Rule rule);
    }

    public interface ProgressCallback {
        void onProgress (int progress, int duration);
    }

}
