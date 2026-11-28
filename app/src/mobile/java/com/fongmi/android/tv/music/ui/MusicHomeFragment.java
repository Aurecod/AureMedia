package com.fongmi.android.tv.music.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicEngine;
import com.fongmi.android.tv.music.MusicSong;

import java.util.ArrayList;
import java.util.List;

public class MusicHomeFragment extends Fragment {

    private RecyclerView mHomeList;
    private MusicHomeAdapter mAdapter;
    private TextView mPlatformBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_home, container, false);
        mHomeList = view.findViewById(R.id.musicHomeList);
        mPlatformBtn = view.findViewById(R.id.musicPlatformBtn);
        mAdapter = new MusicHomeAdapter();
        mHomeList.setLayoutManager(new LinearLayoutManager(getContext()));
        mHomeList.setAdapter(mAdapter);
        updatePlatformBtn();
        mPlatformBtn.setOnClickListener(v -> showPlatformMenu(v));
        loadHome();
        return view;
    }

    /** 更新平台按钮文字（当前平台名） */
    private void updatePlatformBtn() {
        if (getContext() == null) return;
        String platform = MusicEngine.getPlatform(getContext());
        int idx = MusicEngine.platformIndex(platform);
        mPlatformBtn.setText(MusicEngine.platformNames()[idx] + " ▾");
    }

    /** 点击平台按钮 → 展开 5 平台下拉 */
    private void showPlatformMenu(View anchor) {
        if (getContext() == null) return;
        String current = MusicEngine.getPlatform(getContext());

        LinearLayout menu = new LinearLayout(getContext());
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setBackground(rounded(0xFF2A2E37, 20));
        menu.setPadding(8, 8, 8, 8);

        final PopupWindow popup = new PopupWindow(menu,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);

        String[] platforms = MusicEngine.platforms();
        String[] names = MusicEngine.platformNames();
        for (int i = 0; i < platforms.length; i++) {
            TextView tv = new TextView(getContext());
            tv.setText(names[i]);
            tv.setTextSize(15);
            tv.setPadding(24, 18, 24, 18);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(4, 4, 4, 4);
            tv.setLayoutParams(lp);
            boolean sel = platforms[i].equals(current);
            tv.setTextColor(sel ? Color.WHITE : Color.rgb(0x8A, 0x8F, 0x99));
            tv.setBackground(rounded(sel ? 0xFF3B82F6 : 0xFF3A3F4A, 14));
            final int idx = i;
            tv.setOnClickListener(v -> {
                MusicEngine.setPlatform(getContext(), platforms[idx]);
                updatePlatformBtn();
                popup.dismiss();
                loadHome(); // 切换平台刷新榜单
            });
            menu.addView(tv);
        }

        popup.setOutsideTouchable(true);
        popup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
        popup.setElevation(24);
        popup.setAnimationStyle(android.R.style.Animation_Dialog);
        popup.showAsDropDown(anchor, 0, 8, Gravity.END);
    }

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private void loadHome() {
        // 异步加载当前平台热榜（音源驱动）
        new Thread(() -> {
            try {
                List<MusicSong> songs = MusicEngine.hot(getContext());
                if (songs != null && !songs.isEmpty() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        List<Object> items = new ArrayList<>();
                        items.add("热门歌曲"); // 板块标题
                        items.addAll(songs.subList(0, Math.min(20, songs.size())));
                        mAdapter.setItems(items);
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
