package com.nulldreams.bemusic.play;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.RemoteViews;

import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.activity.MainActivity;
import com.nulldreams.bemusic.activity.PlayDetailActivity;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.notification.NotificationAgent;
import com.nulldreams.media.model.Album;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.service.PlayService;

import java.io.File;

import static android.content.ContentValues.TAG;

/**
 * Created by boybe on 2017/1/5.
 */

public class SimpleAgent implements NotificationAgent {

    private NotificationCompat.Builder getBuilderCompat (Context context, PlayManager manager, @PlayService.State int state, Song song) {

        PendingIntent playPauseIt = PendingIntent.getBroadcast(context, 0, new Intent(PlayManager.ACTION_REMOTE_PLAY_PAUSE), 0);
        PendingIntent previousIt = PendingIntent.getBroadcast(context, 0, new Intent(PlayManager.ACTION_REMOTE_PREVIOUS), 0);
        PendingIntent nextIt = PendingIntent.getBroadcast(context, 0, new Intent(PlayManager.ACTION_REMOTE_NEXT), 0);
        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0, new Intent(PlayManager.ACTION_NOTIFICATION_DELETE), 0);
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

}
