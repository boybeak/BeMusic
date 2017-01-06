package com.nulldreams.bemusic.adapter;

import android.content.Context;
import android.graphics.Bitmap;
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
import com.nulldreams.media.model.Album;

/**
 * Created by boybe on 2017/1/6.
 */

public class AlbumHolder extends AbsViewHolder<AlbumDelegate> {

    private ImageView thumbIv;
    private TextView titleTv, artistTv;
    private View infoLayout;

    public AlbumHolder(View itemView) {
        super(itemView);
        thumbIv = (ImageView)findViewById(R.id.album_thumb);
        titleTv = (TextView)findViewById(R.id.album_title);
        artistTv = (TextView)findViewById(R.id.album_artist);
        infoLayout = findViewById(R.id.album_info_layout);
    }

    @Override
    public void onBindView(Context context, final AlbumDelegate albumDelegate, int position, DelegateAdapter adapter) {
        Album album = albumDelegate.getSource();
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
                            if (swatch != null) {
                                int rgb = swatch.getRgb();
                                albumDelegate.setRgb(rgb);
                                infoLayout.setBackgroundColor(rgb);
                            }
                        }
                    });
                }
            }
        });
        titleTv.setText(album.getAlbum());
        artistTv.setText(album.getArtist());
    }
}
