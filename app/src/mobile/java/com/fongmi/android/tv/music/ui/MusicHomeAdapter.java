package com.fongmi.android.tv.music.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicEngine;
import com.fongmi.android.tv.music.MusicSong;

import java.util.ArrayList;
import java.util.List;

public class MusicHomeAdapter extends RecyclerView.Adapter<MusicHomeAdapter.VH> {

    private final List<Object> mItems = new ArrayList<>();
    private static final int TYPE_HEADER = 0, TYPE_SONG = 1;
    private MusicPlayer mPlayer;

    public MusicHomeAdapter() {
        mPlayer = new MusicPlayer();
    }

    public void setItems(List<Object> items) {
        mItems.clear();
        mItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return mItems.get(position) instanceof MusicSong ? TYPE_SONG : TYPE_HEADER;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(viewType == TYPE_HEADER ? R.layout.item_music_header : R.layout.item_music_song, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        Object item = mItems.get(position);
        if (item instanceof MusicSong) {
            MusicSong s = (MusicSong) item;
            holder.title.setText(s.name);
            holder.subtitle.setText(s.singer);
            holder.itemView.setOnClickListener(v -> playSong(holder.itemView, s));
        } else {
            holder.title.setText((String) item);
        }
    }

    private void playSong(View v, MusicSong s) {
        String url = MusicEngine.resolveUrl(v.getContext(), s);
        if (url == null) {
            Toast.makeText(v.getContext(), s.source + " 平台播放受限（需登录/VIP），可切换到网易", Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(v.getContext(), "播放：" + s.name, Toast.LENGTH_SHORT).show();
        mPlayer.play(v.getContext(), s.name, url);
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView title, subtitle;

        VH(View v) {
            super(v);
            title = v.findViewById(R.id.musicTitle);
            subtitle = v.findViewById(R.id.musicSubtitle);
        }
    }
}
