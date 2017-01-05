package com.nulldreams.bemusic;

import android.app.Application;
import android.content.SharedPreferences;

import com.nulldreams.bemusic.play.SimpleAgent;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.manager.ruler.Rule;
import com.nulldreams.media.manager.ruler.Rulers;

/**
 * Created by boybe on 2017/1/5.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), MODE_PRIVATE);
        int id = sharedPreferences.getInt("rule", 0);
        Rule rule = null;
        switch (id) {
            case 0:
                rule = Rulers.RULER_LIST_LOOP;
                break;
            case 1:
                rule = Rulers.RULER_SINGLE_LOOP;
                break;
            case 2:
                rule = Rulers.RULER_RANDOM;
                break;
        }
        PlayManager.getInstance(this).setRule(rule);
        PlayManager.getInstance(this).setNotificationAgent(new SimpleAgent());

    }
}
