package com.fongmi.android.tv.music.ui;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;

/** 音乐播放器封装（Media3 ExoPlayer，单例后台播放） */
public class MusicPlayer {

    private static MusicPlayer sInstance;
    private ExoPlayer mPlayer;
    private String mCurrentTitle;
    private boolean mPlaying;

    public static MusicPlayer get() {
        if (sInstance == null) sInstance = new MusicPlayer();
        return sInstance;
    }

    public MusicPlayer() {
        sInstance = this;
    }

    /** 播放（302 重定向 ExoPlayer 自动跟随） */
    public void play(Context ctx, String title, String url) {
        if (mPlayer == null) {
            mPlayer = new ExoPlayer.Builder(ctx).build();
        }
        mCurrentTitle = title;
        MediaItem item = new MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(new MediaMetadata.Builder().setTitle(title).build())
                .build();
        mPlayer.setMediaItem(item);
        mPlayer.prepare();
        mPlayer.play();
        mPlaying = true;
    }

    public void pause() {
        if (mPlayer != null) mPlayer.pause();
        mPlaying = false;
    }

    public void resume() {
        if (mPlayer != null) mPlayer.play();
        mPlaying = true;
    }

    public void stop() {
        if (mPlayer != null) mPlayer.stop();
        mPlaying = false;
    }

    public String currentTitle() { return mCurrentTitle; }
    public boolean isPlaying() { return mPlaying; }

    public void release() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mPlaying = false;
    }
}
