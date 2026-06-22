package com.filmstore.tv.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.filmstore.tv.R;
import com.filmstore.tv.ui.live.LiveActivity;
import com.filmstore.tv.ui.settings.SettingsActivity;
import com.filmstore.tv.ui.vod.VodSearchActivity;

/**
 * 首页 Fragment - 简单的按钮导航
 */
public class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundResource(R.color.bgPrimary);

        // 标题
        TextView titleView = new TextView(getContext());
        titleView.setText(R.string.app_name);
        titleView.setTextSize(28);
        titleView.setTextColor(getResources().getColor(R.color.textPrimary, getContext().getTheme()));
        titleView.setPadding(0, 0, 0, 32);
        layout.addView(titleView);

        // 导航按钮
        addNavButton(layout, "🔍 点播搜索", v -> {
            startActivity(new Intent(getActivity(), VodSearchActivity.class));
        });
        addNavButton(layout, "📺 直播频道", v -> {
            startActivity(new Intent(getActivity(), LiveActivity.class));
        });
        addNavButton(layout, "⚙️ 系统设置", v -> {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
        });

        return layout;
    }

    private void addNavButton(LinearLayout parent, String text, View.OnClickListener listener) {
        Button btn = new Button(getContext());
        btn.setText(text);
        btn.setTextSize(18);
        btn.setPadding(24, 24, 24, 24);
        btn.setAllCaps(false);
        btn.setOnClickListener(listener);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        btn.setLayoutParams(lp);

        parent.addView(btn);
    }
}
