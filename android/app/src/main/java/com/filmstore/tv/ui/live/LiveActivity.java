package com.filmstore.tv.ui.live;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.util.Log;
import android.widget.Toast;

import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.model.LiveSource;
import com.filmstore.tv.ui.player.PlayerActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播页面 - 展示直播源列表，点击播放
 */
public class LiveActivity extends Activity {

    private LinearLayout rootLayout;
    private ListView sourceListView;
    private ProgressBar loadingIndicator;
    private TextView emptyView;

    private ArrayAdapter<String> adapter;
    private List<LiveSource> sources = new ArrayList<>();
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainHandler = new Handler(Looper.getMainLooper());

        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(getResources().getColor(R.color.bgPrimary));
        setContentView(rootLayout);

        TextView titleView = new TextView(this);
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
        sourceListView = new ListView(this);
        sourceListView.setAdapter(adapter);
        sourceListView.setVisibility(View.GONE);
        sourceListView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < sources.size()) {
                onSourceClicked(sources.get(position));
            }
        });
        rootLayout.addView(sourceListView);

        loadLiveSources();
    }

    private void loadLiveSources() {
        loadingIndicator.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);

        FilmStoreApp.getInstance().getApiClient().getLiveSources(new ApiClient.ApiCallback<List<LiveSource>>() {
            @Override
            public void onSuccess(List<LiveSource> result) {
                mainHandler.post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    if (result == null || result.isEmpty()) {
                        emptyView.setVisibility(View.VISIBLE);
                        return;
                    }
                    sources.clear();
                    adapter.clear();
                    for (LiveSource s : result) {
                        String fmt = s.getType() != null ? "[" + s.getType() + "] " : "";
                        adapter.add(fmt + (s.getName() != null ? s.getName() : "未知源"));
                    }
                    adapter.notifyDataSetChanged();
                    sourceListView.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    loadingIndicator.setVisibility(View.GONE);
                    String errMsg = e != null ? e.getMessage() : "未知错误";
                    Log.e("LiveActivity", "加载直播源失败: " + errMsg, e);
                    emptyView.setText(R.string.error_network);
                    emptyView.setVisibility(View.VISIBLE);
                    Toast.makeText(LiveActivity.this, "错误: " + errMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void onSourceClicked(LiveSource source) {
        String url = source.getUrl();
        String name = source.getName();
        String type = source.getType();

        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "该源未配置地址", Toast.LENGTH_SHORT).show();
            return;
        }

        // m3u/txt 是列表文件，需要通过后端代理 /proxy/live/:id 解析频道列表
        // 但当前版本简化：提示用户通过管理后台添加流地址
        if ("m3u".equalsIgnoreCase(type) || "txt".equalsIgnoreCase(type)) {
            Toast.makeText(this, name + " 为频道列表格式，加载中...", Toast.LENGTH_SHORT).show();
            // TODO: 后续版本实现代理接口调用来获取频道列表
            return;
        }

        // 直接流地址：打开播放器
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("play_url", url);
        intent.putExtra("play_name", name != null ? name : "直播");
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
