package com.nulldreams.media.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class RemoteControlReceiver extends BroadcastReceiver {

    private static final String TAG = RemoteControlReceiver.class.getSimpleName();

    public RemoteControlReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "onReceive " + intent.getAction());
    }
}
