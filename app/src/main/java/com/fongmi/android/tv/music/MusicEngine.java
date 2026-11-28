package com.fongmi.android.tv.music;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.catvod.net.OkHttp;

/**
 * 音乐音源引擎（对标 am-4u2 音源架构）：
 * - 音源驱动：内置官方平台音源（网易/QQ/酷我/酷狗/咪咕），音源决定支持哪些平台
 * - 主页平台选择：设置里选平台 → 主页展示该平台榜单（非播放源）
 * - 搜索/播放：走当前音源对应平台
 */
public class MusicEngine {

    private static final String PREFS = "music";
    private static final String KEY_PLATFORM = "platform";
    private static final String KEY_SOURCE = "source";

    // 平台列表（音源支持的平台）
    public static final String PLATFORM_NETEASE = "netease";
    public static final String PLATFORM_QQ = "qq";
    public static final String PLATFORM_KUWO = "kuwo";
    public static final String PLATFORM_KUGOU = "kugou";
    public static final String PLATFORM_MIGU = "migu";

    public static final String[] PLATFORMS = {
            PLATFORM_QQ, PLATFORM_NETEASE, PLATFORM_KUWO, PLATFORM_KUGOU, PLATFORM_MIGU
    };

    public static final String[] PLATFORM_NAMES = {
            "QQ", "网易", "酷我", "酷狗", "咪咕"
    };

    // 内置音源由 MusicSourceStore 管理（预置 4 个 + 导入），此处不硬编码

    public static String[] platforms() { return PLATFORMS; }

    public static String[] platformNames() { return PLATFORM_NAMES; }

    public static int platformIndex(String platform) {
        for (int i = 0; i < PLATFORMS.length; i++) {
            if (PLATFORMS[i].equals(platform)) return i;
        }
        return 0;
    }

    /** 当前主页展示平台（默认网易，从 active 音源声明的平台读取） */
    public static String getPlatform(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String p = sp.getString(KEY_PLATFORM, null);
        if (p != null) return p;
        // 未选平台 → 用 active 音源第一个支持平台
        MusicSource src = MusicSourceStore.getActive(ctx);
        if (src != null && src.platforms != null && src.platforms.length > 0) {
            String first = src.platforms[0];
            for (String pp : PLATFORMS) if (pp.equals(first)) return pp;
        }
        return PLATFORM_NETEASE;
    }

