package com.filmstore.tv.ui.announcement;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.filmstore.tv.R;
import com.filmstore.tv.model.Announcement;

/**
 * 公告详情页面
 */
public class AnnouncementDetailActivity extends Activity {

    private static final String TAG = "AnnouncementDetailActivity";

    private TextView titleView;
    private TextView contentView;
    private TextView timeView;
    private View closeButton;

    private Announcement announcement;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        loadAnnouncement();
    }

    private void initViews() {
        titleView = findViewById(R.id.announcement_title);
        contentView = findViewById(R.id.announcement_content);
        closeButton = findViewById(R.id.retry_button);
    }

    private void loadAnnouncement() {
        Intent intent = getIntent();
        String title = intent.getStringExtra("title");
        String content = intent.getStringExtra("content");
        String createdAt = intent.getStringExtra("created_at");

        if (titleView != null) {
            titleView.setText(title != null ? title : "");
        }
        if (contentView != null) {
            contentView.setText(content != null ? content : "");
        }
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> finish());
        }
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
