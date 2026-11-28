package com.fongmi.android.tv.music.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.QqMusicApi;
import com.fongmi.android.tv.music.MusicSong;

import java.util.ArrayList;
import java.util.List;

public class MusicHomeFragment extends Fragment {

    private RecyclerView mHomeList;
    private MusicHomeAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_home, container, false);
        mHomeList = view.findViewById(R.id.musicHomeList);
        mAdapter = new MusicHomeAdapter();
        mHomeList.setLayoutManager(new LinearLayoutManager(getContext()));
        mHomeList.setAdapter(mAdapter);
        loadHome();
        return view;
    }

    private void loadHome() {
        // 异步加载热榜
        new Thread(() -> {
            try {
                List<MusicSong> songs = QqMusicApi.playlist(QqMusicApi.HOT_PLAYLISTS[1]); // 热歌榜
                if (songs != null && !songs.isEmpty() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        List<Object> items = new ArrayList<>();
                        items.add("为你推荐"); // 板块标题
                        items.addAll(songs.subList(0, Math.min(10, songs.size())));
                        mAdapter.setItems(items);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
