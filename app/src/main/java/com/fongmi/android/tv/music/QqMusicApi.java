package com.fongmi.android.tv.music;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import com.github.catvod.net.OkHttp;

/**
 * 音乐音源客户端（网易云，无需代理）：
 * - 搜索：api/search/get/web (type=1 歌曲)
 * - 热榜：api/playlist/detail (3778678=热歌榜)
 * - 播放：song/media/outer/url?id=X.mp3 (302 → CDN，ExoPlayer 跟随)
 */
public class QqMusicApi {

    private static final String BASE = "https://music.163.com";
    private static final String SEARCH = BASE + "/api/search/get/web?type=1&s=%s&limit=30";
    private static final String PLAYLIST = BASE + "/api/playlist/detail?id=%d";
    private static final String PLAY_URL = BASE + "/song/media/outer/url?id=%d.mp3";

    // 网易热榜歌单 id
    public static final int[] HOT_PLAYLISTS = {
            3778678,  // 热歌榜
            3779629,  // 新歌榜
            2250011882, // 飙升榜
            2884035   // 抖音
    };

    /** 网易搜索歌曲 */
    public static List<MusicSong> search(String keyword) throws Exception {
        String url = String.format(SEARCH, URLEncoder.encode(keyword, "UTF-8"));
        String json = OkHttp.string(url, headers());
        JSONObject obj = new JSONObject(json);
        JSONObject result = obj.optJSONObject("result");
        JSONArray songs = result != null ? result.optJSONArray("songs") : null;
        List<MusicSong> list = new ArrayList<>();
        if (songs == null) return list;
        for (int i = 0; i < songs.length(); i++) {
            MusicSong s = parseSong(songs.optJSONObject(i));
            if (s != null) list.add(s);
        }
        return list;
    }

    /** 网易热榜歌曲 */
    public static List<MusicSong> playlist(int playlistId) throws Exception {
        String url = String.format(PLAYLIST, playlistId);
        String json = OkHttp.string(url, headers());
        JSONObject obj = new JSONObject(json);
        JSONObject result = obj.optJSONObject("result");
        JSONArray tracks = result != null ? result.optJSONArray("tracks") : null;
        List<MusicSong> list = new ArrayList<>();
        if (tracks == null) return list;
        for (int i = 0; i < tracks.length(); i++) {
            MusicSong s = parseSong(tracks.optJSONObject(i));
            if (s != null) list.add(s);
        }
        return list;
    }

    /** 网易播放直链（302 重定向到 CDN） */
    public static String resolveUrl(long songId) {
        return String.format(PLAY_URL, songId);
    }

    private static MusicSong parseSong(JSONObject item) {
        if (item == null) return null;
        MusicSong s = new MusicSong();
        s.id = String.valueOf(item.optLong("id", 0));
        s.name = item.optString("name");
        s.source = "wy";
        JSONArray artists = item.optJSONArray("artists");
        if (artists == null) artists = item.optJSONArray("ar");
        StringBuilder sb = new StringBuilder();
        if (artists != null) {
            for (int i = 0; i < artists.length(); i++) {
                if (i > 0) sb.append("/");
                JSONObject a = artists.optJSONObject(i);
                sb.append(a != null ? a.optString("name") : "");
            }
        }
        s.singer = sb.toString();
        JSONObject album = item.optJSONObject("album");
        if (album == null) album = item.optJSONObject("al");
        if (album != null) {
            s.album = album.optString("name");
            s.cover = album.optString("picUrl", "");
        }
        return s;
    }

    private static java.util.Map<String, String> headers() {
        java.util.Map<String, String> h = new java.util.HashMap<>();
        h.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        h.put("Referer", "https://music.163.com/");
        return h;
    }
}
