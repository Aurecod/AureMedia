package com.fongmi.android.tv.music;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityMusicBinding;
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
        MusicPlayer.get().setListener((song, playing) -> runOnUiThread(() -> updateMini(song, playing)));
        updateMini(MusicPlayer.get().currentSong(), MusicPlayer.get().isPlaying());

        switchTab(0);
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

        // 高亮当前 tab
        int on = 0xFFFFFFFF, off = 0xFF8A8F99;
        mBinding.musicTabHome.setTextColor(index == 0 ? on : off);
        mBinding.musicTabLib.setTextColor(index == 1 ? on : off);
        mBinding.musicTabSearch.setTextColor(index == 2 ? on : off);
        mBinding.musicTabSetting.setTextColor(index == 3 ? on : off);
    }
}
