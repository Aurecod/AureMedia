package com.fongmi.android.tv.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        // 对标 webhtv `LiveConfig.sync`（url == VodConfig.getUrl()）+ AVBox 直播源列表首项。
        // ★ 必须是纯内存对象：不能 insert()，否则会往数据库写脏记录（且下次 getAll 已含该 url，导致首项消失）。
        followVod = false;
        if (type == 1) {
            String vodUrl = VodConfig.getUrl();
            if (!TextUtils.isEmpty(vodUrl) && !hasUrl(mItems, vodUrl)) {
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

    private boolean isCurrent(Config item) {
        // "跟随点播源"是虚拟项（无 id）：直播当前订阅已等于点播订阅即视为当前
        if (item == followItem && current != null) return TextUtils.equals(current.getUrl(), item.getUrl());
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
        holder.binding.text.setEnabled(!current);
        holder.binding.text.setFocusable(!current);
        holder.binding.text.setOnClickListener(v -> {
            if (!current) listener.onTextClick(item);
        });
        holder.binding.text.setOnLongClickListener(v -> !current && !followVod && listener.onTextLongClick(holder));
        holder.binding.edit.setVisibility(followVod || readOnly ? View.GONE : View.VISIBLE);
        holder.binding.edit.setOnClickListener(v -> listener.onEditClick(item));
        holder.binding.delete.setVisibility(followVod || readOnly ? View.GONE : View.VISIBLE);
        holder.binding.delete.setAlpha(current ? 0.38f : 1f);
        holder.binding.delete.setOnClickListener(v -> listener.onDeleteClick(item));
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        private final AdapterConfigBinding binding;

        public ViewHolder(@NonNull AdapterConfigBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
