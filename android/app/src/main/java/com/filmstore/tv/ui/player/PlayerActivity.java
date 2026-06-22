package com.filmstore.tv.ui.player;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.media3.common.Player;
import androidx.media3.ui.PlayerView;

import com.filmstore.tv.R;
import com.filmstore.tv.player.ExoPlayerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * 播放器活动 - 使用 ExoPlayer
 * 支持点播和直播播放
 */
public class PlayerActivity extends Activity {

    private static final String TAG = "PlayerActivity";

    private PlayerView playerView;
    private ExoPlayerHelper playerHelper;
    private ProgressBar loadingIndicator;
    private View errorLayout;
    private TextView errorText;
    private Button retryButton;
    private Button sourceSelector;

    private String playUrl;
    private String playName;
    private String vodName;
    private boolean isLive;

    // 多播放源
    private List<String> urlList = new ArrayList<>();
    private int currentSourceIndex = 0;

    private Handler mainHandler;

    // 控制栏超时隐藏
    private static final long CONTROLS_TIMEOUT_MS = 5000;
    private Runnable hideControlsRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        setContentView(R.layout.activity_player);
        mainHandler = new Handler(Looper.getMainLooper());
        hideControlsRunnable = this::hideControls;

        // 获取传入参数
        Intent intent = getIntent();
        playUrl = intent.getStringExtra("play_url");
        playName = intent.getStringExtra("play_name");
        vodName = intent.getStringExtra("vod_name");
        isLive = intent.getBooleanExtra("is_live", false);

        if (playUrl == null) playUrl = "";

        initViews();
        initPlayer();
    }

    private void initViews() {
        playerView = findViewById(R.id.player_view);
        loadingIndicator = findViewById(R.id.loading_indicator);
        errorLayout = findViewById(R.id.error_layout);
        errorText = findViewById(R.id.error_text);
        retryButton = findViewById(R.id.retry_button);
        sourceSelector = findViewById(R.id.source_selector);

        if (retryButton != null) {
            retryButton.setOnClickListener(v -> retryPlay());
        }

        if (sourceSelector != null) {
            sourceSelector.setOnClickListener(v -> switchSource());
        }

        showLoading(true);
    }

    private void initPlayer() {
        playerHelper = new ExoPlayerHelper(this);

        try {
            playerView.setPlayer(playerHelper.initPlayer(new ExoPlayerHelper.PlaybackStateListener() {
                @Override
                public void onPlayerReady() {
                    showLoading(false);
                    showError(false);
                }

                @Override
                public void onPlayerError(String message) {
                    showLoading(false);
                    showError(true);
                    if (errorText != null) {
                        errorText.setText(message);
                    }
                }

                @Override
                public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED && !isLive) {
                        // 点播结束
                        finish();
                    }
                }

                @Override
                public void onBuffering(boolean isBuffering) {
                    showLoading(isBuffering);
                }
            }));

            // 设置音量控制
            setVolumeControlStream(AudioManager.STREAM_MUSIC);

            // 开始播放
            playerHelper.playUrl(playUrl);

        } catch (Exception e) {
            showLoading(false);
            showError(true);
            if (errorText != null) {
                errorText.setText(getString(R.string.player_error) + ": " + e.getMessage());
            }
        }
    }

    /**
     * 切换播放源
     */
    private void switchSource() {
        if (urlList.size() <= 1) return;

        currentSourceIndex = (currentSourceIndex + 1) % urlList.size();
        String newUrl = urlList.get(currentSourceIndex);

        if (playerHelper != null) {
            playerHelper.stop();
            playerHelper.playUrl(newUrl);
            showLoading(true);
        }

        if (sourceSelector != null && currentSourceIndex < urlList.size()) {
            sourceSelector.setText("源 " + (currentSourceIndex + 1) + "/" + urlList.size());
        }
    }

    /**
     * 重试播放
     */
    private void retryPlay() {
        showError(false);
        showLoading(true);

        if (playerHelper != null) {
            playerHelper.stop();
            playerHelper.playUrl(playUrl);
        } else {
            initPlayer();
        }
    }

    /**
     * 设置多源列表
     */
    public void setPlayUrlList(List<String> urls) {
        this.urlList.clear();
        if (urls != null) {
            this.urlList.addAll(urls);
        }
        if (sourceSelector != null) {
            sourceSelector.setVisibility(urlList.size() > 1 ? View.VISIBLE : View.GONE);
        }
    }

    private void showLoading(boolean show) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(boolean show) {
        if (errorLayout != null) {
            errorLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void hideControls() {
        if (playerView != null) {
            playerView.hideController();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (playerHelper != null) {
                    if (playerHelper.isPlaying()) {
                        playerHelper.pause();
                    } else {
                        playerHelper.resume();
                    }
                }
                return true;

            case KeyEvent.KEYCODE_MEDIA_STOP:
                finish();
                return true;

            case KeyEvent.KEYCODE_DPAD_UP:
                // 音量+
                adjustVolume(true);
                return true;

            case KeyEvent.KEYCODE_DPAD_DOWN:
                // 音量-
                adjustVolume(false);
                return true;

            case KeyEvent.KEYCODE_DPAD_LEFT:
                // 快退 10s
                if (playerHelper != null) {
                    long pos = Math.max(0, playerHelper.getCurrentPosition() - 10000);
                    playerHelper.seekTo(pos);
                }
                return true;

            case KeyEvent.KEYCODE_DPAD_RIGHT:
                // 快进 10s
                if (playerHelper != null) {
                    long pos = Math.min(playerHelper.getDuration(), playerHelper.getCurrentPosition() + 10000);
                    playerHelper.seekTo(pos);
                }
                return true;

            case KeyEvent.KEYCODE_MEDIA_REWIND:
                if (playerHelper != null) {
                    long pos = Math.max(0, playerHelper.getCurrentPosition() - 30000);
                    playerHelper.seekTo(pos);
                }
                return true;

            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                if (playerHelper != null) {
                    long pos = Math.min(playerHelper.getDuration(), playerHelper.getCurrentPosition() + 30000);
                    playerHelper.seekTo(pos);
                }
                return true;

            case KeyEvent.KEYCODE_INFO:
                // 显示当前源信息
                Toast.makeText(this, playName != null ? playName : "直播", Toast.LENGTH_SHORT).show();
                return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * 调整音量
     */
    private void adjustVolume(boolean up) {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        if (audioManager != null) {
            int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int newVolume = up ? Math.min(currentVolume + 1, maxVolume) : Math.max(currentVolume - 1, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, AudioManager.FLAG_SHOW_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (playerHelper != null) {
            playerHelper.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (playerHelper != null) {
            playerHelper.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (playerHelper != null) {
            playerHelper.release();
            playerHelper = null;
        }
        if (playerView != null) {
            playerView.setPlayer(null);
        }
        mainHandler.removeCallbacks(hideControlsRunnable);
        super.onDestroy();
    }
}
