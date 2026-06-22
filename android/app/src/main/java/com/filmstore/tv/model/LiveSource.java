package com.filmstore.tv.model;

import com.google.gson.annotations.SerializedName;

/**
 * 直播源模型 - 对齐 FilmStore 后端返回格式
 */
public class LiveSource {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type;            // m3u/txt/json

    @SerializedName("url")
    private String url;

    @SerializedName("group_name")
    private String groupName;

    @SerializedName("sort_order")
    private int sortOrder;

    @SerializedName("is_active")
    private boolean isActive;

    @SerializedName("is_default")
    private boolean isDefault;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean aDefault) { isDefault = aDefault; }

    /**
     * 直播频道模型（内部类，用于 LiveActivity）
     */
    public static class Channel {
        private String name;
        private String url;
        private String logo;
        private String group;

        public Channel() {}
        public Channel(String name, String url) {
            this.name = name;
            this.url = url;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getLogo() { return logo; }
        public void setLogo(String logo) { this.logo = logo; }

        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }
    }
}
