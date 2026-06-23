package com.filmstore.tv.api;

import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.model.Announcement;
import com.filmstore.tv.model.ApiResponse;
import com.filmstore.tv.model.ClientConfig;
import com.filmstore.tv.model.LiveSource;
import com.filmstore.tv.model.UpdateInfo;
import com.filmstore.tv.model.VodItem;
import com.filmstore.tv.model.VodSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * API 客户端 - 封装对 FilmStore 后端的所有网络请求
 */
public class ApiClient {

    private OkHttpClient httpClient;
    private Gson gson;

    public ApiClient() {
        // 日志拦截器
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);

        // 超时配置
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .header("User-Agent", "FilmStore-AndroidTV/1.0")
                            .header("Accept", "application/json")
                            .method(original.method(), original.body())
                            .build();
                    return chain.proceed(request);
                })
                .build();

        this.gson = new Gson();
    }

    /**
     * 获取完整服务器 URL
     */
    private String buildUrl(String path) {
        String baseUrl = FilmStoreApp.getServerAddress();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return baseUrl + "/api/client" + path;
    }

    // ==================== 请求执行方法 ====================

    /**
     * 同步 GET 请求
     */
    private <T> T executeSync(String url, Type type) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IOException("请求失败: " + response.code());
            }
            String body = response.body().string();
            return gson.fromJson(body, type);
        }
    }

    /**
     * 异步 GET 请求
     */
    private <T> void executeAsync(String url, Type type, Callback callback) {
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        httpClient.newCall(request).enqueue(callback);
    }

    // ==================== 客户端配置 ====================

    /**
     * 获取全量客户端配置
     */
    public void getConfig(final ApiCallback<ClientConfig> callback) {
        String url = buildUrl("/config");
        Type type = new TypeToken<ApiResponse<ClientConfig>>() {}.getType();

        executeAsync(url, type, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<ClientConfig> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 获取全量配置（同步版本）
     */
    public ClientConfig getConfigSync() throws IOException {
        String url = buildUrl("/config");
        Type type = new TypeToken<ApiResponse<ClientConfig>>() {}.getType();
        ApiResponse<ClientConfig> resp = executeSync(url, type);
        if (resp.isSuccess() && resp.getData() != null) {
            return resp.getData();
        }
        throw new IOException(resp.getMessage() != null ? resp.getMessage() : "获取配置失败");
    }

    // ==================== 更新检测 ====================

    /**
     * 检查更新
     */
    public void checkUpdate(int currentVersionCode, final ApiCallback<UpdateInfo> callback) {
        String url = buildUrl("/check-update?platform=android_tv&version_code=" + currentVersionCode);
        Type type = new TypeToken<ApiResponse<UpdateInfo>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<UpdateInfo> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 检查更新（同步版本）
     */
    public UpdateInfo checkUpdateSync(int currentVersionCode) throws IOException {
        String url = buildUrl("/check-update?platform=android_tv&version_code=" + currentVersionCode);
        Type type = new TypeToken<ApiResponse<UpdateInfo>>() {}.getType();
        ApiResponse<UpdateInfo> resp = executeSync(url, type);
        if (resp.isSuccess()) {
            return resp.getData();
        }
        throw new IOException(resp.getMessage() != null ? resp.getMessage() : "检查更新失败");
    }

    // ==================== 公告 ====================

    /**
     * 获取公告列表
     */
    public void getAnnouncements(final ApiCallback<List<Announcement>> callback) {
        String url = buildUrl("/announcements");
        Type type = new TypeToken<ApiResponse<List<Announcement>>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<List<Announcement>> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    // ==================== 点播源 ====================

    /**
     * 获取点播源列表
     */
    public void getVodSources(final ApiCallback<List<VodSource>> callback) {
        String url = buildUrl("/vod-sources");
        Type type = new TypeToken<ApiResponse<List<VodSource>>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<List<VodSource>> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 点播搜索
     * 调用配置中的点播源的搜索接口
     */
    public void searchVod(long sourceId, String keyword, int page, final ApiCallback<List<VodItem>> callback) {
        String searchUrl = FilmStoreApp.getServerAddress() + "/proxy/vod/search?source_id=" + sourceId
                + "&keyword=" + keyword + "&page=" + page;
        Type type = new TypeToken<ApiResponse<List<VodItem>>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(searchUrl).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<List<VodItem>> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 获取分类列表
     */
    public void getCategoryVod(long sourceId, String categoryId, int page, final ApiCallback<List<VodItem>> callback) {
        String url = FilmStoreApp.getServerAddress() + "/proxy/vod/category?source_id=" + sourceId
                + "&category_id=" + categoryId + "&page=" + page;
        Type type = new TypeToken<ApiResponse<List<VodItem>>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<List<VodItem>> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 获取影片详情和播放地址
     */
    public void getVodDetail(long sourceId, String vodId, final ApiCallback<VodItem> callback) {
        String url = FilmStoreApp.getServerAddress() + "/proxy/vod/detail?source_id=" + sourceId
                + "&vod_id=" + vodId;
        Type type = new TypeToken<ApiResponse<VodItem>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<VodItem> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    /**
     * 获取热门/推荐列表
     */
    public void getHotVod(int page, final ApiCallback<List<VodItem>> callback) {
        String url = FilmStoreApp.getServerAddress() + "/proxy/vod/hot?page=" + page;
        Type type = new TypeToken<ApiResponse<List<VodItem>>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<List<VodItem>> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    // ==================== 直播源 ====================

    /**
     * 获取直播源列表
     */
    public void getLiveSources(final ApiCallback<List<LiveSource>> callback) {
        String url = buildUrl("/live-sources");
        Type type = new TypeToken<ApiResponse<List<LiveSource>>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<List<LiveSource>> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    // ==================== 主题 ====================

    /**
     * 获取主题列表
     */
    public void getThemes(final ApiCallback<List<ClientConfig.ThemeItem>> callback) {
        String url = buildUrl("/themes");
        Type type = new TypeToken<ApiResponse<List<ClientConfig.ThemeItem>>>() {}.getType();

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<List<ClientConfig.ThemeItem>> apiResp = gson.fromJson(body, type);
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    // ==================== 直播代理 ====================

    /**
     * 通过代理获取单个直播源的频道列表
     */
    public void getLiveChannels(long sourceId, final ApiCallback<List<LiveSource.Channel>> callback) {
        String baseUrl = FilmStoreApp.getServerAddress();
        String url = baseUrl + "/proxy/live/" + sourceId + "?format=json";

        httpClient.newCall(new Request.Builder().url(url).get().build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    if (!response.isSuccessful() || response.body() == null) {
                        callback.onError(new IOException("请求失败: " + response.code()));
                        return;
                    }
                    String body = response.body().string();
                    ApiResponse<List<LiveSource.Channel>> apiResp = gson.fromJson(body,
                            new TypeToken<ApiResponse<List<LiveSource.Channel>>>() {}.getType());
                    if (apiResp.isSuccess()) {
                        callback.onSuccess(apiResp.getData());
                    } else {
                        callback.onError(new IOException(apiResp.getMessage()));
                    }
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }

    // ==================== 接口回调 ====================

    /**
     * 通用 API 回调接口
     */
    public interface ApiCallback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }
}
