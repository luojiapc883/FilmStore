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
import androidx.fragment.app.Fragment;

import com.filmstore.tv.R;
import com.filmstore.tv.ui.live.LiveActivity;

/**
 * 直播 Fragment - 主页中的直播快捷入口
 */
public class LiveFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);
        layout.setBackgroundResource(R.color.bgPrimary);

        TextView titleView = new TextView(getContext());
        titleView.setText("📺 直播频道");
        titleView.setTextSize(24);
        titleView.setTextColor(getResources().getColor(R.color.textPrimary, getContext().getTheme()));
        titleView.setPadding(0, 0, 0, 24);
        layout.addView(titleView);

        Button btnOpenLive = new Button(getContext());
        btnOpenLive.setText("进入直播");
        btnOpenLive.setTextSize(18);
        btnOpenLive.setPadding(24, 16, 24, 16);
        btnOpenLive.setAllCaps(false);
        btnOpenLive.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), LiveActivity.class));
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        btnOpenLive.setLayoutParams(lp);
        layout.addView(btnOpenLive);

        return layout;
    }
}
