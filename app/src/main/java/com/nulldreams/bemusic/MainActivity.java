package com.nulldreams.bemusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;
import com.nulldreams.bemusic.utils.MediaUtils;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private PlayService.PlayBinder mBinder = null;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBinder = (PlayService.PlayBinder)service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBinder = null;
        }
    };
    List<Song> songs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songs = MediaUtils.getAudioList(this);

        for (Song song : songs) {
            Log.v(TAG, "song.NAME=" + song.getDisplayName());
        }
    }

    public void startS (View view) {
        startService(new Intent(this, PlayService.class));
    }
    public void stopS (View view) {
        stopService(new Intent(this, PlayService.class));
    }
    public void bindS (View view) {
        bindService(new Intent(this, PlayService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    public void unbindS (View view) {
        unbindService(mConnection);
    }

    public void startPlay (View view) {
        if (songs != null && songs.size() > 0 && mBinder != null) {
            Toast.makeText(this, "startPlay", Toast.LENGTH_SHORT).show();
            mBinder.getService().startPlayer(songs.get(0));
        }
    }
}
