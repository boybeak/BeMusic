package com.nulldreams.media.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

/**
 * Created by gaoyunfei on 2016/12/28.
 */

public abstract class SimpleBroadcastReceiver extends BroadcastReceiver {

    private boolean isRegistered = false, isLocalRegistered = false;

    public void register (Context context, IntentFilter intentFilter) {
        context.registerReceiver(this, intentFilter);
        setRegistered(true);
    }

    public void unregister (Context context) {
        if (isRegistered()) {
            context.unregisterReceiver(this);
            setRegistered(false);
        }
    }

    public void localRegister (Context context, IntentFilter intentFilter) {
        LocalBroadcastManager.getInstance(context).registerReceiver(this, intentFilter);
        setLocalRegistered(true);
    }

    public void localUnregistered (Context context) {
        if (isLocalRegistered()) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(this);
            setLocalRegistered(false);
        }
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    private void setRegistered(boolean registered) {
        isRegistered = registered;
    }

    public boolean isLocalRegistered() {
        return isLocalRegistered;
    }

    private void setLocalRegistered(boolean localRegistered) {
        isLocalRegistered = localRegistered;
    }
}
