package com.nulldreams.media.manager.notification;

import android.content.Context;
import android.support.v7.app.NotificationCompat;

import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.service.PlayService;

/**
 * Created by boybe on 2017/1/5.
 */

public interface NotificationAgent {
    /**
     * custom your notification style
     * @param context
     * @param manager
     * @param state
     * @param song
     * @return
     */
    NotificationCompat.Builder getBuilder (Context context, PlayManager manager, @PlayService.State int state, Song song);

    /**
     * you can recycle a bitmap in this method after the notification is already shown
     */
    void afterNotify();
}
