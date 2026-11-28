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
        buildSettingRows(view.findViewById(R.id.settingRows));
        return view;
    }

    /** 设置 seg 行（对标 am-4u2 set-row：label + hint + seg 按钮组） */
    private void buildSettingRows(LinearLayout container) {
        if (getContext() == null) return;
        container.removeAllViews();

        // 音质（标准/较高/高品/无损/Hi-Res/母带）
        addSegRow(container, "音质", "选母带需音源能出 AI00；仅启用可用源",
                new String[]{"标准", "较高", "高品", "无损", "Hi-Res", "母带"},
                indexOf(MusicEngine.getStr(getContext(), "quality", "无损"), new String[]{"标准", "较高", "高品", "无损", "Hi-Res", "母带"}),
                i -> MusicEngine.setStr(getContext(), "quality", new String[]{"标准", "较高", "高品", "无损", "Hi-Res", "母带"}[i]));

        // 音质自动降级（开/关）
        addSegRow(container, "音质自动降级", "取不到或播失败时自动降到下一档音质",
                new String[]{"开", "关"},
                MusicEngine.getBool(getContext(), "autoDegrade", true) ? 0 : 1,
                i -> MusicEngine.setBool(getContext(), "autoDegrade", i == 0));

        // 外观（浅色/暗色/系统/自动）
        addSegRow(container, "外观", "浅色 / 暗色 / 跟随系统 / 自动（18:00–06:00 暗色）",
                new String[]{"浅色", "暗色", "系统", "自动"},
                indexOf(MusicEngine.getStr(getContext(), "theme", "自动"), new String[]{"浅色", "暗色", "系统", "自动"}),
                i -> MusicEngine.setStr(getContext(), "theme", new String[]{"浅色", "暗色", "系统", "自动"}[i]));

        // 播放页样式（经典/沉浸）
        addSegRow(container, "播放页样式", "经典卡片 / 沉浸海报（取色融合）",
                new String[]{"经典", "沉浸"},
                indexOf(MusicEngine.getStr(getContext(), "npStyle", "经典"), new String[]{"经典", "沉浸"}),
                i -> MusicEngine.setStr(getContext(), "npStyle", new String[]{"经典", "沉浸"}[i]));

        // 进度条样式（普通/心率/星轨）
        addSegRow(container, "进度条样式", "普通 / 心率 / 星轨",
                new String[]{"普通", "心率", "星轨"},
                indexOf(MusicEngine.getStr(getContext(), "npSeek", "星轨"), new String[]{"普通", "心率", "星轨"}),
                i -> MusicEngine.setStr(getContext(), "npSeek", new String[]{"普通", "心率", "星轨"}[i]));

        // 自动播放（关闭/猜你喜欢/为你推荐/随机歌单/随机榜单）
        addSegRow(container, "自动播放", "点「关闭」不自动播 · 点其他项即开启并播该板块",
                new String[]{"关闭", "猜你喜欢", "为你推荐", "随机歌单", "随机榜单"},
                indexOf(MusicEngine.getStr(getContext(), "autoPlay", "关闭"), new String[]{"关闭", "猜你喜欢", "为你推荐", "随机歌单", "随机榜单"}),
                i -> MusicEngine.setStr(getContext(), "autoPlay", new String[]{"关闭", "猜你喜欢", "为你推荐", "随机歌单", "随机榜单"}[i]));

        // 首页全屏（开启/关闭）
        addSegRow(container, "首页全屏", "FongMi 融合模式：隐藏原生顶/底栏，用安全区避让（仅 edge/immersive）",
                new String[]{"开启", "关闭"},
                MusicEngine.getBool(getContext(), "homeFull", true) ? 0 : 1,
                i -> MusicEngine.setBool(getContext(), "homeFull", i == 0));

        // 歌词（翻译/罗马音/波浪/偏移 + 显示翻译行）
        addSegRow(container, "歌词", "有翻译数据时显示译文行",
                new String[]{"翻译", "罗马音", "波浪", "偏移"},
                indexOf(MusicEngine.getStr(getContext(), "lyricTab", "翻译"), new String[]{"翻译", "罗马音", "波浪", "偏移"}),
                i -> MusicEngine.setStr(getContext(), "lyricTab", new String[]{"翻译", "罗马音", "波浪", "偏移"}[i]));
        addSegRow(container, "显示翻译行", "有 tlyric 等数据时",
                new String[]{"开", "关"},
                MusicEngine.getBool(getContext(), "showTrans", true) ? 0 : 1,
                i -> MusicEngine.setBool(getContext(), "showTrans", i == 0));
    }

    private int indexOf(String val, String[] arr) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(val)) return i;
        return 0;
    }

    /** 单行：label + hint + seg 按钮组（选中高亮） */
    private void addSegRow(LinearLayout container, String label, String hint,
                           String[] options, int selected, java.util.function.Consumer<Integer> onPick) {
        if (getContext() == null) return;

        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, 14, 0, 14);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        row.setLayoutParams(rlp);

        TextView lbl = new TextView(getContext());
        lbl.setText(label);
        lbl.setTextSize(15);
        lbl.setTextColor(0xFFFFFFFF);
        row.addView(lbl);

        if (hint != null && !hint.isEmpty()) {
            TextView hnt = new TextView(getContext());
            hnt.setText(hint);
            hnt.setTextSize(12);
            hnt.setTextColor(0xFF8A8F99);
            row.addView(hnt);
        }

        LinearLayout seg = new LinearLayout(getContext());
        seg.setOrientation(LinearLayout.HORIZONTAL);
        seg.setPadding(0, 10, 0, 0);
        row.addView(seg);

        for (int i = 0; i < options.length; i++) {
            TextView btn = new TextView(getContext());
            btn.setText(options[i]);
            btn.setTextSize(13);
            btn.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(0, dp(38), 1);
            int margin = 4;
            blp.setMargins(margin, 0, margin, 0);
            btn.setLayoutParams(blp);
            boolean sel = i == selected;
            btn.setTextColor(sel ? 0xFFFFFFFF : 0xFF8A8F99);
            btn.setBackground(rounded(sel ? 0xFF3B82F6 : 0xFF2A2E37, 10));
            final int idx = i;
            btn.setOnClickListener(v -> {
                onPick.accept(idx);
                buildSettingRows(container); // 刷新选中态
            });
            seg.addView(btn);
        }

        container.addView(row);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
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
