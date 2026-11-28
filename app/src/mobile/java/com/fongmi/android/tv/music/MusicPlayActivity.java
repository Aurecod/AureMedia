package com.fongmi.android.tv.music;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import com.bumptech.glide.Glide;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityMusicPlayBinding;
import com.fongmi.android.tv.music.ui.MusicPlayer;
import com.fongmi.android.tv.ui.base.BaseActivity;

import java.util.List;

public class MusicPlayActivity extends BaseActivity {

    private ActivityMusicPlayBinding mBinding;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private List<MusicEngine.LyricLine> mLyrics = new java.util.ArrayList<>();
    private int mCurLyric = -1;

    private final Runnable mProgressTick = new Runnable() {
        @Override
        public void run() {
            updateProgress();
            updateLyric();
            mHandler.postDelayed(this, 500);
        }
    };

    @Override
    protected ViewBinding getBinding() {
        mBinding = ActivityMusicPlayBinding.inflate(getLayoutInflater());
        return mBinding;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // BaseActivity 状态栏 edge-to-edge

        mBinding.npClose.setOnClickListener(v -> finish());
        mBinding.npPlay.setOnClickListener(v -> {
            MusicPlayer.get().togglePlay();
            updatePlayBtn();
        });
        // 上一首/下一首
        mBinding.getRoot().findViewById(R.id.npPrev).setOnClickListener(v -> {
            if (!MusicPlayer.get().prev()) Toast.makeText(this, "没有上一首", Toast.LENGTH_SHORT).show();
        });
        mBinding.getRoot().findViewById(R.id.npNext).setOnClickListener(v -> {
            if (!MusicPlayer.get().next()) Toast.makeText(this, "没有下一首", Toast.LENGTH_SHORT).show();
        });
        mBinding.npSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) { }
            @Override public void onStartTrackingTouch(SeekBar sb) { }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                long dur = MusicPlayer.get().getDuration();
                if (dur > 0) MusicPlayer.get().seekTo(dur * sb.getProgress() / 100);
            }
        });

        MusicSong song = MusicPlayer.get().currentSong();
        if (song != null) {
            bindSong(song);
            loadLyric(song);
        }
    }

    private void bindSong(MusicSong s) {
        mBinding.npTitleTop.setText(s.name);
        mBinding.npTitle.setText(s.name);
        mBinding.npSinger.setText(s.singer);
        if (s.cover != null && !s.cover.isEmpty()) {
            Glide.with(this).load(s.cover).into(mBinding.npCover);
            Glide.with(this).load(s.cover).into(mBinding.npBg);
        } else {
            mBinding.npCover.setImageResource(R.drawable.ic_launcher_fg);
            mBinding.npBg.setImageResource(R.drawable.ic_launcher_fg);
        }
    }

    /** 异步加载歌词 */
    private void loadLyric(MusicSong song) {
        new Thread(() -> {
            List<MusicEngine.LyricLine> lines = MusicEngine.lyric(this, song);
            runOnUiThread(() -> {
                mLyrics = lines;
                mCurLyric = -1;
                mBinding.npLyricList.removeAllViews();
                if (lines.isEmpty()) {
                    mBinding.npLyricTitle.setText("无歌词");
                    return;
                }
                mBinding.npLyricTitle.setText("歌词");
                for (int i = 0; i < lines.size(); i++) {
                    TextView tv = new TextView(this);
                    tv.setText(lines.get(i).text);
                    tv.setTextSize(13);
                    tv.setPadding(dp(4), dp(6), dp(4), dp(6));
                    tv.setTextColor(0xFF8A8F99);
                    mBinding.npLyricList.addView(tv);
                }
            });
        }).start();
    }

    /** 歌词滚动高亮当前行 */
    private void updateLyric() {
        if (mLyrics.isEmpty()) return;
        long pos = MusicPlayer.get().getPosition();
        int idx = -1;
        for (int i = 0; i < mLyrics.size(); i++) {
            if (mLyrics.get(i).time <= pos) idx = i;
        }
        if (idx != mCurLyric) {
            mCurLyric = idx;
            for (int i = 0; i < mBinding.npLyricList.getChildCount(); i++) {
                TextView tv = (TextView) mBinding.npLyricList.getChildAt(i);
                boolean cur = (i == idx);
                tv.setTextColor(cur ? 0xFFFFFFFF : 0xFF8A8F99);
                tv.setTextSize(cur ? 15 : 13);
            }
            if (idx >= 0) {
                int target = idx * dp(34) - dp(70);
                mBinding.npLyricScroll.scrollTo(0, Math.max(0, target));
            }
        }
    }

    private void updateProgress() {
        long pos = MusicPlayer.get().getPosition();
        long dur = MusicPlayer.get().getDuration();
        if (dur > 0) {
            mBinding.npSeek.setProgress((int) (pos * 100 / dur));
            mBinding.npPos.setText(fmt(pos));
            mBinding.npDur.setText(fmt(dur));
        }
        updatePlayBtn();
    }

    private void updatePlayBtn() {
        boolean playing = MusicPlayer.get().isPlaying();
        mBinding.npPlay.setText(playing ? "⏸" : "▶");
    }

    private String fmt(long ms) {
        long sec = ms / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.post(mProgressTick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mProgressTick);
    }
}