    public static void setPlatform(Context ctx, String platform) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_PLATFORM, platform).apply();
    }

    /** 通用设置读写（音质/外观/歌词等偏好） */
    public static String getStr(Context ctx, String key, String def) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(key, def);
    }

    public static void setStr(Context ctx, String key, String val) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(key, val).apply();
    }

    public static boolean getBool(Context ctx, String key, boolean def) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(key, def);
    }

    public static void setBool(Context ctx, String key, boolean val) {
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(key, val).apply();
    }

    /** 当前 active 音源 id（音源决定支持平台） */
    public static String getSource(Context ctx) {
        return MusicSourceStore.getActive(ctx).id;
    }

    public static void setSource(Context ctx, String source) {
        MusicSourceStore.setActive(ctx, source);
    }

    /** 按平台搜索 */
    public static List<MusicSong> search(Context ctx, String keyword) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.search(keyword);
        if (PLATFORM_QQ.equals(platform)) return QqApi.search(keyword);
        // 酷我/酷狗/咪咕：App 实测
        if (PLATFORM_KUWO.equals(platform)) return KuwoApi.search(keyword);
        if (PLATFORM_KUGOU.equals(platform)) return KugouApi.search(keyword);
        if (PLATFORM_MIGU.equals(platform)) return MiguApi.search(keyword);
        return new ArrayList<>();
    }

    /** 分类搜索（type=1歌曲/100歌手/10专辑/1000歌单），返回 MusicSong 或 String 名称 */
    public static List<Object> searchType(Context ctx, String keyword, int type) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.searchType(keyword, type);
        return new ArrayList<>(search(ctx, keyword));
    }

    /** 按平台热榜 */
    public static List<MusicSong> hot(Context ctx) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.hot();
        if (PLATFORM_QQ.equals(platform)) return QqApi.hot();
        if (PLATFORM_KUWO.equals(platform)) return KuwoApi.hot();
        if (PLATFORM_KUGOU.equals(platform)) return KugouApi.hot();
        if (PLATFORM_MIGU.equals(platform)) return MiguApi.hot();
        return new ArrayList<>();
    }

    /** 为你推荐（网易推荐歌单） */
    public static List<MusicPlaylist> recommend(Context ctx) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.recommend();
        return new ArrayList<>();
    }

    /** 精品榜单（网易各榜单） */
    public static List<MusicPlaylist> ranks(Context ctx) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.ranks();
        return new ArrayList<>();
    }

    /** 频道（网易热门歌单分类） */
    public static List<MusicPlaylist> channels(Context ctx) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.channels();
        return new ArrayList<>();
    }

    /** 按平台解析播放 URL */
    public static String resolveUrl(Context ctx, MusicSong song) {
        String platform = song.source;
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.resolveUrl(song.id);
        if (PLATFORM_QQ.equals(platform)) return QqApi.resolveUrl(song.id);
        if (PLATFORM_KUWO.equals(platform)) return KuwoApi.resolveUrl(song.id);
        if (PLATFORM_KUGOU.equals(platform)) return KugouApi.resolveUrl(song.id);
        if (PLATFORM_MIGU.equals(platform)) return MiguApi.resolveUrl(song.id);
        return null;
    }

    /** 歌词行（时间ms + 文本） */
    public static class LyricLine {
        public long time;
        public String text;
        public LyricLine(long time, String text) { this.time = time; this.text = text; }
    }

    /** 获取歌词（网易 api/song/lyric，LRC 解析） */
    public static List<LyricLine> lyric(Context ctx, MusicSong song) {
        String platform = song.source;
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.lyric(song.id);
        return new ArrayList<>();
    }

    /** 通用歌曲解析（搜索/热榜共用） */
    static MusicSong parseSong(JSONObject item, String source) {
        if (item == null) return null;
        MusicSong s = new MusicSong();
        s.source = source;
        s.id = item.optString("id", String.valueOf(item.optLong("id", 0)));
        s.name = item.optString("name");
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

    /** 网易云客户端 */
    static class NeteaseApi {
        static final String BASE = "https://music.163.com";
        static List<MusicSong> search(String kw) throws Exception {
            String url = BASE + "/api/search/get/web?type=1&s=" + URLEncoder.encode(kw, "UTF-8") + "&limit=30";
            JSONObject obj = new JSONObject(OkHttp.string(url, headers()));
            JSONObject result = obj.optJSONObject("result");
            JSONArray songs = result != null ? result.optJSONArray("songs") : null;
            List<MusicSong> list = new ArrayList<>();
            if (songs != null) {
                for (int i = 0; i < songs.length(); i++) {
                    MusicSong s = parseSong(songs.optJSONObject(i), PLATFORM_NETEASE);
                    if (s != null) list.add(s);
                }
            }
            return list;
        }
        /** 分类搜索（type=1歌曲/100歌手/10专辑/1000歌单） */
        static List<Object> searchType(String kw, int type) throws Exception {
            String url = BASE + "/api/search/get/web?type=" + type + "&s=" + URLEncoder.encode(kw, "UTF-8") + "&limit=30";
            JSONObject obj = new JSONObject(OkHttp.string(url, headers()));
            JSONObject result = obj.optJSONObject("result");
            List<Object> list = new ArrayList<>();
            if (result == null) return list;
            JSONArray arr = null;
            if (type == 1) arr = result.optJSONArray("songs");
            else if (type == 100) arr = result.optJSONArray("artists");
            else if (type == 10) arr = result.optJSONArray("albums");
            else if (type == 1000) arr = result.optJSONArray("playlists");
            if (arr == null) return list;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject item = arr.optJSONObject(i);
                if (item == null) continue;
                if (type == 1) {
                    MusicSong s = parseSong(item, PLATFORM_NETEASE);
                    if (s != null) list.add(s);
                } else {
                    String name = item.optString("name");
                    String sub = "";
                    if (type == 10) {
                        JSONObject artist = item.optJSONObject("artist");
                        if (artist != null) sub = artist.optString("name");
                    }
                    list.add(name + (sub.isEmpty() ? "" : " - " + sub));
                }
            }
            return list;
        }
        static List<MusicSong> hot() throws Exception {
            String url = BASE + "/api/playlist/detail?id=3778678"; // 热歌榜
            JSONObject obj = new JSONObject(OkHttp.string(url, headers()));
            JSONObject result = obj.optJSONObject("result");
            JSONArray tracks = result != null ? result.optJSONArray("tracks") : null;
            List<MusicSong> list = new ArrayList<>();
            if (tracks != null) {
                for (int i = 0; i < tracks.length(); i++) {
                    MusicSong s = parseSong(tracks.optJSONObject(i), PLATFORM_NETEASE);
                    if (s != null) list.add(s);
                }
            }
            return list;
        }
        static String resolveUrl(String id) {
            return BASE + "/song/media/outer/url?id=" + id + ".mp3";
        }
        /** 歌词（LRC 解析：时间ms + 文本，排序） */
        static List<LyricLine> lyric(String id) {
            List<LyricLine> lines = new ArrayList<>();
            try {
                String json = OkHttp.string(BASE + "/api/song/lyric?id=" + id + "&lv=-1&kv=-1&tv=-1", headers());
                JSONObject obj = new JSONObject(json);
                JSONObject lrc = obj.optJSONObject("lrc");
                String lyric = lrc != null ? lrc.optString("lyric") : "";
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
                        .matcher(lyric);
                while (m.find()) {
                    long min = Long.parseLong(m.group(1));
                    long sec = Long.parseLong(m.group(2));
                    long ms = Long.parseLong(m.group(3));
                    long t = min * 60000 + sec * 1000 + ms * 10;
                    String text = m.group(4).trim();
                    if (!text.isEmpty() && !text.startsWith("作曲") && !text.startsWith("作词")
                            && !text.startsWith("编曲") && !text.startsWith("制作")) {
                        lines.add(new LyricLine(t, text));
                    }
                }
                lines.sort((a, b) -> Long.compare(a.time, b.time));
            } catch (Exception e) { }
            return lines;
        }
        /** 为你推荐（推荐歌单） */
        static List<MusicPlaylist> recommend() throws Exception {
            String url = BASE + "/api/personalized/playlist?limit=10";
            JSONObject obj = new JSONObject(OkHttp.string(url, headers()));
            JSONArray result = obj.optJSONArray("result");
            List<MusicPlaylist> list = new ArrayList<>();
            if (result != null) {
                for (int i = 0; i < result.length(); i++) {
                    MusicPlaylist p = parsePlaylist(result.optJSONObject(i));
                    if (p != null) list.add(p);
                }
            }
            return list;
        }
        /** 精品榜单（飙升/新歌/原创榜） */
        static List<MusicPlaylist> ranks() throws Exception {
            int[] ids = {19723756, 3779629, 2884035, 3778678}; // 飙升/新歌/原创/热歌
            List<MusicPlaylist> list = new ArrayList<>();
            for (int id : ids) {
                try {
                    JSONObject obj = new JSONObject(OkHttp.string(BASE + "/api/playlist/detail?id=" + id, headers()));
                    JSONObject result = obj.optJSONObject("result");
                    JSONObject pl = result != null ? result.optJSONObject("playlist") : null;
                    MusicPlaylist p = parsePlaylist(pl);
                    if (p != null) list.add(p);
                } catch (Exception e) { }
            }
            return list;
        }
        /** 频道（热门歌单分类） */
        static List<MusicPlaylist> channels() throws Exception {
            String url = BASE + "/api/personalized/playlist?limit=12";
            JSONObject obj = new JSONObject(OkHttp.string(url, headers()));
            JSONArray result = obj.optJSONArray("result");
            List<MusicPlaylist> list = new ArrayList<>();
            if (result != null) {
                for (int i = 0; i < result.length(); i++) {
                    MusicPlaylist p = parsePlaylist(result.optJSONObject(i));
                    if (p != null) list.add(p);
                }
            }
            return list;
        }
        static MusicPlaylist parsePlaylist(JSONObject o) {
            if (o == null) return null;
            MusicPlaylist p = new MusicPlaylist();
            p.platform = PLATFORM_NETEASE;
            p.id = o.optString("id", String.valueOf(o.optLong("id", 0)));
            p.name = o.optString("name");
            p.cover = o.optString("picUrl", o.optString("coverImgUrl", ""));
            p.playCount = o.optLong("playCount", 0);
            return p;
        }
        static Map<String, String> headers() {
            Map<String, String> h = new HashMap<>();
            h.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
            h.put("Referer", "https://music.163.com/");
            return h;
        }
    }

    /** QQ 客户端（搜索可用，播放需登录/VIP） */
    static class QqApi {
        static final String BASE = "https://u.y.qq.com";
        static List<MusicSong> search(String kw) throws Exception {
            String json = "{\"req_1\":{\"module\":\"music.search.SearchCgiService\",\"method\":\"DoSearchForQQMusicDesktop\",\"param\":{\"query\":\"" + kw + "\",\"num_per_page\":30,\"page_num\":1}}}";
            String url = BASE + "/cgi-bin/musicu.fcg?data=" + URLEncoder.encode(json, "UTF-8");
            JSONObject obj = new JSONObject(OkHttp.string(url, qqHeaders()));
            JSONObject data = obj.optJSONObject("req_1");
            JSONObject body = data != null ? data.optJSONObject("data") : null;
            JSONObject song = body != null ? body.optJSONObject("song") : null;
            JSONArray list = song != null ? song.optJSONArray("list") : null;
            List<MusicSong> result = new ArrayList<>();
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.optJSONObject(i);
                    MusicSong s = parseQq(item);
                    if (s != null) result.add(s);
                }
            }
            return result;
        }
        static List<MusicSong> hot() throws Exception {
            // QQ 热歌榜 topId=63（TopList）
            String json = "{\"req_1\":{\"module\":\"musicToplist.ToplistInfoServer\",\"method\":\"GetDetail\",\"param\":{\"topId\":63}}}";
            String url = BASE + "/cgi-bin/musicu.fcg?data=" + URLEncoder.encode(json, "UTF-8");
            JSONObject obj = new JSONObject(OkHttp.string(url, qqHeaders()));
            JSONObject data = obj.optJSONObject("req_1");
            JSONObject body = data != null ? data.optJSONObject("data") : null;
            JSONObject songList = body != null ? body.optJSONObject("songList") : null;
            JSONArray list = songList != null ? songList.optJSONArray("songList") : null;
            List<MusicSong> result = new ArrayList<>();
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    MusicSong s = parseQq(list.optJSONObject(i));
                    if (s != null) result.add(s);
                }
            }
            return result;
        }
        static String resolveUrl(String id) {
            // QQ 播放需 vkey（登录/VIP），无 vkey 时返回 null → 提示受限
            return null;
        }
        static MusicSong parseQq(JSONObject item) {
            if (item == null) return null;
            MusicSong s = new MusicSong();
            s.source = PLATFORM_QQ;
            s.id = item.optString("mid");
            s.name = item.optString("name");
            JSONArray singers = item.optJSONArray("singer");
            StringBuilder sb = new StringBuilder();
            if (singers != null) {
                for (int i = 0; i < singers.length(); i++) {
                    if (i > 0) sb.append("/");
                    JSONObject a = singers.optJSONObject(i);
                    sb.append(a != null ? a.optString("name") : "");
                }
            }
            s.singer = sb.toString();
            JSONObject album = item.optJSONObject("album");
            if (album != null) s.album = album.optString("name");
            return s;
        }
        static Map<String, String> qqHeaders() {
            Map<String, String> h = new HashMap<>();
            h.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
            h.put("Referer", "https://y.qq.com/");
            return h;
        }
    }

    /** 酷我客户端（占位，App 实测） */
    static class KuwoApi {
        static List<MusicSong> search(String kw) throws Exception { return new ArrayList<>(); }
        static List<MusicSong> hot() throws Exception { return new ArrayList<>(); }
        static String resolveUrl(String id) { return null; }
    }

    /** 酷狗客户端（占位，App 实测） */
    static class KugouApi {
        static List<MusicSong> search(String kw) throws Exception { return new ArrayList<>(); }
        static List<MusicSong> hot() throws Exception { return new ArrayList<>(); }
        static String resolveUrl(String id) { return null; }
    }

    /** 咪咕客户端（占位，App 实测） */
    static class MiguApi {
        static List<MusicSong> search(String kw) throws Exception { return new ArrayList<>(); }
        static List<MusicSong> hot() throws Exception { return new ArrayList<>(); }
        static String resolveUrl(String id) { return null; }
    }
}
