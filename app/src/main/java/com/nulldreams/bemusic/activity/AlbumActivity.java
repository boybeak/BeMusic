package com.nulldreams.bemusic.activity;

import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.adapter.DelegateParser;
import com.nulldreams.adapter.impl.DelegateImpl;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.adapter.SongDelegate;
import com.nulldreams.media.manager.PlayManager;
import com.nulldreams.media.model.Album;
import com.nulldreams.media.model.Song;
import com.nulldreams.media.utils.MediaUtils;

import java.util.List;

public class AlbumActivity extends AppCompatActivity {

    private CollapsingToolbarLayout mColTbLayout;
    private Toolbar mTb;
    private ImageView mThumbIv;
    private RecyclerView mRv;
    private FloatingActionButton mFab;

    private Album mAlbum;

    private DelegateAdapter mAdapter;

    private View.OnClickListener mClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final int id = v.getId();
            if (id == mFab.getId()) {
                //PlayManager.getInstance(v.getContext()).dispatch(mAlbum);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_album);

        mAlbum = (Album)getIntent().getSerializableExtra("album");

        mColTbLayout = (CollapsingToolbarLayout)findViewById(R.id.album_col_toolbar_layout);
        mTb = (Toolbar)findViewById(R.id.album_toolbar);
        mThumbIv = (ImageView)findViewById(R.id.album_thumb);
        mRv = (RecyclerView)findViewById(R.id.album_rv);
        mFab = (FloatingActionButton)findViewById(R.id.album_start);

        mRv.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new DelegateAdapter(this);
        mRv.setAdapter(mAdapter);

        mFab.setOnClickListener(mClickListener);

        setSupportActionBar(mTb);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mColTbLayout.setTitle(mAlbum.getAlbum());
        Glide.with(this).load(mAlbum.getAlbumArt()).into(mThumbIv);

        List<Song> songList = PlayManager.getInstance(this).getAlbumSongList(mAlbum.getId());
        if (songList != null) {
            mAdapter.addAll(songList, new DelegateParser<Song>() {
                @Override
                public DelegateImpl parse(Song data) {
                    return new SongDelegate(data);
                }
            });
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
