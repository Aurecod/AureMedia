package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.setting.InterfaceOrderStore;
import com.fongmi.android.tv.databinding.AdapterConfigBinding;

import java.util.List;

public class ConfigAdapter extends RecyclerView.Adapter<ConfigAdapter.ViewHolder> {

    private final OnClickListener listener;
    private List<Config> mItems;
    private boolean readOnly;
    private boolean protectCurrent;
    private boolean followVod;
    private Config followItem;
    private Config current;

    public ConfigAdapter(OnClickListener listener) {
        this.listener = listener;
    }

    public interface OnClickListener {

        void onTextClick(Config item);

        boolean onTextLongClick(ViewHolder holder);

        void onEditClick(Config item);

        void onDeleteClick(Config item);
    }

    public ConfigAdapter readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public ConfigAdapter protectCurrent(boolean protectCurrent) {
        this.protectCurrent = protectCurrent;
        return this;
    }

    public ConfigAdapter addAll(int type) {
        return addAll(type, null);
    }

    public ConfigAdapter addAll(int type, Config current) {
        this.current = current;
        mItems = type == 0 ? InterfaceOrderStore.sortVodConfigs(Config.getAll(type)) : Config.getAll(type);
        // 清理历史脏记录（旧版本曾把"跟随点播源"insert 进数据库）
        if (type == 1) mItems.removeIf(item -> TextUtils.equals(item.getName(), "跟随点播源"));
        // 直播源列表第 0 项："跟随点播源"（直播 URL = 点播 URL，加载点播订阅里的 lives）。
        // ★ 对标 QussMedia/AVBox：显式 live_follow_vod flag，首项【始终显示】（不再依赖
        //   hasUrl 条件），点它 = setFollowVod(true)，选具体订阅 = setFollowVod(false)。
        // ★ 必须是纯内存对象：不能 insert()，否则会往数据库写脏记录（且下次 getAll 已含该 url，导致首项消失）。
        followVod = false;
        if (type == 1) {
            String vodUrl = VodConfig.getUrl();
            if (!TextUtils.isEmpty(vodUrl)) {
                mItems.add(0, new Config().type(1).url(vodUrl).name("跟随点播源"));
                followVod = true;
            }
        }
        // 虚拟项不参与 removeIf（避免直播当前 url 已等于点播 url 时被一并删掉）
        Config follow = followItem = followVod ? mItems.get(0) : null;
        String currentUrl = current == null ? null : current.getUrl();
        if (!readOnly && !protectCurrent && !TextUtils.isEmpty(currentUrl)) {
            mItems.removeIf(item -> item != follow && TextUtils.equals(item.getUrl(), currentUrl));
        }
        if (follow != null) mItems.add(0, follow);
        return this;
    }

    private boolean hasUrl(List<Config> items, String url) {
        for (Config item : items) if (TextUtils.equals(item.getUrl(), url)) return true;
        return false;
    }

    public boolean isFollowVod(int position) {
        return followVod && position == 0;
    }

    public boolean isFollowVodItem(Config item) {
        return followItem != null && item == followItem;
    }

    private boolean isCurrent(Config item) {
        // "跟随点播源"是虚拟项：当前是否为跟随态由显式 flag 判定（对标 QussMedia followVod）
        if (item == followItem && current != null) return LiveConfig.isFollowVod();
        if (!protectCurrent || current == null) return false;
        if (current.getId() > 0 && item.getId() == current.getId()) return true;
        return current.getType() == item.getType()
                && !TextUtils.isEmpty(current.getInterfaceKey())
                && TextUtils.equals(current.getInterfaceKey(), item.getInterfaceKey());
    }

    public boolean isProtectedCurrent(Config item) {
        return isCurrent(item);
    }

    public int remove(Config item) {
        int position = mItems.indexOf(item);
        if (position == -1) return -1;
        item.delete();
        mItems.remove(position);
        if (item.getType() == 0) InterfaceOrderStore.saveVodConfigs(mItems);
        notifyItemRemoved(position);
        return getItemCount();
    }

    public boolean drag(int from, int to) {
        if (from < 0 || to < 0 || from >= mItems.size() || to >= mItems.size() || from == to) return false;
        Config item = mItems.remove(from);
        mItems.add(to, item);
        notifyItemMoved(from, to);
        if (mItems.size() > 0) InterfaceOrderStore.saveVodConfigs(mItems);
        return true;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(AdapterConfigBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Config item = mItems.get(position);
        boolean current = isCurrent(item);
        boolean followVod = isFollowVod(position);
        holder.binding.text.setText(followVod ? "跟随点播源" : item.getDesc());
        holder.binding.text.setEnabled(true);
        holder.binding.text.setFocusable(!current);
        holder.binding.text.setOnClickListener(v -> {
            if (!current) listener.onTextClick(item);
        });
        holder.binding.text.setOnLongClickListener(v -> !current && !followVod && listener.onTextLongClick(holder));
        // ★ 当前项：填充颜色高亮（对标站源切换 selected 状态）
        applyCurrentHighlight(holder, current);
        holder.binding.edit.setVisibility(followVod || readOnly ? View.GONE : View.VISIBLE);
        holder.binding.edit.setOnClickListener(v -> listener.onEditClick(item));
        holder.binding.delete.setVisibility(followVod || readOnly ? View.GONE : View.VISIBLE);
        holder.binding.delete.setAlpha(current ? 0.38f : 1f);
        holder.binding.delete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    /** 当前接口项 = 填充 primary 色 + onPrimary 字（对标 SiteDialogTheme 站源 selected）；非当前 = 原描边样式 */
    private void applyCurrentHighlight(ViewHolder holder, boolean current) {
        android.content.Context ctx = holder.itemView.getContext();
        // ★ MaterialColors.getColor(View, int) 两参；colorPrimary 在 appcompat R，colorOnPrimary 在 material R（对标 SiteDialogTheme）
        int primary = com.google.android.material.color.MaterialColors.getColor(holder.itemView, androidx.appcompat.R.attr.colorPrimary);
        int onPrimary = com.google.android.material.color.MaterialColors.getColor(holder.itemView, com.google.android.material.R.attr.colorOnPrimary);
        android.content.res.ColorStateList normalBg = androidx.core.content.ContextCompat.getColorStateList(ctx, R.color.dialog_outlined_button_bg);
        android.content.res.ColorStateList normalText = androidx.core.content.ContextCompat.getColorStateList(ctx, R.color.dialog_outlined_button_text);
        if (current) {
            holder.binding.text.setBackgroundTintList(new android.content.res.ColorStateList(
                    new int[][]{{android.R.attr.state_selected}, {}},
                    new int[]{primary, normalBg.getDefaultColor()}));
            holder.binding.text.setTextColor(new android.content.res.ColorStateList(
                    new int[][]{{android.R.attr.state_selected}, {}},
                    new int[]{onPrimary, normalText.getDefaultColor()}));
        } else {
            holder.binding.text.setBackgroundTintList(normalBg);
            holder.binding.text.setTextColor(normalText);
        }
        holder.binding.text.setSelected(current);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterConfigBinding binding;

        public ViewHolder(@NonNull AdapterConfigBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
