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

    /** 外观：浅色/暗色/系统/自动(18-06暗色) → 是否暗色背景 */
    public static boolean isDarkTheme(Context ctx) {
        String theme = getStr(ctx, "theme", "自动");
        if ("浅色".equals(theme)) return false;
        if ("暗色".equals(theme)) return true;
        if ("系统".equals(theme)) {
            int uiMode = ctx.getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            return uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES;
        }
        // 自动：18:00–06:00 暗色
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        return hour >= 18 || hour < 6;
    }

    /** 当前主题背景色 */
    public static int themeBgColor(Context ctx) {
        return isDarkTheme(ctx) ? 0xFF12141A : 0xFFF2F3F5;
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

    /** 为你推荐（各平台） */
    public static List<MusicPlaylist> recommend(Context ctx) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.recommend();
        if (PLATFORM_QQ.equals(platform)) return QqApi.recommend();
        return new ArrayList<>();
    }

    /** 精品榜单（各平台） */
    public static List<MusicPlaylist> ranks(Context ctx) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.ranks();
        if (PLATFORM_QQ.equals(platform)) return QqApi.ranks();
        return new ArrayList<>();
    }

    /** 频道（各平台） */
    public static List<MusicPlaylist> channels(Context ctx) throws Exception {
        String platform = getPlatform(ctx);
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.channels();
        if (PLATFORM_QQ.equals(platform)) return QqApi.channels();
        return new ArrayList<>();
    }

    /** 按平台解析播放 URL */
    public static String resolveUrl(Context ctx, MusicSong song) {
        String platform = song.source;
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.resolveUrl(song.id, qualityBr(ctx));
        if (PLATFORM_QQ.equals(platform)) return QqApi.resolveUrl(song.id);
        if (PLATFORM_KUWO.equals(platform)) return KuwoApi.resolveUrl(song.id);
        if (PLATFORM_KUGOU.equals(platform)) return KugouApi.resolveUrl(song.id);
        if (PLATFORM_MIGU.equals(platform)) return MiguApi.resolveUrl(song.id);
        return null;
    }

    /** 音质档位 → 码率(网易) */
    public static int qualityBr(Context ctx) {
        String q = getStr(ctx, "音质", "无损");
        switch (q) {
            case "标准": return 128;
            case "较高": return 192;
            case "高品": return 320;
            case "Hi-Res": return 9990;
            case "母带": return 9990;
            case "无损":
            default: return 9990;
        }
    }

    /** 音质降一档（自动降级用），最低"标准"返回 null */
    public static String lowerQuality(String q) {
        switch (q) {
            case "母带": return "Hi-Res";
            case "Hi-Res": return "无损";
            case "无损": return "高品";
            case "高品": return "较高";
            case "较高": return "标准";
            default: return null; // 标准已最低
        }
    }

    /** 歌词行（时间ms + 文本） */
    public static class LyricLine {
        public long time;
        public String text;
        public String trans; // 翻译(可选)
        public LyricLine(long time, String text) { this.time = time; this.text = text; }
    }

    /** 获取歌词（网易 api/song/lyric，LRC 解析） */
    public static List<LyricLine> lyric(Context ctx, MusicSong song) {
        String platform = song.source;
        if (PLATFORM_NETEASE.equals(platform)) return NeteaseApi.lyric(song.id);
        if (PLATFORM_QQ.equals(platform)) return QqApi.lyric(song.id);
        return new ArrayList<>();
    }

    /** 公共：LRC 解析（主歌词），供各平台复用 */
    static void parseLrc(List<LyricLine> lines, String lrc) {
        if (lrc == null) return;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
                .matcher(lrc);
        while (m.find()) {
            try {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                long ms = Long.parseLong(m.group(3));
                long t = min * 60000 + sec * 1000 + ms * 10;
                String text = m.group(4).trim();
                if (!text.isEmpty() && !text.startsWith("作曲") && !text.startsWith("作词")
                        && !text.startsWith("编曲") && !text.startsWith("制作")) {
                    lines.add(new LyricLine(t, text));
                }
            } catch (NumberFormatException ignored) { }
        }
    }

    /** 公共：翻译 LRC 按最近时间合并 */
    static void parseLrcTrans(List<LyricLine> lines, String trans) {
        if (trans == null || trans.isEmpty()) return;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
                .matcher(trans);
        while (m.find()) {
            try {
                long min = Long.parseLong(m.group(1));
                long sec = Long.parseLong(m.group(2));
                long ms = Long.parseLong(m.group(3));
                long t = min * 60000 + sec * 1000 + ms * 10;
                String text = m.group(4).trim();
                if (text.isEmpty()) continue;
                LyricLine best = null;
                for (LyricLine line : lines) {
                    if (Math.abs(line.time - t) <= 3000) { best = line; break; }
                }
                if (best != null) best.trans = text;
            } catch (NumberFormatException ignored) { }
        }
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
            String url = BASE + "/api/v6/playlist/detail?id=3778678"; // 热歌榜(v6 有 tracks)
            JSONObject obj = new JSONObject(OkHttp.string(url, headers()));
            JSONObject playlist = obj.optJSONObject("playlist");
            JSONArray tracks = playlist != null ? playlist.optJSONArray("tracks") : null;
            List<MusicSong> list = new ArrayList<>();
            if (tracks != null) {
                for (int i = 0; i < tracks.length(); i++) {
                    MusicSong s = parseSong(tracks.optJSONObject(i), PLATFORM_NETEASE);
                    if (s != null) list.add(s);
                }
            }
            return list;
        }
        static String resolveUrl(String id, int br) {
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
                // 翻译(tlyric)按最近时间合并
                JSONObject tlyr = obj.optJSONObject("tlyric");
                String tly = tlyr != null ? tlyr.optString("lyric") : "";
                java.util.regex.Matcher tm = java.util.regex.Pattern
                        .compile("\\[(\\d+):(\\d+)\\.(\\d+)\\](.*)")
                        .matcher(tly);
                while (tm.find()) {
                    long min = Long.parseLong(tm.group(1));
                    long sec = Long.parseLong(tm.group(2));
                    long ms = Long.parseLong(tm.group(3));
                    long t = min * 60000 + sec * 1000 + ms * 10;
                    String text = tm.group(4).trim();
                    if (text.isEmpty()) continue;
                    LyricLine best = null;
                    for (LyricLine line : lines) {
                        if (Math.abs(line.time - t) <= 3000) { best = line; break; }
                    }
                    if (best != null) best.trans = text;
                }
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
                    JSONObject obj = new JSONObject(OkHttp.string(BASE + "/api/v6/playlist/detail?id=" + id, headers()));
                    JSONObject pl = obj.optJSONObject("playlist");
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
            // QQ 热歌榜 topId=26（GetDetail 返回 songInfoList）
            String json = "{\"comm\":{\"ct\":24},\"req_1\":{\"module\":\"musicToplist.ToplistInfoServer\",\"method\":\"GetDetail\",\"param\":{\"topId\":26,\"offset\":0,\"num\":30,\"withRes\":1}}}";
            String url = BASE + "/cgi-bin/musicu.fcg?data=" + URLEncoder.encode(json, "UTF-8");
            JSONObject obj = new JSONObject(OkHttp.string(url, qqHeaders()));
            JSONObject data = obj.optJSONObject("req_1");
            JSONObject body = data != null ? data.optJSONObject("data") : null;
            JSONArray list = body != null ? body.optJSONArray("songInfoList") : null;
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
        /** QQ 精品榜单（topList 26热歌/63巅峰榜/19欧美/16电音） */
        static List<MusicPlaylist> ranks() throws Exception {
            int[] ids = {26, 63, 19, 16};
            String[] names = {"热歌榜", "巅峰榜", "欧美榜", "电音榜"};
            List<MusicPlaylist> list = new ArrayList<>();
            for (int i = 0; i < ids.length; i++) {
                MusicPlaylist p = new MusicPlaylist();
                p.platform = PLATFORM_QQ;
                p.id = String.valueOf(ids[i]);
                p.name = names[i];
                p.cover = "";
                list.add(p);
            }
            return list;
        }
        /** QQ 为你推荐（热歌榜兜底） */
        static List<MusicPlaylist> recommend() throws Exception {
            List<MusicPlaylist> list = new ArrayList<>();
            MusicPlaylist p = new MusicPlaylist();
            p.platform = PLATFORM_QQ;
            p.id = "26";
            p.name = "QQ热歌";
            p.cover = "";
            list.add(p);
            return list;
        }
        /** QQ 频道（榜单分类） */
        static List<MusicPlaylist> channels() throws Exception {
            return ranks();
        }
        /** QQ 歌词（fcg_query_lyric_new，lyric/trans） */
        static List<LyricLine> lyric(String id) {
            List<LyricLine> lines = new ArrayList<>();
            try {
                String url = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg?songmid=" + id + "&format=json&nobase64=1";
                Map<String, String> h = new HashMap<>();
                h.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
                h.put("Referer", "https://y.qq.com/");
                h.put("Cookie", "g_tk=5381; uin=0; cv=770");
                String json = OkHttp.string(url, h);
                JSONObject obj = new JSONObject(json);
                String lyric = obj.optString("lyric");
                if (lyric.startsWith("\"") && lyric.endsWith("\"")) {
                    lyric = lyric.substring(1, lyric.length() - 1);
                }
                String trans = obj.optString("trans");
                if (trans.startsWith("\"") && trans.endsWith("\"")) {
                    trans = trans.substring(1, trans.length() - 1);
                }
                // QQ 罗马音（yrc 字段，日文歌）
                String yrc = obj.optString("yrc");
                if (trans.isEmpty() && yrc.startsWith("\"") && yrc.endsWith("\"")) {
                    trans = yrc.substring(1, yrc.length() - 1);
                }
                parseLrc(lines, lyric);
                parseLrcTrans(lines, trans);
                lines.sort((a, b) -> Long.compare(a.time, b.time));
            } catch (Exception e) { }
            return lines;
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

    /** 酷我客户端（App 内 OkHttp 尝试，榜单/搜索需 csrf 完整处理） */
    static class KuwoApi {
        static final String BASE = "https://www.kuwo.cn";
        static List<MusicSong> search(String kw) throws Exception {
            // 酷我搜索需 csrf（kw_token cookie + t 参数），App 内尝试
            String json = "{\"key\":\"" + kw + "\",\"pn\":1,\"rn\":30,\"https\":true}";
            String url = "https://m.kuwo.cn/newh5app/api/www/search/searchMusicBykeyWord?key=" + URLEncoder.encode(kw, "UTF-8") + "&pn=1&rn=30&t=" + System.currentTimeMillis();
            JSONObject obj = new JSONObject(OkHttp.string(url, kuwoHeaders()));
            JSONObject data = obj.optJSONObject("data");
            JSONArray list = data != null ? data.optJSONArray("list") : null;
            List<MusicSong> result = new ArrayList<>();
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    MusicSong s = parseKuwo(list.optJSONObject(i));
                    if (s != null) result.add(s);
                }
            }
            return result;
        }
        static List<MusicSong> hot() throws Exception {
            // 酷我热榜（bangList，App 内尝试）
            String url = "https://www.kuwo.cn/api/www/bang/bang/bangList?limit=30&t=" + System.currentTimeMillis();
            JSONObject obj = new JSONObject(OkHttp.string(url, kuwoHeaders()));
            JSONObject data = obj.optJSONObject("data");
            JSONArray list = data != null ? data.optJSONArray("list") : null;
            List<MusicSong> result = new ArrayList<>();
            if (list != null) {
                for (int i = 0; i < list.length(); i++) {
                    MusicSong s = parseKuwo(list.optJSONObject(i));
                    if (s != null) result.add(s);
                }
            }
            return result;
        }
        static String resolveUrl(String id) { return null; } // 酷我播放需 csrf，App 内尝试
        static MusicSong parseKuwo(JSONObject item) {
            if (item == null) return null;
            MusicSong s = new MusicSong();
            s.source = PLATFORM_KUWO;
            s.id = item.optString("rid", item.optString("id"));
            s.name = item.optString("name");
            s.singer = item.optString("artist", item.optString("artistName", ""));
            s.cover = item.optString("pic", item.optString("picUrl", ""));
            return s;
        }
        static Map<String, String> kuwoHeaders() {
            Map<String, String> h = new HashMap<>();
            h.put("User-Agent", "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36");
            h.put("Referer", "https://www.kuwo.cn/");
            h.put("Cookie", "kw_token=" + System.currentTimeMillis());
            h.put("csrf", "abc");
            return h;
        }
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
