package com.fongmi.android.tv.music.ui;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.fongmi.android.tv.music.MusicEngine;
import com.fongmi.android.tv.music.MusicSong;

/** 音乐播放器（单例，Media3 ExoPlayer，后台播放 + 状态监听） */
public class MusicPlayer {

    private static MusicPlayer sInstance;
    private ExoPlayer mPlayer;
    private Listener mListener;

    private MusicSong mSong;      // 当前歌曲
    private boolean mPlaying;

    public interface Listener {
        void onSongChanged(MusicSong song, boolean playing);
    }

    public static MusicPlayer get() {
        if (sInstance == null) sInstance = new MusicPlayer();
        return sInstance;
    }

    public void setListener(Listener l) { mListener = l; }

    /** 播放（302 重定向 ExoPlayer 自动跟随） */
    public void play(Context ctx, MusicSong song) {
        if (song == null) return;
        if (mPlayer == null) {
            mPlayer = new ExoPlayer.Builder(ctx).build();
            mPlayer.addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    mPlaying = isPlaying;
                    notifyChange();
                }
            });
        }
        mSong = song;
        String url = MusicEngine.resolveUrl(ctx, song);
        if (url == null) {
            mPlaying = false;
            notifyChange();
            return;
        }
        MediaItem item = new MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(song.name)
                        .setArtist(song.singer)
                        .build())
                .build();
        mPlayer.setMediaItem(item);
        mPlayer.prepare();
        mPlayer.play();
        mPlaying = true;
        notifyChange();
    }

    public void togglePlay() {
        if (mPlayer == null) return;
        if (mPlaying) mPlayer.pause();
        else mPlayer.play();
    }

    public void stop() {
        if (mPlayer != null) mPlayer.stop();
        mPlaying = false;
        notifyChange();
    }

    public MusicSong currentSong() { return mSong; }
    public boolean isPlaying() { return mPlaying; }

    private void notifyChange() {
        if (mListener != null) mListener.onSongChanged(mSong, mPlaying);
    }

    public void release() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        mPlaying = false;
        mSong = null;
    }
}
