package com.fongmi.android.tv.music;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 音源存储（对标 am-4u2 aq_sources）：
 * - 预置 4 个内置音源（星河入梦/松间听雨/淡月疏星/雾隐青山，均支持 5 平台）
 * - 支持添加（在线导入）/删除/选择（active 决定平台）
 * - SharedPreferences JSON 持久化
 */
public class MusicSourceStore {

    private static final String PREFS = "music";
    private static final String KEY_SOURCES = "sources";
    private static final String KEY_ACTIVE = "active_source";

    /** 预置音源（am-4u2 aq_sources 4 个内置音源元数据） */
    private static final String[][] BUILTIN = {
            {"cx", "星河入梦", "js", "https://gitee.com/Myn_1/Mao_Yuna/raw/MYN_update/lx-music/YuXi.js"},
            {"nianxin", "松间听雨", "api", ""},
            {"changqing", "淡月疏星", "js", ""},
            {"yuxi", "雾隐青山", "js", "https://zddyr.top/"},
    };

    private static final String[] ALL_PLATFORMS = {"qq", "netease", "kugou", "kuwo", "migu"};

    public static List<MusicSource> getSources(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY_SOURCES, null);
        if (json == null) {
            List<MusicSource> builtins = builtins();
            saveSources(ctx, builtins);
            return builtins;
        }
        List<MusicSource> list = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                MusicSource s = fromJson(arr.optJSONObject(i));
                if (s != null) list.add(s);
            }
        } catch (Exception e) {
            return builtins();
        }
        if (list.isEmpty()) list = builtins();
        return list;
    }

    /** 当前 active 音源 */
    public static MusicSource getActive(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String id = sp.getString(KEY_ACTIVE, "cx");
        List<MusicSource> all = getSources(ctx);
        for (MusicSource s : all) {
            if (s.id.equals(id)) return s;
        }
        return all.get(0);
    }

    public static void setActive(Context ctx, String id) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_ACTIVE, id).apply();
    }

    /** 添加音源（在线导入） */
    public static void addSource(Context ctx, String name, String type, String link, List<String> platforms) {
        List<MusicSource> all = getSources(ctx);
        MusicSource s = new MusicSource("u" + System.currentTimeMillis(), name, type, link);
        s.platforms = platforms.toArray(new String[0]);
        all.add(s);
        saveSources(ctx, all);
    }

    /** 删除音源 */
    public static void removeSource(Context ctx, String id) {
        List<MusicSource> all = getSources(ctx);
        List<MusicSource> kept = new ArrayList<>();
        for (MusicSource s : all) {
            if (!s.id.equals(id)) kept.add(s);
        }
        saveSources(ctx, kept);
        // 删的是 active 则回退第一源
        if (getActive(ctx).id.equals(id)) setActive(ctx, kept.get(0).id);
    }

    /** 编辑音源（名称/类型/链接/平台） */
    public static void updateSource(Context ctx, String id, String name, String type, String link, List<String> platforms) {
        List<MusicSource> all = getSources(ctx);
        for (MusicSource s : all) {
            if (s.id.equals(id)) {
                s.name = name;
                s.type = type;
                s.link = link;
                s.platforms = platforms.toArray(new String[0]);
                break;
            }
        }
        saveSources(ctx, all);
    }

    private static List<MusicSource> builtins() {
        List<MusicSource> list = new ArrayList<>();
        for (String[] b : BUILTIN) {
            MusicSource s = new MusicSource(b[0], b[1], b[2], b[3]);
            s.builtin = true;
            s.platforms = ALL_PLATFORMS.clone();
            list.add(s);
        }
        return list;
    }

    private static void saveSources(Context ctx, List<MusicSource> list) {
        JSONArray arr = new JSONArray();
        try {
            for (MusicSource s : list) arr.put(toJson(s));
        } catch (Exception e) { }
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_SOURCES, arr.toString()).apply();
    }

    private static JSONObject toJson(MusicSource s) throws Exception {
        JSONObject o = new JSONObject();
        o.put("id", s.id);
        o.put("name", s.name);
        o.put("type", s.type);
        o.put("builtin", s.builtin);
        o.put("link", s.link);
        o.put("platforms", new JSONArray(java.util.Arrays.asList(s.platforms)));
        return o;
    }

    private static MusicSource fromJson(JSONObject o) {
        if (o == null) return null;
        MusicSource s = new MusicSource(o.optString("id"), o.optString("name"), o.optString("type"), o.optString("link"));
        s.builtin = o.optBoolean("builtin", false);
        JSONArray pl = o.optJSONArray("platforms");
        List<String> list = new ArrayList<>();
        if (pl != null) {
            for (int i = 0; i < pl.length(); i++) list.add(pl.optString(i));
        }
        s.platforms = list.isEmpty() ? ALL_PLATFORMS.clone() : list.toArray(new String[0]);
        return s;
    }
}
