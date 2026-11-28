package com.fongmi.android.tv.music;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityMusicBinding;
import com.fongmi.android.tv.music.ui.MusicHomeFragment;
import com.fongmi.android.tv.music.ui.MusicLibFragment;
import com.fongmi.android.tv.music.ui.MusicSearchFragment;
import com.fongmi.android.tv.music.ui.MusicSettingFragment;

public class MusicActivity extends AppCompatActivity {

    private ActivityMusicBinding mBinding;
    private int currentTab = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityMusicBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());

        mBinding.musicTabHome.setOnClickListener(v -> switchTab(0));
        mBinding.musicTabLib.setOnClickListener(v -> switchTab(1));
        mBinding.musicTabSearch.setOnClickListener(v -> switchTab(2));
        mBinding.musicTabSetting.setOnClickListener(v -> switchTab(3));

        switchTab(0);
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
