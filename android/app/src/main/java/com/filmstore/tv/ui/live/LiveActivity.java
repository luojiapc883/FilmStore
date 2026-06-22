package com.filmstore.tv.ui.live;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.model.LiveSource;
import com.filmstore.tv.model.LiveChannel;
import com.filmstore.tv.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 直播页面 - 分组频道列表 + 播放
 */
public class LiveActivity extends Activity {

    private static final String TAG = "LiveActivity";

    // 分组列表
    private RecyclerView groupRecyclerView;
    private GroupAdapter groupAdapter;

    // 频道列表
    private RecyclerView channelRecyclerView;
    private ChannelAdapter channelAdapter;

    private ProgressBar loadingIndicator;
    private TextView emptyView;

    private List<LiveSource.Group> groups = new ArrayList<>();
    private List<LiveSource.Channel> channels = new ArrayList<>();
    private Map<String, List<LiveSource.Channel>> groupChannels = new HashMap<>();
    private int selectedGroupIndex = 0;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        loadLiveSources();
    }

    private void initViews() {
        // 直播使用两个列表：分组和频道
        // 这里简化为一个 RecyclerView
        groupRecyclerView = findViewById(R.id.browse_grid);
        groupRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        groupRecyclerView.setHasFixedSize(true);
    }

    /**
     * 加载直播源
     */
    private void loadLiveSources() {
        FilmStoreApp.getInstance().getApiClient().getLiveSources(new ApiClient.ApiCallback<List<LiveSource>>() {
            @Override
            public void onSuccess(List<LiveSource> result) {
                mainHandler.post(() -> parseLiveSources(result));
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(LiveActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 解析直播源
     */
    private void parseLiveSources(List<LiveSource> sources) {
        if (sources == null || sources.isEmpty()) {
            showEmpty(R.string.live_no_source);
            return;
        }

        groups.clear();
        groupChannels.clear();

        for (LiveSource source : sources) {
            if (!source.isActive()) continue;

            if (source.getGroupName() != null) {
                // 通过代理接口获取的频道以 source 分组
                continue;
            }
        }

        // groups 从 source 代理获取，此处暂时跳过
        if (false) {
            showEmpty(R.string.live_no_source);
            return;
        }

        // 默认选中第一个分组
        selectGroup(0);
    }

    /**
     * 选中分组
     */
    private void selectGroup(int index) {
        if (index < 0 || index >= groups.size()) return;

        selectedGroupIndex = index;
        LiveSource.Group group = groups.get(index);
        channels.clear();

        List<LiveSource.Channel> channelList = groupChannels.get(group.getGroupName());
        if (channelList != null) {
            channels.addAll(channelList);
        }

        // 更新频道显示
        updateChannelDisplay();
    }

    /**
     * 更新频道显示
     */
    private void updateChannelDisplay() {
        // 使用频道列表更新界面
        // 简化实现 - 显示第一个频道并播放
        if (!channels.isEmpty()) {
            playChannel(new com.filmstore.tv.model.LiveChannel());
        } else {
            showEmpty(R.string.live_no_source);
        }
    }

    /**
     * 播放频道
     */
    private void playChannel(LiveChannel channel) {
        if (channel == null || channel.getUrl() == null) return;

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("play_url", channel.getUrl());
        intent.putExtra("play_name", channel.getName());
        intent.putExtra("is_live", true);
        startActivity(intent);
    }

    private void showEmpty(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
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
