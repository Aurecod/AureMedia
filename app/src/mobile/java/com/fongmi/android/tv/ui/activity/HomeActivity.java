package com.fongmi.android.tv.ui.activity;

import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.Intent;

import com.fongmi.android.tv.music.MusicActivity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.core.content.pm.ShortcutInfoCompat;
import androidx.core.content.pm.ShortcutManagerCompat;
import androidx.core.graphics.drawable.IconCompat;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.api.config.WallConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.databinding.ActivityHomeBinding;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.event.ServerEvent;
import com.fongmi.android.tv.event.StateEvent;
import com.fongmi.android.tv.following.FollowingPlaybackBridge;
import com.fongmi.android.tv.following.FollowingSettings;
import com.fongmi.android.tv.following.FollowingScheduler;
import com.fongmi.android.tv.impl.Callback;
import com.fongmi.android.tv.lab.LabActivity;
import com.fongmi.android.tv.lab.LabConfig;
import com.fongmi.android.tv.player.Source;
import com.fongmi.android.tv.receiver.ShortcutReceiver;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.service.PlaybackService;
import com.fongmi.android.tv.setting.AutoBackupPolicy;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.FragmentStateManager;
import com.fongmi.android.tv.ui.fragment.SettingEnhanceFragment;
import com.fongmi.android.tv.ui.fragment.SettingAdFragment;
import com.fongmi.android.tv.ui.fragment.SettingAiFragment;
import com.fongmi.android.tv.ui.fragment.SettingTmdbFragment;
import com.fongmi.android.tv.ui.fragment.SettingDanmakuFragment;
import com.fongmi.android.tv.ui.fragment.SettingFragment;
import com.fongmi.android.tv.ui.fragment.SettingPersonalFragment;
import com.fongmi.android.tv.ui.fragment.SettingPlayerFragment;
import com.fongmi.android.tv.ui.fragment.SettingSubtitleFragment;
import com.fongmi.android.tv.ui.fragment.VodFragment;
import com.fongmi.android.tv.utils.CrashRestartMode;
import com.fongmi.android.tv.utils.PreviousProcessCrashRecovery;
import com.fongmi.android.tv.utils.FileChooser;
import com.fongmi.android.tv.utils.MobileWindow;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.PermissionUtil;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.UrlUtil;
import com.fongmi.android.tv.utils.Util;
import com.fongmi.android.tv.web.WebHomeChromeStartup;
import com.fongmi.android.tv.web.WebHomeViewport;
import com.github.catvod.net.OkHttp;
import com.google.android.material.navigation.NavigationBarView;
import com.google.gson.JsonObject;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HomeActivity extends BaseActivity implements NavigationBarView.OnItemSelectedListener, WebHomeChromeController.Host {

    public static final String EXTRA_NAV_POSITION = "nav_position";
    private static final String STATE_RETURN_VOD_FROM_ENHANCE = "returnVodFromEnhance";
    private static final String STATE_CURRENT_POSITION = "currentPosition";

    private FragmentStateManager mManager;
    private ActivityHomeBinding mBinding;
    private WebHomeChromeController mChrome;
    private Config mStartupConfig;
    private boolean wideWindow;
    private int currentPosition;
    private boolean returnVodFromEnhance;
    private boolean sideNavVisible;
    private int sideNavWidth;
    private boolean sideNavFromLeft = true; // 侧栏从哪边弹出（由边缘手势方向决定）
    private float mSideDownX = -1;
    private float mSideDownY = -1;
    private int mSideEdge = 0; // -1 左边缘, 0 无, 1 右边缘

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityHomeBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        checkAction(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_App);
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void initView(Bundle savedInstanceState) {
        wideWindow = MobileWindow.isWide(this);
        returnVodFromEnhance = savedInstanceState != null && savedInstanceState.getBoolean(STATE_RETURN_VOD_FROM_ENHANCE);
        currentPosition = savedInstanceState == null ? 0 : savedInstanceState.getInt(STATE_CURRENT_POSITION, 0);
        mStartupConfig = Config.vod();
        mChrome = new WebHomeChromeController(this, mBinding, this, savedInstanceState, WebHomeChromeStartup.restore(mStartupConfig));
        mBinding.getRoot().addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> checkWindowShape(right - left, bottom - top));
        mBinding.navigation.setOnItemSelectedListener(this);
        setNavigation();
        PermissionUtil.requestFile(this, allGranted -> PermissionUtil.requestNotify(this));
        FollowingScheduler.ensurePeriodic(this);
        FollowingScheduler.enqueueDueNow(this);
        initFragment(savedInstanceState);
        if (savedInstanceState == null && getIntent().hasExtra(EXTRA_NAV_POSITION)) checkAction(getIntent());
        initConfig();
        initSideNav();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBinding.navigation.getMenu().findItem(R.id.lab).isVisible() != LabConfig.get().getNavEntry()
                || mBinding.navigation.getMenu().findItem(R.id.following).isVisible() != FollowingSettings.isEnabled()) setNavigation();
        updateFollowingBadge();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_RETURN_VOD_FROM_ENHANCE, returnVodFromEnhance);
        outState.putInt(STATE_CURRENT_POSITION, currentPosition);
        if (mChrome != null) mChrome.save(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void initEvent() {
        mBinding.navigation.findViewById(R.id.live).setOnLongClickListener(this::addShortcut);
    }

    private void checkAction(Intent intent) {
        PreviousProcessCrashRecovery.checkBeforeConfig(this, () -> handleAction(intent));
    }

    private void handleAction(Intent intent) {
        if (intent.hasExtra(EXTRA_NAV_POSITION)) {
            change(intent.getIntExtra(EXTRA_NAV_POSITION, 0));
            intent.removeExtra(EXTRA_NAV_POSITION);
        } else if (Intent.ACTION_SEND.equals(intent.getAction())) {
            VideoActivity.push(this, intent.getStringExtra(Intent.EXTRA_TEXT));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            PermissionUtil.requestFile(this, allGranted -> checkType(intent));
        } else if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String keyword = intent.getStringExtra(SearchManager.QUERY);
            if (!TextUtils.isEmpty(keyword)) SearchActivity.start(this, keyword);
        }
    }

    private void checkType(Intent intent) {
        if ("text/plain".equals(intent.getType()) || UrlUtil.path(intent.getData()).endsWith(".m3u")) {
            loadLive("file:/" + FileChooser.getPathFromUri(intent.getData()));
        } else {
            VideoActivity.push(this, intent.getData().toString());
        }
    }

    private void initFragment(Bundle savedInstanceState) {
        mManager = new FragmentStateManager(mBinding.container, getSupportFragmentManager(), position -> switch (position) {
            case 0 -> VodFragment.newInstance();
            case 1 -> SettingFragment.newInstance();
            case 2 -> SettingPlayerFragment.newInstance();
            case 3 -> SettingEnhanceFragment.newInstance();
            case 4 -> SettingDanmakuFragment.newInstance();
            case 5 -> SettingPersonalFragment.newInstance();
            case 6 -> SettingSubtitleFragment.newInstance();
            case 7 -> SettingTmdbFragment.newInstance();
            case 8 -> SettingAiFragment.newInstance();
            case 9 -> SettingAdFragment.newInstance();
            default -> null;
        });
        if (savedInstanceState == null) change(0);
        else restorePosition(currentPosition);
    }

    private void restorePosition(int position) {
        setNavigation();
        syncNavigationSelection();
        changeFragment(position <= 0 ? 0 : position);
    }

    private void initConfig() {
        PreviousProcessCrashRecovery.checkBeforeConfig(this, this::loadConfig);
    }

    private void loadConfig() {
        if (CrashRestartMode.consume()) {
            checkAction(getIntent());
            StateEvent.empty();
            return;
        }
        VodConfig.get().config(mStartupConfig == null ? Config.vod() : mStartupConfig).load(getCallback());
        LiveConfig.get().init().load();
        WallConfig.get().init();
    }

    private Callback getCallback() {
        return new Callback() {
            @Override
            public void success() {
                checkAction(getIntent());
            }

            /**
             * 还没有配置任何内容源（重置应用、重装或恢复出厂后的首次启动）。
             *
             * <p>这不是失败：没有地址可请求、没有失败原因、重试也永远不会成功。改动前
             * {@code BaseConfig} 把它当 {@code error("")} 抛出来，首页便显示「内容源加载失败 /
             * 重试」——一个不存在的故障加一个点了没用的按钮。这里改发「未配置」状态：
             * 不接管首页，由 {@code VodFragment} 收起状态浮层、保留底部导航；添加接口仍走
             * 既有的「设置 → 点播」管理面板（{@code HistoryDialog} 的「添加」）。
             */
            @Override
            public void noConfig() {
                resetVodChrome();
                checkAction(getIntent());
                StateEvent.noConfig();
            }

            @Override
            public void error(String msg) {
                resetVodChrome();
                checkAction(getIntent());
                // 失败发 ERROR（带原因），不发 EMPTY（EMPTY 现在渲染全屏空态）：消费方收到
                // ERROR 只收起浮层与转圈、回到原生首页，顶栏与切换内容源照常可用，失败由吐司提示。
                StateEvent.error(msg);
                Notify.show(msg);
            }
        };
    }

    private void loadLive(String url) {
        LiveConfig.load(Config.find(url, 1), new Callback() {
            @Override
            public void success() {
                openLive();
            }
        });
    }

    private void setNavigation() {
        mBinding.navigation.getMenu().findItem(R.id.vod).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.setting).setVisible(true);
        mBinding.navigation.getMenu().findItem(R.id.lab).setVisible(LabConfig.get().getNavEntry());
        mBinding.navigation.getMenu().findItem(R.id.live).setVisible(LiveConfig.hasUrl());
        mBinding.navigation.getMenu().findItem(R.id.following).setVisible(FollowingSettings.isEnabled());
        updateFollowingBadge();
        syncNavigationSelection();
    }

    private boolean openLive() {
        LiveActivity.start(this);
        return false;
    }

    private boolean addShortcut(View view) {
        ShortcutInfoCompat info = new ShortcutInfoCompat.Builder(this, getString(R.string.nav_live)).setIcon(IconCompat.createWithResource(this, R.mipmap.ic_launcher)).setIntent(new Intent(Intent.ACTION_VIEW, null, this, LiveActivity.class)).setShortLabel(getString(R.string.nav_live)).build();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, new Intent(this, ShortcutReceiver.class).setAction(ShortcutReceiver.ACTION), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        ShortcutManagerCompat.requestPinShortcut(this, info, pendingIntent.getIntentSender());
        return true;
    }

    public void change(int position) {
        if (position != 3) returnVodFromEnhance = false;
        setNavigationVisible(true);
        if (position < 2) selectNavigation(position);
        else changeFragment(position);
    }

    public void setNavigationVisible(boolean visible) {
        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mBinding.container.getLayoutParams();
        if (visible) {
            params.addRule(RelativeLayout.ABOVE, R.id.navigation);
            mBinding.navigation.setVisibility(View.VISIBLE);
        } else {
            params.removeRule(RelativeLayout.ABOVE);
            mBinding.navigation.setVisibility(View.GONE);
        }
        mBinding.container.setLayoutParams(params);
        mBinding.getRoot().requestApplyInsets();
    }

    // ★ 侧边版块导航（视频/音乐/阅读）：默认隐藏，侧滑返回一次弹出，再次返回退出。
    private void initSideNav() {
        mBinding.sideNavOverlay.setOnClickListener(v -> hideSideNav());
        mBinding.sideVod.setOnClickListener(v -> onSideSection(0));
        mBinding.sideMusic.setOnClickListener(v -> onSideSection(1));
        mBinding.sideRead.setOnClickListener(v -> onSideSection(2));
        // ★ 应用图标：镂空无底色，慢速向右（顺时针）旋转
        android.view.animation.RotateAnimation rotate = new android.view.animation.RotateAnimation(
                0, 360,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f,
                android.view.animation.Animation.RELATIVE_TO_SELF, 0.5f);
        rotate.setDuration(8000);
        rotate.setRepeatCount(android.view.animation.Animation.INFINITE);
        rotate.setRepeatMode(android.view.animation.Animation.RESTART);
        mBinding.sideAppIcon.startAnimation(rotate);
    }

    private void onSideSection(int section) {
        if (section == 0) {
            // 视频（点播）= 当前 App 主页
            if (currentPosition != 0) change(0);
        } else if (section == 1) {
            // 音乐模块
            startActivity(new Intent(this, MusicActivity.class));
        } else {
            // 阅读 待开发
            Notify.show("「阅读」版块待开发");
        }
        hideSideNav();
    }

    private void showSideNav() {
        if (sideNavVisible) return;
        sideNavVisible = true;
        // 宽 = 屏宽 1/4 + 20dp，高 = 屏高 1/2，距顶 60dp（布局已设）
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mBinding.sideNav.getLayoutParams();
        lp.width = ResUtil.getScreenWidth(this) / 4 + ResUtil.dp2px(20);
        lp.height = ResUtil.getScreenHeight(this) / 2;
        mBinding.sideNav.setLayoutParams(lp);
        sideNavWidth = lp.width;
        mBinding.sideNavOverlay.setVisibility(View.VISIBLE);
        mBinding.sideNav.setVisibility(View.VISIBLE);
        applyFrostedGlass();
        mBinding.sideNav.setTranslationX(sideNavFromLeft ? -sideNavWidth : sideNavWidth); // 从对应侧滑入
        mBinding.sideNav.animate().translationX(0).setDuration(220).start();
    }

    /**
     * 磨砂透明底：截取面板区域的容器画面 → 降采样模糊（跨版本稳定，无需 RenderEffect）→
     * 圆角裁剪 → 叠加半透明深色。失败兜底为半透明深色+圆角。
     */
    private void applyFrostedGlass() {
        try {
            int screenW = ResUtil.getScreenWidth(this);
            int screenH = ResUtil.getScreenHeight(this);
            int w = sideNavWidth;
            int h = screenH / 2;
            int left = sideNavFromLeft ? 0 : screenW - w; // 面板按方向靠左/靠右
            int top = ResUtil.dp2px(90); // 面板距顶 90dp（与布局 marginTop 一致）
            android.graphics.Bitmap src = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(src);
            canvas.translate(-left, -top);
            mBinding.container.draw(canvas);
            // 降采样模糊（磨砂核心）
            android.graphics.Bitmap small = android.graphics.Bitmap.createScaledBitmap(src, Math.max(1, w / 8), Math.max(1, h / 8), true);
            android.graphics.Bitmap blur = android.graphics.Bitmap.createScaledBitmap(small, w, h, true);
            android.graphics.Bitmap result = blur.copy(android.graphics.Bitmap.Config.ARGB_8888, true);
            // 叠加半透明深色（磨砂透明底）
            android.graphics.Canvas c2 = new android.graphics.Canvas(result);
            c2.drawColor(0xB312141A);
            androidx.core.graphics.drawable.RoundedBitmapDrawable rbd = androidx.core.graphics.drawable.RoundedBitmapDrawableFactory.create(getResources(), result);
            rbd.setCornerRadius(ResUtil.dp2px(20));
            rbd.setAntiAlias(true);
            mBinding.sideNav.setBackground(rbd);
        } catch (Throwable t) {
            mBinding.sideNav.setBackgroundResource(R.drawable.bg_side_nav);
        }
    }

    private void hideSideNav() {
        if (!sideNavVisible) return;
        sideNavVisible = false;
        mBinding.sideNav.animate().translationX(sideNavFromLeft ? -sideNavWidth : sideNavWidth).setDuration(200).withEndAction(() -> {
            mBinding.sideNav.setVisibility(View.GONE);
            mBinding.sideNavOverlay.setVisibility(View.GONE);
        }).start();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        switch (event.type()) {
            case VOD:
                RefreshEvent.home();
                break;
            case COMMON:
                setNavigation();
                break;
            case BOOT:
                LiveActivity.start(this);
                break;
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onServerEvent(ServerEvent event) {
        if (event.type() == ServerEvent.Type.PUSH) VideoActivity.push(this, event.text());
        if (event.type() == ServerEvent.Type.SEARCH) SearchActivity.start(this, event.text());
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        returnVodFromEnhance = false;
        setNavigationVisible(true);
        if (item.getItemId() == R.id.setting) return changeFragment(1);
        if (item.getItemId() == R.id.lab) {
            LabActivity.start(this);
            return false;
        }
        if (item.getItemId() == R.id.vod) return changeFragment(0);
        if (item.getItemId() == R.id.live) return openLive();
        if (item.getItemId() == R.id.following) {
            FollowingActivity.start(this, null);
            return false;
        }
        return false;
    }

    private void updateFollowingBadge() {
        if (mBinding == null || mBinding.navigation.getMenu().findItem(R.id.following) == null) return;
        if (!FollowingSettings.isEnabled()) {
            mBinding.navigation.removeBadge(R.id.following);
            return;
        }
        applyFollowingBadge(FollowingPlaybackBridge.cachedUnreadCount());
        FollowingPlaybackBridge.refreshUnreadCountAsync(unread -> {
            if (!isFinishing() && !isDestroyed()) applyFollowingBadge(unread);
        });
    }

    private void applyFollowingBadge(int unread) {
        if (mBinding == null || mBinding.navigation.getMenu().findItem(R.id.following) == null) return;
        if (!FollowingSettings.isEnabled()) {
            mBinding.navigation.removeBadge(R.id.following);
            return;
        }
        if (unread > 0) mBinding.navigation.getOrCreateBadge(R.id.following).setNumber(unread);
        else mBinding.navigation.removeBadge(R.id.following);
    }

    private void selectNavigation(int position) {
        int itemId = position == 0 ? R.id.vod : R.id.setting;
        if (mBinding.navigation.getSelectedItemId() == itemId) changeFragment(position);
        else mBinding.navigation.setSelectedItemId(itemId);
    }

    private void syncNavigationSelection() {
        int itemId = currentPosition == 0 ? R.id.vod : R.id.setting;
        if (mBinding.navigation.getSelectedItemId() == itemId) return;
        mBinding.navigation.setOnItemSelectedListener(null);
        mBinding.navigation.setSelectedItemId(itemId);
        mBinding.navigation.setOnItemSelectedListener(this);
    }

    private boolean changeFragment(int position) {
        boolean changed = mManager.change(position);
        if (changed) currentPosition = position;
        refreshWebHomeChromeLayout();
        return changed;
    }

    private boolean isSettingSubPageVisible() {
        return mManager.isVisible(2) || mManager.isVisible(3) || mManager.isVisible(4) || mManager.isVisible(5) || mManager.isVisible(6) || mManager.isVisible(7) || mManager.isVisible(8) || mManager.isVisible(9);
    }

    private void refreshWebHomeChromeLayout() {
        if (mChrome != null) mChrome.refreshLayout();
    }

    private void resetVodChrome() {
        if (mChrome != null) mChrome.setLegacyToolbar(true);
    }

    public void applyWebHomeDefaultChrome(Site site) {
        if (!Setting.isWebHomeFullscreen()) {
            if (mChrome != null) mChrome.setChrome(normalWebHomeChrome());
            return;
        }
        if (mChrome != null) mChrome.applyDefault(WebHomeChromeStartup.resolve(VodConfig.get().getConfig(), site));
    }

    public void setWebHomeChrome(JsonObject payload) {
        if (!Setting.isWebHomeFullscreen()) {
            if (mChrome != null) mChrome.setChrome(normalWebHomeChrome());
            return;
        }
        if (isStartupChrome(payload)) WebHomeChromeStartup.remember(VodConfig.get().getConfig(), VodConfig.get().getHome(), payload);
        if (mChrome != null) mChrome.setChrome(payload);
    }

    private boolean isStartupChrome(JsonObject payload) {
        try {
            return payload != null && payload.has("startup") && payload.get("startup").getAsBoolean();
        } catch (Throwable e) {
            return false;
        }
    }

    public void restoreWebHomeChrome() {
        if (!Setting.isWebHomeFullscreen()) {
            if (mChrome != null) mChrome.setChrome(normalWebHomeChrome());
            return;
        }
        if (mChrome != null) mChrome.restore();
    }

    public void setWebHomeLegacyToolbar(boolean visible) {
        if (!Setting.isWebHomeFullscreen()) {
            if (mChrome != null) mChrome.setChrome(normalWebHomeChrome());
            return;
        }
        if (mChrome != null) mChrome.setLegacyToolbar(visible);
    }

    public void refreshWebHomeChromeState() {
        onWebHomeChromeChanged(getWebHomeChromeMode());
    }

    private JsonObject normalWebHomeChrome() {
        JsonObject object = new JsonObject();
        object.addProperty("mode", "normal");
        return object;
    }

    public void openVod() {
        resetVodChrome();
        setNavigationVisible(true);
        mBinding.navigation.setSelectedItemId(R.id.vod);
        VodFragment fragment = (VodFragment) mManager.getFragment(0);
        if (fragment != null) fragment.openVodHome();
    }

    public void openEnhanceFromVod() {
        returnVodFromEnhance = true;
        setNavigationVisible(true);
        changeFragment(3);
    }

    public String getWebHomeChromeMode() {
        return mChrome == null ? "normal" : mChrome.getMode();
    }

    public WebHomeViewport getWebHomeViewport() {
        return mChrome == null ? WebHomeViewport.EMPTY : mChrome.getViewport();
    }

    @Override
    public boolean isWebHomeChromeActive() {
        return mManager != null && mManager.isVisible(0);
    }

    @Override
    public void onWebHomeChromeChanged(String mode) {
        if (mManager == null) return;
        VodFragment fragment = (VodFragment) mManager.getFragment(0);
        if (fragment != null) fragment.applyWebHomeChrome(mode);
    }

    @Override
    public void onWebHomeViewportChanged(WebHomeViewport viewport) {
        if (mManager == null) return;
        VodFragment fragment = (VodFragment) mManager.getFragment(0);
        if (fragment != null) fragment.applyWebHomeViewport(viewport);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mChrome != null) mChrome.onConfigurationChanged();
        App.post(this::checkWindowShape, 100);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mChrome != null) mChrome.onWindowFocusChanged(hasFocus);
    }

    private void checkWindowShape() {
        checkWindowShape(MobileWindow.getWidth(this), MobileWindow.getHeight(this));
    }

    private void checkWindowShape(int width, int height) {
        if (width <= 0 || height <= 0) return;
        boolean wide = width > height;
        if (wideWindow != wide) {
            wideWindow = wide;
            RefreshEvent.home();
        }
    }

    /**
     * ★ 哪边边缘侧滑从哪边弹出：监听左右边缘的水平滑动（不依赖 BackEvent，无 API 34 引用）。
     * 仅当侧栏隐藏且在首页可返回时检测；Android 15+ 系统预测性返回手势可能占用边缘，
     * 此时该手势收不到，回退 onBackInvoked（默认左侧）。
     */
    @Override
    public boolean dispatchTouchEvent(android.view.MotionEvent ev) {
        if (!sideNavVisible && mManager.canBack(0) && !PlaybackService.isRunning()) {
            int edge = ResUtil.dp2px(48);
            int sw = ResUtil.getScreenWidth(this);
            switch (ev.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    mSideDownX = ev.getX();
                    mSideDownY = ev.getY();
                    if (mSideDownX <= edge) mSideEdge = -1;
                    else if (mSideDownX >= sw - edge) mSideEdge = 1;
                    else mSideEdge = 0;
                    break;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (mSideEdge != 0 && mSideDownX >= 0) {
                        float dx = ev.getX() - mSideDownX;
                        float dy = ev.getY() - mSideDownY;
                        if (Math.abs(dx) > ResUtil.dp2px(60) && Math.abs(dx) > 2 * Math.abs(dy)) {
                            sideNavFromLeft = (mSideEdge == -1);
                            mSideEdge = 0;
                            mSideDownX = -1;
                            showSideNav();
                            return true;
                        }
                    }
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    mSideEdge = 0;
                    mSideDownX = -1;
                    break;
            }
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onBackInvoked() {
        if (mChrome != null && mChrome.consumeBack()) {
            return;
        } else if (!mBinding.navigation.getMenu().findItem(R.id.vod).isVisible()) {
            setNavigation();
        } else if (returnVodFromEnhance && mManager.isVisible(3)) {
            returnVodFromEnhance = false;
            change(0);
        } else if (isSettingSubPageVisible()) {
            change(1);
        } else if (mManager.isVisible(1)) {
            change(0);
        } else if (mManager.canBack(0)) {
            // ★ 侧边版块导航：第一次返回弹出，再次返回退出（播放中则后台）
            if (!sideNavVisible) {
                showSideNav();
            } else if (PlaybackService.isRunning()) {
                Util.moveToBackground(this);
            } else {
                super.onBackInvoked();
            }
        }
    }

    @Override
    protected void onDestroy() {
        PreviousProcessCrashRecovery.release(this);
        if (mChrome != null) mChrome.destroy();
        LiveConfig.get().clear();
        VodConfig.get().clear("mobile-home-destroy");
        if (AutoBackupPolicy.shouldRun(
                Setting.isAutoBackup(),
                Setting.hasFileAccess(),
                isFinishing(),
                isChangingConfigurations())) {
            AppDatabase.autoBackup();
        }
        OkHttp.get().clear();
        Source.get().exit();
        Server.get().stop();
        super.onDestroy();
    }
}
