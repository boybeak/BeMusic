package com.nulldreams.bemusic.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.ViewPropertyAnimation;
import com.nulldreams.bemusic.R;
import com.nulldreams.bemusic.manager.PlayManager;
import com.nulldreams.bemusic.model.Song;

import java.io.File;

import jp.wasabeef.glide.transformations.BlurTransformation;

/**
 * Created by boybe on 2016/12/28.
 */

public class PlayDetailFragment extends Fragment {

    private static final String TAG = PlayDetailFragment.class.getSimpleName();

    public static PlayDetailFragment newInstance () {
        return new PlayDetailFragment();
    }

    private ImageView mThumbIv, mBgIv;

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = null;
        if (enter) {
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.anim_bottom_in);
        } else {
            animation = AnimationUtils.loadAnimation(getContext(), R.anim.anim_bottom_out);
        }
        return animation;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_play_detail, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mThumbIv = (ImageView)view.findViewById(R.id.play_detail_thumb);
        mBgIv = (ImageView)view.findViewById(R.id.play_detail_bg);

        Log.v(TAG, "onViewCreated view.width=" + view.getWidth() + " view.height=" + view.getHeight());

        Song song = PlayManager.getInstance(getContext()).getCurrentSong();
        if (song != null) {
            File file = song.getCoverFile(getContext());
            if (file.exists()) {
                Glide.with(getContext()).load(file).into(mThumbIv);
                Glide.with(getContext()).load(file).asBitmap().animate(android.R.anim.fade_in)
                        .transform(new BlurTransformation(getContext())).into(mBgIv);
            }
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.v(TAG, "onDetach");
    }
}
