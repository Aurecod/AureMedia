package com.fongmi.android.tv.music.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicEngine;
import com.fongmi.android.tv.music.MusicPlaylist;
import com.fongmi.android.tv.music.MusicSong;

import java.util.List;

/** 主页（对标 am-4u2：为你推荐/频道/热门歌曲/精品榜单，可滚动） */
public class MusicHomeFragment extends Fragment {

    private LinearLayout mContent;
    private TextView mPlatformBtn;
    private boolean mAutoPlayed; // 本次页面已自动播放（防重复）

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_home, container, false);
        mContent = view.findViewById(R.id.musicHomeList);
        mPlatformBtn = view.findViewById(R.id.musicPlatformBtn);
        updatePlatformBtn();
        mPlatformBtn.setOnClickListener(v -> showPlatformMenu(v));
        loadAll();
        return view;
    }

    /** 更新平台按钮文字 */
    private void updatePlatformBtn() {
        if (getContext() == null) return;
        String platform = MusicEngine.getPlatform(getContext());
        int idx = MusicEngine.platformIndex(platform);
        mPlatformBtn.setText(MusicEngine.platformNames()[idx] + " ▾");
        // 平台按钮用平台官方色（am-4u2 accent）
        mPlatformBtn.setBackgroundColor(MusicEngine.platformColor(platform));
    }

    /** 加载全部板块 */
    private void loadAll() {
        new Thread(() -> {
            try {
                final List<MusicSong> hot = MusicEngine.hot(getContext());
                final List<MusicPlaylist> recommend = MusicEngine.recommend(getContext());
                final List<MusicPlaylist> ranks = MusicEngine.ranks(getContext());
                final List<MusicPlaylist> channels = MusicEngine.channels(getContext());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        mContent.removeAllViews();
                        // 为你推荐（横向）
                        addSectionTitle("为你推荐");
                        addHorizontalCards(recommend);
                        // 频道（grid 3列）
                        addSectionTitle("频道");
                        addGridCards(channels);
                        // 热门歌曲（列表）
                        addSectionTitle("热门歌曲");
                        addSongList(hot);
                        // 精品榜单（grid 3列）
                        addSectionTitle("精品榜单");
                        addGridCards(ranks);
                        // 自动播放（读设置，非关闭则播热门第一首）
                        autoPlay(hot);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    /** 自动播放：读"自动播放"设置，非关闭则播热门第一首（首次进入） */
    private void autoPlay(List<MusicSong> hot) {
        String mode = MusicEngine.getStr(getContext(), "自动播放", "关闭");
        if ("关闭".equals(mode) || hot == null || hot.isEmpty()) return;
        if (mAutoPlayed) return;
        mAutoPlayed = true;
        MusicPlayer.get().play(getContext(), hot.get(0), hot, 0);
    }

    /** 板块标题 */
    /** 卡片背景色（按主题） */
    private int cardBg() {
        return MusicEngine.isDarkTheme(getContext()) ? 0xFF2A2E37 : 0xFFF0F1F4;
    }

    private void addSectionTitle(String title) {
        TextView tv = new TextView(getContext());
        tv.setText(title);
        tv.setTextSize(16);
        tv.setTextColor(MusicEngine.isDarkTheme(getContext()) ? 0xFFFFFFFF : 0xFF1C1C1E);
        tv.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tv.setPadding(dp(16), dp(18), dp(16), dp(8));
        mContent.addView(tv);
    }

    /** 横向滚动歌单卡（为你推荐） */
    private void addHorizontalCards(List<MusicPlaylist> list) {
        if (list == null || list.isEmpty()) return;
        HorizontalScrollView hsv = new HorizontalScrollView(getContext());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 0, 0, 0);
        hsv.addView(row);
        for (MusicPlaylist p : list) row.addView(buildPlaylistCard(p, dp(120)));
        mContent.addView(hsv);
    }

    /** grid 3列卡片（频道/精品榜单） */
    private void addGridCards(List<MusicPlaylist> list) {
        if (list == null || list.isEmpty()) return;
        GridLayout grid = new GridLayout(getContext());
        grid.setColumnCount(3);
        grid.setPadding(0, 0, 0, 0);
        for (MusicPlaylist p : list) {
            LinearLayout card = buildPlaylistCard(p, dp(110));
            GridLayout.LayoutParams glp = new GridLayout.LayoutParams();
            glp.width = 0;
            glp.height = GridLayout.LayoutParams.WRAP_CONTENT;
            glp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            glp.setMargins(dp(4), dp(4), dp(4), dp(4));
            card.setLayoutParams(glp);
            grid.addView(card);
        }
        mContent.addView(grid);
    }

    /** 歌单卡（封面+名称） */
    private LinearLayout buildPlaylistCard(MusicPlaylist p, int coverSize) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(6), dp(6), dp(6), dp(6));
        ImageView cover = new ImageView(getContext());
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(coverSize, coverSize);
        cover.setLayoutParams(clp);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        cover.setBackground(rounded(cardBg(), 10));
        if (p.cover != null && !p.cover.isEmpty()) {
            Glide.with(this).load(p.cover).into(cover);
        } else {
            // 无封面：方块内显示名称首字/序号（避免纯色方块）
            TextView ph = new TextView(getContext());
            ph.setText(p.name.length() > 0 ? p.name.substring(0, Math.min(2, p.name.length())) : "♪");
            ph.setTextSize(20);
            ph.setTextColor(MusicEngine.isDarkTheme(getContext()) ? 0xFF8A8F99 : 0xFF9A9FA8);
            ph.setGravity(Gravity.CENTER);
            cover.addView(ph, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        }
        card.addView(cover);
        TextView name = new TextView(getContext());
        name.setText(p.name);
        name.setTextSize(12);
        name.setTextColor(MusicEngine.isDarkTheme(getContext()) ? 0xFFFFFFFF : 0xFF1C1C1E);
        name.setMaxLines(2);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        card.addView(name);
        card.setOnClickListener(v -> Toast.makeText(getContext(), "歌单：" + p.name, Toast.LENGTH_SHORT).show());
        return card;
    }

    /** 热门歌曲列表（歌曲行） */
    private void addSongList(List<MusicSong> list) {
        if (list == null || list.isEmpty()) return;
        LinearLayout wrap = new LinearLayout(getContext());
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.setPadding(0, 0, 0, 0);
        for (MusicSong s : list.subList(0, Math.min(15, list.size()))) {
            wrap.addView(buildSongRow(s));
        }
        mContent.addView(wrap);
    }

    private View buildSongRow(MusicSong s) {
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(10), dp(8), dp(10));

        TextView num = new TextView(getContext());
        num.setText("▶");
        num.setTextSize(13);
        num.setTextColor(MusicEngine.platformColor(MusicEngine.getPlatform(getContext())));
        num.setPadding(0, 0, dp(10), 0);
        row.addView(num);

        LinearLayout meta = new LinearLayout(getContext());
        meta.setOrientation(LinearLayout.VERTICAL);
        meta.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(meta);

        TextView name = new TextView(getContext());
        name.setText(s.name);
        name.setTextSize(14);
        name.setTextColor(MusicEngine.isDarkTheme(getContext()) ? 0xFFFFFFFF : 0xFF1C1C1E);
        meta.addView(name);

        TextView singer = new TextView(getContext());
        singer.setText(s.singer);
        singer.setTextSize(12);
        singer.setTextColor(MusicEngine.isDarkTheme(getContext()) ? 0xFF8A8F99 : 0xFF9A9FA8);
        meta.addView(singer);

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

    /** 平台下拉菜单 */
    private void showPlatformMenu(View anchor) {
        if (getContext() == null) return;
        String current = MusicEngine.getPlatform(getContext());
        LinearLayout menu = new LinearLayout(getContext());
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackground(rounded(cardBg(), 20));
        menu.setPadding(dp(8), dp(8), dp(8), dp(8));
        final PopupWindow popup = new PopupWindow(menu,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        String[] plats = MusicEngine.platforms();
        String[] names = MusicEngine.platformNames();
        for (int i = 0; i < plats.length; i++) {
            TextView tv = new TextView(getContext());
            tv.setText(names[i]);
            tv.setTextSize(15);
            tv.setPadding(dp(24), dp(18), dp(24), dp(18));
            boolean sel = plats[i].equals(current);
            tv.setTextColor(sel ? 0xFFFFFFFF : 0xFF8A8F99);
            tv.setBackground(rounded(sel ? MusicEngine.platformColor(plats[idx]) : 0xFF3A3F4A, 14));
            final int idx = i;
            tv.setOnClickListener(v -> {
                MusicEngine.setPlatform(getContext(), plats[idx]);
                updatePlatformBtn();
                popup.dismiss();
                loadAll();
            });
            menu.addView(tv);
        }
        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popup.setElevation(24);
        popup.showAsDropDown(anchor, 0, dp(8), Gravity.END);
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
