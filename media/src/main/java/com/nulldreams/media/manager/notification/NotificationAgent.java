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
    NotificationCompat.Builder getBuilder (Context context, PlayManager manager, @PlayService.State int state, Song song);
    void afterNotify();
}
