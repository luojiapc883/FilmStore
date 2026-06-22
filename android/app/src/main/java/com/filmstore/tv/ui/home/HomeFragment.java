package com.filmstore.tv.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.VerticalGridView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.filmstore.tv.FilmStoreApp;
import com.filmstore.tv.R;
import com.filmstore.tv.api.ApiClient;
import com.filmstore.tv.model.Announcement;
import com.filmstore.tv.model.UpdateInfo;
import com.filmstore.tv.model.ClientConfig;
import com.filmstore.tv.model.VodItem;
import com.filmstore.tv.model.VodSource;
import com.filmstore.tv.ui.live.LiveActivity;
import com.filmstore.tv.ui.settings.SettingsActivity;
import com.filmstore.tv.ui.vod.VodCategoryActivity;
import com.filmstore.tv.ui.vod.VodDetailActivity;
import com.filmstore.tv.ui.vod.VodSearchActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页 Fragment - Leanback Browse 风格
 */
public class HomeFragment extends Fragment implements OnItemViewClickedListener {

    private static final String TAG = "HomeFragment";

    // 行索引
    private static final int ROW_NAV = 0;
    private static final int ROW_HOT = 1;
    private static final int ROW_LATEST = 2;
    private static final int ROW_CATEGORIES = 3;

    private VerticalGridView verticalGridView;
    private ArrayObjectAdapter rowsAdapter;
    private Handler mainHandler;

    // 缓存配置
    private ClientConfig cachedConfig;
    private List<VodSource> vodSources;
    private List<VodItem> hotVodItems;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_browse, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mainHandler = new Handler(Looper.getMainLooper());

