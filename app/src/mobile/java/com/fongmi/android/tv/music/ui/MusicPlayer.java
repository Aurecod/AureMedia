package com.fongmi.android.tv.music.ui;

import android.content.Context;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.fongmi.android.tv.music.MusicEngine;
import com.fongmi.android.tv.music.MusicLibStore;
import com.fongmi.android.tv.music.MusicSong;

/** 音乐播放器（单例，Media3 ExoPlayer，后台播放 + 状态监听） */
public class MusicPlayer {

    private static MusicPlayer sInstance;
    private ExoPlayer mPlayer;
    private Listener mListener;

    private MusicSong mSong;      // 当前歌曲
    private boolean mPlaying;
    private final java.util.List<MusicSong> mQueue = new java.util.ArrayList<>();
    private int mQueueIndex = -1;
    private boolean mDegraded;    // 本次播放已降级（避免无限循环）
    private Context mCtx;
    private long mPositionMs;      // 缓存位置（歌词同步用）

    public interface Listener {
        void onSongChanged(MusicSong song, boolean playing);
        void onProgress(long posMs, long durMs);
    }

    public static MusicPlayer get() {
        if (sInstance == null) sInstance = new MusicPlayer();
        return sInstance;
    }

    public void setListener(Listener l) { mListener = l; }

    /** 播放单曲（无队列） */
    public void play(Context ctx, MusicSong song) {
        play(ctx, song, null, 0);
    }

    /** 播放带队列（列表 + 起始下标），供歌单/榜单播放 */
    public void play(Context ctx, MusicSong song, java.util.List<MusicSong> queue, int index) {
        if (song == null) return;
        mCtx = ctx;
        mDegraded = false; // 新播放重置降级标记
        if (mPlayer == null) {
            mPlayer = new ExoPlayer.Builder(ctx).build();
            mPlayer.addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    mPlaying = isPlaying;
                    notifyChange();
                }
                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) {
                        // 自动下一首
                        if (mQueueIndex >= 0 && mQueueIndex < mQueue.size() - 1) {
                            next();
                        }
                    }
                }
                @Override
                public void onPlayerError(androidx.media3.common.PlaybackException e) {
                    // 音质自动降级：播放失败且音质>标准，降档重试一次
                    boolean autoDown = MusicEngine.getBool(mCtx, "音质自动降级", true);
                    if (autoDown && !mDegraded) {
                        String q = MusicEngine.getStr(mCtx, "音质", "无损");
                        String lower = MusicEngine.lowerQuality(q);
                        if (lower != null) {
                            mDegraded = true;
                            MusicEngine.setStr(mCtx, "音质", lower);
                            retryCurrent();
                            return;
                        }
                    }
                    mDegraded = false;
                }
            });
        }
        if (queue != null) {
            mQueue.clear();
            mQueue.addAll(queue);
            mQueueIndex = index;
        } else {
            mQueue.clear();
            mQueueIndex = -1;
        }
        mSong = song;
        MusicLibStore.addRecent(ctx, song); // 记录最近播放
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

    /** 音质降档后重播当前歌曲 */
    private void retryCurrent() {
        if (mCtx == null || mSong == null) return;
        String url = MusicEngine.resolveUrl(mCtx, mSong);
        if (url == null) return;
        MediaItem item = new MediaItem.Builder()
                .setUri(url)
                .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(mSong.name)
                        .setArtist(mSong.singer)
                        .build())
                .build();
        mPlayer.setMediaItem(item);
        mPlayer.prepare();
        mPlayer.play();
        mPlaying = true;
        notifyChange();
    }

    /** 下一首（有队列时） */
    public boolean next() {
        if (mQueueIndex >= 0 && mQueueIndex < mQueue.size() - 1) {
            mQueueIndex++;
            play(mCtx, mQueue.get(mQueueIndex), mQueue, mQueueIndex);
            return true;
        }
        return false;
    }

    /** 上一首（有队列时） */
    public boolean prev() {
        if (mQueueIndex > 0) {
            mQueueIndex--;
            play(mCtx, mQueue.get(mQueueIndex), mQueue, mQueueIndex);
            return true;
        }
        return false;
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

    public long getPosition() { return mPlayer != null ? mPlayer.getCurrentPosition() : 0; }
    public long getDuration() { return mPlayer != null && mPlayer.getDuration() > 0 ? mPlayer.getDuration() : 0; }
    public void seekTo(long ms) { if (mPlayer != null) mPlayer.seekTo(ms); }

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
