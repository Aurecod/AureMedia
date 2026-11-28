package com.fongmi.android.tv.music.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicEngine;
import com.fongmi.android.tv.music.MusicSong;

import java.util.List;

/** 搜索（对标 am-4u2 #p-search：搜索框 + 分类 tabs 歌曲/歌手/专辑/歌单 + 热词 chips + 结果） */
public class MusicSearchFragment extends Fragment {

    private static final String[][] TABS = {
            {"歌曲", "1"},
            {"歌手", "100"},
            {"专辑", "10"},
            {"歌单", "1000"},
    };
    private static final String[] HOT_CHIPS = {"消愁", "热歌", "成全", "多远都要在一起", "民谣", "摇滚", "演员", "董小姐"};

    private EditText mInput;
    private LinearLayout mTabs;
    private LinearLayout mChips;
    private LinearLayout mResult;
    private int mType = 1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_search, container, false);
        mInput = view.findViewById(R.id.musicSearchInput);
        mTabs = view.findViewById(R.id.searchTabs);
        mChips = view.findViewById(R.id.searchChips);
        mResult = view.findViewById(R.id.searchResult);

        buildTabs();
        buildChips();
        view.findViewById(R.id.musicSearchBtn).setOnClickListener(v -> doSearch());
        return view;
    }

    private void buildTabs() {
        if (getContext() == null) return;
        mTabs.removeAllViews();
        for (int i = 0; i < TABS.length; i++) {
            TextView tab = new TextView(getContext());
            tab.setText(TABS[i][0]);
            tab.setTextSize(14);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(16), dp(8), dp(16), dp(8));
            boolean sel = i == typeIndex(mType);
            tab.setTextColor(sel ? 0xFFFFFFFF : 0xFF8A8F99);
            tab.setBackground(rounded(sel ? 0xFF3B82F6 : 0xFF2A2E37, 18));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            lp.setMargins(dp(3), 0, dp(3), 0);
            tab.setLayoutParams(lp);
            final int idx = i;
            tab.setOnClickListener(v -> {
                mType = Integer.parseInt(TABS[idx][1]);
                buildTabs();
                doSearch();
            });
            mTabs.addView(tab);
        }
    }

    private int typeIndex(int type) {
        for (int i = 0; i < TABS.length; i++) if (Integer.parseInt(TABS[i][1]) == type) return i;
        return 0;
    }

    private void buildChips() {
        if (getContext() == null) return;
        mChips.removeAllViews();
        for (String chip : HOT_CHIPS) {
            TextView c = new TextView(getContext());
            c.setText(chip);
            c.setTextSize(12);
            c.setPadding(dp(12), dp(6), dp(12), dp(6));
            c.setTextColor(0xFF8A8F99);
            c.setBackground(rounded(0xFF2A2E37, 16));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(3), dp(3), dp(3), dp(3));
            c.setLayoutParams(lp);
            c.setOnClickListener(v -> {
                mInput.setText(chip);
                doSearch();
            });
            mChips.addView(c);
        }
    }

    private void doSearch() {
        String kw = mInput.getText().toString().trim();
        if (kw.isEmpty()) return;
        mResult.removeAllViews();
        new Thread(() -> {
            try {
                List<Object> results = MusicEngine.searchType(getContext(), kw, mType);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> renderResults(results));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void renderResults(List<Object> results) {
        if (getContext() == null) return;
        mResult.removeAllViews();
        if (results.isEmpty()) {
            addEmpty();
            return;
        }
        for (Object r : results) {
            if (r instanceof MusicSong) {
                mResult.addView(buildSongRow((MusicSong) r));
            } else {
                mResult.addView(buildTextRow(String.valueOf(r)));
            }
        }
    }

    private View buildSongRow(MusicSong s) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(10), dp(8), dp(10));

        TextView name = new TextView(getContext());
        name.setText(s.name);
        name.setTextSize(14);
        name.setTextColor(0xFFFFFFFF);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(name);

        TextView singer = new TextView(getContext());
        singer.setText(s.singer);
        singer.setTextSize(12);
        singer.setTextColor(0xFF8A8F99);
        row.addView(singer);

        row.setOnClickListener(v -> {
            String url = MusicEngine.resolveUrl(getContext(), s);
            if (url == null) {
                Toast.makeText(getContext(), s.source + " 播放受限", Toast.LENGTH_SHORT).show();
                return;
            }
            MusicPlayer.get().play(getContext(), s);
        });
        return row;
    }

    private View buildTextRow(String text) {
        TextView row = new TextView(getContext());
        row.setText(text);
        row.setTextSize(14);
        row.setTextColor(0xFFFFFFFF);
        row.setPadding(dp(8), dp(12), dp(8), dp(12));
        return row;
    }

    private void addEmpty() {
        TextView empty = new TextView(getContext());
        empty.setText("无结果");
        empty.setTextSize(13);
        empty.setTextColor(0xFF8A8F99);
        empty.setPadding(dp(8), dp(16), dp(8), dp(16));
        mResult.addView(empty);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    private android.graphics.drawable.Drawable rounded(int color, int radius) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }
}
