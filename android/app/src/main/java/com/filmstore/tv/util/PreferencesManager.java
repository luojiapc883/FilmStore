package com.filmstore.tv.util;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * 偏好设置管理器
 */
public class PreferencesManager {

    private static final String PREF_NAME = "filmstore_config";
    private static final String KEY_SERVER_ADDRESS = "server_address";
    private static final String KEY_SELECTED_THEME = "selected_theme";
    private static final String KEY_LAST_VERSION_CODE = "last_version_code";
    private static final String KEY_FIRST_LAUNCH = "first_launch";
    private static final String KEY_PLAY_HISTORY = "play_history";

    private static final String DEFAULT_SERVER = "http://192.168.31.161:3000";
    private static final int MAX_HISTORY_ITEMS = 50;

    private SharedPreferences prefs;
    private static PreferencesManager instance;

    private PreferencesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context);
        }
        return instance;
    }

    /**
     * 获取服务器地址
     */
    public String getServerAddress() {
        String addr = prefs.getString(KEY_SERVER_ADDRESS, DEFAULT_SERVER);
        if (addr != null && addr.endsWith("/")) {
            addr = addr.substring(0, addr.length() - 1);
        }
        return addr;
    }

    /**
     * 保存服务器地址
     */
    public void setServerAddress(String address) {
        if (address != null && address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        prefs.edit().putString(KEY_SERVER_ADDRESS, address).apply();
    }

    /**
     * 获取选中主题
     */
    public String getSelectedTheme() {
        return prefs.getString(KEY_SELECTED_THEME, "default");
    }

    /**
     * 保存选中主题
     */
    public void setSelectedTheme(String themeName) {
        prefs.edit().putString(KEY_SELECTED_THEME, themeName).apply();
    }

    /**
     * 获取记录的上次版本号
     */
    public int getLastVersionCode() {
        return prefs.getInt(KEY_LAST_VERSION_CODE, 0);
    }

    /**
     * 更新记录的版本号
     */
    public void setLastVersionCode(int versionCode) {
        prefs.edit().putInt(KEY_LAST_VERSION_CODE, versionCode).apply();
    }

    /**
     * 是否首次启动
     */
    public boolean isFirstLaunch() {
        boolean first = prefs.getBoolean(KEY_FIRST_LAUNCH, true);
        if (first) {
            prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply();
        }
        return first;
    }

    /**
     * 清除所有配置
     */
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
