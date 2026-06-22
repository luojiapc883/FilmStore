package com.filmstore.tv.ui.home;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import androidx.leanback.app.BrowseSupportFragment;

import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.ui.live.LiveActivity;
import com.filmstore.tv.ui.settings.SettingsActivity;
import com.filmstore.tv.ui.vod.VodSearchActivity;
import com.filmstore.tv.util.PreferencesManager;
import com.filmstore.tv.model.UpdateInfo;
import com.filmstore.tv.util.UpdateChecker;

/**
 * 主页面 - 使用 Leanback BrowseFragment 实现导航
 */
public class MainActivity extends FragmentActivity {

    private static final String TAG = "MainActivity";

    private BrowseSupportFragment browseFragment;
    private HomeFragment homeFragment;
    private VodFragment vodFragment;
    private LiveFragment liveFragment;

    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 检查首次启动
        checkFirstLaunch();

        // 检查更新
        checkUpdateOnStart();

        // 初始化首页
        if (savedInstanceState == null) {
            homeFragment = new HomeFragment();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.browse_grid, homeFragment)
                    .commit();
        }
    }

    /**
     * 首次启动检查 - 显示设置引导
     */
    private void checkFirstLaunch() {
        PreferencesManager prefs = PreferencesManager.getInstance(this);
        if (prefs.isFirstLaunch()) {
            Toast.makeText(this, "首次使用请先在设置中配置服务器地址", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * 启动时检查更新
     */
    private void checkUpdateOnStart() {
        UpdateChecker updateChecker = new UpdateChecker(this);
        updateChecker.checkUpdate(new UpdateChecker.UpdateCallback() {
            @Override
            public void onUpdateAvailable(UpdateInfo info) {
                updateChecker.showUpdateDialog(info);
            }

            @Override
            public void onNoUpdate() {
                // 静默，无更新
            }

            @Override
            public void onError(String message) {
                // 静默，更新检查失败不影响使用
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 处理遥控器按键
        switch (keyCode) {
            case KeyEvent.KEYCODE_MENU:
            case KeyEvent.KEYCODE_SETTINGS:
                openSettings();
                return true;

            case KeyEvent.KEYCODE_SEARCH:
                openSearch();
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                // TODO: 切换左侧导航分类
                break;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                break;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 打开设置页
     */
    public void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * 打开搜索
     */
    public void openSearch() {
        Intent intent = new Intent(this, VodSearchActivity.class);
        startActivity(intent);
    }

    /**
     * 按返回键退出
     */
    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finishAffinity();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, R.string.exit_app, Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> doubleBackToExitPressedOnce = false, 2000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isFinishing()) {
            FilmStoreApp.getInstance().shutdown();
        }
    }
}
