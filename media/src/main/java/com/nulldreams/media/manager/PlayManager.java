package com.nulldreams.media.manager;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.RatingCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;

import com.nulldreams.media.manager.notification.NotificationAgent;
import com.nulldreams.media.manager.ruler.Rule;
import com.nulldreams.media.manager.ruler.Rulers;
import com.nulldreams.media.model.Album;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.receiver.RemoteControlReceiver;
import com.nulldreams.media.receiver.SimpleBroadcastReceiver;
import com.nulldreams.media.service.PlayService;
import com.nulldreams.media.utils.MediaUtils;

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
    private List<Album> mTotalAlbumList;
    private List<Song> mTotalList;
    private List<Song> mCurrentList;
    private Album mCurrentAlbum;
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
                mTotalAlbumList = MediaUtils.getAlbumList(context);
                for (Song song : songs) {
                    song.setAlbumObj(getAlbum(song.getAlbumId()));
                }
                return songs;
            }

            @Override
            protected void onPostExecute(List<Song> songs) {
                mTotalList = songs;
                mCurrentList = mTotalList;
                /*bindPlayService();
                startPlayService();*/
                for (Callback callback : mCallbacks) {
                    callback.onPlayListPrepared(songs);
                    callback.onAlbumListPrepared(mTotalAlbumList);
                }
            }
        }.execute(mContext);
    }

    public List<Song> getTotalList () {
        return mTotalList;
    }

    public List<Song> getAlbumSongList (int albumId) {
        List<Song> songs = MediaUtils.getAlbumSongList(mContext, albumId);
        for (Song song : songs) {
            song.setAlbumObj(getAlbum(song.getAlbumId()));
        }
        return songs;
    }

    public List<Album> getAlbumList () {
        return mTotalAlbumList;
    }

    public Album getAlbum (int albumId) {
        for (Album album : mTotalAlbumList) {
            if (album.getId() == albumId) {
                return album;
            }
        }
        return null;
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

    /*public void dispatch (Album album, Song song) {
        if (album != null) {
            mCurrentList = getAlbumSongList(album.getId());
        } else {
            mCurrentList = mTotalList;
        }
        dispatch(song);
        mCurrentAlbum = album;
    }

    public void dispatch (Album album) {
        dispatch(album, mPlayRule.next(mSong, mCurrentList, true));
    }*/

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
        //mCurrentAlbum = null;
        if (mService != null) {
            if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
                if (song == null && mSong == null) {
                    Song defaultSong = mPlayRule.next(song, mCurrentList, false);
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
        dispatch(mPlayRule.next(mSong, mCurrentList, isUserAction));
    }

    /**
     * previous song by user action
     */
    public void previous () {
        previous(true);
    }

    private void previous (boolean isUserAction) {
        dispatch(mPlayRule.previous(mSong, mCurrentList, isUserAction));
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

    private MediaSessionCompat mMediaSessionCompat;
    private void startRemoteControl() {
        ComponentName mediaButtonReceiver = new ComponentName(mContext, RemoteControlReceiver.class);
        mMediaSessionCompat = new MediaSessionCompat(mContext, TAG, mediaButtonReceiver, null);
        mMediaSessionCompat.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS |
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        mMediaSessionCompat.setCallback(mSessionCallback);
        mMediaSessionCompat.setActive(true);
    }

    private void stopRemoteControl () {
        mMediaSessionCompat.release();
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
                startRemoteControl();
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
                mMediaSessionCompat.setMetadata(
                        new MediaMetadataCompat.Builder()
                        .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mSong.getTitle())
                        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mSong.getAlbum())
                        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mSong.getArtist())
                        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeFile(mSong.getAlbumObj().getAlbumArt()))
                        .build()
                );
                mMediaSessionCompat.setPlaybackState(
                        new PlaybackStateCompat.Builder()
                        .setState(PlaybackStateCompat.STATE_PLAYING, mService.getPosition(), 0)
                        .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                        .build()
                );
                break;
            case PlayService.STATE_PAUSED:
                unregisterNoisyReceiver();
                //releaseAudioFocus();
                notification(state);
                mMediaSessionCompat.setMetadata(
                        new MediaMetadataCompat.Builder()
                                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, mSong.getTitle())
                                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, mSong.getAlbum())
                                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, mSong.getArtist())
                                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, BitmapFactory.decodeFile(mSong.getAlbumObj().getAlbumArt()))
                                .build()
                );
                mMediaSessionCompat.setPlaybackState(
                        new PlaybackStateCompat.Builder()
                                .setState(PlaybackStateCompat.STATE_PAUSED, mService.getPosition(), 0)
                                .setActions(PlaybackStateCompat.ACTION_PLAY_PAUSE)
                                .build()
                );
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
                stopRemoteControl();
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

    private MediaSessionCompat.Callback mSessionCallback = new MediaSessionCompat.Callback() {
        @Override
        public void onCommand(String command, Bundle extras, ResultReceiver cb) {
            super.onCommand(command, extras, cb);
            Log.v(TAG, "mSessionCallback onCommand command=" + command);
        }

        @Override
        public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
            Log.v(TAG, "mSessionCallback onMediaButtonEvent mediaButtonEvent=" + mediaButtonEvent.getAction());
            return super.onMediaButtonEvent(mediaButtonEvent);
        }

        @Override
        public void onPrepare() {
            super.onPrepare();
            Log.v(TAG, "mSessionCallback onPrepare");
        }

        @Override
        public void onPrepareFromMediaId(String mediaId, Bundle extras) {
            super.onPrepareFromMediaId(mediaId, extras);
            Log.v(TAG, "mSessionCallback onPrepareFromMediaId mediaId=" + mediaId);
        }

        @Override
        public void onPrepareFromSearch(String query, Bundle extras) {
            super.onPrepareFromSearch(query, extras);
            Log.v(TAG, "mSessionCallback onPrepareFromSearch query=" + query);
        }

        @Override
        public void onPrepareFromUri(Uri uri, Bundle extras) {
            super.onPrepareFromUri(uri, extras);
            Log.v(TAG, "mSessionCallback onPrepareFromUri uri=" + uri.toString());
        }

        @Override
        public void onPlay() {
            super.onPlay();
            dispatch();
            Log.v(TAG, "mSessionCallback onPlay");
        }

        @Override
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            super.onPlayFromMediaId(mediaId, extras);
            Log.v(TAG, "mSessionCallback onPlayFromMediaId mediaId=" + mediaId);
        }

        @Override
        public void onPlayFromSearch(String query, Bundle extras) {
            super.onPlayFromSearch(query, extras);
            Log.v(TAG, "mSessionCallback onPlayFromSearch query=" + query);
        }

        @Override
        public void onPlayFromUri(Uri uri, Bundle extras) {
            super.onPlayFromUri(uri, extras);
            Log.v(TAG, "mSessionCallback onPlayFromUri uri=" + uri.toString());
        }

        @Override
        public void onSkipToQueueItem(long id) {
            super.onSkipToQueueItem(id);
            Log.v(TAG, "mSessionCallback onSkipToQueueItem id=" + id);
        }

        @Override
        public void onPause() {
            pause(true);
            Log.v(TAG, "mSessionCallback onPause");
        }

        @Override
        public void onSkipToNext() {
            super.onSkipToNext();
            Log.v(TAG, "mSessionCallback onSkipToNext");
        }

        @Override
        public void onSkipToPrevious() {
            super.onSkipToPrevious();
            Log.v(TAG, "mSessionCallback onSkipToPrevious");
        }

        @Override
        public void onFastForward() {
            super.onFastForward();
            Log.v(TAG, "mSessionCallback onFastForward");
        }

        @Override
        public void onRewind() {
            super.onRewind();
            Log.v(TAG, "mSessionCallback onRewind");
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.v(TAG, "mSessionCallback onStop");
        }

        @Override
        public void onSeekTo(long pos) {
            super.onSeekTo(pos);
            Log.v(TAG, "mSessionCallback onSeekTo pos=" + pos);
        }

        @Override
        public void onSetRating(RatingCompat rating) {
            super.onSetRating(rating);
            Log.v(TAG, "mSessionCallback onSetRating rating=" + rating.toString());
        }

        @Override
        public void onCustomAction(String action, Bundle extras) {
            super.onCustomAction(action, extras);
            Log.v(TAG, "mSessionCallback onCustomAction action=" + action);
        }
    };

    public interface Callback {
        void onPlayListPrepared (List<Song> songs);
        void onAlbumListPrepared (List<Album> albums);
        void onPlayStateChanged (@PlayService.State int state, Song song);
        void onPlayRuleChanged (Rule rule);
    }

    public interface ProgressCallback {
        void onProgress (int progress, int duration);
    }

}
