package com.nulldreams.bemusic.manager;

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
import android.widget.RemoteViews;

import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.manager.ruler.Rule;
import com.nulldreams.bemusic.manager.ruler.Rulers;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.receiver.LockControlReceiver;
import com.nulldreams.bemusic.receiver.SimpleBroadcastReceiver;
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

public class PlayManager implements PlayService.PlayStateChangeListener {

    private static final String TAG = PlayManager.class.getSimpleName();

    public static final String ACTION_NOTIFICATION_DELETE = "com.nulldreams.music.Action.ACTION_NOTIFICATION_DELETE";

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
                pause();
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

    private AudioManager.OnAudioFocusChangeListener mAfListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            Log.v(TAG, "onAudioFocusChange = " + focusChange);
            if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                pause();
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
    private PlayService mService;

    private Rule mPlayRule = Rulers.RULER_LIST_LOOP;

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

    public void dispatch () {
        dispatch(mSong);
    }

    public void dispatch(final Song song) {
        Log.v(TAG, "dispatch song=" + song);
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
            Log.v(TAG, "dispatch getAudioFocus mService=" + mService);
            if (mService != null) {
                if (song == null && mSong == null) {
                    Song defaultSong = mPlayRule.next(song, mTotalList, false);
                    dispatch(defaultSong);
                } else if (song.equals(mSong)) {
                    if (mService.isStarted()) {
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
            } else {
                Log.v(TAG, "dispatch mService == null");
                mSong = song;
                bindPlayService();
                startPlayService();
            }
        }

    }

    public void setRule (@NonNull Rule rule) {
        mPlayRule = rule;
        for (Callback callback : mCallbacks) {
            callback.onPlayRuleChanged(mPlayRule);
        }
    }

    public void next() {
        next(true);
    }

    private void next(boolean isUserAction) {
        dispatch(mPlayRule.next(mSong, mTotalList, isUserAction));
    }

    public void previous () {
        previous(true);
    }

    private void previous (boolean isUserAction) {
        dispatch(mPlayRule.previous(mSong, mTotalList, isUserAction));
    }

    public void resume () {
        if (AudioManager.AUDIOFOCUS_REQUEST_GRANTED == requestAudioFocus()) {
            mService.resumePlayer();
        }
    }

    public void pause () {
        mService.pausePlayer();
    }

    public void release () {
        mService.releasePlayer();
        unbindPlayService();
        stopPlayService();

        mService.setPlayStateChangeListener(null);
        mService = null;
    }

    public boolean isPlaying () {
        return mService != null && mService.isStarted();
    }

    public Song getCurrentSong () {
        return mSong;
    }

    private ComponentName mEventReceiver = null;
    private RemoteControlClient mRemoteControlClient = null;

    public void lockScreenControls () {
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

    public void unlockScreenControls () {
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

    @Override
    public void onStateChanged(@PlayService.State int state) {

        switch (state) {
            case PlayService.STATE_STARTED:
                registerNoisyReceiver();
                notification(state);
                startUpdateProgressIfNeed();
                break;
            case PlayService.STATE_PAUSED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                break;
            case PlayService.STATE_ERROR:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                break;
            case PlayService.STATE_STOPPED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                break;
            case PlayService.STATE_COMPLETED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                notification(state);
                next(false);
                break;
            case PlayService.STATE_RELEASED:
                unregisterNoisyReceiver();
                releaseAudioFocus();
                break;
        }
        for (Callback callback : mCallbacks) {
            callback.onPlayStateChanged(state, mSong);
        }
    }
    private int mLastNotificationId;
    private void notification (@PlayService.State int state) {
        NotificationManager manager = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        if (mLastNotificationId > 0) {
            mService.stopForeground(true);
            manager.cancel(mLastNotificationId);
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setContentTitle(mSong.getTitle());
        builder.setContentText(mSong.getArtistAlbum());
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);

        File file = mSong.getCoverFile(mContext);
        Bitmap bmp = null;
        if (file.exists()) {
            bmp = BitmapFactory.decodeFile(file.getAbsolutePath());
            builder.setLargeIcon(bmp);
        } else {
            builder.setLargeIcon(null);
        }

        RemoteViews remoteViews = new RemoteViews(mContext.getPackageName(), R.layout.layout_notification);
        remoteViews.setTextViewText(R.id.notification_title, mSong.getTitle());
        remoteViews.setTextViewText(R.id.notification_artist_album, mSong.getArtistAlbum());
        remoteViews.setImageViewBitmap(R.id.notification_thumb, bmp);
        builder.setCustomBigContentView(remoteViews);

        boolean onGoing = isPlaying();

        builder.setOngoing(onGoing);
        builder.setAutoCancel(!onGoing);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_NOTIFICATION_DELETE), 0);
        builder.setDeleteIntent(deleteIntent);
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
        mLastNotificationId = notificationId;
        if (bmp != null) {
            bmp.recycle();
        }
    }

    /*private void showNotificationLollipop (@PlayService.State int state) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
        builder.setContentTitle(mSong.getTitle());
        builder.setContentText(mSong.getArtistAlbum());
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.mipmap.ic_launcher);

        MediaSessionManager sessionManager = (MediaSessionManager)mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
        MediaController.
    }*/

    public interface Callback {
        void onPlayListPrepared (List<Song> songs);
        void onPlayStateChanged (@PlayService.State int state, Song song);
        void onPlayRuleChanged (Rule rule);
    }

    public interface ProgressCallback {
        void onProgress (int progress, int duration);
    }

}
