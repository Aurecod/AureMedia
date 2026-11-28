package com.fongmi.android.tv.music;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 资料库存储（对标 am-4u2 lib：喜欢的音乐/喜欢的歌单/最近播放/已下载）
 * - 收藏/最近播放/下载 均为 MusicSong JSON 列表，SharedPreferences 持久化
 */
public class MusicLibStore {

    private static final String PREFS = "music";
    private static final String KEY_FAV = "lib_fav";
    private static final String KEY_RECENT = "lib_recent";
    private static final String KEY_DL = "lib_dl";

    public static List<MusicSong> getFav(Context ctx) { return getList(ctx, KEY_FAV); }
    public static List<MusicSong> getRecent(Context ctx) { return getList(ctx, KEY_RECENT); }
    public static List<MusicSong> getDownloaded(Context ctx) { return getList(ctx, KEY_DL); }

    public static boolean isFav(Context ctx, String songId) {
        for (MusicSong s : getFav(ctx)) if (s.id.equals(songId)) return true;
        return false;
    }

    /** 切换收藏，返回是否已收藏 */
    public static boolean toggleFav(Context ctx, MusicSong song) {
        List<MusicSong> fav = getFav(ctx);
        for (int i = 0; i < fav.size(); i++) {
            if (fav.get(i).id.equals(song.id)) {
                fav.remove(i);
                saveList(ctx, KEY_FAV, fav);
                return false;
            }
        }
        fav.add(0, song);
        saveList(ctx, KEY_FAV, fav);
        return true;
    }

    /** 记录最近播放（去重，最新在前，最多 50） */
    public static void addRecent(Context ctx, MusicSong song) {
        List<MusicSong> recent = getRecent(ctx);
        recent.removeIf(s -> s.id.equals(song.id));
        recent.add(0, song);
        while (recent.size() > 50) recent.remove(recent.size() - 1);
        saveList(ctx, KEY_RECENT, recent);
    }

    /** 添加到已下载 */
    public static void addDownloaded(Context ctx, MusicSong song) {
        List<MusicSong> dl = getDownloaded(ctx);
        dl.removeIf(s -> s.id.equals(song.id));
        dl.add(0, song);
        saveList(ctx, KEY_DL, dl);
    }

    public static void removeDownloaded(Context ctx, String id) {
        List<MusicSong> dl = getDownloaded(ctx);
        dl.removeIf(s -> s.id.equals(id));
        saveList(ctx, KEY_DL, dl);
    }

    private static List<MusicSong> getList(Context ctx, String key) {
        List<MusicSong> list = new ArrayList<>();
        String json = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, null);
        if (json == null) return list;
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                MusicSong s = fromJson(arr.optJSONObject(i));
                if (s != null) list.add(s);
            }
        } catch (Exception e) { }
        return list;
    }

    private static void saveList(Context ctx, String key, List<MusicSong> list) {
        JSONArray arr = new JSONArray();
        try {
            for (MusicSong s : list) arr.put(toJson(s));
        } catch (Exception e) { }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, arr.toString()).apply();
    }

    private static JSONObject toJson(MusicSong s) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", s.id);
        o.put("name", s.name);
        o.put("singer", s.singer);
        o.put("album", s.album);
        o.put("cover", s.cover);
        o.put("source", s.source);
        return o;
    }

    private static MusicSong fromJson(JSONObject o) {
        if (o == null) return null;
        MusicSong s = new MusicSong();
        s.id = o.optString("id");
        s.name = o.optString("name");
        s.singer = o.optString("singer");
        s.album = o.optString("album");
        s.cover = o.optString("cover");
        s.source = o.optString("source");
        return s;
    }
}
