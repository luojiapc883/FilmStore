package com.filmstore.tv.ui.vod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.model.VodItem;
import com.filmstore.tv.model.VodSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 点播搜索页面
 */
public class VodSearchActivity extends Activity {

    private static final String TAG = "VodSearchActivity";

    private EditText searchInput;
    private RecyclerView recyclerView;
    private VodGridAdapter adapter;
    private ProgressBar loadingIndicator;
    private TextView emptyView;
    private View categoryGrid;

    private List<VodItem> searchResults = new ArrayList<>();
    private List<VodSource> vodSources = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private String currentKeyword = "";

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        initViews();
        loadSources();
    }

    private void initViews() {
        searchInput = findViewById(R.id.search_input);
        // 如果没有专门的搜索输入框，我们用 TextView 作为临时方案
        if (searchInput == null) {
            // 在布局中动态创建搜索输入
        }

        recyclerView = findViewById(R.id.browse_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        recyclerView.setHasFixedSize(true);

        adapter = new VodGridAdapter(this, searchResults);
        recyclerView.setAdapter(adapter);

        // 加载更多
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMore && !currentKeyword.isEmpty() &&
                            (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMore();
                    }
                }
            }
        });
    }

    /**
     * 加载点播源列表
     */
    private void loadSources() {
        FilmStoreApp.getInstance().getApiClient().getVodSources(new ApiClient.ApiCallback<List<VodSource>>() {
            @Override
            public void onSuccess(List<VodSource> result) {
                vodSources.clear();
                if (result != null) {
                    for (VodSource s : result) {
                        if (s.isActive()) {
                            vodSources.add(s);
                        }
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                // 忽略
            }
        });
    }

    /**
     * 执行搜索
     */
    private void performSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return;

        currentKeyword = keyword.trim();
        currentPage = 1;
        isLoading = true;

        searchResults.clear();
        adapter.notifyDataSetChanged();

        // 使用第一个搜索源
        if (vodSources.isEmpty()) {
            Toast.makeText(this, "没有可用的点播源", Toast.LENGTH_SHORT).show();
            isLoading = false;
            return;
        }

        String sourceId = vodSources.get(0).getId();

        FilmStoreApp.getInstance().getApiClient().searchVod(sourceId, currentKeyword, currentPage,
                new ApiClient.ApiCallback<List<VodItem>>() {
                    @Override
                    public void onSuccess(List<VodItem> result) {
                        isLoading = false;
                        mainHandler.post(() -> {
                            if (result != null && !result.isEmpty()) {
                                searchResults.addAll(result);
                                hasMore = result.size() >= 20;
                            } else {
                                hasMore = false;
                                Toast.makeText(VodSearchActivity.this, R.string.vod_no_results, Toast.LENGTH_SHORT).show();
                            }
                            adapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        isLoading = false;
                        mainHandler.post(() -> {
                            Toast.makeText(VodSearchActivity.this, "搜索失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    /**
     * 加载更多搜索结果
     */
    private void loadMore() {
        isLoading = true;
        currentPage++;

        if (vodSources.isEmpty()) {
            isLoading = false;
            return;
        }

        String sourceId = vodSources.get(0).getId();

        FilmStoreApp.getInstance().getApiClient().searchVod(sourceId, currentKeyword, currentPage,
                new ApiClient.ApiCallback<List<VodItem>>() {
                    @Override
                    public void onSuccess(List<VodItem> result) {
                        isLoading = false;
                        mainHandler.post(() -> {
                            if (result != null && !result.isEmpty()) {
                                searchResults.addAll(result);
                                hasMore = result.size() >= 20;
                            } else {
                                hasMore = false;
                            }
                            adapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        isLoading = false;
                    }
                });
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
