package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.BuildConfig;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AppVersion {

    /**
     * Release tags carry a build stamp, for example {@code v5.6.0-202609221328} (stable) or
     * {@code v5.6.0-beta-202610091755} (beta). The stamp is the release moment, so it is the only
     * reliable way to order two builds that publish the same {@code versionCode}.
     */
    private static final Pattern BUILD_STAMP = Pattern.compile("(?<!\\d)(\\d{12,14})(?!\\d)");

    /** Width of {@code yyyyMMddHHmmss}; the widest stamp {@link #BUILD_STAMP} can return. */
    private static final int STAMP_LENGTH = 14;

    private AppVersion() {
    }

    public static String fullName() {
        String tag = BuildConfig.BUILD_TAG == null ? "" : BuildConfig.BUILD_TAG.trim();
        if (tag.isEmpty()) tag = BuildConfig.VERSION_NAME;
        return stripPrefix(tag);
    }

    /** build 计数（同版本每次 build +1，CI 递增写回 build.gradle 的 buildSeq）。 */
    public static int buildSeq() {
        try {
            return Integer.parseInt(BuildConfig.BUILD_SEQ);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * ★ 关于页显示名：版本 + build 计数以上角标加在 build 上（如 {@code 1.0.9-build⁺²}）。
     * 上角标只加在 build 标识上，不改主版本号。
     */
    public static String displayName() {
        String base = fullName();
        if (base.isEmpty()) return base;
        int seq = buildSeq();
        if (seq <= 0) return base;
        // 去掉 -build-N / -build 后缀，统一还原为 versionBase-build 再附上标计数
        String cleaned = base.replaceFirst("(?i)-build(?:-[0-9]+)?$", "");
        return cleaned + "-build" + superscript(seq);
    }

    private static String superscript(int n) {
        char[] map = {'⁰', '¹', '²', '³', '⁴', '⁵', '⁶', '⁷', '⁸', '⁹'};
        StringBuilder sb = new StringBuilder("⁺");
        for (char c : String.valueOf(n).toCharArray()) {
            int d = c - '0';
            sb.append(d >= 0 && d <= 9 ? map[d] : c);
        }
        return sb.toString();
    }

    public static boolean isCurrent(String name) {
        return stripPrefix(name).equals(stripPrefix(fullName()));
    }

    /** True only when {@code target} is a strictly newer build than the running one. */
    public static boolean isNewer(String target) {
        return isNewer(fullName(), target);
    }

    /**
     * Orders two release tags by their build stamp.
     *
     * <p>An older or identical tag is never an update. That matters because the stable and beta
     * channels of the same release line can publish the same {@code versionCode}: treating
     * "different tag" as "newer" made an older package look installable over a newer one, so the
     * app kept offering a downgrade and the two channels overwrote each other in a loop.
     *
     * <p>Tags without a usable stamp keep the historical "different means update" behavior. Two
     * usable stamps are always ordered against each other, also when one carries seconds and the
     * other does not; only a missing stamp falls back to "different means update".
     */
    public static boolean isNewer(String current, String target) {
        String currentTag = stripPrefix(current);
        String targetTag = stripPrefix(target);
        if (targetTag.isEmpty() || targetTag.equals(currentTag)) return false;
        String currentStamp = buildStamp(currentTag);
        String targetStamp = buildStamp(targetTag);
        if (currentStamp.isEmpty() || targetStamp.isEmpty()) return true;
        return padStamp(targetStamp).compareTo(padStamp(currentStamp)) > 0;
    }

    /**
     * Widens a {@code yyyyMMddHHmm} stamp to {@code yyyyMMddHHmmss} so two stamps of different
     * lengths stay comparable; a minute stamp means second zero of that minute.
     */
    private static String padStamp(String stamp) {
        StringBuilder padded = new StringBuilder(stamp);
        while (padded.length() < STAMP_LENGTH) padded.append('0');
        return padded.toString();
    }

    /** Returns the trailing build stamp ({@code yyyyMMddHHmm[ss]}) of a release tag, or {@code ""}. */
    public static String buildStamp(String value) {
        if (value == null) return "";
        Matcher matcher = BUILD_STAMP.matcher(value);
        String stamp = "";
        while (matcher.find()) stamp = matcher.group(1);
        return stamp;
    }

    public static String stripPrefix(String value) {
        if (value == null) return "";
        value = value.trim();
        return value.startsWith("v") && value.length() > 1 && Character.isDigit(value.charAt(1)) ? value.substring(1) : value;
    }
}
