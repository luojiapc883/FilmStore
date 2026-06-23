package com.filmstore.tv.model;

import com.google.gson.annotations.SerializedName;

/**
 * 点播源模型 - 对齐 FilmStore 后端返回格式
 */
public class VodSource {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("type")
    private String type;            // spider/json/api

    @SerializedName("url")
    private String url;

    @SerializedName("spider_key")
    private String spiderKey;

    @SerializedName("group_name")
    private String groupName;

    @SerializedName("sort_order")
    private int sortOrder;

    @SerializedName("is_active")
    private int isActive;

    @SerializedName("is_default")
    private int isDefault;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getSpiderKey() { return spiderKey; }
    public void setSpiderKey(String spiderKey) { this.spiderKey = spiderKey; }

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return isActive != 0; }
    public void setActive(boolean active) { isActive = active ? 1 : 0; }

    public boolean isDefault() { return isDefault != 0; }
    public void setDefault(boolean aDefault) { isDefault = aDefault ? 1 : 0; }
}
