package com.filmstore.tv.model;

import com.google.gson.annotations.SerializedName;

/**
 * 直播频道模型
 */
public class LiveChannel {

    @SerializedName("name")
    private String name;

    @SerializedName("url")
    private String url;

    @SerializedName("logo")
    private String logo;

    @SerializedName("group")
    private String group;

    @SerializedName("sourceId")
    private long sourceId;

    @SerializedName("sourceName")
    private String sourceName;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getLogo() { return logo; }
    public void setLogo(String logo) { this.logo = logo; }

    public String getGroup() { return group; }
    public void setGroup(String group) { this.group = group; }

    public long getSourceId() { return sourceId; }
    public void setSourceId(long sourceId) { this.sourceId = sourceId; }

    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
}
