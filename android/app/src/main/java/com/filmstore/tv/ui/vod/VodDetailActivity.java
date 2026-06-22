package com.filmstore.tv.ui.vod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.model.VodItem;
import com.filmstore.tv.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 点播详情页面 - 显示影片详情和播放源
 */
public class VodDetailActivity extends Activity {

    private static final String TAG = "VodDetailActivity";

    private ImageView posterView;
    private TextView titleView;
    private TextView scoreView;
    private TextView yearView;
    private TextView areaView;
    private TextView actorView;
    private TextView directorView;
    private TextView contentView;
    private TextView remarksView;
    private Button playButton;
    private ProgressBar loadingIndicator;
    private View errorLayout;

    // 播放源列表
    private View sourceListContainer;

    private String sourceId;
    private String vodId;
    private String vodName;
    private String vodPic;
    private String vodRemarks;

    private VodItem detail;
    private List<String> playUrls = new ArrayList<>();
    private List<String> playNames = new ArrayList<>();

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        // 获取传入参数
        Intent intent = getIntent();
        sourceId = intent.getStringExtra("source_id");
        vodId = intent.getStringExtra("vod_id");
        vodName = intent.getStringExtra("vod_name");
        vodPic = intent.getStringExtra("vod_pic");
        vodRemarks = intent.getStringExtra("vod_remarks");

        if (sourceId == null) sourceId = "";
        if (vodId == null) vodId = "";

        initViews();
        loadDetail();
    }

    private void initViews() {
        // 这里简单使用 TextView 作为布局替代
        // 实际应该创建专门的 detail layout
        View rootView = findViewById(android.R.id.content);
    }

    /**
     * 加载详情
     */
    private void loadDetail() {
        FilmStoreApp.getInstance().getApiClient().getVodDetail(sourceId, vodId,
                new ApiClient.ApiCallback<VodItem>() {
                    @Override
                    public void onSuccess(VodItem result) {
                        detail = result;
                        mainHandler.post(() -> displayDetail(result));
                    }

                    @Override
                    public void onError(Exception e) {
                        mainHandler.post(() -> {
                            // 显示错误
                            showError(e.getMessage());
                        });
                    }
                });
    }

    /**
     * 展示详情
     */
    private void displayDetail(VodItem item) {
        // 解析播放地址
        parsePlayUrls(item);
    }

    /**
     * 解析播放地址
     * 格式: 播放源名称$$播放地址###播放源名称2$$播放地址2
     */
    private void parsePlayUrls(VodItem item) {
        playUrls.clear();
        playNames.clear();

        String playUrl = item.getVodPlayUrl();
        List<String> playFrom = item.getVodPlayFrom();

        if (playUrl != null && !playUrl.isEmpty()) {
            // 解析多源
            String[] sources = playUrl.split("###");
            for (int i = 0; i < sources.length; i++) {
                String[] parts = sources[i].split("\\$\\$", 2);
                if (parts.length == 2) {
                    String name = (playFrom != null && i < playFrom.size()) ? playFrom.get(i) : "源" + (i + 1);
                    playNames.add(name);
                    playUrls.add(parts[1]);
                } else {
                    playNames.add("源" + (i + 1));
                    playUrls.add(sources[i]);
                }
            }
        }

        // 默认播放第一个
        playSelected(0);
    }

    /**
     * 播放选中的源
     */
    private void playSelected(int index) {
        if (index < 0 || index >= playUrls.size()) {
            return;
        }

        String url = playUrls.get(index);
        String name = playNames.get(index);

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("play_url", url);
        intent.putExtra("play_name", name);
        intent.putExtra("vod_name", detail != null ? detail.getVodName() : vodName);
        startActivity(intent);
    }

    private void showError(String message) {
        // 显示错误提示
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
