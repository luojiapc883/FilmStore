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
}
