package com.nulldreams.bemusic;

import android.app.Application;
import android.content.SharedPreferences;

import com.nulldreams.bemusic.play.SimpleAgent;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.ruler.Rule;
import com.nulldreams.media.manager.ruler.Rulers;
import com.squareup.leakcanary.LeakCanary;

/**
 * Created by boybe on 2017/1/5.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler.getInstance(this);

        if (LeakCanary.isInAnalyzerProcess(this)) {
            return;
        }
        LeakCanary.install(this);
    }
}
