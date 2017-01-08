package com.nulldreams.bemusic.adapter;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.graphics.Palette;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.nulldreams.adapter.AbsViewHolder;
import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.activity.AlbumActivity;
import com.nulldreams.media.model.Album;

/**
 * Created by boybe on 2017/1/6.
 */

public class AlbumHolder extends AbsViewHolder<AlbumDelegate> {

    private ImageView thumbIv;
    private TextView titleTv, artistTv, countTv;
    private View infoLayout;

    public AlbumHolder(View itemView) {
        super(itemView);
        thumbIv = (ImageView)findViewById(R.id.album_thumb);
        titleTv = (TextView)findViewById(R.id.album_title);
        artistTv = (TextView)findViewById(R.id.album_artist);
        countTv = (TextView)findViewById(R.id.album_count);
        infoLayout = findViewById(R.id.album_info_layout);
    }

    @Override
    public void onBindView(final Context context, final AlbumDelegate albumDelegate, int position, DelegateAdapter adapter) {
        final Album album = albumDelegate.getSource();
        Glide.with(context).load(album.getAlbumArt()).asBitmap().placeholder(R.mipmap.ic_launcher).animate(android.R.anim.fade_in).into(new SimpleTarget<Bitmap>() {
            @Override
            public void onResourceReady(Bitmap resource, GlideAnimation<? super Bitmap> glideAnimation) {
                thumbIv.setImageBitmap(resource);
                if (albumDelegate.getRgb() > 0) {
                    infoLayout.setBackgroundColor(albumDelegate.getRgb());
                } else {
                    Palette.from(resource).generate(new Palette.PaletteAsyncListener() {
                        @Override
                        public void onGenerated(Palette palette) {
                            Palette.Swatch swatch = palette.getDarkMutedSwatch();
                            int rgb;
                            if (swatch != null) {
                                rgb = swatch.getRgb();
                            } else {
                                rgb = context.getResources().getColor(R.color.colorAccent);
                            }
                            albumDelegate.setRgb(rgb);
                            infoLayout.setBackgroundColor(rgb);
                        }
                    });
                }
            }
        });
        titleTv.setText(album.getAlbum());
        artistTv.setText(album.getArtist());
        int count = album.getNumSongs();
        countTv.setText(context.getString(count > 1 ? R.string.text_album_songs : R.string.text_album_song, count));
        itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(context, AlbumActivity.class);
                it.putExtra("album", album);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    ActivityOptionsCompat compat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            (Activity)context, thumbIv, context.getString(R.string.translation_thumb));
                    context.startActivity(it, compat.toBundle());
                } else {
                    context.startActivity(it);
                }
            }
        });
    }
}
