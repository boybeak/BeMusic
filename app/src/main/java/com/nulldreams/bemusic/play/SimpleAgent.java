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
import android.support.v7.app.NotificationCompat;
import android.support.v7.widget.AppCompatDrawableManager;
import android.widget.RemoteViews;

import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.activity.PlayDetailActivity;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.notification.NotificationAgent;
import com.nulldreams.media.model.Album;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.service.PlayService;

import java.io.File;

/**
 * Created by boybe on 2017/1/5.
 */

public class SimpleAgent implements NotificationAgent {

    private Bitmap mThumbBmp, mPreviousBmp, mPlayPauseBmp, mNextBmp;

    @Override
    public NotificationCompat.Builder getBuilder(Context context, PlayManager manager, @PlayService.State int state, Song song) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle(song.getTitle());
        builder.setContentText(song.getArtistAlbum());
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(android.R.mipmap.sym_def_app_icon);

        Album album = song.getAlbumObj();
        mThumbBmp = null;
        if (album != null) {
            mThumbBmp = BitmapFactory.decodeFile(album.getAlbumArt());
            builder.setLargeIcon(mThumbBmp);
        } else {
            builder.setLargeIcon(null);
        }

        boolean onGoing = manager.isPlaying();

        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), R.layout.layout_notification);
        remoteViews.setImageViewBitmap(R.id.notification_thumb, mThumbBmp);
        remoteViews.setTextViewText(R.id.notification_title, song.getTitle());
        remoteViews.setTextViewText(R.id.notification_artist_album, song.getArtistAlbum());

        RemoteViews remoteBigViews = new RemoteViews(context.getPackageName(), R.layout.layout_notification_big);
        remoteBigViews.setTextViewText(R.id.notification_title, song.getTitle());
        remoteBigViews.setTextViewText(R.id.notification_artist_album, song.getArtistAlbum());
        remoteBigViews.setImageViewBitmap(R.id.notification_thumb, mThumbBmp);

        PendingIntent playPauseIt = PendingIntent.getBroadcast(context, 0, new Intent(PlayManager.ACTION_REMOTE_PLAY_PAUSE), 0);
        PendingIntent previousIt = PendingIntent.getBroadcast(context, 0, new Intent(PlayManager.ACTION_REMOTE_PREVIOUS), 0);
        PendingIntent nextIt = PendingIntent.getBroadcast(context, 0, new Intent(PlayManager.ACTION_REMOTE_NEXT), 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            remoteViews.setImageViewResource(R.id.notification_action_previous, R.drawable.ic_skip_previous);
            remoteViews.setImageViewResource(R.id.notification_action_play_pause, onGoing ? R.drawable.ic_pause : R.drawable.ic_play);
            remoteViews.setImageViewResource(R.id.notification_action_next, R.drawable.ic_skip_next);

            remoteBigViews.setImageViewResource(R.id.notification_action_previous, R.drawable.ic_skip_previous);
            remoteBigViews.setImageViewResource(R.id.notification_action_play_pause, onGoing ? R.drawable.ic_pause : R.drawable.ic_play);
            remoteBigViews.setImageViewResource(R.id.notification_action_next, R.drawable.ic_skip_next);

        } else {
            mPreviousBmp = getRemoteViewsPreLollipop(context, R.drawable.ic_skip_previous);
            mPlayPauseBmp = getRemoteViewsPreLollipop(context, onGoing ? R.drawable.ic_pause : R.drawable.ic_play);
            mNextBmp = getRemoteViewsPreLollipop(context, R.drawable.ic_skip_next);

            remoteViews.setImageViewBitmap(R.id.notification_action_previous, mPreviousBmp);
            remoteViews.setImageViewBitmap(R.id.notification_action_play_pause, mPlayPauseBmp);
            remoteViews.setImageViewBitmap(R.id.notification_action_next, mNextBmp);

            remoteBigViews.setImageViewBitmap(R.id.notification_action_previous, mPreviousBmp);
            remoteBigViews.setImageViewBitmap(R.id.notification_action_play_pause, mPlayPauseBmp);
            remoteBigViews.setImageViewBitmap(R.id.notification_action_next, mNextBmp);
        }

        remoteViews.setOnClickPendingIntent(R.id.notification_action_previous, previousIt);
        remoteViews.setOnClickPendingIntent(R.id.notification_action_play_pause, playPauseIt);
        remoteViews.setOnClickPendingIntent(R.id.notification_action_next, nextIt);

        remoteBigViews.setOnClickPendingIntent(R.id.notification_action_previous, previousIt);
        remoteBigViews.setOnClickPendingIntent(R.id.notification_action_play_pause, playPauseIt);
        remoteBigViews.setOnClickPendingIntent(R.id.notification_action_next, nextIt);

        builder.setCustomContentView(remoteViews);
        builder.setCustomBigContentView(remoteBigViews);

        PendingIntent deleteIntent = PendingIntent.getBroadcast(context, 0, new Intent(PlayManager.ACTION_NOTIFICATION_DELETE), 0);
        builder.setDeleteIntent(deleteIntent);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, new Intent(context, PlayDetailActivity.class), 0);
        builder.setContentIntent(contentIntent);
        return builder;
    }

    @Override
    public void afterNotify() {
        if (mThumbBmp != null && !mThumbBmp.isRecycled()) {
            mThumbBmp.recycle();
        }
        if (mPreviousBmp != null && !mPreviousBmp.isRecycled()) {
            mPreviousBmp.recycle();
        }
        if (mPlayPauseBmp != null && !mPlayPauseBmp.isRecycled()) {
            mPlayPauseBmp.recycle();
        }
        if (mNextBmp != null && !mNextBmp.isRecycled()) {
            mNextBmp.recycle();
        }
    }

    private Bitmap getRemoteViewsPreLollipop (Context context, @DrawableRes int res) {
        Drawable drawable = AppCompatDrawableManager.get().getDrawable(context, res);
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }
}
