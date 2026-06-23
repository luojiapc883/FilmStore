package com.filmstore.tv.ui.live;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.model.LiveSource;
import com.filmstore.tv.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 直播页面 - 分组 + 频道列表，点击进入播放器
 */
public class LiveActivity extends Activity {

    private LinearLayout rootLayout;
    private ListView groupListView;
    private ListView channelListView;
    private ProgressBar loadingIndicator;
    private TextView emptyView;

    private ArrayAdapter<String> groupAdapter;
    private ArrayAdapter<String> channelAdapter;
    private List<String> groupNames = new ArrayList<>();
    private List<LiveSource.Channel> currentChannels = new ArrayList<>();

    private Map<String, List<LiveSource.Channel>> groupChannels = new LinkedHashMap<>();
    private int selectedGroupIndex = -1;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainHandler = new Handler(Looper.getMainLooper());

        // 纯代码创建布局
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(getResources().getColor(R.color.bgPrimary));
        setContentView(rootLayout);

        // 标题
        TextView titleView = new TextView(this);
        titleView.setText(R.string.title_live);
        titleView.setTextSize(28);
        titleView.setTextColor(getResources().getColor(R.color.textPrimary, getTheme()));
        titleView.setPadding(32, 32, 32, 16);
        rootLayout.addView(titleView);

        // 加载指示器
        loadingIndicator = new ProgressBar(this);
        loadingIndicator.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) loadingIndicator.getLayoutParams()).gravity = Gravity.CENTER_HORIZONTAL;
        loadingIndicator.setPadding(0, 32, 0, 32);
        rootLayout.addView(loadingIndicator);

        // 空视图
        emptyView = new TextView(this);
        emptyView.setText(R.string.live_no_source);
        emptyView.setTextSize(18);
        emptyView.setTextColor(getResources().getColor(R.color.textSecondary, getTheme()));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(32, 64, 32, 32);
        emptyView.setVisibility(View.GONE);
        rootLayout.addView(emptyView);

        // 分组列表
        groupAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, groupNames);
        groupListView = new ListView(this);
        groupListView.setAdapter(groupAdapter);
        groupListView.setVisibility(View.GONE);
        groupListView.setOnItemClickListener((parent, view, position, id) -> {
            selectGroup(position);
        });
        LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.35f);
        groupListView.setLayoutParams(glp);

        // 频道列表
        channelAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        channelListView = new ListView(this);
        channelListView.setAdapter(channelAdapter);
        channelListView.setVisibility(View.GONE);
        channelListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < currentChannels.size()) {
                playChannel(currentChannels.get(position));
            }
        });
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 0.65f);
        channelListView.setLayoutParams(clp);

        // 双列表并排
        LinearLayout listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.HORIZONTAL);
        listContainer.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        listContainer.addView(groupListView);
        listContainer.addView(channelListView);
        rootLayout.addView(listContainer);

        loadLiveSources();
    }

    /**
     * 加载直播源列表
     */
    private void loadLiveSources() {
        loadingIndicator.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        FilmStoreApp.getInstance().getApiClient().getLiveSources(new ApiClient.ApiCallback<List<LiveSource>>() {
            @Override
            public void onSuccess(List<LiveSource> result) {
                mainHandler.post(() -> parseLiveSources(result));
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    emptyView.setText(R.string.error_network);
                    emptyView.setVisibility(View.VISIBLE);
                });
            }
        });
    }

    /**
     * 解析直播源：从 LiveSource 列表构建分组-频道映射
     */
    private void parseLiveSources(List<LiveSource> sources) {
        loadingIndicator.setVisibility(View.GONE);
        groupChannels.clear();
        groupNames.clear();

        if (sources == null || sources.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        for (LiveSource source : sources) {
            if (!source.isActive()) continue;

            List<LiveSource.Channel> channels = source.getChannels();
            if (channels == null || channels.isEmpty()) continue;

            String groupName = source.getGroupName() != null && !source.getGroupName().isEmpty()
                    ? source.getGroupName() : (source.getName() != null ? source.getName() : "默认分组");

            List<LiveSource.Channel> existing = groupChannels.get(groupName);
            if (existing == null) {
                existing = new ArrayList<>();
                groupChannels.put(groupName, existing);
            }
            existing.addAll(channels);
        }

        if (groupChannels.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            return;
        }

        groupNames.addAll(groupChannels.keySet());
        groupAdapter.notifyDataSetChanged();
        groupListView.setVisibility(View.VISIBLE);

        // 默认选中第一个分组
        selectGroup(0);
    }

    /**
     * 选中分组，显示该分组下的频道
     */
    private void selectGroup(int index) {
        if (index < 0 || index >= groupNames.size()) return;

        selectedGroupIndex = index;
        String groupName = groupNames.get(index);

        List<LiveSource.Channel> channels = groupChannels.get(groupName);
        currentChannels.clear();

        List<String> channelNames = new ArrayList<>();
        if (channels != null) {
            currentChannels.addAll(channels);
            for (LiveSource.Channel ch : channels) {
                channelNames.add(ch.getName() != null ? ch.getName() : "未知频道");
            }
        }

        channelAdapter.clear();
        channelAdapter.addAll(channelNames);
        channelAdapter.notifyDataSetChanged();
        channelListView.setVisibility(View.VISIBLE);
    }

    /**
     * 播放频道
     */
    private void playChannel(LiveSource.Channel channel) {
        if (channel == null || channel.getUrl() == null || channel.getUrl().isEmpty()) {
            Toast.makeText(this, "频道地址无效", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("play_url", channel.getUrl());
        intent.putExtra("play_name", channel.getName() != null ? channel.getName() : "直播");
        intent.putExtra("is_live", true);
        startActivity(intent);
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
