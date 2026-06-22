package com.filmstore.tv.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * 全量客户端配置 - 字段对齐 FilmStore 后端返回格式
 */
public class ClientConfig {

    @SerializedName("app")
    private AppConfig app;

    @SerializedName("announcements")
    private List<Announcement> announcements;

    @SerializedName("vodSources")
    private List<VodSource> vodSources;

    @SerializedName("liveSources")
    private List<LiveSource> liveSources;

    @SerializedName("themes")
    private ThemeConfig themes;

    public AppConfig getApp() { return app; }
    public void setApp(AppConfig app) { this.app = app; }

    public List<Announcement> getAnnouncements() { return announcements; }
    public void setAnnouncements(List<Announcement> announcements) { this.announcements = announcements; }

    public List<VodSource> getVodSources() { return vodSources; }
    public void setVodSources(List<VodSource> vodSources) { this.vodSources = vodSources; }

    public List<LiveSource> getLiveSources() { return liveSources; }
    public void setLiveSources(List<LiveSource> liveSources) { this.liveSources = liveSources; }

    public ThemeConfig getThemes() { return themes; }
    public void setThemes(ThemeConfig themes) { this.themes = themes; }

    // ==================== APP 基础配置 ====================
    public static class AppConfig {
        @SerializedName("appName")
        private String appName;

        @SerializedName("appNotice")
        private String appNotice;

        @SerializedName("serverAddr")
        private String serverAddr;

        public String getAppName() { return appName; }
        public void setAppName(String appName) { this.appName = appName; }

        public String getAppNotice() { return appNotice; }
        public void setAppNotice(String appNotice) { this.appNotice = appNotice; }

        public String getServerAddr() { return serverAddr; }
        public void setServerAddr(String serverAddr) { this.serverAddr = serverAddr; }
    }

    // ==================== 主题配置 ====================
    public static class ThemeConfig {
        @SerializedName("list")
        private List<ThemeItem> list;

        @SerializedName("default")
        private ThemeItem defaultTheme;

        public List<ThemeItem> getList() { return list; }
        public void setList(List<ThemeItem> list) { this.list = list; }

        public ThemeItem getDefaultTheme() { return defaultTheme; }
        public void setDefaultTheme(ThemeItem defaultTheme) { this.defaultTheme = defaultTheme; }
    }

    // ==================== 主题条目 ====================
    public static class ThemeItem {
        @SerializedName("name")
        private String name;

        @SerializedName("title")
        private String title;

        @SerializedName("config")
        private ThemeColors config;

        @SerializedName("is_default")
        private boolean isDefault;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public ThemeColors getConfig() { return config; }
        public void setConfig(ThemeColors config) { this.config = config; }

        public boolean isDefault() { return isDefault; }
        public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    }

    // ==================== 主题颜色 - 对齐后端字段 ====================
    public static class ThemeColors {
        @SerializedName("primaryColor")
        private String primaryColor;

        @SerializedName("backgroundColor")
        private String backgroundColor;

        @SerializedName("surfaceColor")
        private String surfaceColor;

        @SerializedName("textColor")
        private String textColor;

        @SerializedName("accentColor")
        private String accentColor;

        @SerializedName("logoUrl")
        private String logoUrl;

        @SerializedName("appName")
        private String appName;

        @SerializedName("startupImage")
        private String startupImage;

        @SerializedName("tabBarStyle")
        private String tabBarStyle;

        @SerializedName("borderRadius")
        private int borderRadius;

        @SerializedName("darkMode")
        private boolean darkMode;

        public String getPrimaryColor() { return primaryColor; }
        public void setPrimaryColor(String primaryColor) { this.primaryColor = primaryColor; }

        public String getBackgroundColor() { return backgroundColor; }
        public void setBackgroundColor(String backgroundColor) { this.backgroundColor = backgroundColor; }

        public String getSurfaceColor() { return surfaceColor; }
        public void setSurfaceColor(String surfaceColor) { this.surfaceColor = surfaceColor; }

        public String getTextColor() { return textColor; }
        public void setTextColor(String textColor) { this.textColor = textColor; }

        public String getAccentColor() { return accentColor; }
        public void setAccentColor(String accentColor) { this.accentColor = accentColor; }

        public String getLogoUrl() { return logoUrl; }
        public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

        public String getAppName() { return appName; }
        public void setAppName(String appName) { this.appName = appName; }

        public String getStartupImage() { return startupImage; }
        public void setStartupImage(String startupImage) { this.startupImage = startupImage; }

        public String getTabBarStyle() { return tabBarStyle; }
        public void setTabBarStyle(String tabBarStyle) { this.tabBarStyle = tabBarStyle; }

        public int getBorderRadius() { return borderRadius; }
        public void setBorderRadius(int borderRadius) { this.borderRadius = borderRadius; }

        public boolean isDarkMode() { return darkMode; }
        public void setDarkMode(boolean darkMode) { this.darkMode = darkMode; }
    }
}
