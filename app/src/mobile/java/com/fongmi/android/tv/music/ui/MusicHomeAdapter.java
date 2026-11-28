package com.fongmi.android.tv.music.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicSong;

import java.util.ArrayList;
import java.util.List;

public class MusicHomeAdapter extends RecyclerView.Adapter<MusicHomeAdapter.VH> {

    private final List<Object> mItems = new ArrayList<>();
    private static final int TYPE_HEADER = 0, TYPE_SONG = 1;

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
        } else {
            holder.title.setText((String) item);
        }
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
