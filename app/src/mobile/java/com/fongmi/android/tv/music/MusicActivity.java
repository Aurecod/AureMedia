package com.fongmi.android.tv.music;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityMusicBinding;
import com.fongmi.android.tv.music.MusicSong;
import com.fongmi.android.tv.music.ui.MusicHomeFragment;
import com.fongmi.android.tv.music.ui.MusicLibFragment;
import com.fongmi.android.tv.music.ui.MusicPlayer;
import com.fongmi.android.tv.music.ui.MusicSearchFragment;
import com.fongmi.android.tv.music.ui.MusicSettingFragment;
import com.fongmi.android.tv.ui.base.BaseActivity;

public class MusicActivity extends BaseActivity {

    private ActivityMusicBinding mBinding;
    private int currentTab = 0;

    @Override
    protected ViewBinding getBinding() {
        mBinding = ActivityMusicBinding.inflate(getLayoutInflater());
        return mBinding;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // BaseActivity 处理状态栏 edge-to-edge

        mBinding.musicTabHome.setOnClickListener(v -> switchTab(0));
        mBinding.musicTabLib.setOnClickListener(v -> switchTab(1));
        mBinding.musicTabSearch.setOnClickListener(v -> switchTab(2));
        mBinding.musicTabSetting.setOnClickListener(v -> switchTab(3));

        // 迷你播放条：播放/暂停切换 + 显示当前歌曲 + 点击展开播放页
        mBinding.musicMiniPlay.setOnClickListener(v -> MusicPlayer.get().togglePlay());
        mBinding.musicMini.setOnClickListener(v -> startActivity(new Intent(this, MusicPlayActivity.class)));
        MusicPlayer.get().setListener(new MusicPlayer.Listener() {
            @Override
            public void onSongChanged(MusicSong song, boolean playing) {
                runOnUiThread(() -> updateMini(song, playing));
            }
            @Override
            public void onProgress(long posMs, long durMs) { }
        });
        updateMini(MusicPlayer.get().currentSong(), MusicPlayer.get().isPlaying());

        applyTheme();
        applyFullscreen();
        // 状态栏避让：edge-to-edge 下根布局加状态栏高度 padding（平台切换按钮不再顶到状态栏）
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(mBinding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets sb = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.statusBars());
            v.setPadding(0, sb.top, 0, sb.bottom);
            return insets;
        });
        switchTab(0);
    }

    /** 首页全屏：隐藏迷你播放条 + 底部 tab（仅首页生效，读"首页全屏"设置） */
    private void applyFullscreen() {
        boolean fs = MusicEngine.getBool(this, "首页全屏", true);
        int vis = fs ? View.GONE : View.VISIBLE;
        mBinding.musicMini.setVisibility(fs && MusicPlayer.get().currentSong() != null ? View.VISIBLE : View.GONE);
        mBinding.musicTabHome.setVisibility(vis);
        mBinding.musicTabLib.setVisibility(vis);
        mBinding.musicTabSearch.setVisibility(vis);
        mBinding.musicTabSetting.setVisibility(vis);
    }

    /** 应用外观设置（背景色 + 主题色文字） */
    private void applyTheme() {
        boolean dark = MusicEngine.isDarkTheme(this);
        int bg = dark ? 0xFF12141A : 0xFFF2F3F5;
        int text = dark ? 0xFFFFFFFF : 0xFF12141A;
        int sub = dark ? 0xFF8A8F99 : 0xFF606770;
        mBinding.getRoot().setBackgroundColor(bg);
        // 底部 tab 容器背景 + 文字
        View bottomBar = mBinding.getRoot().findViewById(R.id.musicBottomBar);
        if (bottomBar != null) bottomBar.setBackgroundColor(dark ? 0xFF1A1D26 : 0xFFE4E6EA);
        mBinding.musicTabHome.setTextColor(text);
        mBinding.musicTabLib.setTextColor(sub);
        mBinding.musicTabSearch.setTextColor(sub);
        mBinding.musicTabSetting.setTextColor(sub);
        // 迷你条背景
        View mini = mBinding.getRoot().findViewById(R.id.musicMini);
        if (mini != null) mini.setBackgroundColor(dark ? 0xFF1F232C : 0xFFE4E6EA);
    }

    /** 更新迷你播放条 */
    private void updateMini(MusicSong song, boolean playing) {
        if (song == null) {
            mBinding.musicMini.setVisibility(android.view.View.GONE);
            return;
        }
        mBinding.musicMini.setVisibility(android.view.View.VISIBLE);
        mBinding.musicMiniTitle.setText(song.name);
        mBinding.musicMiniSinger.setText(song.singer);
        mBinding.musicMiniPlay.setImageResource(
                playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 播放器单例不释放（后台继续播），但清监听
        MusicPlayer.get().setListener(null);
    }

    private void switchTab(int index) {
        if (index == currentTab && getSupportFragmentManager().getFragments().size() > 0) return;
        currentTab = index;
        Fragment fragment;
        switch (index) {
            case 1: fragment = new MusicLibFragment(); break;
            case 2: fragment = new MusicSearchFragment(); break;
            case 3: fragment = new MusicSettingFragment(); break;
            default: fragment = new MusicHomeFragment(); break;
        }
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.musicContent, fragment)
                .commit();

        // 高亮当前 tab（按主题色）
        boolean dark = MusicEngine.isDarkTheme(this);
        int on = dark ? 0xFFFFFFFF : 0xFF12141A;
        int off = dark ? 0xFF8A8F99 : 0xFF606770;
        mBinding.musicTabHome.setTextColor(index == 0 ? on : off);
        mBinding.musicTabLib.setTextColor(index == 1 ? on : off);
        mBinding.musicTabSearch.setTextColor(index == 2 ? on : off);
        mBinding.musicTabSetting.setTextColor(index == 3 ? on : off);
    }
}
