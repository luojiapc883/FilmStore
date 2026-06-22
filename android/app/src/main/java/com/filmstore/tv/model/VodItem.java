package com.filmstore.tv.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * 点播条目 - 搜索结果/分类列表中的单个影片
 */
public class VodItem {

    @SerializedName("vod_id")
    private String vodId;

    @SerializedName("vod_name")
    private String vodName;

    @SerializedName("vod_pic")
    private String vodPic;

    @SerializedName("vod_actor")
    private String vodActor;

    @SerializedName("vod_director")
    private String vodDirector;

    @SerializedName("vod_content")
    private String vodContent;

    @SerializedName("vod_remarks")
    private String vodRemarks;

    @SerializedName("vod_year")
    private String vodYear;

    @SerializedName("vod_area")
    private String vodArea;

    @SerializedName("type_name")
    private String typeName;

    @SerializedName("vod_score")
    private String vodScore;

    @SerializedName("vod_play_from")
    private List<String> vodPlayFrom;

    @SerializedName("vod_play_url")
    private String vodPlayUrl;

    @SerializedName("vod_tag")
    private List<String> vodTag;

    @SerializedName("vod_time")
    private String vodTime;

    // 本地字段
    private boolean isHistory;
    private long lastPlayTime;

    public String getVodId() {
        return vodId;
    }

    public void setVodId(String vodId) {
        this.vodId = vodId;
    }

    public String getVodName() {
        return vodName;
    }

    public void setVodName(String vodName) {
        this.vodName = vodName;
    }

    public String getVodPic() {
        return vodPic;
    }

    public void setVodPic(String vodPic) {
        this.vodPic = vodPic;
    }

    public String getVodActor() {
        return vodActor;
    }

    public void setVodActor(String vodActor) {
        this.vodActor = vodActor;
    }

    public String getVodDirector() {
        return vodDirector;
    }

    public void setVodDirector(String vodDirector) {
        this.vodDirector = vodDirector;
    }

    public String getVodContent() {
        return vodContent;
    }

    public void setVodContent(String vodContent) {
        this.vodContent = vodContent;
    }

    public String getVodRemarks() {
        return vodRemarks;
    }

    public void setVodRemarks(String vodRemarks) {
        this.vodRemarks = vodRemarks;
    }

    public String getVodYear() {
        return vodYear;
    }

    public void setVodYear(String vodYear) {
        this.vodYear = vodYear;
    }

    public String getVodArea() {
        return vodArea;
    }

    public void setVodArea(String vodArea) {
        this.vodArea = vodArea;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getVodScore() {
        return vodScore;
    }

    public void setVodScore(String vodScore) {
        this.vodScore = vodScore;
    }

    public List<String> getVodPlayFrom() {
        return vodPlayFrom;
    }

    public void setVodPlayFrom(List<String> vodPlayFrom) {
        this.vodPlayFrom = vodPlayFrom;
    }

    public String getVodPlayUrl() {
        return vodPlayUrl;
    }

    public void setVodPlayUrl(String vodPlayUrl) {
        this.vodPlayUrl = vodPlayUrl;
    }

    public List<String> getVodTag() {
        return vodTag;
    }

    public void setVodTag(List<String> vodTag) {
        this.vodTag = vodTag;
    }

    public String getVodTime() {
        return vodTime;
    }

    public void setVodTime(String vodTime) {
        this.vodTime = vodTime;
    }

    public boolean isHistory() {
        return isHistory;
    }

    public void setHistory(boolean history) {
        isHistory = history;
    }

    public long getLastPlayTime() {
        return lastPlayTime;
    }

    public void setLastPlayTime(long lastPlayTime) {
        this.lastPlayTime = lastPlayTime;
    }

    /**
     * 获取完整海报 URL（如果是相对路径则拼接服务器地址）
     */
    public String getFullPosterUrl() {
        if (vodPic == null || vodPic.isEmpty()) {
            return null;
        }
        if (vodPic.startsWith("http://") || vodPic.startsWith("https://")) {
            return vodPic;
        }
        // 相对路径
        return com.filmstore.tv.FilmStoreApp.getServerAddress() + "/" + vodPic;
    }
}
