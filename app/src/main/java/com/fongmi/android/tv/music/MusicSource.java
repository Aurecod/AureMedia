package com.fongmi.android.tv.music;

/** 音乐音源模型（对标 am-4u2 aq_sources：音源决定支持哪些平台） */
public class MusicSource {
    public String id;
    public String name;
    public String type;       // js / api
    public boolean enabled;
    public boolean builtin;   // 内置（不可删）
    public String link;       // 音源 JS 在线地址（gitee 等）
    public String[] platforms; // qq / netease / kugou / kuwo / migu

    public MusicSource(String id, String name, String type, String link, String... platforms) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.link = link;
        this.enabled = true;
        this.builtin = false;
        this.platforms = platforms;
    }

    public boolean supports(String platform) {
        if (platforms == null) return false;
        for (String p : platforms) {
            if (p.equals(platform)) return true;
        }
        return false;
    }
}
