package com.nulldreams.bemusic.adapter;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.nulldreams.adapter.AbsViewHolder;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.manager.PlayManager;
import com.nulldreams.bemusic.model.Song;
import com.nulldreams.bemusic.utils.MediaUtils;

import java.io.File;

/**
 * Created by boybe on 2016/12/27.
 */

public class SongHolder extends AbsViewHolder<SongDelegate> {

    private ImageView thumbIv;
    private TextView titleTv, artistAlbumTv, durationTv;

    public SongHolder(View itemView) {
        super(itemView);
        thumbIv = (ImageView) findViewById(R.id.song_thumb);
        titleTv = (TextView) findViewById(R.id.song_title);
        artistAlbumTv = (TextView) findViewById(R.id.song_artist_album);
        durationTv = (TextView) findViewById(R.id.song_duration);
    }

    @Override
    public void onBindView(final Context context, SongDelegate songDelegate, int position, DelegateAdapter adapter) {
        final Song song = songDelegate.getSource();
        titleTv.setText(song.getTitle());
        artistAlbumTv.setText(song.getArtist() + " - " + song.getAlbum());
        File file = song.getCoverFile(context);
        if (file.exists()) {
            Glide.with(context).load(file).placeholder(R.mipmap.ic_launcher).into(thumbIv);
        } else {
            Glide.with(context).load("").placeholder(R.mipmap.ic_launcher).into(thumbIv);
        }
        durationTv.setText(MediaUtils.formatTime(song.getDuration()));
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayManager.getInstance(context).dispatch(song);
            }
        });
    }
}