        // 初始化 Leanback Browse
        initBrowse(view);
        loadData();
    }

    /**
     * 初始化浏览界面
     */
    private void initBrowse(View view) {
        verticalGridView = view.findViewById(R.id.browse_grid);
        rowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        // 设置卡片点击事件
        // 由于我们使用自定义的 VerticalGridView 而非 BrowseFragment，需要自己管理行
        loadSampleData();
    }

    /**
     * 加载样本数据（在实际使用时会被服务器数据替换）
     */
    private void loadSampleData() {
        // 导航行
        rowsAdapter.add(createNavRow());

        // 热门推荐行
        rowsAdapter.add(createSectionRow(
                getString(R.string.vod_hot),
                getString(R.string.title_vod),
                R.drawable.ic_app_icon));

        // 最新更新行
        rowsAdapter.add(createSectionRow(
                getString(R.string.vod_latest),
                getString(R.string.title_vod),
                R.drawable.ic_app_icon));

        if (verticalGridView != null) {
            // rowsAdapter 用于 ListRow 样式
            rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
            rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
        }
    }

    /**
     * 创建导航行
     */
    private ListRow createNavRow() {
        ArrayObjectAdapter listAdapter = new ArrayObjectAdapter(new NavItemPresenter());
        listAdapter.add(new NavItem(getString(R.string.title_home), R.drawable.ic_home, "home"));
        listAdapter.add(new NavItem(getString(R.string.title_vod), R.drawable.ic_search, "vod"));
        listAdapter.add(new NavItem(getString(R.string.title_live), R.drawable.ic_live, "live"));
        listAdapter.add(new NavItem(getString(R.string.title_settings), R.drawable.ic_settings, "settings"));

        HeaderItem header = new HeaderItem(ROW_NAV, getString(R.string.app_name));
        return new ListRow(header, listAdapter);
    }

    /**
     * 创建分类行
     */
    private ListRow createSectionRow(String title, String subtitle, int iconRes) {
        ArrayObjectAdapter listAdapter = new ArrayObjectAdapter(new SectionItemPresenter());
        listAdapter.add(new SectionItem(title, subtitle, iconRes));

        HeaderItem header = new HeaderItem(0, title);
        return new ListRow(header, listAdapter);
    }

    /**
     * 从服务器加载数据
     */
    private void loadData() {
        FilmStoreApp.getInstance().getApiClient().getConfig(new ApiClient.ApiCallback<ClientConfig>() {
            @Override
            public void onSuccess(ClientConfig result) {
                cachedConfig = result;
                vodSources = result.getVodSources();

                // 更新公告横幅
                updateAnnouncements(result.getAnnouncements());

                // 更新主题
                if (result.getThemes() != null) {
                    applyTheme(result.getThemes());
                }

                // 加载热门推荐
                loadHotVod();
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    if (getActivity() != null) {
                        Toast.makeText(getActivity(), R.string.error_network, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    /**
     * 加载热门点播
     */
    private void loadHotVod() {
        FilmStoreApp.getInstance().getApiClient().getHotVod(1, new ApiClient.ApiCallback<List<VodItem>>() {
            @Override
            public void onSuccess(List<VodItem> result) {
                hotVodItems = result;
                mainHandler.post(() -> updateBrowseRows());
            }

            @Override
            public void onError(Exception e) {
                // 静默
            }
        });
    }

    /**
     * 更新公告横幅
     */
    private void updateAnnouncements(List<Announcement> announcements) {
        if (getView() == null || announcements == null || announcements.isEmpty()) return;

        CardView bannerView = getView().findViewById(R.id.announcement_banner);
        TextView textView = getView().findViewById(R.id.announcement_text);

        if (bannerView != null && textView != null) {
            // 找到有效的跑马灯公告或第一条公告
            for (Announcement a : announcements) {
                if (a.isValid() && (a.isMarquee() || a.isBanner())) {
                    textView.setText(a.getTitle() + ": " + a.getContent());
                    bannerView.setVisibility(View.VISIBLE);
                    textView.setSelected(true); // 启动跑马灯
                    break;
                }
            }
        }
    }

    /**
     * 应用主题
     */
    private void applyTheme(ClientConfig.ThemeConfig themeConfig) {
        String selectedTheme = getActivity() != null ?
                getActivity().getSharedPreferences("filmstore_config", 0)
                        .getString("selected_theme", "default") : "default";
        FilmStoreApp.getInstance().getThemeManager()
                .applyThemeByName(selectedTheme, themeConfig);
    }

    /**
     * 更新浏览行
     */
    private void updateBrowseRows() {
        if (verticalGridView == null) return;

        rowsAdapter.clear();

        // 导航行
        rowsAdapter.add(createNavRow());

        // 热门推荐
        if (hotVodItems != null && !hotVodItems.isEmpty()) {
            ArrayObjectAdapter hotAdapter = new ArrayObjectAdapter(new VodCardPresenter());
            for (VodItem item : hotVodItems) {
                hotAdapter.add(item);
            }
            HeaderItem hotHeader = new HeaderItem(ROW_HOT, getString(R.string.vod_hot));
            rowsAdapter.add(new ListRow(hotHeader, hotAdapter));
        }

        // 分类行
        if (vodSources != null) {
            for (VodSource source : vodSources) {
                if (!source.isActive()) continue;

                ArrayObjectAdapter catAdapter = new ArrayObjectAdapter(new VodCardPresenter());

                // 添加"查看分类"项
                VodItem categoryItem = new VodItem();
                categoryItem.setVodId("category_" + source.getId());
                categoryItem.setVodName(source.getName());
                catAdapter.add(categoryItem);

                HeaderItem sourceHeader = new HeaderItem(ROW_CATEGORIES, source.getName());
                rowsAdapter.add(new ListRow(sourceHeader, catAdapter));
            }
        }

        // rowsAdapter 用于 ListRow 样式
            rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
            rowsAdapter.notifyArrayItemRangeChanged(0, rowsAdapter.size());
    }

    @Override
    public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                              RowPresenter.ViewHolder rowViewHolder, Row row) {
        if (item instanceof NavItem) {
            handleNavClick((NavItem) item);
        } else if (item instanceof SectionItem) {
            handleSectionClick((SectionItem) item);
        } else if (item instanceof VodItem) {
            handleVodClick((VodItem) item);
        }
    }

    private void handleNavClick(NavItem nav) {
        if (getActivity() == null) return;
        Intent intent;
        switch (nav.getAction()) {
            case "vod":
                intent = new Intent(getActivity(), VodSearchActivity.class);
                startActivity(intent);
                break;
            case "live":
                intent = new Intent(getActivity(), LiveActivity.class);
                startActivity(intent);
                break;
            case "settings":
                intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(intent);
                break;
            default:
                // 首页，不做操作
                break;
        }
    }

    private void handleSectionClick(SectionItem section) {
        if (getActivity() == null) return;
        if (section.getTitle().equals(getString(R.string.vod_hot))) {
            Intent intent = new Intent(getActivity(), VodSearchActivity.class);
            startActivity(intent);
        }
    }

    private void handleVodClick(VodItem item) {
        if (getActivity() == null) return;
        Intent intent = new Intent(getActivity(), VodDetailActivity.class);
        intent.putExtra("vod_id", item.getVodId());
        intent.putExtra("vod_name", item.getVodName());
        intent.putExtra("vod_pic", item.getVodPic());
        intent.putExtra("vod_remarks", item.getVodRemarks());
        intent.putExtra("vod_score", item.getVodScore());
        startActivity(intent);
    }

    // ==================== 内部模型类 ====================

    /**
     * 导航项目
     */
    public static class NavItem {
        private String title;
        private int iconRes;
        private String action;

        public NavItem(String title, int iconRes, String action) {
            this.title = title;
            this.iconRes = iconRes;
            this.action = action;
        }

        public String getTitle() { return title; }
        public int getIconRes() { return iconRes; }
        public String getAction() { return action; }
    }

    /**
     * 分区项目
     */
    public static class SectionItem {
        private String title;
        private String subtitle;
        private int iconRes;

        public SectionItem(String title, String subtitle, int iconRes) {
            this.title = title;
            this.subtitle = subtitle;
            this.iconRes = iconRes;
        }

        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public int getIconRes() { return iconRes; }
    }

    // ==================== View Presenters ====================

    /**
     * 导航项目 Presenter
     */
    public static class NavItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vod_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            if (item instanceof NavItem) {
                NavItem nav = (NavItem) item;
                TextView titleView = viewHolder.view.findViewById(R.id.title);
                if (titleView != null) {
                    titleView.setText(nav.getTitle());
                }
            }
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {}
    }

    /**
     * 分区项目 Presenter
     */
    public static class SectionItemPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vod_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            if (item instanceof SectionItem) {
                SectionItem section = (SectionItem) item;
                TextView titleView = viewHolder.view.findViewById(R.id.title);
                if (titleView != null) {
                    titleView.setText(section.getTitle());
                }
            }
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {}
    }

    /**
     * 点播卡片 Presenter
     */
    public static class VodCardPresenter extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_vod_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            if (item instanceof VodItem) {
                VodItem vod = (VodItem) item;
                TextView titleView = viewHolder.view.findViewById(R.id.title);
                if (titleView != null) {
                    titleView.setText(vod.getVodName());
                }
            }
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {}
    }
}
