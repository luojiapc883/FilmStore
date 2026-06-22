package com.filmstore.tv.player;

import android.content.Context;
import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.exoplayer.source.hls.HlsMediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

import com.filmstore.tv.FilmStoreApp;

import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * ExoPlayer 播放器辅助类
 */
@UnstableApi
public class ExoPlayerHelper {

    private ExoPlayer exoPlayer;
    private Context context;
    private PlaybackStateListener stateListener;

    /**
     * 播放状态监听
     */
    public interface PlaybackStateListener {
        void onPlayerReady();
        void onPlayerError(String message);
        void onPlaybackStateChanged(int state);
        void onBuffering(boolean isBuffering);
    }

    public ExoPlayerHelper(Context context) {
        this.context = context;
    }

    /**
     * 初始化播放器
     */
    public ExoPlayer initPlayer(PlaybackStateListener listener) {
        this.stateListener = listener;

        DefaultRenderersFactory renderersFactory = new DefaultRenderersFactory(context)
                .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER);

        DefaultTrackSelector trackSelector = new DefaultTrackSelector(context);
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        50000,       // min buffer
                        100000,      // max buffer
                        2500,        // buffer for playback start
                        5000         // buffer for rebuffer
                )
                .build();

        exoPlayer = new ExoPlayer.Builder(context)
                .setRenderersFactory(renderersFactory)
                .setTrackSelector(trackSelector)
                .setLoadControl(loadControl)
                .build();

        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (stateListener != null) {
                    stateListener.onPlaybackStateChanged(playbackState);
                    stateListener.onBuffering(playbackState == Player.STATE_BUFFERING);
                }

                if (playbackState == Player.STATE_READY && stateListener != null) {
                    stateListener.onPlayerReady();
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                if (stateListener != null) {
                    String msg = error.getLocalizedMessage();
                    if (msg == null) {
                        msg = "播放错误 (" + error.errorCodeName + ")";
                    }
                    stateListener.onPlayerError(msg);
                }
            }
        });

        return exoPlayer;
    }

    /**
     * 播放视频 URL - 自动判断流类型
     */
    public void playUrl(String url) {
        if (exoPlayer == null || url == null || url.isEmpty()) return;

        Uri uri = Uri.parse(url);
        MediaItem mediaItem = MediaItem.fromUri(uri);

        MediaSource mediaSource;
        if (url.contains(".m3u8") || url.contains(".m3u")) {
            // HLS 流
            DataSource.Factory dataSourceFactory = createDataSourceFactory();
            mediaSource = new HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        } else {
            // 常规视频文件（MP4, FLV, AVI 等）
            DataSource.Factory dataSourceFactory = createDataSourceFactory();
            mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem);
        }

        exoPlayer.setMediaSource(mediaSource);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);
    }

    /**
     * 创建数据源工厂
     */
    private DataSource.Factory createDataSourceFactory() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .build();

        OkHttpDataSource.Factory httpDataSourceFactory = new OkHttpDataSource.Factory(httpClient)
                .setUserAgent("FilmStore-AndroidTV/1.0")
                .setAllowCrossProtocolRedirects(true);

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(context, httpDataSourceFactory);
        return dataSourceFactory;
    }

    /**
     * 暂停
     */
    public void pause() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
        }
    }

    /**
     * 恢复
     */
    public void resume() {
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);
        }
    }

    /**
     * 停止
     */
    public void stop() {
        if (exoPlayer != null) {
            exoPlayer.stop();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    /**
     * 获取当前播放位置 (ms)
     */
    public long getCurrentPosition() {
        if (exoPlayer != null) {
            return exoPlayer.getCurrentPosition();
        }
        return 0;
    }

    /**
     * 获取视频总时长 (ms)
     */
    public long getDuration() {
        if (exoPlayer != null) {
            return exoPlayer.getDuration();
        }
        return 0;
    }

    /**
     * 跳转到指定位置
     */
    public void seekTo(long positionMs) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(positionMs);
        }
    }

    /**
     * 设置音量 (0.0 - 1.0)
     */
    public void setVolume(float volume) {
        if (exoPlayer != null) {
            exoPlayer.setVolume(volume);
        }
    }

    /**
     * 是否正在播放
     */
    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    /**
     * 获取 ExoPlayer 实例
     */
    public ExoPlayer getExoPlayer() {
        return exoPlayer;
    }
}
