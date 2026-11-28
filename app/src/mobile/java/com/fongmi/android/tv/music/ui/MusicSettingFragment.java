package com.fongmi.android.tv.music.ui;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicEngine;

public class MusicSettingFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_setting, container, false);
        LinearLayout group = view.findViewById(R.id.musicPlatformGroup);
        TextView sourceInfo = view.findViewById(R.id.musicSourceInfo);
        buildPlatformGroup(group, sourceInfo);
        return view;
    }

    /** 生成平台选择按钮组（主页展示平台） */
    private void buildPlatformGroup(LinearLayout group, TextView sourceInfo) {
        if (getContext() == null) return;
        group.removeAllViews();
        String[] platforms = MusicEngine.platforms();
        String[] names = MusicEngine.platformNames();
        String current = MusicEngine.getPlatform(getContext());
        int selected = MusicEngine.platformIndex(current);

        for (int i = 0; i < platforms.length; i++) {
            TextView tv = new TextView(getContext());
            tv.setText(names[i]);
            tv.setTextSize(16);
            tv.setPadding(24, 20, 24, 20);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 6, 0, 6);
            tv.setLayoutParams(lp);
            applyStyle(tv, i == selected);
            final int idx = i;
            tv.setOnClickListener(v -> {
                MusicEngine.setPlatform(getContext(), platforms[idx]);
                // 刷新选中态
                buildPlatformGroup(group, sourceInfo);
                // 通知主页刷新（重新进主页即可）
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.musicContent, new MusicHomeFragment())
                            .commit();
                }
            });
            group.addView(tv);
        }
        sourceInfo.setText("网易云音源（支持 网易/QQ/酷我/酷狗/咪咕）");
    }

    private void applyStyle(TextView tv, boolean selected) {
        tv.setTextColor(selected ? 0xFF12141A : 0xFFFFFFFF);
        tv.setBackground(selected ? roundedBackground(0xFF3B82F6) : roundedBackground(0xFF2A2E37));
    }

    private android.graphics.drawable.Drawable roundedBackground(int color) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(24);
        return g;
    }
}
