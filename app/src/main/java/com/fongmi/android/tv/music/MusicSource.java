package com.fongmi.android.tv.music;

/** 音乐音源模型（对标 am-4u2 aq_sources：音源决定支持哪些平台） */
public class MusicSource {
    public String id;
    public String name;
    public String type;       // js / api
    public boolean enabled;
    public String[] platforms; // qq / netease / kugou / kuwo / migu

    public MusicSource(String id, String name, String type, String... platforms) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.enabled = true;
        this.platforms = platforms;
    }
}
