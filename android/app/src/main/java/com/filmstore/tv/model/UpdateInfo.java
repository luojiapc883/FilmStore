package com.filmstore.tv.model;

import com.google.gson.annotations.SerializedName;

/**
 * 更新信息
 */
public class UpdateInfo {

    @SerializedName("hasUpdate")
    private boolean hasUpdate;

    @SerializedName("latestVersion")
    private VersionInfo latestVersion;

    public boolean hasUpdate() {
        return hasUpdate;
    }

    public void setHasUpdate(boolean hasUpdate) {
        this.hasUpdate = hasUpdate;
    }

    public VersionInfo getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(VersionInfo latestVersion) {
        this.latestVersion = latestVersion;
    }

    /**
     * 版本信息
     */
    public static class VersionInfo {
        @SerializedName("id")
        private long id;

        @SerializedName("version_name")
        private String versionName;

        @SerializedName("version_code")
        private int versionCode;

        @SerializedName("platform")
        private String platform;

        @SerializedName("apk_url")
        private String apkUrl;

        @SerializedName("apk_size")
        private long apkSize;

        @SerializedName("update_log")
        private String updateLog;

        @SerializedName("force_update")
        private boolean forceUpdate;

        @SerializedName("created_at")
        private String createdAt;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getVersionName() {
            return versionName;
        }

        public void setVersionName(String versionName) {
            this.versionName = versionName;
        }

        public int getVersionCode() {
            return versionCode;
        }

        public void setVersionCode(int versionCode) {
            this.versionCode = versionCode;
        }

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getApkUrl() {
            return apkUrl;
        }

        public void setApkUrl(String apkUrl) {
            this.apkUrl = apkUrl;
        }

        public long getApkSize() {
            return apkSize;
        }

        public void setApkSize(long apkSize) {
            this.apkSize = apkSize;
        }

        public String getUpdateLog() {
            return updateLog;
        }

        public void setUpdateLog(String updateLog) {
            this.updateLog = updateLog;
        }

        public boolean isForceUpdate() {
            return forceUpdate;
        }

        public void setForceUpdate(boolean forceUpdate) {
            this.forceUpdate = forceUpdate;
        }

        public String getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(String createdAt) {
            this.createdAt = createdAt;
        }

        /**
         * 获取完整 APK 下载 URL
         */
        public String getFullApkUrl() {
            if (apkUrl == null || apkUrl.isEmpty()) {
                return null;
            }
            if (apkUrl.startsWith("http://") || apkUrl.startsWith("https://")) {
                return apkUrl;
            }
            return com.filmstore.tv.FilmStoreApp.getServerAddress() + "/" + apkUrl;
        }

        /**
         * 格式化文件大小
         */
        public String getFormattedSize() {
            if (apkSize <= 0) return "未知";
            if (apkSize < 1024 * 1024) {
                return String.format("%.1f KB", apkSize / 1024.0);
            }
            return String.format("%.1f MB", apkSize / (1024.0 * 1024.0));
        }
    }
}
