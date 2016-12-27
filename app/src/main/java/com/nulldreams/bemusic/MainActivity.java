package com.nulldreams.bemusic;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateFilter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.DelegateImpl;
import com.nulldreams.adapter.impl.LayoutImpl;
import com.nulldreams.bemusic.adapter.SongDelegate;
import com.nulldreams.bemusic.manager.PlayManager;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.service.PlayService;
import com.nulldreams.bemusic.utils.MediaUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements PlayManager.Callback{

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
    private DelegateAdapter mAdapter;
    private RecyclerView mRv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mRv = (RecyclerView)findViewById(R.id.main_rv);
        mRv.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new DelegateAdapter(this);
        mRv.setAdapter(mAdapter);

        PlayManager.getInstance(this).registerCallback(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        PlayManager.getInstance(this).unregisterCallback(this);
    }

    @Override
    public void onPlayListPrepared(List<Song> songs) {
        mAdapter.addAll(songs, new DelegateParser<Song>() {
            @Override
            public DelegateImpl parse(Song data) {
                return new SongDelegate(data);
            }
        });
        mAdapter.notifyDataSetChanged();
    }
}
