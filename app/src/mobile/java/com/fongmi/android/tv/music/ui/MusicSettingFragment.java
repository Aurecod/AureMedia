package com.fongmi.android.tv.music.ui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.music.MusicEngine;
import com.fongmi.android.tv.music.MusicSource;
import com.fongmi.android.tv.music.MusicSourceStore;

import java.util.ArrayList;
import java.util.List;

public class MusicSettingFragment extends Fragment {

    private LinearLayout mSrcList;
    private LinearLayout mSrcPlats;
    private EditText mSrcName;
    private EditText mSrcJsUrl;
    private final List<CheckBox> mPlatChecks = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_music_setting, container, false);
        mSrcList = view.findViewById(R.id.srcList);
        mSrcPlats = view.findViewById(R.id.srcPlats);
        mSrcName = view.findViewById(R.id.srcName);
        mSrcJsUrl = view.findViewById(R.id.srcJsUrl);

        buildPlatformChecks();
        view.findViewById(R.id.btnSrcAdd).setOnClickListener(v -> importOnline());
        view.findViewById(R.id.btnSrcLocal).setOnClickListener(v -> Toast.makeText(getContext(), "本地文件：请将 .js 音源放入 存储/AureCod/music/", Toast.LENGTH_SHORT).show());

        renderSourceList();
        return view;
    }

    /** 平台多选 checkbox（qq/netease/kugou/kuwo/migu） */
    private void buildPlatformChecks() {
        if (getContext() == null) return;
        mPlatChecks.clear();
        String[] plats = MusicEngine.platforms();
        String[] names = MusicEngine.platformNames();
        for (int i = 0; i < plats.length; i++) {
            CheckBox cb = new CheckBox(getContext());
            cb.setText(names[i]);
            cb.setTextSize(13);
            cb.setTextColor(0xFFFFFFFF);
            cb.setChecked(i == 1); // 默认选网易
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            cb.setLayoutParams(lp);
            mSrcPlats.addView(cb);
            mPlatChecks.add(cb);
        }
    }

    /** 渲染音源列表（src-card：名称/平台/状态 + 选择/删除） */
    private void renderSourceList() {
        if (getContext() == null) return;
        mSrcList.removeAllViews();
        List<MusicSource> sources = MusicSourceStore.getSources(getContext());
        MusicSource active = MusicSourceStore.getActive(getContext());

        for (MusicSource s : sources) {
            mSrcList.addView(buildSourceCard(s, s.id.equals(active.id)));
        }
    }

    private View buildSourceCard(MusicSource s, boolean selected) {
        LinearLayout card = new LinearLayout(getContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(14, 14, 14, 14);
        card.setBackground(rounded(selected ? 0xFF2E3A52 : 0xFF22252E, 14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 6, 0, 6);
        card.setLayoutParams(lp);

        // 名称 + 平台 + 状态
        LinearLayout main = new LinearLayout(getContext());
        main.setOrientation(LinearLayout.VERTICAL);
        main.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(main);

        TextView name = new TextView(getContext());
        name.setText(s.name + (s.builtin ? "" : "") + (selected ? " · 使用中" : ""));
        name.setTextSize(14);
        name.setTextColor(0xFFFFFFFF);
        main.addView(name);

        TextView plats = new TextView(getContext());
        StringBuilder sb = new StringBuilder();
        if (s.platforms != null) {
            for (String p : s.platforms) {
                sb.append(platName(p)).append(" ");
            }
        }
        plats.setText(sb.toString().trim() + " · " + s.type.toUpperCase());
        plats.setTextSize(11);
        plats.setTextColor(0xFF8A8F99);
        main.addView(plats);

        // 删除按钮（内置不可删）
        if (!s.builtin) {
            TextView del = new TextView(getContext());
            del.setText("删除");
            del.setTextSize(12);
            del.setTextColor(0xFFE05555);
            del.setPadding(14, 8, 8, 8);
            del.setOnClickListener(v -> {
                MusicSourceStore.removeSource(getContext(), s.id);
                renderSourceList();
            });
            card.addView(del);
        }

        // 点击卡片 = 选择音源（active 决定平台）
        card.setOnClickListener(v -> {
            MusicSourceStore.setActive(getContext(), s.id);
            renderSourceList();
            Toast.makeText(getContext(), "已切换音源：" + s.name, Toast.LENGTH_SHORT).show();
        });
        return card;
    }

    /** 在线导入：校验名称 + URL，添加音源 */
    private void importOnline() {
        if (getContext() == null) return;
        String name = mSrcName.getText().toString().trim();
        String url = mSrcJsUrl.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "请填写音源名称", Toast.LENGTH_SHORT).show();
            return;
        }
        List<String> plats = new ArrayList<>();
        String[] platsArr = MusicEngine.platforms();
        for (int i = 0; i < mPlatChecks.size(); i++) {
            if (mPlatChecks.get(i).isChecked()) plats.add(platsArr[i]);
        }
        if (plats.isEmpty()) {
            Toast.makeText(getContext(), "请选择对接平台", Toast.LENGTH_SHORT).show();
            return;
        }
        String type = url.startsWith("http") ? "js" : "api";
        MusicSourceStore.addSource(getContext(), name, type, url, plats);
        mSrcName.setText("");
        mSrcJsUrl.setText("");
        renderSourceList();
        Toast.makeText(getContext(), "已导入音源：" + name, Toast.LENGTH_SHORT).show();
    }

    private String platName(String p) {
        String[] plats = MusicEngine.platforms();
        String[] names = MusicEngine.platformNames();
        for (int i = 0; i < plats.length; i++) {
            if (plats[i].equals(p)) return names[i];
        }
        return p;
    }

    private android.graphics.drawable.Drawable rounded(int color, int radius) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }
}
