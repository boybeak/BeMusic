package com.nulldreams.bemusic.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.nulldreams.adapter.DelegateAdapter;
import com.nulldreams.bemusic.R;

/**
 * Created by gaoyunfei on 2017/1/1.
 */

public abstract class RvFragment extends Fragment {

    private DelegateAdapter mAdapter;
    private RecyclerView mRv;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rv, null);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRv = (RecyclerView)view.findViewById(R.id.rv);
        mRv.setLayoutManager(getLayoutManager());
        mAdapter = new DelegateAdapter(getContext());
        mRv.setAdapter(mAdapter);
    }

    public abstract RecyclerView.LayoutManager getLayoutManager ();

    public RecyclerView getRecyclerView () {
        return mRv;
    }

    public DelegateAdapter getAdapter () {
        return mAdapter;
    }

    public abstract CharSequence getTitle (Context context, Object ... params);
}
