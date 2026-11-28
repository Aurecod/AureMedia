package com.fongmi.android.tv.music;

import android.content.Context;
import android.text.TextUtils;

import com.github.catvod.net.OkHttp;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 音乐音源客户端（对标 am-4u2 内嵌 API）：
 * - QQ 音乐：搜索 / 热榜 / 歌单（JSONP，songmid）
 * - 网易 / 酷我：播放直链（c.wwwweb.top/music/url POST）
 */
public class QqMusicApi {

    private static final String QQ_SEARCH = "https://c.y.qq.com/soso/fcgi-bin/client_search_cp?w=%s&format=jsonp&n=20&p=1&jsonpCallback=_qq";
    private static final String QQ_RANK = "https://c.y.qq.com/qzone/fcg-bin/fcg_ucc_getcdinfo_byids_cp.fcg?type=1&json=1&utf8=1&onlysong=0&nosign=1&disstid=%s";
    private static final String WWW_WEB_URL = "https://c.wwwweb.top/music/url";

    // 常用热榜歌单 id（QQ 排行）
    public static final String[] HOT_PLAYLISTS = {
            "441935",  // 新歌榜
            "441936",  // 热歌榜
            "441932",  // 飙升榜
            "2884035"  // 抖音热歌
    };

    public static class Song {}

    /** QQ 搜索歌曲 */
    public static List<MusicSong> search(String keyword) throws Exception {
        String url = String.format(QQ_SEARCH, URLEncoder.encode(keyword, "UTF-8"));
        String jsonp = OkHttp.string(url);
        String json = extractJsonp(jsonp);
        JSONObject obj = new JSONObject(json);
        JSONObject data = obj.optJSONObject("data");
        if (data == null) return new ArrayList<>();
        JSONObject song = data.optJSONObject("song");
        if (song == null) return new ArrayList<>();
        JSONArray list = song.optJSONArray("list");
        List<MusicSong> result = new ArrayList<>();
        if (list == null) return result;
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            MusicSong s = new MusicSong();
            s.id = item.optString("songmid");
            s.name = item.optString("songname");
            s.singer = parseSinger(item.optJSONArray("singer"));
            s.album = item.optJSONObject("album") != null ? item.optJSONObject("album").optString("name") : "";
            s.duration = item.optInt("interval", 0);
            result.add(s);
        }
        return result;
    }

    /** QQ 热榜歌单歌曲 */
    public static List<MusicSong> playlist(String disstid) throws Exception {
        String url = String.format(QQ_RANK, disstid);
        String json = OkHttp.string(url);
        JSONObject obj = new JSONObject(json);
        JSONObject cd = obj.optJSONObject("cdlist");
        if (cd == null || cd.length() == 0) return new ArrayList<>();
        JSONObject first = cd.optJSONObject(cd.keys().next());
        JSONArray list = first != null ? first.optJSONArray("songlist") : null;
        List<MusicSong> result = new ArrayList<>();
        if (list == null) return result;
        for (int i = 0; i < list.length(); i++) {
            JSONObject item = list.optJSONObject(i);
            MusicSong s = new MusicSong();
            s.id = item.optString("songmid");
            s.name = item.optString("songname");
            s.singer = parseSinger(item.optJSONArray("singer"));
            s.duration = item.optInt("interval", 0);
            result.add(s);
        }
        return result;
    }

    /** 网易/酷我播放直链 */
    public static String resolveUrl(String source, String songId) throws Exception {
        JSONObject body = new JSONObject();
        body.put("source", source);
        body.put("id", songId);
        body.put("quality", "320");
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
        okhttp3.Response resp = OkHttp.newCall(WWW_WEB_URL, headers, RequestBody.create(MediaType.parse("application/json"), body.toString())).execute();
        String json = resp.body().string();
        JSONObject obj = new JSONObject(json);
        if (obj.optInt("code", -1) == 200 && obj.has("url")) return obj.optString("url");
        throw new Exception(obj.optString("msg", "未返回有效链接"));
    }

    private static String parseSinger(JSONArray singers) {
        if (singers == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < singers.length(); i++) {
            if (i > 0) sb.append("/");
            sb.append(singers.optJSONObject(i).optString("name"));
        }
        return sb.toString();
    }

    private static String extractJsonp(String jsonp) {
        int s = jsonp.indexOf('(');
        int e = jsonp.lastIndexOf(')');
        if (s >= 0 && e > s) return jsonp.substring(s + 1, e);
        return jsonp;
    }
}
