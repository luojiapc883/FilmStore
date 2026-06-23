package com.filmstore.tv.ui.live;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
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
import java.util.List;

/**
 * 直播页面 - 两步：源列表 → 频道列表 → 播放
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
    private List<LiveSource.Channel> channels = new ArrayList<>();
    private boolean showingChannels = false; // false=源列表, true=频道列表

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
        listView.setOnItemClickListener((parent, view, position, id) -> onItemClick(position));
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
                    showingChannels = false;
                    titleView.setText(R.string.title_live);
                    adapter.clear();
                    for (LiveSource s : sources) {
                        String fmt = s.getType() != null ? "[" + s.getType() + "] " : "";
                        adapter.add(fmt + (s.getName() != null ? s.getName() : "未知源"));
                    }
                    adapter.notifyDataSetChanged();
                    listView.setVisibility(View.VISIBLE);
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

    private void onItemClick(int position) {
        if (showingChannels) {
            // 频道列表 → 播放
            if (position >= 0 && position < channels.size()) {
                LiveSource.Channel ch = channels.get(position);
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
        } else {
            // 源列表 → 加载频道
            if (position >= 0 && position < sources.size()) {
                LiveSource source = sources.get(position);
                loadChannels(source);
            }
        }
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
                    channels = result;
                    showingChannels = true;
                    titleView.setText(source.getName() != null ? source.getName() : "频道列表");
                    adapter.clear();
                    for (LiveSource.Channel ch : channels) {
                        adapter.add(ch.getName() != null ? ch.getName() : "未知频道");
                    }
                    adapter.notifyDataSetChanged();
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (showingChannels) {
                // 返回源列表
                showingChannels = false;
                titleView.setText(R.string.title_live);
                adapter.clear();
                for (LiveSource s : sources) {
                    String fmt = s.getType() != null ? "[" + s.getType() + "] " : "";
                    adapter.add(fmt + (s.getName() != null ? s.getName() : "未知源"));
                }
                adapter.notifyDataSetChanged();
                return true;
            }
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
