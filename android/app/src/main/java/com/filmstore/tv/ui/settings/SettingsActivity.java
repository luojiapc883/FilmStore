package com.filmstore.tv.ui.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filmstore.tv.BuildConfig;
import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.model.ClientConfig;
import com.filmstore.tv.ui.home.MainActivity;
import com.filmstore.tv.util.PreferencesManager;
import com.filmstore.tv.model.UpdateInfo;
import com.filmstore.tv.util.UpdateChecker;

import java.util.List;

/**
 * 设置页面 - 服务器配置、主题切换、版本信息
 */
public class SettingsActivity extends Activity {

    private static final String TAG = "SettingsActivity";

    private EditText serverAddressInput;
    private Button saveButton;
    private Button checkUpdateButton;
    private TextView versionText;
    private RecyclerView themeRecyclerView;
    private ThemeAdapter themeAdapter;

    private PreferencesManager preferences;
    private List<ClientConfig.ThemeItem> themeList;
    private String selectedThemeName;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mainHandler = new Handler(Looper.getMainLooper());
        preferences = PreferencesManager.getInstance(this);

        initViews();
        loadCurrentConfig();
        loadThemeList();
    }

    private void initViews() {
        serverAddressInput = findViewById(R.id.edit_server_address);
        saveButton = findViewById(R.id.btn_save);
        checkUpdateButton = findViewById(R.id.btn_check_update);
        versionText = findViewById(R.id.tv_version);

        // 显示当前版本
        if (versionText != null) {
            versionText.setText("v" + BuildConfig.VERSION_NAME);
        }

        // 服务器地址输入
        if (serverAddressInput != null) {
            String currentAddr = preferences.getServerAddress();
            if (!currentAddr.equals(getString(R.string.settings_server_default))) {
                serverAddressInput.setText(currentAddr);
            } else {
                serverAddressInput.setHint(R.string.settings_server_hint);
            }
        }

        // 保存按钮
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> saveSettings());
        }

        // 检查更新按钮
        if (checkUpdateButton != null) {
            checkUpdateButton.setOnClickListener(v -> checkUpdate());
        }

        // 主题列表
        themeRecyclerView = findViewById(R.id.theme_list);
        if (themeRecyclerView != null) {
            themeRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            themeAdapter = new ThemeAdapter(this, new java.util.ArrayList<>());
            themeAdapter.setOnThemeClickListener((name, title) -> {
                selectedThemeName = name;
                // 高亮显示选中
            });
            themeRecyclerView.setAdapter(themeAdapter);
        }
    }

    /**
     * 加载当前配置
     */
    private void loadCurrentConfig() {
        selectedThemeName = preferences.getSelectedTheme();
    }

    /**
     * 从服务器加载主题列表
     */
    private void loadThemeList() {
        FilmStoreApp.getInstance().getApiClient().getConfig(new ApiClient.ApiCallback<ClientConfig>() {
            @Override
            public void onSuccess(ClientConfig result) {
                mainHandler.post(() -> {
                    if (result.getThemes() != null && result.getThemes().getList() != null) {
                        themeList = result.getThemes().getList();
                        if (themeAdapter != null) {
                            themeAdapter.updateList(themeList);
                        }

                        // 标记当前选中的主题
                        selectedThemeName = preferences.getSelectedTheme();
                        themeAdapter.setSelectedTheme(selectedThemeName);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                // 静默
            }
        });
    }

    /**
     * 保存设置
     */
    private void saveSettings() {
        if (serverAddressInput == null) return;

        String address = serverAddressInput.getText().toString().trim();
        if (address.isEmpty()) {
            Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // 验证地址格式
        if (!address.startsWith("http://") && !address.startsWith("https://")) {
            address = "http://" + address;
        }

        // 保存服务器地址
        preferences.setServerAddress(address);
        FilmStoreApp.setServerAddress(address);

        // 保存主题选择
        if (selectedThemeName != null) {
            preferences.setSelectedTheme(selectedThemeName);
        }

        Toast.makeText(this, R.string.settings_save_success, Toast.LENGTH_SHORT).show();

        // 保存成功后，应用主题
        if (themeList != null && selectedThemeName != null) {
            for (ClientConfig.ThemeItem item : themeList) {
                if (item.getName().equals(selectedThemeName) && item.getConfig() != null) {
                    FilmStoreApp.getInstance().getThemeManager().applyTheme(item.getConfig());
                    break;
                }
            }
        }
    }

    /**
     * 检查更新
     */
    private void checkUpdate() {
        Toast.makeText(this, R.string.settings_checking, Toast.LENGTH_SHORT).show();

        UpdateChecker updateChecker = new UpdateChecker(this);
        updateChecker.checkUpdate(new UpdateChecker.UpdateCallback() {
            @Override
            public void onUpdateAvailable(UpdateInfo info) {
                runOnUiThread(() -> updateChecker.showUpdateDialog(info));
            }

            @Override
            public void onNoUpdate() {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, R.string.update_no_update, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "检查更新失败: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
