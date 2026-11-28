package com.fongmi.android.tv.music.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicLibStore;
import com.fongmi.android.tv.music.MusicSong;

import java.util.List;

/** 资料库（对标 am-4u2 lib：顶栏 4 分类图标，选中展开名称，单行歌曲/歌单，歌单可展开） */
public class MusicLibFragment extends Fragment {

    private LinearLayout mTabs;
    private LinearLayout mContent;
    private int mCurrent = -1;

    // 4 分类：图标 + 名称 + 内容
    private static final String[][] CATS = {
            {"♡", "喜欢的音乐"},
            {"🎵", "喜欢的歌单"},
            {"🕐", "最近播放"},
            {"⬇", "已下载"},
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_lib, container, false);
        mTabs = view.findViewById(R.id.libTabs);
        mContent = view.findViewById(R.id.libContent);
        renderTabs();
        // 默认选"喜欢的音乐"
        selectTab(0);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mContent != null) renderContent();
    }

    /** 顶栏 4 分类（默认图标，选中展开名称） */
    private void renderTabs() {
        if (getContext() == null) return;
        mTabs.removeAllViews();
        for (int i = 0; i < CATS.length; i++) {
            final int idx = i;
            TextView tab = new TextView(getContext());
            tab.setGravity(Gravity.CENTER);
            tab.setTextSize(20);
            tab.setPadding(dp(14), dp(10), dp(14), dp(10));
            tab.setBackground(rounded(i == mCurrent ? 0xFF3B82F6 : 0xFF2A2E37, 22));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(6), 0, dp(6), 0);
            tab.setLayoutParams(lp);
            tab.setOnClickListener(v -> selectTab(idx));
            mTabs.addView(tab);
        }
        updateTabLabels();
    }

    /** 更新 tab 文字：选中显示 图标+名称，未选中只图标 */
    private void updateTabLabels() {
        for (int i = 0; i < mTabs.getChildCount(); i++) {
            TextView tab = (TextView) mTabs.getChildAt(i);
            if (i == mCurrent) {
                tab.setText(CATS[i][0] + " " + CATS[i][1]);
                tab.setTextSize(14);
                tab.setTextColor(0xFFFFFFFF);
            } else {
                tab.setText(CATS[i][0]);
                tab.setTextSize(20);
                tab.setTextColor(0xFF8A8F99);
            }
        }
    }

    private void selectTab(int idx) {
        mCurrent = idx;
        renderTabs();
        renderContent();
    }

    /** 渲染当前分类内容（单行歌曲，歌单可展开） */
    private void renderContent() {
        if (getContext() == null) return;
        mContent.removeAllViews();
        switch (mCurrent) {
            case 0: renderSongs("喜欢的音乐", MusicLibStore.getFav(getContext()), false); break;
            case 1: renderPlaylists("喜欢的歌单", true); break;
            case 2: renderSongs("最近播放", MusicLibStore.getRecent(getContext()), false); break;
            case 3: renderSongs("已下载", MusicLibStore.getDownloaded(getContext()), false); break;
        }
    }

    /** 单行歌曲列表（歌曲行） */
    private void renderSongs(String title, List<MusicSong> songs, boolean expandable) {
        if (songs == null || songs.isEmpty()) {
            addEmpty();
            return;
        }
        for (MusicSong s : songs) {
            mContent.addView(buildSongRow(s));
        }
    }

    /** 喜欢的歌单（点击可向下展开包含歌曲） */
    private void renderPlaylists(String title, boolean expandable) {
        // 歌单来自收藏歌曲的分组（简化：按歌单名分组，暂以收藏歌曲模拟"喜欢的歌单"）
        List<MusicSong> fav = MusicLibStore.getFav(getContext());
        if (fav == null || fav.isEmpty()) {
            addEmpty();
            return;
        }
        // 单行歌单（点开显示其中歌曲）
        LinearLayout plRow = buildPlaylistRow("我的收藏", fav);
        mContent.addView(plRow);
    }

    /** 歌单行：点击向下展开包含歌曲 */
    private LinearLayout buildPlaylistRow(String name, List<MusicSong> songs) {
        LinearLayout pl = new LinearLayout(getContext());
        pl.setOrientation(LinearLayout.VERTICAL);
        pl.setBackground(rounded(0xFF1A1D26, 12));
        pl.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        plp.setMargins(0, dp(4), 0, dp(4));
        pl.setLayoutParams(plp);

        TextView head = new TextView(getContext());
        head.setText("🎵 " + name + " (" + songs.size() + ")");
        head.setTextSize(15);
        head.setTextColor(0xFFFFFFFF);
        pl.addView(head);

        LinearLayout songsWrap = new LinearLayout(getContext());
        songsWrap.setOrientation(LinearLayout.VERTICAL);
        songsWrap.setVisibility(View.GONE); // 默认收起
        pl.addView(songsWrap);

        head.setOnClickListener(v -> {
            boolean open = songsWrap.getVisibility() == View.GONE;
            songsWrap.setVisibility(open ? View.VISIBLE : View.GONE);
            head.setText("🎵 " + name + " (" + songs.size() + ")" + (open ? " ⌄" : " ›"));
        });

        for (MusicSong s : songs) {
            songsWrap.addView(buildSongRow(s));
        }
        return pl;
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
            Toast.makeText(getContext(), "播放：" + s.name, Toast.LENGTH_SHORT).show();
            MusicPlayer.get().play(getContext(), s);
        });
        return row;
    }

    private void addEmpty() {
        TextView empty = new TextView(getContext());
        empty.setText("空空如也");
        empty.setTextSize(13);
        empty.setTextColor(0xFF8A8F99);
        empty.setPadding(dp(8), dp(16), dp(8), dp(16));
        mContent.addView(empty);
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
