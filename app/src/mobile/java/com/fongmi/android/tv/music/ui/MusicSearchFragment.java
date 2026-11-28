package com.fongmi.android.tv.music.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicSong;
import com.fongmi.android.tv.music.QqMusicApi;

import java.util.ArrayList;
import java.util.List;

public class MusicSearchFragment extends Fragment {

    private EditText mInput;
    private RecyclerView mList;
    private MusicHomeAdapter mAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_search, container, false);
        mInput = view.findViewById(R.id.musicSearchInput);
        mList = view.findViewById(R.id.musicSearchList);
        mAdapter = new MusicHomeAdapter();
        mList.setLayoutManager(new LinearLayoutManager(getContext()));
        mList.setAdapter(mAdapter);
        TextView btn = view.findViewById(R.id.musicSearchBtn);
        btn.setOnClickListener(v -> doSearch());
        return view;
    }

    private void doSearch() {
        String kw = mInput.getText().toString().trim();
        if (kw.isEmpty()) return;
        new Thread(() -> {
            try {
                List<MusicSong> songs = QqMusicApi.search(kw);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        List<Object> items = new ArrayList<>();
                        items.addAll(songs);
                        mAdapter.setItems(items);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
