package com.nulldreams.bemusic.play;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.activity.PlayDetailActivity;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.notification.NotificationAgent;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.service.PlayService;

import static android.content.ContentValues.TAG;

/**
 * Created by boybe on 2017/1/5.
 */

public class SimpleAgent implements NotificationAgent {

    private NotificationCompat.Builder getBuilderCompat (Context context, PlayManager manager, @PlayService.State int state, Song song) {

        PendingIntent playPauseIt = getActionIntent(context, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
        PendingIntent previousIt = getActionIntent(context, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        PendingIntent nextIt = getActionIntent(context, KeyEvent.KEYCODE_MEDIA_NEXT);
        PendingIntent deleteIntent = getActionIntent(context, KeyEvent.KEYCODE_MEDIA_STOP);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, PlayDetailActivity.class), 0);

        boolean isPlaying = manager.isPlaying();
        MediaSessionCompat sessionCompat = manager.getMediaSessionCompat();
        MediaControllerCompat controllerCompat = sessionCompat.getController();
        PendingIntent sessionActivity = controllerCompat.getSessionActivity();
        Log.v(TAG, "getBuilderCompat sessionActivity = " + sessionActivity);
        MediaDescriptionCompat descriptionCompat = controllerCompat.getMetadata().getDescription();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(descriptionCompat.getTitle());
        builder.setShowWhen(false);
        builder.setContentText(descriptionCompat.getSubtitle());
        builder.setSubText(descriptionCompat.getDescription());
        builder.setSmallIcon(R.drawable.ic_notification_queue_music);
        builder.addAction(R.drawable.ic_skip_previous, "previous", previousIt);
        builder.addAction(isPlaying ? R.drawable.ic_pause : R.drawable.ic_play, "play pause", playPauseIt);
        builder.addAction(R.drawable.ic_skip_next, "next", nextIt);
        builder.setContentIntent(contentIntent);
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        builder.setLargeIcon(descriptionCompat.getIconBitmap());
        NotificationCompat.MediaStyle mediaStyle = new NotificationCompat.MediaStyle();
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            builder.setOngoing(true);
            mediaStyle.setShowCancelButton(true);
            mediaStyle.setCancelButtonIntent(deleteIntent);
        } else {
            builder.setOngoing(isPlaying);
            builder.setDeleteIntent(deleteIntent);
        }
        mediaStyle.setShowActionsInCompactView(0, 1, 2);

        if (sessionCompat != null) {
            mediaStyle.setMediaSession(sessionCompat.getSessionToken());
        }
        builder.setStyle(mediaStyle);
        return builder;
    }

    @Override
    public NotificationCompat.Builder getBuilder(Context context, PlayManager manager, @PlayService.State int state, Song song) {
        return getBuilderCompat(context, manager, state, song);
    }

    @Override
    public void afterNotify() {
    }

    public static PendingIntent getActionIntent (Context context, int mediaKeyEvent) {
        Intent it = new Intent(Intent.ACTION_MEDIA_BUTTON);
        it.setPackage(context.getPackageName());
        it.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, mediaKeyEvent));
        return PendingIntent.getBroadcast(context, mediaKeyEvent, it, 0);
    }

}
