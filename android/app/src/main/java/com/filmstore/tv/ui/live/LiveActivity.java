package com.filmstore.tv.ui.live;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
 * 直播页面
 * - 有m3u源时：直接展开频道列表（按group分组）
 * - 按OK键：弹出分组选择菜单
 * - 点击频道：播放
 * - Back键：回到源列表
 */
public class LiveActivity extends Activity {

    private LinearLayout rootLayout;
    private ListView listView;
    private ProgressBar loadingIndicator;
    private TextView emptyView;
    private TextView titleView;

    private ArrayAdapter<String> adapter;
    private Handler mainHandler;

    // 状态
    private List<LiveSource> sources = new ArrayList<>();
    private List<LiveSource.Channel> allChannels = new ArrayList<>();
    private Map<String, List<LiveSource.Channel>> groupedChannels = new LinkedHashMap<>();
    private List<String> groupNames = new ArrayList<>();
    private String currentGroup = null; // null = 显示全部
    private boolean showingChannels = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(getResources().getColor(R.color.bgPrimary));
        setContentView(rootLayout);

        titleView = new TextView(this);
        titleView.setText(R.string.title_live);
        titleView.setTextSize(28);
        titleView.setTextColor(getResources().getColor(R.color.textPrimary, getTheme()));
        titleView.setPadding(32, 32, 32, 16);
        rootLayout.addView(titleView);

        loadingIndicator = new ProgressBar(this);
        LinearLayout.LayoutParams loaderLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        loaderLp.gravity = Gravity.CENTER_HORIZONTAL;
        loadingIndicator.setLayoutParams(loaderLp);
        loadingIndicator.setPadding(0, 32, 0, 32);
        rootLayout.addView(loadingIndicator);

        emptyView = new TextView(this);
        emptyView.setText(R.string.live_no_source);
        emptyView.setTextSize(18);
        emptyView.setTextColor(getResources().getColor(R.color.textSecondary, getTheme()));
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setPadding(32, 64, 32, 32);
        emptyView.setVisibility(View.GONE);
        rootLayout.addView(emptyView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setVisibility(View.GONE);
        listView.setOnItemClickListener((parent, view, position, id) -> onChannelClick(position));
        rootLayout.addView(listView);

        loadSources();
    }

    private void loadSources() {
        loadingIndicator.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        listView.setVisibility(View.GONE);

        FilmStoreApp.getInstance().getApiClient().getLiveSources(new ApiClient.ApiCallback<List<LiveSource>>() {
            @Override
            public void onSuccess(List<LiveSource> result) {
                mainHandler.post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    if (result == null || result.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }
                    sources = result;
                    // 只取第一个源（NAS178直播 m3u），自动加载频道
                    LiveSource firstSource = sources.get(0);
                    loadChannels(firstSource);
                });
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

    private void loadChannels(LiveSource source) {
        loadingIndicator.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);

        FilmStoreApp.getInstance().getApiClient().getLiveChannels(source.getId(),
                new ApiClient.ApiCallback<List<LiveSource.Channel>>() {
            @Override
            public void onSuccess(List<LiveSource.Channel> result) {
                mainHandler.post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    if (result == null || result.isEmpty()) {
                        emptyView.setText("该源暂无频道数据");
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }
                    allChannels = result;
                    groupChannels();
                    showingChannels = true;
                    currentGroup = null; // 默认显示全部
                    titleView.setText(source.getName() != null ? source.getName() : "频道列表");
                    applyGroupFilter();
                    listView.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    Toast.makeText(LiveActivity.this,
                            "加载频道失败: " + (e != null ? e.getMessage() : "未知错误"),
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * 按 group 分组
     */
    private void groupChannels() {
        groupedChannels.clear();
        groupNames.clear();
        for (LiveSource.Channel ch : allChannels) {
            String key = ch.getGroup() != null && !ch.getGroup().isEmpty() ? ch.getGroup() : "未分组";
            List<LiveSource.Channel> list = groupedChannels.get(key);
            if (list == null) {
                list = new ArrayList<>();
                groupedChannels.put(key, list);
                groupNames.add(key);
            }
            list.add(ch);
        }
    }

    /**
     * 应用分组过滤到列表
     */
    private void applyGroupFilter() {
        adapter.clear();
        if (currentGroup == null) {
            // 显示全部
            for (LiveSource.Channel ch : allChannels) {
                adapter.add(ch.getName() != null ? ch.getName() : "未知频道");
            }
        } else {
            List<LiveSource.Channel> groupList = groupedChannels.get(currentGroup);
            if (groupList != null) {
                for (LiveSource.Channel ch : groupList) {
                    adapter.add(ch.getName() != null ? ch.getName() : "未知频道");
                }
            }
        }
        adapter.notifyDataSetChanged();
        String title = "NAS178直播";
        if (currentGroup != null) {
            title += " · " + currentGroup;
        }
        titleView.setText(title);
    }

    private void onChannelClick(int position) {
        LiveSource.Channel ch = getChannelAtPosition(position);
        if (ch == null) return;
        if (ch.getUrl() != null && !ch.getUrl().isEmpty()) {
            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putExtra("play_url", ch.getUrl());
            intent.putExtra("play_name", ch.getName() != null ? ch.getName() : "直播");
            intent.putExtra("is_live", true);
            startActivity(intent);
        } else {
            Toast.makeText(this, "频道地址无效", Toast.LENGTH_SHORT).show();
        }
    }

    private LiveSource.Channel getChannelAtPosition(int position) {
        if (currentGroup == null) {
            if (position >= 0 && position < allChannels.size()) {
                return allChannels.get(position);
            }
        } else {
            List<LiveSource.Channel> groupList = groupedChannels.get(currentGroup);
            if (groupList != null && position >= 0 && position < groupList.size()) {
                return groupList.get(position);
            }
        }
        return null;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (showingChannels) {
            // 遥控器 OK/DPAD_CENTER：弹出分组菜单
            if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || 
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_BUTTON_A) {
                showGroupDialog();
                return true;
            }
        }

        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (showingChannels) {
                showingChannels = false;
                titleView.setText(R.string.title_live);
                adapter.clear();
                for (LiveSource s : sources) {
                    String fmt = s.getType() != null ? "[" + s.getType() + "] " : "";
                    adapter.add(fmt + (s.getName() != null ? s.getName() : "未知源"));
                }
                adapter.notifyDataSetChanged();
                listView.setVisibility(View.VISIBLE);
                return true;
            }
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * 弹出分组选择菜单
     */
    private void showGroupDialog() {
        String[] items = new String[groupNames.size() + 1];
        items[0] = "全部频道 (" + allChannels.size() + ")";
        for (int i = 0; i < groupNames.size(); i++) {
            List<LiveSource.Channel> g = groupedChannels.get(groupNames.get(i));
            items[i + 1] = groupNames.get(i) + " (" + (g != null ? g.size() : 0) + ")";
        }

        // 找当前选中项
        int checkedItem = 0;
        if (currentGroup != null) {
            int idx = groupNames.indexOf(currentGroup);
            if (idx >= 0) checkedItem = idx + 1;
        }

        new AlertDialog.Builder(this)
                .setTitle("选择分组")
                .setSingleChoiceItems(items, checkedItem, (dialog, which) -> {
                    if (which == 0) {
                        currentGroup = null;
                    } else {
                        currentGroup = groupNames.get(which - 1);
                    }
                    applyGroupFilter();
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
