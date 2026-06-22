package com.filmstore.tv.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.filmstore.tv.R;
import com.filmstore.tv.model.ClientConfig;

/**
 * 主题管理器 - 动态换肤
 */
public class ThemeManager {

    private Context context;
    private PreferencesManager preferences;
    private ClientConfig.ThemeColors currentColors;

    // 当前主题颜色
    private int primaryColor;
    private int primaryDarkColor;
    private int accentColor;
    private int backgroundColor;
    private int cardBackgroundColor;
    private int textPrimaryColor;
    private int textSecondaryColor;

    public ThemeManager(Context context) {
        this.context = context;
        this.preferences = PreferencesManager.getInstance(context);
        loadDefaultColors();
    }

    /**
     * 加载默认颜色
     */
    private void loadDefaultColors() {
        Resources res = context.getResources();
        primaryColor = ContextCompat.getColor(context, R.color.colorPrimary);
        primaryDarkColor = ContextCompat.getColor(context, R.color.colorPrimaryDark);
        accentColor = ContextCompat.getColor(context, R.color.colorAccent);
        backgroundColor = ContextCompat.getColor(context, R.color.bgPrimary);
        cardBackgroundColor = ContextCompat.getColor(context, R.color.bgCard);
        textPrimaryColor = ContextCompat.getColor(context, R.color.textPrimary);
        textSecondaryColor = ContextCompat.getColor(context, R.color.textSecondary);
    }

    /**
     * 应用主题
     */
    public void applyTheme(ClientConfig.ThemeColors colors) {
        if (colors == null) return;

        this.currentColors = colors;

        if (colors.getPrimaryColor() != null) {
            primaryColor = parseColor(colors.getPrimaryColor(), primaryColor);
        }
        if (colors.getAccentColor() != null) {
            accentColor = parseColor(colors.getAccentColor(), accentColor);
        }
        if (colors.getBackgroundColor() != null) {
            backgroundColor = parseColor(colors.getBackgroundColor(), backgroundColor);
        }
        if (colors.getSurfaceColor() != null) {
            cardBackgroundColor = parseColor(colors.getSurfaceColor(), cardBackgroundColor);
        }
        if (colors.getTextColor() != null) {
            textPrimaryColor = parseColor(colors.getTextColor(), textPrimaryColor);
            textSecondaryColor = textPrimaryColor;
        }
    }

    /**
     * 从服务器配置中应用主题（通过名称查找）
     */
    public void applyThemeByName(String themeName, ClientConfig.ThemeConfig themeConfig) {
        if (themeConfig == null || themeConfig.getList() == null) return;

        for (ClientConfig.ThemeItem item : themeConfig.getList()) {
            if (item.getName().equals(themeName) && item.getConfig() != null) {
                applyTheme(item.getConfig());
                preferences.setSelectedTheme(themeName);
                return;
            }
        }

        // 如果没找到，尝试应用默认主题
        if (themeConfig.getDefaultTheme() != null && themeConfig.getDefaultTheme().getConfig() != null) {
            applyTheme(themeConfig.getDefaultTheme().getConfig());
        }
    }

    /**
     * 重置为默认颜色
     */
    public void resetToDefault() {
        loadDefaultColors();
        currentColors = null;
        preferences.setSelectedTheme("default");
    }

    /**
     * 解析颜色字符串，如果失败返回默认值
     */
    private int parseColor(String colorStr, int defaultColor) {
        try {
            if (colorStr != null && colorStr.startsWith("#")) {
                return android.graphics.Color.parseColor(colorStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultColor;
    }

    // ==================== Getter 方法 ====================

    public int getPrimaryColor() {
        return primaryColor;
    }

    public int getPrimaryDarkColor() {
        return primaryDarkColor;
    }

    public int getAccentColor() {
        return accentColor;
    }

    public int getBackgroundColor() {
        return backgroundColor;
    }

    public int getCardBackgroundColor() {
        return cardBackgroundColor;
    }

    public int getTextPrimaryColor() {
        return textPrimaryColor;
    }

    public int getTextSecondaryColor() {
        return textSecondaryColor;
    }

    public ClientConfig.ThemeColors getCurrentColors() {
        return currentColors;
    }
}
