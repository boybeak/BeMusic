package com.nulldreams.bemusic.receiver;

import android.content.BroadcastReceiver;

/**
 * Created by gaoyunfei on 2016/12/28.
 */

public abstract class NoisyBroadcastReceiver extends BroadcastReceiver {

    private boolean isRegistered = false;

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }
}
