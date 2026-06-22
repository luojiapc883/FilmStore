package com.filmstore.tv.ui.vod;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
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
 * 点播分类页面 - 显示某个源下的分类列表
 */
public class VodCategoryActivity extends Activity {

    private static final String TAG = "VodCategoryActivity";

    private RecyclerView recyclerView;
    private VodGridAdapter adapter;
    private TextView emptyView;

    private String sourceId;
    private String sourceName;
    private String categoryId;
    private String categoryName;

    private List<VodItem> vodItems = new ArrayList<>();
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;

    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainHandler = new Handler(Looper.getMainLooper());

        // 获取传入参数
        Intent intent = getIntent();
        sourceId = intent.getStringExtra("source_id");
        sourceName = intent.getStringExtra("source_name");
        categoryId = intent.getStringExtra("category_id");
        categoryName = intent.getStringExtra("category_name");

        if (sourceId == null) sourceId = "";
        if (sourceName == null) sourceName = getString(R.string.vod_all_categories);

        initViews();
        loadData();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.browse_grid);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 5));
        recyclerView.setHasFixedSize(true);

        adapter = new VodGridAdapter(this, vodItems);
        recyclerView.setAdapter(adapter);

        // 设置加载更多监听
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if (!isLoading && hasMore && (visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 5) {
                        loadMore();
                    }
                }
            }
        });
    }

    private void loadData() {
        isLoading = true;
        currentPage = 1;

        FilmStoreApp.getInstance().getApiClient().getCategoryVod(sourceId, categoryId, currentPage,
                new ApiClient.ApiCallback<List<VodItem>>() {
                    @Override
                    public void onSuccess(List<VodItem> result) {
                        isLoading = false;
                        mainHandler.post(() -> {
                            vodItems.clear();
                            if (result != null && !result.isEmpty()) {
                                vodItems.addAll(result);
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
                        mainHandler.post(() -> {
                            Toast.makeText(VodCategoryActivity.this, R.string.error_network, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void loadMore() {
        isLoading = true;
        currentPage++;

        FilmStoreApp.getInstance().getApiClient().getCategoryVod(sourceId, categoryId, currentPage,
                new ApiClient.ApiCallback<List<VodItem>>() {
                    @Override
                    public void onSuccess(List<VodItem> result) {
                        isLoading = false;
                        mainHandler.post(() -> {
                            if (result != null && !result.isEmpty()) {
                                vodItems.addAll(result);
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
