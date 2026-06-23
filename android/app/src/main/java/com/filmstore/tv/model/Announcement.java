package com.filmstore.tv.model;

import com.google.gson.annotations.SerializedName;

/**
 * 公告模型 - 对齐 FilmStore 后端返回格式
 */
public class Announcement {

    @SerializedName("id")
    private long id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("type")
    private String type;            // text/rich/link/image

    @SerializedName("link_url")
    private String linkUrl;

    @SerializedName("image_url")
    private String imageUrl;

    @SerializedName("is_pinned")
    private int isPinned;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isPinned() { return isPinned != 0; }
    public void setPinned(boolean pinned) { isPinned = pinned ? 1 : 0; }

    /**
     * 是否有效（后端已过滤过期公告，客户端无需再次检查）
     */
    public boolean isValid() {
        return title != null && !title.isEmpty();
    }

    /**
     * 是否为弹窗公告（后端 type=link 且无链接时视为弹窗）
     */
    public boolean isPopup() {
        return "text".equals(type) || "rich".equals(type);
    }

    /**
     * 是否为横幅公告
     */
    public boolean isBanner() {
        return "image".equals(type) || "link".equals(type);
    }

    /**
     * 是否为跑马灯
     */
    public boolean isMarquee() {
        return false; // 后端暂不支持跑马灯类型
    }
}
