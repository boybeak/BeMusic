package com.nulldreams.bemusic;

import android.app.Application;

import com.nulldreams.bemusic.play.SimpleAgent;
import com.nulldreams.media.manager.PlayManager;

/**
 * Created by boybe on 2017/1/5.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PlayManager.getInstance(this).setNotificationAgent(new SimpleAgent());
    }
}
