package com.nulldreams.media.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.nulldreams.media.manager.PlayManager;

import java.util.Set;

public class RemoteControlReceiver extends BroadcastReceiver {

    private static final String TAG = RemoteControlReceiver.class.getSimpleName();

    public RemoteControlReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive " + intent.getAction() + " " + intent.toString());
        Bundle bundle = intent.getExtras();
        Set<String> set = bundle.keySet();
        KeyEvent event = bundle.getParcelable(Intent.EXTRA_KEY_EVENT);
        if (event.getAction() == KeyEvent.ACTION_UP) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    PlayManager.getInstance(context).next();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    PlayManager.getInstance(context).previous();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    PlayManager.getInstance(context).dispatch();
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    PlayManager.getInstance(context).stop();
                    PlayManager.getInstance(context).release();
                    break;
            }
        }
        /*for (String key : set) {
            Log.v(TAG, "onReceive key=" + key + " value=" + bundle.get(key));
        }*/
    }
}
