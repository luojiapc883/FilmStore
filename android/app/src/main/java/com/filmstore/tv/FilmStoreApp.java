package com.filmstore.tv;

import android.app.Application;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.util.ThemeManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FilmStore Android TV Application
 */
public class FilmStoreApp extends Application {

    private static FilmStoreApp instance;
    private static Context appContext;

    // 后台线程池
    private ExecutorService executorService;

    // 主线程 Handler
    private Handler mainHandler;

    // API 客户端
    private ApiClient apiClient;

    // 主题管理器
    private ThemeManager themeManager;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        appContext = getApplicationContext();

        // 初始化线程池
        executorService = Executors.newFixedThreadPool(4);
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化 API 客户端
        apiClient = new ApiClient();

        // 初始化主题管理器
        themeManager = new ThemeManager(this);

        // 启动时检查服务器配置
        initServerConfig();
    }

    private void initServerConfig() {
        // 检查 SharedPreferences 中是否有保存的服务器地址
        String savedServer = getSharedPreferences("filmstore_config", MODE_PRIVATE)
                .getString("server_address", null);
        if (savedServer == null) {
            // 使用默认地址
            getSharedPreferences("filmstore_config", MODE_PRIVATE)
                    .edit()
                    .putString("server_address", getString(R.string.settings_server_default))
                    .apply();
        }
    }

    public static FilmStoreApp getInstance() {
        return instance;
    }

    public static Context getAppContext() {
        return appContext;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public Handler getMainHandler() {
        return mainHandler;
    }

    public ApiClient getApiClient() {
        return apiClient;
    }

    public ThemeManager getThemeManager() {
        return themeManager;
    }

    /**
     * 获取已保存的服务器地址
     */
    public static String getServerAddress() {
        String addr = appContext.getSharedPreferences("filmstore_config", MODE_PRIVATE)
                .getString("server_address", null);
        if (addr == null) {
            addr = appContext.getString(R.string.settings_server_default);
        }
        // 确保不以斜杠结尾
        if (addr.endsWith("/")) {
            addr = addr.substring(0, addr.length() - 1);
        }
        Log.d("FilmStoreApp", "Server address: " + addr);
        return addr;
    }

    /**
     * 保存服务器地址
     */
    public static void setServerAddress(String address) {
        if (address != null && address.endsWith("/")) {
            address = address.substring(0, address.length() - 1);
        }
        appContext.getSharedPreferences("filmstore_config", MODE_PRIVATE)
                .edit()
                .putString("server_address", address)
                .apply();
    }

    /**
     * 释放资源 - 低内存时调用
     */
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        Glide.get(this).clearMemory();
    }

    /**
     * 应用退出时释放资源
     */
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
